package com.aranaira.arcanearchives;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class ArchiveNetwork {
    public record Search(int menu,String text) implements CustomPacketPayload {
        public static final Type<Search> TYPE=new Type<>(AA.id("search"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Search> CODEC=StreamCodec.composite(
                ByteBufCodecs.VAR_INT,Search::menu,ByteBufCodecs.stringUtf8(64),Search::text,Search::new);
        @Override public Type<Search> type(){return TYPE;}
    }
    public static void init(IEventBus bus){bus.addListener(ArchiveNetwork::register);}
    public record View(net.minecraft.nbt.CompoundTag data) implements CustomPacketPayload {
        public static final Type<View> TYPE=new Type<>(AA.id("view"));
        public static final StreamCodec<RegistryFriendlyByteBuf,View> CODEC=StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG,View::data,View::new);
        @Override public Type<View> type(){return TYPE;}
    }
    public record Action(int action,int menu,int slot) implements CustomPacketPayload{
        public static final Type<Action> TYPE=new Type<>(AA.id("action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Action::action,ByteBufCodecs.VAR_INT,Action::menu,ByteBufCodecs.VAR_INT,Action::slot,Action::new);
        @Override public Type<Action> type(){return TYPE;}
    }
    public record Query(net.minecraft.nbt.CompoundTag data) implements CustomPacketPayload{
        public static final Type<Query> TYPE=new Type<>(AA.id("query"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Query> CODEC=StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG,Query::data,Query::new);
        @Override public Type<Query> type(){return TYPE;}
    }
    private static void register(RegisterPayloadHandlersEvent event){
        event.registrar("6").playToServer(Search.TYPE,Search.CODEC,(payload,ctx)->{
            var p=ctx.player();
            if(p.containerMenu instanceof ManifestMenu m&&m.containerId==payload.menu()&&m.stillValid(p))m.search(payload.text());
        });
        event.registrar("6").playToClient(View.TYPE,View.CODEC,(payload,ctx)->ViewMessages.receiver.accept(payload.data()));
        event.registrar("6").playToServer(Action.TYPE,Action.CODEC,(payload,ctx)->ArchiveActions.handle((net.minecraft.server.level.ServerPlayer)ctx.player(),payload.action(),payload.menu(),payload.slot()));
        event.registrar("6").playToServer(Query.TYPE,Query.CODEC,(payload,ctx)->ArchiveActions.query((net.minecraft.server.level.ServerPlayer)ctx.player(),payload.data()));
    }
    public static void search(int id,String query){PacketDistributor.sendToServer(new Search(id,query));}
    public static void action(int action,int menu,int slot){PacketDistributor.sendToServer(new Action(action,menu,slot));}
    public static void query(net.minecraft.nbt.CompoundTag tag){PacketDistributor.sendToServer(new Query(tag));}
    public static void view(net.minecraft.server.level.ServerPlayer player,net.minecraft.nbt.CompoundTag data){
        if(player.connection!=null)PacketDistributor.sendToPlayer(player,new View(data));
    }
}
