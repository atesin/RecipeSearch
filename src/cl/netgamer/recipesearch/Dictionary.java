package cl.netgamer.recipesearch;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Dictionary
{
	
	private Main plugin;
	private Map<String, ItemStack> dictionary = new HashMap<String, ItemStack>();
	private String locales = "";
	
	
	public Dictionary(Main plugin)
	{
		this.plugin = plugin;
		
		// load language files -> lines<displayedName, globalName[`globalname2...]>
		Map<String, String> langLines = new HashMap<String, String>();
		
		File[] langFiles = plugin.getDataFolder().listFiles((FileFilter)new WildcardFileFilter("*.lang"));
		if (langFiles != null)
			for (File langFile : langFiles)
				langLines = readFileLines(langFile, langLines);
		
		// no files loaded = no name search available
		if (langLines.isEmpty())
		{
			plugin.getLogger().warning("No languages loaded, search item disabled, read 'config.yml' comments.");
			return;
		}
		
		// get materials<global name, material data>
		Map<String, String> materials = getServerMaterials();
		
		// fill dictionary with found names:
		// lines<displayedName, globalName[`globalname2...]> -> dictionary<displayedName(s), materialData>
		String globalNames;
		Set<String> materialDatas;
		for (String displayName : langLines.keySet())
		{
			// corrections, replacing duplicated global name by unique material names
			globalNames = langLines.get(displayName);
			switch (globalNames)
			{
			case "tile.button.name":
				globalNames = "STONE_BUTTON`WOOD_BUTTON";
				break;
			case "tile.snow.name":
				globalNames = "SNOW`SNOW_BLOCK";
				break;
			case "tile.mushroom.name":
				globalNames = "BROWN_MUSHROOM`HUGE_MUSHROOM_1`HUGE_MUSHROOM_2`RED_MUSHROOM";
			}
			
			// previous set to prepare duplicated names
			materialDatas = new HashSet<String>();
			for (String globalName2 : globalNames.split("`"))
				if (materials.containsKey(globalName2))
					materialDatas.add(materials.get(globalName2));
					//materials.remove(globalName2);
			
			for (String data : materialDatas)
			{
				String[] d = data.split("`");
				Material mat = Material.getMaterial(d[0]);
				
				// don't overwork
				ItemStack item = plugin.newItemStack(mat, Byte.parseByte(d[1]));
				if (!plugin.isIngredient(item) && plugin.getServer().getRecipesFor(item).size() < 1)
					continue;
				
				// add item at last, with some info
				dictionary.put(displayName+(materialDatas.size() > 1 ? " ("+data.split("`")[0].toLowerCase()+")" : ""), item);
			}
		}
		//System.out.println(materials);
		plugin.getLogger().info("Loaded "+dictionary.size()+" unique localized item names.");

	}
	
	
	// constructor utility: language file -> lines<displayed name, global name>
	private Map<String, String> readFileLines(File langFile, Map<String, String> lines)
	{
		String[] term;
		List<String> fileLines;
		String discName = "";
		
		plugin.getLogger().info("Loading file '"+langFile.getName()+"'...");
		try
		{
			fileLines = IOUtils.readLines(new FileInputStream(langFile), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			plugin.getLogger().warning("Error loading file, check your config and language file... skipping.");
			return lines;
		}
		
		for (String line : fileLines)
		{
			// get file locale
			if (line.startsWith("language.code="))
				locales += " "+line.split("=")[1];
			
			if (!line.matches("((item)|(tile))\\..*?\\.name=.*") && !line.matches("item\\.record\\..*?\\.desc=.*"))
				continue;
			
			// {globalName, displayed}
			term = line.split("=");
			
			// fix music discs name
			if (term[0].equals("item.record.name"))
				discName = term[1];
			if (term[0].matches("item\\.record\\..*?\\.desc"))
				term[1] = discName+" ("+term[1]+")";
			
			// save pre processed lines for later use
			if (lines.containsKey(term[1]))
			{
				lines.put(term[1], lines.get(term[1])+"`"+term[0]);
				//System.out.println("DUPLICATED DISPLAY: "+term[1]+" = "+lines.get(term[1]));
			}
			else
				lines.put(term[1], term[0]);
		}
		return lines;
	}
	
	
	// constructor utility: materials<global name, material data>
	private Map<String, String> getServerMaterials()
	{
		// nms reflection methods wrapper
		Method methodAsNMSCopy = null;
		Method methodA = null;
		String version = plugin.getServer().getClass().getName().split("\\.")[3];
		try
		{
			methodAsNMSCopy = Class.forName("org.bukkit.craftbukkit."+version+".inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);
			methodA = Class.forName("net.minecraft.server."+version+".ItemStack").getMethod("a");
		}
		catch (ClassNotFoundException | NoSuchMethodException | SecurityException e)
		{ e.printStackTrace(); }
		
		// walk throught materials+variants to take note global names -> materials<globalName, materialData>
		ItemStack item;
		String globalName = "", materialName;
		Map<String, String> materials = new HashMap<String, String>();
		String[] color = new String[]{"white","orange","magenta","lightBlue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black"};
		Map<String, String> discs = new HashMap<String, String>();
		discs.put("GOLD_RECORD",  "13"     );
		discs.put("GREEN_RECORD", "cat"    );
		discs.put("RECORD_3",     "blocks" );
		discs.put("RECORD_4",     "chirp"  );
		discs.put("RECORD_5",     "far"    );
		discs.put("RECORD_6",     "mall"   );
		discs.put("RECORD_7",     "mellohi");
		discs.put("RECORD_8",     "stal"   );
		discs.put("RECORD_9",     "strad"  );
		discs.put("RECORD_10",    "ward"   );
		discs.put("RECORD_11",    "11"     );
		discs.put("RECORD_12",    "wait"   );
		
		materialLoop:
		for (Material material : Material.values())
		{
			materialName = material.toString();
			for (byte data = 0; data < 16; ++data)
			{
				// get global name by item stack
				item = plugin.newItemStack(material, data);
				//if (plugin.getServer().getRecipesFor(item).size() < 1)
				//	break;
				
				try
				{
					globalName = methodA.invoke(methodAsNMSCopy.invoke(null, item)).toString();
				}
				catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
				{ e.printStackTrace(); }
				
				// unobtainable item
				if (globalName.equals("tile.air"))
					break;
				
				// corrections: there is no uncolored banner, all them have a base color
				if (globalName.equals("tile.banner"))
					globalName = "item.banner."+color[data];
				globalName += ".name";
				
				// corrections: avoid confussion with used and unused items with same name
				switch (materialName+data)
				{
				case "COOKED_FISH2":
				case "LEAVES_22":
				case "LOG4":
				case "LOG_22":
					continue materialLoop;
				}
				
				// corrections: replace duplicated global names with unique material name
				switch (materialName)
				{
				case "STONE_BUTTON":
				case "WOOD_BUTTON":
				case "SNOW":
				case "SNOW_BLOCK":
				case "BROWN_MUSHROOM":
				case "HUGE_MUSHROOM_1":
				case "HUGE_MUSHROOM_2":
				case "RED_MUSHROOM":
					globalName = materialName;
				}
				
				// corrections: generic music discs name replaced by specific ones
				if (globalName.equals("item.record.name"))
					globalName = "item.record."+discs.get(materialName)+".desc";
				
				// save global names found for later check, avoiding overwriting
				if (materials.containsKey(globalName))
				{
					//if (!materials.get(globalName).startsWith(materialName+"`"))
					//	System.out.println("DUPLICATED MATERIAL: "+globalName+", "+materials.get(globalName)+", "+materialName+"`"+data);
					break;
				}
				materials.put(globalName, materialName+"`"+data);
			}
		}
		plugin.getLogger().info("Found "+materials.size()+" server global names.");
		return materials;
	}
	
	
	String locales()
	{
		return locales;
	}
	
	
	// keywords --> list(result itemstacks)
	List<ItemStack> searchItemsByName(String... keywords)
	{
		List<ItemStack> items = new ArrayList<ItemStack>();
		if (keywords.length < 1)
			return items;
		
		/* String pattern = "(?i)";
		if (keywords[0].startsWith("\""))
			pattern += String.join(" ", keywords).replaceAll("\"", "");
		else
			pattern += ".*?"+String.join(".*?", keywords)+".*?"; */
		String pattern = "(?i).*?"+String.join(".*?", keywords)+".*?";
		
		ItemStack item;
		//String[] data;
		for (String itemName : dictionary.keySet())
		{
			if (itemName.matches(pattern))
			{
				//data = dictionary.get(itemName).split("`");
				//item = new ItemStack(Material.getMaterial(data[0]), 1, (short) 0, Byte.parseByte(data[1]));
				item = dictionary.get(itemName);
				
				if (!items.contains(item))
					items.add(item);
			}
		}
		return items;
	}
	
}
