package com.aranaira.arcanearchives;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Every key action resolves inventory and authorization again on the server. */
public final class ArchiveActions {
    public static void query(ServerPlayer player,net.minecraft.nbt.CompoundTag tag){
        if(player==null||tag==null)return;
        if(tag.getString("Kind").equals("preferences")){NetworkData.get(player.level()).preferences(player.getUUID(),tag.getBoolean("TrovesDispense"),tag.getBoolean("NoNew"));return;}
        if(player==null||tag==null||tag.getInt("Menu")!=player.containerMenu.containerId||!player.containerMenu.stillValid(player))return;
        if(tag.getString("Kind").equals("search")&&player.containerMenu instanceof ManifestMenu menu){
            var items=tag.getList("Items",net.minecraft.nbt.Tag.TAG_STRING);java.util.Set<String> ids=new java.util.HashSet<>();
            for(int i=0;i<Math.min(1024,items.size());i++)ids.add(items.getString(i));
            menu.search(tag.getString("Query"),ids);
        }else if(tag.getString("Kind").equals("track")&&hasManifest(player)){
            var template=StackData.load(tag.getCompound("Stack"),player.level());
            if(!template.isEmpty())ManifestMenu.trackTemplate(player,template.copyWithCount(1),false,player.isShiftKeyDown(),true);
        }else if(player.containerMenu instanceof ArchiveMenu menu){
            switch(tag.getString("Kind")){
                case"brazier_radius"->menu.radius(player,tag.getInt("Value"));
                case"chest_name"->menu.name(player,tag.getString("Value"));
                case"gct_select"->menu.selectRecipe(player,tag.getString("Value"));
            }
        }else if(tag.getString("Kind").equals("manifest_scroll")&&player.containerMenu instanceof ManifestMenu menu){
            menu.scrollTo(tag.getInt("Value"));
        }else if(tag.getString("Kind").equals("manifest_hover")&&player.containerMenu instanceof ManifestMenu menu){
            menu.hover(player,tag.getInt("Value"),tag.getInt("Page"));
        }
    }
    public static boolean hasManifest(net.minecraft.world.entity.player.Player player){
        if(!ArchiveConfig.MANIFEST_PRESENCE.get())return true;
        for(int i=0;i<36;i++)if(player.getInventory().getItem(i).is(AA.item("manifest")))return true;
        return player.getOffhandItem().is(AA.item("manifest"));
    }
    public static void handle(ServerPlayer player,int action,int menu,int slot){
        if(player==null)return;
        if(action==0&&hasManifest(player)){
            if(player.isShiftKeyDown()){var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Kind","tracking_clear");ArchiveNetwork.view(player,tag);}
            else ManifestMenu.open(player);
        }else if(action==1){
            for(var stack:com.aranaira.arcanearchives.integration.CuriosBridge.equipped(player))
                if(stack.is(AA.item("gemsocket"))){SocketMenu.open(player,stack);return;}
            for(int i=0;i<36;i++){var stack=player.getInventory().getItem(i);if(stack.is(AA.item("gemsocket"))){SocketMenu.open(player,stack);return;}}
            if(player.getOffhandItem().is(AA.item("gemsocket")))SocketMenu.open(player,player.getOffhandItem());
        }else if(action==2&&hasManifest(player)&&player.containerMenu.containerId==menu&&player.containerMenu.stillValid(player)
                &&slot>=0&&slot<player.containerMenu.slots.size()){
            ItemStack template=player.containerMenu.getSlot(slot).getItem();
            if(!template.isEmpty())ManifestMenu.trackTemplate(player,template,false,player.isShiftKeyDown(),true);
        }
    }
}
