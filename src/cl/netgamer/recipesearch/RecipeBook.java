package cl.netgamer.recipesearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

public class RecipeBook
{
	
	private Main plugin;
	private Map<String, Set<String>> inverseRecipes = new HashMap<String, Set<String>>();
	
	
	RecipeBook(Main plugin)
	{
		this.plugin = plugin;
		
		// inverse recipes
		Iterator<Recipe> recipes = plugin.getServer().recipeIterator();
		Recipe recipe;
		ItemStack item;
		Collection<ItemStack> items;
		String result;
		String ingredient;
		while (recipes.hasNext())
		{
			recipe = recipes.next();
			item = recipe.getResult();
			if (item == null || item.getType() == Material.AIR)
				continue;
			
			result = item.getType().toString()+"`"+plugin.getData(item);
			if (recipe instanceof ShapedRecipe)
				items = ((ShapedRecipe)recipe).getIngredientMap().values();
			else if (recipe instanceof ShapelessRecipe)
				items = ((ShapelessRecipe)recipe).getIngredientList();
			else
			{
				Set<ItemStack> input = new HashSet<ItemStack>();
				input.add(((FurnaceRecipe)recipe).getInput());
				items = input;
			}
			
			for (ItemStack item2 : items)
			{
				if (item2 != null && item2.getType() != Material.AIR)
				{
					ingredient = item2.getType().toString()+"`"+plugin.getData(item2);
					if (!inverseRecipes.containsKey(ingredient))
						inverseRecipes.put(ingredient, new HashSet<String>());
					inverseRecipes.get(ingredient).add(result);
				}
			}
		}
		plugin.getLogger().info("Loaded "+inverseRecipes.size()+" inverse recipes.");
	}
	
	
	List<ItemStack> getRecipesFor(ItemStack item)
	{
		List<ItemStack> results = new ArrayList<ItemStack>();
		if (item != null)
			for (Recipe recipe : plugin.getServer().getRecipesFor(item))
				results.add(setLore(recipe.getResult(), recipe.getClass().getSimpleName()));
		return results;
	}
	
	
	// adds the recipe type and if the item is ingredient
	private ItemStack setLore(ItemStack item, String recipeClass)
	{
		List<String> lores = new ArrayList<String>();
		
		lores.add(plugin.msg(recipeClass));
		if (isIngredient(item))
			lores.add(plugin.msg("ingredient"));
		
		ItemMeta meta = item.getItemMeta();
		meta.setLore(lores);
		item.setItemMeta(meta);
		
		return item;
	}
	
	
	List<ItemStack> getInverseRecipesFor(ItemStack item)
	{
		List<ItemStack> items = new ArrayList<ItemStack>();
		
		String type = item.getType().toString();
		Set<String> data2;
		String[] d;
		for (String data : new String[]{"`"+plugin.getData(item), "`-1"})
			if ((data2 = inverseRecipes.get(type+data)) != null)
				for (String data3 : data2)
				{
					d = data3.split("`");
					items.add(plugin.newItemStack(Material.getMaterial(d[0]), Byte.parseByte(d[1])));
				}
		return items;
	}
	
	
	List<ItemStack> getRecipe(ItemStack item, int index)
	{
		List<ItemStack> ingredients;
		Recipe recipe = plugin.getServer().getRecipesFor(item).get(index);
		ItemStack result = recipe.getResult();
		
		// shapeless recipe: result + ingredients, length = 10
		if (recipe instanceof ShapelessRecipe)
		{
			ingredients = ((ShapelessRecipe)recipe).getIngredientList();
			ingredients.add(0, result);
			while (ingredients.size() < 10)
				ingredients.add(null);
			return ingredients;
		}
		
		// furnace recipe, length = 3
		ingredients = new ArrayList<ItemStack>();
		if (recipe instanceof FurnaceRecipe)
		{
			ingredients.add(((FurnaceRecipe)recipe).getInput());
			ingredients.add(null);
			ingredients.add(result);
			return ingredients;
		}
		
		// shaped recipe: result + ingredients (centered), length = 10
		ingredients.add(result);
		while (ingredients.size() < 10)
			ingredients.add(null);
		
		Map<Character, ItemStack> items = ((ShapedRecipe)recipe).getIngredientMap();
		int i = 0;
		for (String row : ((ShapedRecipe)recipe).getShape())
		{
			int j = 1;
			for (Character ch : row.toCharArray())
			{
				ingredients.set(i*3+j, items.get(ch));
				++j;
			}
			++i;
		}
		
		// center shaped recipe
		if (ingredients.get(3) == null && ingredients.get(6) == null && ingredients.get(9) == null)
		{
			ingredients.add(1, null);
			ingredients.remove(10);
		}
		
		if (ingredients.get(7) == null && ingredients.get(8) == null && ingredients.get(9) == null)
		{
			ingredients.add(1, null);
			ingredients.add(2, null);
			ingredients.add(3, null);
			ingredients.remove(12);
			ingredients.remove(11);
			ingredients.remove(10);
		}
		return ingredients;
	}
	
	
	boolean isIngredient(ItemStack item)
	{
		if (inverseRecipes.containsKey(item.getType().toString()+"`-1"))
			return true;
		if (inverseRecipes.containsKey(item.getType().toString()+"`"+plugin.getData(item)))
			return true;
		return false;
	}
	
}
