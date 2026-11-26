package com.aranaira.arcanearchives;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
public final class ArchiveProfiles {
    public static UUID uuid(ServerPlayer player,String name){var server=((net.minecraft.server.level.ServerLevel)player.level()).getServer();var cache=server.getProfileCache();return cache==null?null:cache.get(name).map(com.mojang.authlib.GameProfile::getId).orElse(null);}
}
