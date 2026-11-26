package com.aranaira.arcanearchives;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ArchiveNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(AA.id("main"),()->"6","6"::equals,"6"::equals);
    public record Search(int menu,String text) {
        static void encode(Search p,FriendlyByteBuf b){b.writeVarInt(p.menu);b.writeUtf(p.text,64);}
        static Search decode(FriendlyByteBuf b){return new Search(b.readVarInt(),b.readUtf(64));}
        static void handle(Search p,Supplier<NetworkEvent.Context> supplier){
            NetworkEvent.Context ctx=supplier.get();
            ctx.enqueueWork(()->{var sender=ctx.getSender();if(sender!=null&&sender.containerMenu instanceof ManifestMenu m&&m.containerId==p.menu&&m.stillValid(sender))m.search(p.text);});
            ctx.setPacketHandled(true);
        }
    }
    public record View(net.minecraft.nbt.CompoundTag data) {
        static void encode(View p,FriendlyByteBuf b){b.writeNbt(p.data);}
        static View decode(FriendlyByteBuf b){return new View(b.readNbt());}
        static void handle(View p,Supplier<NetworkEvent.Context> supplier){
            var ctx=supplier.get();ctx.enqueueWork(()->{if(p.data!=null)ViewMessages.receiver.accept(p.data);});ctx.setPacketHandled(true);
        }
    }
    public record Action(int action,int menu,int slot){
        static void encode(Action p,FriendlyByteBuf b){b.writeVarInt(p.action);b.writeVarInt(p.menu);b.writeVarInt(p.slot);}
        static Action decode(FriendlyByteBuf b){return new Action(b.readVarInt(),b.readVarInt(),b.readVarInt());}
        static void handle(Action p,Supplier<NetworkEvent.Context> supplier){
            var ctx=supplier.get();ctx.enqueueWork(()->ArchiveActions.handle(ctx.getSender(),p.action,p.menu,p.slot));ctx.setPacketHandled(true);
        }
    }
    public record Query(net.minecraft.nbt.CompoundTag data){
        static void encode(Query p,FriendlyByteBuf b){b.writeNbt(p.data);}
        static Query decode(FriendlyByteBuf b){return new Query(b.readNbt());}
        static void handle(Query p,Supplier<NetworkEvent.Context> supplier){var ctx=supplier.get();ctx.enqueueWork(()->ArchiveActions.query(ctx.getSender(),p.data));ctx.setPacketHandled(true);}
    }
    public static void init(){
        CHANNEL.registerMessage(0,Search.class,Search::encode,Search::decode,Search::handle,java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1,View.class,View::encode,View::decode,View::handle,java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2,Action.class,Action::encode,Action::decode,Action::handle,java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3,Query.class,Query::encode,Query::decode,Query::handle,java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void search(int id,String query){CHANNEL.sendToServer(new Search(id,query));}
    public static void action(int action,int menu,int slot){CHANNEL.sendToServer(new Action(action,menu,slot));}
    public static void query(net.minecraft.nbt.CompoundTag tag){CHANNEL.sendToServer(new Query(tag));}
    public static void view(net.minecraft.server.level.ServerPlayer player,net.minecraft.nbt.CompoundTag data){
        if(player.connection!=null)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new View(data));
    }
}
