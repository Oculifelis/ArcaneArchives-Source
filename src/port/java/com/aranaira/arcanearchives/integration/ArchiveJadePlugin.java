package com.aranaira.arcanearchives.integration;

import com.aranaira.arcanearchives.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Modern successor to the legacy Hwyla block providers. No Jade is required. */
@WailaPlugin
public final class ArchiveJadePlugin implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration){registration.registerBlockDataProvider(ServerProvider.INSTANCE,ArchiveBlockEntity.class);}
    @Override public void registerClient(IWailaClientRegistration registration){registration.registerBlockComponent(ClientProvider.INSTANCE,ArchiveBlock.class);}
    public enum ServerProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid(){return AA.id("storage");}
        @Override public void appendServerData(CompoundTag data,BlockAccessor accessor){if(accessor.getBlockEntity() instanceof ArchiveBlockEntity tile)data.put("ArcaneArchives",ArchiveDiagnostics.snapshot(tile));}
    }
    public enum ClientProvider implements IBlockComponentProvider {
        INSTANCE;
        @Override public ResourceLocation getUid(){return AA.id("storage");}
        @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config){
            var data=accessor.getServerData();if(data.contains("ArcaneArchives"))for(var line:ArchiveDiagnostics.lines(data.getCompound("ArcaneArchives"),accessor.getLevel()))tooltip.add(line);
        }
    }
}
