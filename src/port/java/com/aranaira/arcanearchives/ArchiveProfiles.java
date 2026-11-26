package com.aranaira.arcanearchives;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
public final class ArchiveProfiles {
    public static UUID uuid(ServerPlayer player,String name){return player.level().getServer().services().nameToIdCache().get(name).map(net.minecraft.server.players.NameAndId::id).orElse(null);}
}
