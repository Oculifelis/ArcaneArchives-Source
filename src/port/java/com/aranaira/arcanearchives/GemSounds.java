package com.aranaira.arcanearchives;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.*;

/** The legacy GemParticle handler actually plays these sounds; its visual branches are empty. */
public final class GemSounds {
    public static void play(Player player,String name){
        if(!ArchiveConfig.SOUNDS.get()||player.level().isClientSide())return;
        SoundEvent sound=switch(name){case"munchstone"->SoundEvents.PLAYER_BURP;case"mindspindle"->SoundEvents.ENCHANTMENT_TABLE_USE;case"phoenixway"->SoundEvents.FIRECHARGE_USE;default->null;};
        if(sound!=null)player.level().playSound(null,player.blockPosition(),sound,SoundSource.PLAYERS,1,name.equals("munchstone")?player.getRandom().nextFloat()*.5f+.75f:1);
    }
}
