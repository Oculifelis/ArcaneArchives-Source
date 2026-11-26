package com.aranaira.arcanearchives;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
/** Native recipe lookup and assembly; this small boundary differs between the three targets. */
public final class BookmarkRecipes {
    public record Match(String id,List<Ingredient> ingredients,ItemStack result,List<ItemStack> remainders,Object nativeRecipe){}
    public static Match find(Player p,List<ItemStack> items){
        var grid=new TransientCraftingContainer(new AbstractContainerMenu(null,-1){public boolean stillValid(Player p){return false;}public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}},3,3);
        for(int i=0;i<9;i++)grid.setItem(i,items.get(i).copyWithCount(items.get(i).isEmpty()?0:1));
        var found=p.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,p.level());
        if(found.isEmpty())return null;var recipe=found.get();
        return new Match(recipe.getId().toString(),recipe.getIngredients().stream().filter(i->!i.isEmpty()).toList(),recipe.assemble(grid,p.level().registryAccess()),recipe.getRemainingItems(grid),recipe);
    }
    public static List<ItemStack> finish(Player p,Match match,List<ItemStack> items){
        var grid=new TransientCraftingContainer(new AbstractContainerMenu(null,-1){public boolean stillValid(Player p){return false;}public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}},3,3);for(int i=0;i<9;i++)grid.setItem(i,items.get(i).copy());
        var result=new ResultContainer();result.setRecipeUsed((CraftingRecipe)match.nativeRecipe());result.setItem(0,match.result().copy());var slot=new ResultSlot(p,grid,result,0,0,0);var output=slot.remove(match.result().getCount());slot.onTake(p,output);
        var returned=new ArrayList<ItemStack>();returned.add(output);for(int i=0;i<9;i++){var remaining=grid.removeItemNoUpdate(i);if(!remaining.isEmpty())returned.add(remaining);}return returned;
    }
}
