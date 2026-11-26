package com.aranaira.arcanearchives;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=AA.ID)
public final class DevouringCharmEvents {
    private static final java.util.Map<Player,Long> LAST_SOUND=new java.util.WeakHashMap<>();
    @SubscribeEvent public static void pickup(EntityItemPickupEvent event){if(consume(event.getEntity(),event.getItem()))event.setCanceled(true);}
    public static boolean consume(Player p,ItemEntity entity){
        if(p.level().isClientSide||!entity.isAlive()||entity.getItem().isEmpty())return false;
        for(int i=0;i<p.getInventory().getContainerSize();i++){var charm=p.getInventory().getItem(i);if(charm.is(AA.item("devouring_charm"))&&DevouringCharmMenu.matches(charm,entity.getItem(),p)){
            entity.getItem().setCount(0);entity.discard();long tick=p.level().getGameTime();
            if(ArchiveConfig.SOUNDS.get()&&tick-LAST_SOUND.getOrDefault(p,-20L)>=10){p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_EAT,net.minecraft.sounds.SoundSource.PLAYERS,.4f,.7f+p.level().getRandom().nextFloat()*.6f);LAST_SOUND.put(p,tick);}return true;
        }}return false;
    }
}
