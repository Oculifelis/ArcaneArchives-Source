package com.aranaira.arcanearchives.client;
import com.aranaira.arcanearchives.*;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Per-block looping sound follows authoritative online/growth state and stops on unload. */
public final class ArchiveAudio {
    private static final java.util.Map<ArchiveBlockEntity,Resonator> loops=new java.util.WeakHashMap<>();
    public static void tick(ArchiveBlockEntity tile){
        if(!tile.kind().equals("radiant_resonator")||!tile.resonating||!ArchiveConfig.SOUNDS.get()||!ArchiveLegacyConfig.RESONATOR_TICKING.get())return;
        var manager=Minecraft.getInstance().getSoundManager();var existing=loops.get(tile);
        if(existing!=null&&manager.isActive(existing))return;
        var sound=new Resonator(tile);loops.put(tile,sound);manager.play(sound);
    }
    private static final class Resonator extends AbstractTickableSoundInstance{
        private final WeakReference<ArchiveBlockEntity> reference;
        Resonator(ArchiveBlockEntity tile){
            super(AA.RESONATOR_LOOP.get(),SoundSource.BLOCKS,RandomSource.create());reference=new WeakReference<>(tile);
            looping=true;delay=0;volume=ArchiveLegacyConfig.RESONATOR_VOLUME.get().floatValue();x=tile.getBlockPos().getX()+.5;y=tile.getBlockPos().getY()+.5;z=tile.getBlockPos().getZ()+.5;
        }
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){
            var tile=reference.get();
            volume=ArchiveLegacyConfig.RESONATOR_VOLUME.get().floatValue();
            if(tile==null||tile.isRemoved()||Minecraft.getInstance().level!=tile.getLevel()||!tile.resonating||!ArchiveConfig.SOUNDS.get()||!ArchiveLegacyConfig.RESONATOR_TICKING.get()){
                if(tile!=null)tile.clientSoundActive=false;stop();
            }
        }
    }
}
