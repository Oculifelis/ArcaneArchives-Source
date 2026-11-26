package com.aranaira.arcanearchives.integration;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Optional Baubles BODY-slot successor. No Curios classes are linked when absent. */
public final class CuriosBridge {
    private static final Method INVENTORY,EQUIPPED;
    private static boolean reported;
    static{
        Method inventory=null,equipped=null;
        try{
            var api=Class.forName("top.theillusivec4.curios.api.CuriosApi");
            var handler=Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
            inventory=api.getMethod("getCuriosInventory",LivingEntity.class);
            equipped=handler.getMethod("getEquippedCurios");
        }catch(ClassNotFoundException absent){/* Ordinary standalone operation. */}
        catch(ReflectiveOperationException failure){System.getLogger("ArcaneArchives").log(System.Logger.Level.WARNING,"Curios inventory API unavailable",failure);}
        INVENTORY=inventory;EQUIPPED=equipped;
    }
    public static List<ItemStack> equipped(Player player){
        if(INVENTORY==null||EQUIPPED==null)return List.of();
        try{
            Object result=INVENTORY.invoke(null,player);
            // Forge 1.20.1 uses LazyOptional; newer Curios returns Optional.
            if(!(result instanceof Optional<?>))result=result.getClass().getMethod("resolve").invoke(result);
            Object handler=((Optional<?>)result).orElse(null);if(handler==null)return List.of();
            IItemHandler stacks=(IItemHandler)EQUIPPED.invoke(handler);
            List<ItemStack> values=new ArrayList<>();
            for(int i=0;i<stacks.getSlots();i++)if(!stacks.getStackInSlot(i).isEmpty())values.add(stacks.getStackInSlot(i));
            return values;
        }catch(ReflectiveOperationException|ClassCastException failure){
            if(!reported){reported=true;System.getLogger("ArcaneArchives").log(System.Logger.Level.WARNING,"Could not inspect equipped Curios",failure);}
            return List.of();
        }
    }
}
