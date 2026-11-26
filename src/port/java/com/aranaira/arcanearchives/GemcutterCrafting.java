package com.aranaira.arcanearchives;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Counted matching across the legacy combined table/main-player inventory. */
public final class GemcutterCrafting {
    public static List<ItemStack> inputs(ArchiveBlockEntity tile,Player p){var all=new ArrayList<ItemStack>(54);for(int i=0;i<18;i++)all.add(tile.inventory.getStackInSlot(i));for(int i=0;i<36;i++)all.add(p.getInventory().getItem(i));return all;}
    public static boolean same(List<ItemStack> a,List<ItemStack> b){if(a.size()!=b.size())return false;for(int i=0;i<a.size();i++)if(a.get(i).getCount()!=b.get(i).getCount()||!ItemStack.isSameItemSameTags(a.get(i),b.get(i)))return false;return true;}
    public static boolean permitted(GemcutterRecipes.Recipe recipe,Player p){return !ArchiveRecipes.key(recipe.id()).equals("arcanearchives:letter_invitation")||NetworkData.get(p.level()).network(p.getUUID()).equals(p.getUUID());}
    /** Integral maximum flow avoids underpayment or false rejection for overlapping ingredients. */
    public static int[] plan(GemcutterRecipes.Recipe recipe,List<ItemStack> inputs){
        return plan(recipe,inputs,1);
    }
    public static int[] plan(GemcutterRecipes.Recipe recipe,List<ItemStack> inputs,int crafts){
        var requirements=crafts==1?recipe.ingredients():recipe.ingredients().stream().map(r->new GemcutterRecipes.Requirement("",false,Math.multiplyExact(r.count(),crafts),r::test,List.of())).toList();
        var allocation=allocate(requirements,inputs);if(allocation==null)return null;int[] used=new int[inputs.size()];for(var requirement:allocation)for(int s=0;s<used.length;s++)used[s]+=requirement[s];return used;
    }
    public static int[][] allocate(List<GemcutterRecipes.Requirement> requirements,List<ItemStack> inputs){
        int count=requirements.size(),stocks=inputs.size(),sink=1+count+stocks,n=sink+1,total=0;
        int[][] capacity=new int[n][n];
        for(int r=0;r<count;r++){var requirement=requirements.get(r);total+=requirement.count();capacity[0][1+r]=requirement.count();for(int s=0;s<stocks;s++)if(requirement.test(inputs.get(s)))capacity[1+r][1+count+s]=Math.min(requirement.count(),inputs.get(s).getCount());}
        for(int s=0;s<stocks;s++)capacity[1+count+s][sink]=inputs.get(s).getCount();
        int flow=0;
        while(flow<total){
            int[] parent=new int[n],queue=new int[n];Arrays.fill(parent,-1);parent[0]=0;int head=0,tail=1;
            while(head<tail&&parent[sink]<0){int u=queue[head++];for(int v=1;v<n;v++)if(parent[v]<0&&capacity[u][v]>0){parent[v]=u;queue[tail++]=v;}}
            if(parent[sink]<0)return null;int amount=total-flow;
            for(int v=sink;v!=0;v=parent[v])amount=Math.min(amount,capacity[parent[v]][v]);
            for(int v=sink;v!=0;v=parent[v]){capacity[parent[v]][v]-=amount;capacity[v][parent[v]]+=amount;}flow+=amount;
        }
        int[][] allocation=new int[count][stocks];for(int r=0;r<count;r++)for(int s=0;s<stocks;s++)allocation[r][s]=capacity[1+count+s][1+r];return allocation;
    }
    public static void consume(ArchiveBlockEntity tile,Player p,int[] used){
        for(int s=0;s<used.length;s++)if(used[s]>0){
            ItemStack input=(s<18?tile.inventory.getStackInSlot(s):p.getInventory().getItem(s-18)).copy();
            ItemStack remains=input.getCraftingRemainingItem();input.shrink(used[s]);
            if(s<18)tile.inventory.setStackInSlot(s,input);else p.getInventory().setItem(s-18,input);
            if(!remains.isEmpty()){remains.setCount(used[s]);if(!p.getInventory().add(remains))p.drop(remains,false);}
        }
        p.getInventory().setChanged();tile.changed();
    }
}
