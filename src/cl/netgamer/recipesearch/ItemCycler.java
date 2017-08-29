package cl.netgamer.recipesearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ItemCycler
{
	
	private Map<String, Short> maxData = new HashMap<String, Short>();
	private List<String> fuels = new ArrayList<String>();
	
	
	ItemCycler(Main plugin)
	{
		// i found no other way to get this info than to put it on by hand
		maxData.put("ANVIL", (short)2);
		maxData.put("COAL", (short)1);
		maxData.put("COBBLE_WALL", (short)1);
		maxData.put("DIRT", (short)2);
		maxData.put("DOUBLE_PLANT", (short)5);
		maxData.put("LEAVES", (short)3);
		maxData.put("LOG", (short)3);
		maxData.put("LOG_2", (short)1);
		maxData.put("LONG_GRASS", (short)3);
		maxData.put("MONSTER_EGGS", (short)5);
		maxData.put("PRISMARINE", (short)2);
		maxData.put("QUARTZ_BLOCK", (short)2);
		maxData.put("RAW_FISH", (short)3);
		maxData.put("RED_ROSE", (short)8);
		maxData.put("RED_SANDSTONE", (short)2);
		maxData.put("SAND", (short)1);
		maxData.put("SANDSTONE", (short)2);
		maxData.put("SAPLING", (short)5);
		maxData.put("SKULL_ITEM", (short)5);
		maxData.put("SMOOTH_BRICK", (short)3);
		maxData.put("SPONGE", (short)1);
		maxData.put("STEP", (short)7);
		maxData.put("STONE", (short)6);
		maxData.put("WOOD", (short)5);
		maxData.put("WOOD_STEP", (short)5);
		maxData.put("WOOL", (short)15);
		
		for (Material material : Material.values())
			if (material.isFuel())
				fuels.add(material.toString());
		
		plugin.getLogger().info("Found "+fuels.size()+" fuel materials.");
	}
	
	
	ItemStack nextIngredient(ItemStack item)
	{
		if (item == null)
			return null;
		
		String material = item.getType().toString();
		Short max = maxData.get(material);
		if (max == null)
			return item;
		
		short d = item.getDurability();
		// logs are specific materials to craft planks, but generic to smelt charcoal
		switch (material+d)
		{
		case "LOG3":
			item.setType(Material.LOG_2);
			d = -1;
			break;
		case "LOG_21":
			item.setType(Material.LOG);
			d = -1;
		}
		
		item.setDurability(d >= max ? 0 : ++d);
		return item;
	}
	
	
	ItemStack shuffleFuel()
	{
		return new ItemStack(Material.getMaterial(fuels.get((int)(Math.random()*fuels.size()))));
	}
	
}
