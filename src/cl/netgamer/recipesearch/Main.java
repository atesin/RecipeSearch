package cl.netgamer.recipesearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Main extends JavaPlugin implements Listener
{
	
	private FileConfiguration conf;
	private RecipeBook reciper;
	private Dictionary dictionary;
	private InventoryKeeper inventorer;
	private Sight sight;
	private Map<String, String[]> lastCmd = new HashMap<String, String[]>();
	private Map<String, ItemStack> nearItem = new HashMap<String, ItemStack>();
	
	
	public void onEnable()
	{
		saveDefaultConfig();
		conf = getConfig();
		reciper = new RecipeBook(this);
		dictionary = new Dictionary(this);
		inventorer = new InventoryKeeper(this);
		sight = new Sight(this);
		getServer().getPluginManager().registerEvents(this, this);
		getLogger().info("Plugin ready to work.");
	}
	
	
	public void onDisable()
	{
		for (Player player : getServer().getOnlinePlayers())
			if ((inventorer.unsetViewing(player)) == true)
				// is a little ugly not to close player inventory but at least it keeps integrity
				// this should be disappear later because is redundant
				player.updateInventory();
	}
	
	
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent e)
	{
		Player player = (Player) e.getPlayer();
		if (inventorer.unsetViewing(player))
		{
			lastCmd.remove(player.getUniqueId().toString());
			nearItem.remove(player.getUniqueId().toString());
		}
	}
	
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e)
	{
		// not using the plugin at this moment
		if (!inventorer.isViewing((Player)e.getWhoClicked()))
			return;
		
		// plugin in use, but get some shortcuts before proceed
		e.setCancelled(true);
		Player player = (Player)e.getWhoClicked();
		SlotType slot = e.getSlotType();
		ClickType click = e.getClick();
		
		// special functions, early because "outside" slot has no item to click (replay command, close)
		if (slot == SlotType.OUTSIDE)
		{
			if (click == ClickType.LEFT)
				runLastCommand(player, lastCmd.get(player.getUniqueId().toString()));
			else
				// close inventory here should be scheduled, see InventoryClickEvent javadoc
				new BukkitRunnable()
				{
					@Override
					public void run()
					{
						player.closeInventory();
					}
				}.runTask(this);
			return;
		}
		
		// clicked an empty slot: does nothing
		ItemStack item = e.getCurrentItem();
		if (item == null || item.getType() == Material.AIR)
			return;
		
		// browse result pages with any click type
		if (slot == SlotType.QUICKBAR)
		{
			inventorer.listPage(player, e.getSlot(), false);
			return;
		}
		
		// left click: continue, else get inverse recipes list and return (other slot types discarded above)
		if (click != ClickType.LEFT)
		{
			if (hasPermission(player, "ingredient"))
				inventorer.setItemList(player, reciper.getInverseRecipesFor(item), false);
			return;
		}
		
		// show recipe shape: if upper inventory is not empty then bottom have a recipes list
		if (slot == SlotType.CONTAINER && inventorer.isShowingRecipe(player))
		{
			if (hasPermission(player, "recipe"))
				inventorer.showRecipe(player, reciper.getRecipe(item, e.getSlot()-9), false);
			return;
		}
		
		// click with any other remaining condition: show recipes list for this item with 1st crafting recipe above
		List<ItemStack> recipes = reciper.getRecipesFor(item);
		if (recipes != null && recipes.size() > 0)
		{
			if (!hasPermission(player, "list"))
				return;
			inventorer.listRecipes(player, recipes);
			inventorer.showRecipe(player, reciper.getRecipe(item, 0), false);
		}
	}
	
	
	// command: /rc [search keywords] | [reserved words]
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args)
	{
		if (!(cmd.getName().equalsIgnoreCase("rc")))
			return true;
		
		// help page??
		if (args.length < 1)
		{
			sender.sendMessage(msg("helpPage", dictionary.locales(), listPermissions(sender)));
			return true;
		}
		
		if (sender instanceof Player)
			runLastCommand((Player)sender, args);
		else
			getLogger().info("This command needs the Minecraft client GUI.");
		return true;
	}
	
	
	private boolean runLastCommand(Player player, String[] args)
	{
		// save this command for later replay
		String playerId = player.getUniqueId().toString();
		lastCmd.put(playerId, args);
		
		// pre process item near player
		switch (args[0].toLowerCase()+args.length)
		{
		case "hand1":
		case "h1":
			if (!hasPermissionMsg(player, "hand"))
				return false;
			if (!nearItem.containsKey(playerId))
				nearItem.put(playerId, player.getInventory().getItemInMainHand());
			return searchNearItemRecipes(player);
		case "target1":
		case "t1":
			if (!hasPermissionMsg(player, "target"))
				return false;
			if (!nearItem.containsKey(playerId))
				nearItem.put(playerId, sight.getTargetItem(player));
			return searchNearItemRecipes(player);
		}
		
		// reserved words passed, take args as search terms, check list permission before
		nearItem.remove(player.getUniqueId().toString());
		if (!hasPermissionMsg(player, "nosearch"))
			return false;
		
		int listSize = inventorer.setItemList(player, dictionary.searchItemsByName(args), true);
		if (listSize < 1)
			player.sendMessage(msg("noItemsFound"));
		else if (listSize > 243)
			player.sendMessage(msg("tooManyResults"));
		return false;
	}
	
	
	private boolean searchNearItemRecipes(Player player)
	{
		// process specified item near player, skip unspecified|invalid item
		ItemStack item = nearItem.get(player.getUniqueId().toString());
		
		if (item == null || item.getType() == Material.AIR)
			return true;
		
		// search recipes with item as result
		List<ItemStack> items = reciper.getRecipesFor(item);
		if (items != null && !items.isEmpty() && hasPermission(player, "list"))
		{
			inventorer.listRecipes(player, items);
			inventorer.showRecipe(player, reciper.getRecipe(item, 0), true);
			return true;
		}
		
		// search recipes with item as ingredient
		items = reciper.getInverseRecipesFor(item);
		if (items != null && !items.isEmpty())
		{
			if (!hasPermissionMsg(player, "ingredient"))
				return false;
			inventorer.setItemList(player, items, false);
			return true;
		}
		
		// previous permission check now displaying message
		if (!hasPermissionMsg(player, "list"))
			return false;
		
		// no recipe found for specific item
		player.sendMessage(msg("recipesFound"));
		nearItem.remove(player.getUniqueId().toString());
		return false;
	}
	
	
	private String listPermissions(CommandSender sender)
	{
		if (!(sender instanceof Player))
			return msg("needsMinecraftClient");
		Player player = (Player)sender;
		String perms = hasPermission(player, "search") ? " +search" : " -search";
		perms += hasPermission(player, "hand") ? " +hand" : " -hand";
		perms += hasPermission(player, "target") ? " +target" : " -target";
		perms += hasPermission(player, "list") ? " +list" : " -list";
		perms += hasPermission(player, "ingredient") ? " +ingredient" : " -ingredient";
		perms += hasPermission(player, "recipe") ? " +recipe" : " -recipe";
		return perms;
	}
	
	
	// next methods are utility ones, mainly for comfort, readability and isolate methods
	
	
	private boolean hasPermission(Player player, String permission)
	{
		if (!player.isPermissionSet("recipesearch."+permission))
			return true;
		return player.hasPermission("recipesearch."+permission);
	}
	
	
	private boolean hasPermissionMsg(Player player, String permission)
	{
		if (!hasPermission(player, permission))
		{
			player.sendMessage(msg("noPermission"));
			return false;
		}
		return true;
	}
	
	
	boolean isIngredient(ItemStack item)
	{
		return reciper.isIngredient(item);
	}
	
	
	ItemStack newItemStack(String material, Number data)
	{
		return newItemStack(Material.getMaterial(material), data);
	}
	
	
	ItemStack newItemStack(Material material, Number data)
	{
		return new ItemStack(material, 1, (short)0, data.byteValue());
	}
	
	
	byte getData(ItemStack item)
	{
		return (item.getData().getData());
	}
	
	
	String msg(String key, Object... count)
	{
		if (conf.contains(key))
			return String.format("\u00A7E"+conf.getString(key), count);
		else
			return "\u00A7EString not found in conf: "+key;
	}
}
