package cl.netgamer.recipesearch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Banner;
import org.bukkit.block.Bed;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;


public class Sight
{
	private Main plugin;
	private Set<Material> transparent = new HashSet<Material>();
	private Map<String, String> blockItems = new HashMap<String, String>();
	private Map<String, String> entityItems = new HashMap<String, String>();
	private Map<String, String> boatType = new HashMap<String, String>();
	
	Sight(Main plugin)
	{
		this.plugin = plugin;
		
		transparent.add(Material.AIR);
		transparent.add(Material.STATIONARY_WATER);
		transparent.add(Material.WATER);
		transparent.add(Material.BARRIER);
		
		//blockItems.put("BED_BLOCK", "BED");
		blockItems.put("MELON_STEM", "MELON_SEEDS");
		blockItems.put("PUMPKIN_STEM", "PUMPKIN_SEEDS");
		blockItems.put("REDSTONE_LAMP_ON", "REDSTONE_LAMP_OFF");
		blockItems.put("CROPS", "WHEAT");
		blockItems.put("SIGN_POST", "SIGN");
		blockItems.put("WALL_SIGN", "SIGN");
		blockItems.put("IRON_DOOR_BLOCK", "IRON_DOOR");
		blockItems.put("SUGAR_CANE_BLOCK", "SUGAR_CANE");
		blockItems.put("CAKE_BLOCK", "CAKE");
		blockItems.put("DIODE_BLOCK_OFF", "DIODE");
		blockItems.put("DIODE_BLOCK_ON", "DIODE");
		blockItems.put("NETHER_WARTS", "NETHER_STALK");
		blockItems.put("BREWING_STAND", "BREWING_STAND_ITEM");
		blockItems.put("CAULDRON", "CAULDRON_ITEM");
		//blockItems.put("COCOA", "INK_SACK");
		blockItems.put("TRIPWIRE", "STRING");
		blockItems.put("FLOWER_POT", "FLOWER_POT_ITEM");
		blockItems.put("CARROT", "CARROT_ITEM");
		blockItems.put("POTATO", "POTATO_ITEM");
		blockItems.put("BEETROOT_BLOCK", "BEETROOT");
		//blockItems.put("SKULL", "SKULL_ITEM");
		blockItems.put("REDSTONE_COMPARATOR_OFF", "REDSTONE_COMPARATOR");
		//blockItems.put("STANDING_BANNER", "BANNER");
		//blockItems.put("WALL_BANNER", "BANNER");
		blockItems.put("WOODEN_DOOR", "WOOD_DOOR");
		blockItems.put("SPRUCE_DOOR", "SPRUCE_DOOR_ITEM");
		blockItems.put("BIRCH_DOOR", "BIRCH_DOOR_ITEM");
		blockItems.put("JUNGLE_DOOR", "JUNGLE_DOOR_ITEM");
		blockItems.put("ACACIA_DOOR", "ACACIA_DOOR_ITEM");
		blockItems.put("DARK_OAK_DOOR", "DARK_OAK_DOOR_ITEM");
		
		entityItems.put("MINECART", "MINECART");
		entityItems.put("MINECART_CHEST", "STORAGE_MINECART");
		entityItems.put("MINECART_FURNACE", "POWERED_MINECART");
		entityItems.put("MINECART_TNT", "EXPLOSIVE_MINECART");
		entityItems.put("MINECART_HOPPER", "HOPPER_MINECART");
		entityItems.put("MINECART_COMMAND", "COMMAND_MINECART");
		entityItems.put("FISHING_HOOK", "FISHING_ROD");
		entityItems.put("LEASH_HITCH", "LEASH");
		entityItems.put("PAINTING", "PAINTING");
		entityItems.put("ITEM_FRAME", "ITEM_FRAME");
		entityItems.put("ARMOR_STAND", "ARMOR_STAND");
		entityItems.put("ENDER_CRYSTAL", "END_CRYSTAL");
		entityItems.put("PRIMED_TNT", "TNT");
		entityItems.put("ARROW", "ARROW");
		entityItems.put("TIPPED_ARROW", "TIPPED_ARROW");
		entityItems.put("SPECTRAL_ARROW", "SPECTRAL_ARROW");
		entityItems.put("SNOWBALL", "SNOW_BALL");
		entityItems.put("EGG", "EGG");
		entityItems.put("ENDER_PEARL", "ENDER_PEARL");
		entityItems.put("ENDER_SIGNAL", "EYE_OF_ENDER");
		entityItems.put("FIREWORK", "FIREWORK");
		entityItems.put("FIREBALL", "FIREBALL");
		
		boatType.put("GENERIC", "BOAT");
		boatType.put("REDWOOD", "BOAT_SPRUCE");
		boatType.put("BIRCH", "BOAT_BIRCH");
		boatType.put("JUNGLE", "BOAT_JUNGLE");
		boatType.put("ACACIA", "BOAT_ACACIA");
		boatType.put("DARK_OAK", "BOAT_DARK_OAK");
		
		plugin.getLogger().info("Loaded 4 transparent blocks.");
	}
	
	
	// idea: draw an imaginary line from player eye forward to crosshair
	// draw another line from player eye to entity center, like using peripherial vision field
	// the more the narrow the angle beetween these lines, the more the player is looking to the entity
	private ItemStack getEntityTarget(Player player, double nearest)
	{
		// get some properties before proceed
		ItemStack targeted = null;
		Location pLoc = player.getEyeLocation();
		Vector toCrosshair = pLoc.getDirection();
		
		// loop nearby entities
		for (Entity entity : player.getNearbyEntities(5, 5, 5))
		{
			// special case: a single armor stand can hold and display up to 4 items, loop them
			int slot = entity instanceof ArmorStand ? 0 : 3;
			while (slot < 4)
			{
				++slot;
				
				// find entity center, the method also skips entities with no related item
				Location eLoc = getEntityLocation(entity, slot);
				if (eLoc == null)
					continue;
				
				// check if entity is closer than closest, remember to overwrite after find candidates
				double sqDistance = eLoc.distanceSquared(pLoc);
				if (sqDistance >= nearest)
					continue;
				
				// is entity behind player (has wide angle)?
				Vector toEntity =  eLoc.clone().subtract(pLoc).toVector();
				if (toCrosshair.dot(toEntity) < 0)
					continue;
				
				// take advantage that aim direction is a unit vector so cross product is proportional to distance*sine
				// is angle proportional to entity width and inverse to distance? (length = half entity width)
				if (toEntity.crossProduct(toCrosshair).length()*2.0 > entity.getWidth())
					continue;
				
				// found possible targeted entity
				if ((targeted = getEntityItem(entity, slot)) != null)
					nearest = sqDistance;
			}
		}
		return targeted;
	}
	
	
	private Location getEntityLocation(Entity entity, int slot)
	{
		// early handle entities that have no matching item (skip them) and special cases
		switch (entity.getType().toString())
		{
		case "EXPERIENCE_ORB":
		case "LIGHTNING":
		case "MINECART_MOB_SPAWNER":
		case "WITHER_SKULL":
		case "DRAGON_FIREBALL":
			return null;
		case "ITEM_FRAME":
			return entity.getLocation();
		case "ARMOR_STAND":
			return entity.getLocation().add(0, slot/2.0 - 0.25, 0);
		case "":
			
		}
		
		// living entites have no related item
		if (entity instanceof LivingEntity)
			return null;
		
		// generic case with center point at half height
		return entity.getLocation().add(0, entity.getHeight()/2, 0);
	}
	
	
	private ItemStack getEntityItem(Entity entity, int slot)
	{
		// get entity item according particular case
		String type = entity.getType().toString();
		switch (type)
		{
		case "DROPPED_ITEM":
			return ((Item)entity).getItemStack();
		case "BOAT":
			return new ItemStack(Material.getMaterial(boatType.get(((Boat)entity).getWoodType().toString())));
		case "FALLING_BLOCK":
			return new ItemStack(((FallingBlock)entity).getMaterial());
		case "ITEM_FRAME":
			ItemStack item = ((ItemFrame)entity).getItem();
			if (item != null && item.getType() != Material.AIR)
				return item;
			break;
		case "ARMOR_STAND":
			ItemStack armor = null;
			switch (slot)
			{
			case 4:
				armor = ((ArmorStand)entity).getHelmet();
				break;
			case 3:
				armor = ((ArmorStand)entity).getChestplate();
				break;
			case 2:
				armor = ((ArmorStand)entity).getLeggings();
				break;
			case 1:
				armor = ((ArmorStand)entity).getBoots();
			}
			if (armor != null && armor.getType() != Material.AIR)
				return armor;
		}
		
		// generic case
		if (entityItems.containsKey(type))
			return new ItemStack(Material.getMaterial(entityItems.get(type)));
		return null;
	}
	
	
	ItemStack getTargetItem(Player player)
	{
		// get target block first
		double nearest = 50D;
		Block targetBlock = player.getTargetBlock(transparent, 5);
		
		// get distance from player's eye to block center, can be squared since is just for later comparison
		ItemStack blockItem = getBlockItem(targetBlock);
		if (blockItem != null && blockItem.getType() != Material.AIR)
			nearest = player.getEyeLocation().distanceSquared(getBlockLocation(targetBlock));
		
		// return entity target item if closer than block and has correct angles
		ItemStack entityItem = getEntityTarget(player, nearest);
		if (entityItem != null)
			return entityItem;
		return blockItem;
	}
	
	
	private Location getBlockLocation(Block block)
	{
		if (block.getType().toString().contains("RAIL"))
			return block.getLocation().add(0.5, 0.125, 0.5);
		return block.getLocation().add(0.5, 0.5, 0.5);
	}
	
	
	private ItemStack getBlockItem(Block block)
	{
		if (block == null || block.getType() == Material.AIR || block.getType() == Material.STATIONARY_WATER)
			return null;
		
		// some materials have different id and|or data for block and item
		String type = block.getType().toString();
		byte data;
		DyeColor color;
		switch (type)
		{
		case "COCOA":
			return plugin.newItemStack(Material.INK_SACK, 3);
		case "DOUBLE_PLANT":
			data = block.getData();
			if (data > 7)
				data = block.getRelative(BlockFace.DOWN).getData();
			return plugin.newItemStack(type, data);
		case "SKULL":
			SkullType skull = ((Skull)block.getState()).getSkullType();
			return plugin.newItemStack(Material.SKULL_ITEM, Arrays.asList(SkullType.values()).indexOf(skull));
		case "STANDING_BANNER":
		case "WALL_BANNER":
			color = ((Banner)block.getState()).getBaseColor();
			return plugin.newItemStack(Material.BANNER, 15 - Arrays.asList(DyeColor.values()).indexOf(color));
		case "BED_BLOCK":
			color = ((Bed)block.getState()).getColor();
			return plugin.newItemStack(Material.BED, Arrays.asList(DyeColor.values()).indexOf(color));
		case "LEAVES":
			return plugin.newItemStack(type, block.getData() % 4);
		case "LEAVES_2":
			return plugin.newItemStack(type, block.getData() % 2);
		case "PISTON_EXTENSION":
			if (block.getData() > 7)
				return new ItemStack(Material.PISTON_STICKY_BASE);
			return new ItemStack(Material.PISTON_BASE);
		case "QUARTZ_BLOCK":
			data = (byte)Math.min(block.getData(), 2);
			return plugin.newItemStack(type, data);
		case "ANVIL":
			return new ItemStack(Material.ANVIL);
		case "DOUBLE_STEP":
		case "WOOD_DOUBLE_STEP":
		case "DOUBLE_STONE_SLAB2":
		case "PURPUR_DOUBLE_SLAB":
			type = type.replace("DOUBLE_", "");
		case "STEP":
		case "WOOD_STEP":
		case "STONE_SLAB2":
		case "PURPUR_SLAB":
			return plugin.newItemStack(type, block.getData() % 8);
		}
		
		if (blockItems.containsKey(type))
			return new ItemStack(Material.getMaterial(blockItems.get(type)));
		return plugin.newItemStack(block.getType(), block.getData());
	}
	
}
