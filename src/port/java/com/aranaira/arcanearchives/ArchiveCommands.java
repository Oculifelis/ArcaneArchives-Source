package com.aranaira.arcanearchives;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/** Player diagnostics act only on the caller's own network, including offline members. */
@net.neoforged.fml.common.EventBusSubscriber(modid=AA.ID)
public final class ArchiveCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        var dispatcher=event.getDispatcher();
        dispatcher.register(Commands.literal("hive")
            .executes(ctx->describe(ctx.getSource().getPlayerOrException()))
            .then(Commands.literal("expel").then(Commands.argument("uuid",StringArgumentType.word()).executes(ctx->{
                var player=ctx.getSource().getPlayerOrException();java.util.UUID member;
                try{member=java.util.UUID.fromString(StringArgumentType.getString(ctx,"uuid"));}catch(IllegalArgumentException invalid){player.sendSystemMessage(Component.literal("Enter a member UUID from /hive."));return 0;}
                int writ=-1;for(int i=0;i<36;i++)if(player.getInventory().getItem(i).is(AA.item("writ_expulsion"))){writ=i;break;}
                if(writ<0){player.sendSystemMessage(Component.literal("An expulsion writ is required."));return 0;}
                if(!NetworkData.get(player.level()).expel(player.getUUID(),member)){player.sendSystemMessage(Component.literal("Only the owner can expel a member of their hive."));return 0;}
                player.getInventory().getItem(writ).shrink(1);player.sendSystemMessage(Component.literal("Member expelled."));return 1;
            }))));
        dispatcher.register(Commands.literal("rebuild_network").executes(ctx->{
            var p=ctx.getSource().getPlayerOrException();int count=NetworkData.get(p.level()).rebuild(ctx.getSource().getServer(),p.getUUID());
            p.sendSystemMessage(Component.literal("Revalidated "+count+" loaded archives in your network. Unloaded archives remain registered."));return count;
        }));
        dispatcher.register(Commands.literal("brazier").executes(ctx->{
            var p=ctx.getSource().getPlayerOrException();var stack=p.getMainHandItem();if(stack.isEmpty()){p.sendSystemMessage(Component.literal("Hold an item to inspect its routing."));return 0;}
            var source=new ArchiveBlockEntity(p.blockPosition(),AA.block("brazier_of_hoarding").defaultBlockState());source.setLevel(p.level());source.owner=p.getUUID();
            var targets=NetworkData.get(p.level()).brazierTargets(source,stack);
            p.sendSystemMessage(Component.literal("Brazier candidates: "+targets.size()));
            for(int i=0;i<Math.min(64,targets.size());i++){var tile=targets.get(i);p.sendSystemMessage(Component.literal(tile.kind()+" at "+tile.getBlockPos().toShortString()+": weight "+NetworkData.routingWeight(tile,stack)));}
            return targets.size();
        }));
    }
    private static int describe(ServerPlayer player){
        var data=NetworkData.get(player.level());var owner=data.network(player.getUUID());var members=data.members(player.getUUID());
        player.sendSystemMessage(Component.literal("Network owner: "+owner+"; "+members.size()+" member(s); "+data.nodes(player.getUUID()).size()+" known archives."));
        for(var member:members)player.sendSystemMessage(Component.literal("Member: "+member));
        return members.size();
    }
}
