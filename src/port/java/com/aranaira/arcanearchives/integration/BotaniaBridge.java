package com.aranaira.arcanearchives.integration;

import com.aranaira.arcanearchives.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/** The original Rivertear water shortcut through Botania's current public API. */
public final class BotaniaBridge {
    private static Class<?> contract,state;
    private static java.lang.reflect.Method getFluid,setFluid;
    private static Object empty,water;
    private static boolean checked;
    @SuppressWarnings({"unchecked","rawtypes"})private static void locate(){
        if(checked)return;checked=true;
        try{
            contract=Class.forName("vazkii.botania.api.block.PetalApothecary");state=Class.forName("vazkii.botania.api.block.PetalApothecary$State");
            getFluid=contract.getMethod("getFluid");setFluid=contract.getMethod("setFluid",state);empty=Enum.valueOf((Class)state,"EMPTY");water=Enum.valueOf((Class)state,"WATER");
        }catch(ClassNotFoundException absent){contract=null;}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Unsupported installed Botania Apothecary API",failure);}
    }
    public static boolean interact(Player player,BlockPos pos){
        if(player.level().isClientSide())return false;locate();if(contract==null)return false;
        var tile=player.level().getBlockEntity(pos);if(tile==null||!contract.isInstance(tile))return false;
        boolean river=false;for(int i=0;i<36;i++)if(player.getInventory().getItem(i).is(AA.item("rivertear"))){river=true;break;}
        if(!river)river=GemRules.held(player).stream().anyMatch(s->s.is(AA.item("rivertear")));if(!river)return false;
        try{
            if(getFluid.invoke(tile)!=empty)return false;setFluid.invoke(tile,water);tile.setChanged();var block=tile.getBlockState();player.level().sendBlockUpdated(pos,block,block,3);return true;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Botania Apothecary interaction failed",failure);}
    }
}
