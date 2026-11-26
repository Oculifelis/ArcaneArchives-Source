package com.aranaira.arcanearchives;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
/** Stores recipe identity plus a witness grid; every craft is revalidated against live recipes and actual inputs. */
public final class CraftingBookmarks {
    public static List<ItemStack> grid(PersistentCraftingMenu menu){var items=new ArrayList<ItemStack>();for(int i=0;i<9;i++)items.add(menu.getSlot(i+1).getItem().copy());return items;}
    public static CompoundTag capture(Player p,PersistentCraftingMenu menu){var grid=grid(menu);var match=BookmarkRecipes.find(p,grid);if(match==null)return new CompoundTag();var tag=new CompoundTag();tag.putString("Recipe",match.id());var list=new ListTag();for(var s:grid)list.add(StackData.save(s.isEmpty()?ItemStack.EMPTY:s.copyWithCount(1),p.level()));tag.put("Grid",list);return tag;}
    private static List<ItemStack> witness(Player p,CompoundTag tag){var grid=new ArrayList<ItemStack>();var list=tag.getListOrEmpty("Grid");for(int i=0;i<9;i++)grid.add(i<list.size()?StackData.load(list.getCompoundOrEmpty(i),p.level()):ItemStack.EMPTY);return grid;}
    private static Plan plan(Player p,PersistentCraftingMenu menu,CompoundTag tag){
        if(tag.isEmpty())return null;var template=witness(p,tag);var recipe=BookmarkRecipes.find(p,template);if(recipe==null||!recipe.id().equals(tag.getStringOr("Recipe","")))return null;
        var positions=new ArrayList<Integer>();for(int i=0;i<9;i++)if(!template.get(i).isEmpty())positions.add(i);
        var requirements=new ArrayList<GemcutterRecipes.Requirement>();
        for(var ingredient:recipe.ingredients())requirements.add(new GemcutterRecipes.Requirement("",false,1,ingredient::test,List.of()));
        // Special recipes expose no ingredient list: retain the exact witness components.
        if(requirements.size()!=positions.size()){requirements.clear();for(int at:positions){var expected=template.get(at);requirements.add(new GemcutterRecipes.Requirement("",false,1,s->ItemStack.isSameItemSameComponents(s,expected),List.of()));}}
        var mapping=GemcutterCrafting.allocate(requirements,template);if(mapping==null)return null;
        var stocks=grid(menu);for(int i=0;i<36;i++)stocks.add(p.getInventory().getItem(i));
        var payment=GemcutterCrafting.allocate(requirements,stocks);if(payment==null)return null;int[] used=new int[45];var actual=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));
        for(int r=0;r<payment.length;r++){int position=-1;for(int i=0;i<9;i++)if(mapping[r][i]>0){position=i;break;}if(position<0)return null;for(int i=0;i<45;i++)if(payment[r][i]>0){used[i]+=payment[r][i];actual.set(position,stocks.get(i).copyWithCount(1));}}
        var assembled=BookmarkRecipes.find(p,actual);if(assembled==null||!assembled.id().equals(recipe.id()))return null;return new Plan(used,assembled,actual);
    }
    private record Plan(int[] used,BookmarkRecipes.Match recipe,List<ItemStack> actual){}
    public static ItemStack display(Player p,CompoundTag tag){if(tag.isEmpty())return ItemStack.EMPTY;var found=BookmarkRecipes.find(p,witness(p,tag));return found!=null&&found.id().equals(tag.getStringOr("Recipe",""))?found.result():ItemStack.EMPTY;}
    public static boolean available(Player p,PersistentCraftingMenu menu,CompoundTag tag){return plan(p,menu,tag)!=null;}
    public static boolean craft(Player p,PersistentCraftingMenu menu,CompoundTag tag){
        var plan=plan(p,menu,tag);if(plan==null)return false;
        for(int i=0;i<45;i++)if(plan.used()[i]>0){if(i<9)menu.getSlot(i+1).remove(plan.used()[i]);else p.getInventory().removeItem(i-9,plan.used()[i]);}
        for(var returned:BookmarkRecipes.finish(p,plan.recipe(),plan.actual()))give(p,returned);p.getInventory().setChanged();menu.slotsChanged(menu.getSlot(1).container);return true;
    }
    private static void give(Player p,ItemStack s){if(!s.isEmpty()&&!p.getInventory().add(s))p.drop(s,false);}
}
