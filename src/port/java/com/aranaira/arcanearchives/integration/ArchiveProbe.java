package com.aranaira.arcanearchives.integration;

import com.aranaira.arcanearchives.*;
import java.lang.reflect.*;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** TOP's public IMC API, loaded only when the optional mod is installed. */
public final class ArchiveProbe {
    public static void enqueue(net.neoforged.fml.event.lifecycle.InterModEnqueueEvent event){
        ArchiveLegacyConfig.guideFlag();
        if(net.neoforged.fml.ModList.get().isLoaded("theoneprobe"))net.neoforged.fml.InterModComms.sendTo("theoneprobe","getTheOneProbe",()->(Function<Object,Void>)ArchiveProbe::register);
    }
    private static Void register(Object top){
        try{
            Class<?> providerType=Class.forName("mcjty.theoneprobe.api.IProbeInfoProvider");
            Class<?> infoType=Class.forName("mcjty.theoneprobe.api.IProbeInfo");
            Method text=infoType.getMethod("text",Component.class);
            Method pos=Class.forName("mcjty.theoneprobe.api.IProbeHitData").getMethod("getPos");
            Object provider=Proxy.newProxyInstance(providerType.getClassLoader(),new Class<?>[]{providerType},(proxy,method,args)->{
                return switch(method.getName()){
                    case"getID"->AA.id("top_integration");
                    case"toString"->"Arcane Archives probe provider";
                    case"hashCode"->System.identityHashCode(proxy);
                    case"equals"->args!=null&&args.length==1&&proxy==args[0];
                    case"addProbeInfo"->{
                        if(args[3] instanceof Level level&&level.getBlockEntity((BlockPos)pos.invoke(args[5])) instanceof ArchiveBlockEntity tile)
                            for(var line:ArchiveDiagnostics.lines(tile))text.invoke(args[1],line);
                        yield null;
                    }
                    default->null;
                };
            });
            Class.forName("mcjty.theoneprobe.api.ITheOneProbe").getMethod("registerProvider",providerType).invoke(top,provider);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Could not register the installed The One Probe API",failure);}
        return null;
    }
}
