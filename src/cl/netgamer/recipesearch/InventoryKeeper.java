package cl.netgamer.recipesearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class InventoryKeeper
{
	
	/* que hace este?
	 * 
	 * administra los inventarios del jugador
	 * - toma el contenido del inventario del jugador
	 * - lo guarda
	 * - coloca el resultado en el inventario del jugador: la lista de items entregada
	 * 
	 * al cerrar el inventario el jugador
	 * - coloca el contenido guardado de regreso en el inventario del jugador
	 */
	
	
	private Main plugin;
	private Map<String, List<ItemStack>> itemList = new HashMap<String, List<ItemStack>>();
	private Map<String, ItemStack[]> inventoryBackup = new HashMap<String, ItemStack[]>();
	private Set<String> updating = new HashSet<String>();
	private ItemCycler cycler;
	
	
	InventoryKeeper(Main plugin)
	{
		this.plugin = plugin;
		cycler = new ItemCycler(plugin);
	}
	
	
	int setItemList(Player player, List<ItemStack> itemList, boolean firstRun)
	{
		// check size limits
		if (itemList == null || itemList.isEmpty() || itemList.size() > 243)
			return itemList == null ? 0 : itemList.size();
		
		// sort list by id+data before store for page navigation
		itemList.sort(new Comparator<ItemStack>()
		{
			@Override
			public int compare(ItemStack item1, ItemStack item2)
			{
				int delta = item1.getTypeId() - item2.getTypeId();
				if (delta == 0)
					return plugin.getData(item1) - plugin.getData(item2);
				return delta;
			}
		});
		this.itemList.put(player.getUniqueId().toString(), itemList);
		
		// do player inventory backup before
		if (!isViewing(player))
			inventoryBackup.put(player.getUniqueId().toString(), player.getInventory().getContents());
		// flag needed because openInventory() triggers InventoryCloseEvent on currently opened inventory
		else
			updating.add(player.getUniqueId().toString());
		
		// list first page
		listPage(player, 0, firstRun);
		return itemList.size();
	}
	
	
	void listPage(Player player, int page, boolean firstRun)
	{
		Inventory inv = player.getOpenInventory().getBottomInventory();
		List<ItemStack> pages = itemList.get(player.getUniqueId().toString());
		int numPages = (int) Math.ceil(pages.size()/27F);
		int from = page*27;
		int to = Math.min(from+27, pages.size());
		int slot = 0;
		
		// fill quickbar
		while (slot < numPages)
		{
			ItemStack pageIcon = new ItemStack(slot == page ? Material.EMPTY_MAP : Material.PAPER, slot+1);
			List<String> lores = new ArrayList<String>();
			lores.add(plugin.msg("pageNumber", slot+1));
			if (slot == page)
				lores.add(plugin.msg("current"));
			ItemMeta meta = pageIcon.getItemMeta();
			meta.setLore(lores);
			pageIcon.setItemMeta(meta);
			inv.setItem(slot, pageIcon);
			++slot;
		}
		while (slot < 9)
		{
			inv.setItem(slot, null);
			++slot;
		}
		
		// fill container
		while (from < to)
		{
			inv.setItem(slot, setLore(pages.get(from)));
			++from;
			++slot;
		}
		while (slot < 36)
		{
			inv.setItem(slot, null);
			++slot;
		}
		
		// flag needed because openInventory() triggers InventoryCloseEvent on currently opened inventory
		if (!firstRun)
			updating.add(player.getUniqueId().toString());
		player.openInventory(plugin.getServer().createInventory(player, InventoryType.WORKBENCH));
	}
	
	
	void listRecipes(Player player, List<ItemStack> recipes)
	{
		// directly opened?
		Inventory inv = player.getOpenInventory().getBottomInventory();
		int i = 0;
		if (!isViewing(player))
		{
			inventoryBackup.put(player.getUniqueId().toString(), player.getInventory().getContents());
			while (i < 9)
			{
				inv.setItem(i, null);
				++i;
			}
		}
		// flag needed because openInventory() triggers InventoryCloseEvent on currently opened inventory
		else
			updating.add(player.getUniqueId().toString());

		// list common recipes
		i = 0;
		int slot = 9; // skip quickbar
		while (i < recipes.size())
		{
			inv.setItem(slot, recipes.get(i));
			++i;
			++slot;
		}
		while (slot < 36)
		{
			inv.setItem(slot, null);
			++slot;
		}
	}
	
	
	void showRecipe(Player player, List<ItemStack> listRecipe, boolean firstRun)
	{
		if (listRecipe == null || listRecipe.size() < 1)
			return;
		
		Inventory inv = plugin.getServer().createInventory(player, listRecipe.size() < 4 ? InventoryType.FURNACE : InventoryType.WORKBENCH);
		
		// put items from passed list (recipe) to upper inventory, while cycle variable items and set lores
		ItemStack item;
		Set<Integer> cycled = new HashSet<Integer>();
		for (int i = 0; i < listRecipe.size(); ++i)
		{
			item = listRecipe.get(i);
			if (item != null && plugin.getData(item) < 0)
				cycled.add(i);
			inv.setItem(i, setLore(listRecipe.get(i)));
		}
		cycleItems(player, inv, cycled);
		
		// flag needed because openInventory() triggers InventoryCloseEvent
		
		if (!firstRun)
			updating.add(player.getUniqueId().toString());
		player.openInventory(inv);
	}
	
	
	boolean isShowingRecipe(Player player)
	{
		InventoryView view = player.getOpenInventory();
		switch (view.getType().toString())
		{
		case "WORKBENCH":
			return view.getItem(0) != null;
		case "FURNACE":
			return view.getItem(2) != null;
		}
		return false;
	}
	
	
	boolean isViewing(Player player)
	{
		return inventoryBackup.containsKey(player.getUniqueId().toString());
	}
	
	
	boolean unsetViewing(Player player)
	{
		String playerId = player.getUniqueId().toString();
		
		if (updating.contains(playerId))
		{
			updating.remove(playerId);
			return false;
		}
		
		if (isViewing(player))
		{
			player.getInventory().setContents(inventoryBackup.get(playerId));
			player.updateInventory();
			inventoryBackup.remove(playerId);
			itemList.remove(playerId);
			return true;
		}
		return false;
	}
	
	
	private void cycleItems(Player player, Inventory inv, Set<Integer> indexes)
	{
		// shuffle fuel in furnace recipe view
		if (inv.getType() == InventoryType.FURNACE)
		{
			inv.setItem(1, cycler.shuffleFuel());
			new BukkitRunnable()
			{
				@Override
				public void run()
				{
					if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inv)
					{
						this.cancel();
						return;
					}
					inv.setItem(1, cycler.shuffleFuel());
				}
			}.runTaskTimer(plugin, 20, 20);
		}
		
		// cycle generic items in workbench recipe view
		if (indexes.isEmpty())
			return;
		
		for (int index : indexes)
			inv.getItem(index).setDurability((short)0);
		
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inv)
				{
					this.cancel();
					return;
				}
				for (int i : indexes)
					inv.setItem(i, setLore(cycler.nextIngredient(inv.getItem(i))));
			}
		}.runTaskTimer(plugin, 20, 20);
	}
	
	
	private ItemStack setLore(ItemStack item)
	{
		if (item == null)
			return item;
		
		List<String> lores = new ArrayList<String>();
		int numRecipes = plugin.getServer().getRecipesFor(item).size();
		if (numRecipes > 0)
			lores.add(plugin.msg("recipesCount", numRecipes));
		if (plugin.isIngredient(item))
			lores.add(plugin.msg("ingredient"));
		
		ItemMeta meta = item.getItemMeta();
		meta.setLore(lores);
		item.setItemMeta(meta);
		return item;
	}
	
}
