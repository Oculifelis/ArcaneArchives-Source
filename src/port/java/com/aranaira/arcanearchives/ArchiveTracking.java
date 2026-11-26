package com.aranaira.arcanearchives;
import net.minecraft.world.entity.player.Player;
public final class ArchiveTracking {
    public static void closed(Player player,ArchiveBlockEntity tile){
        if(tile==null||!(player instanceof net.minecraft.server.level.ServerPlayer server))return;
        var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Kind","tracking_visited");
        tag.putString("Dimension",tile.getLevel().dimension().identifier().toString());tag.putLong("Position",tile.getBlockPos().asLong());ArchiveNetwork.view(server,tag);
    }
}
