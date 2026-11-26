package com.aranaira.arcanearchives.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import com.aranaira.arcanearchives.StackData;

/** Session-local tracking, cleared on connection changes rather than retained across worlds. */
public final class ArchiveViews {
    public record Target(String dimension,BlockPos position,String label) {}
    private record Group(ItemStack template,List<Target> targets){}
    private static final Map<String,Group> groups=new LinkedHashMap<>();
    private static Object connection;
    public static void tick(){
        Object current=Minecraft.getInstance().getConnection();
        if(current!=connection){groups.clear();BrazierRange.clear();ArchiveRecipeClient.clear();connection=current;}
        ArchiveRecipeClient.tick();
    }
    public static void accept(CompoundTag tag){
        tick();String kind=tag.getString("Kind");
        if(kind.startsWith("recipes_")){ArchiveRecipeClient.accept(tag);return;}
        if(kind.startsWith("range_")){BrazierRange.accept(tag,kind.equals("range_toggle"));return;}
        if(kind.equals("chest_state")){var p=Minecraft.getInstance().player;if(p!=null&&p.containerMenu instanceof com.aranaira.arcanearchives.ArchiveMenu menu&&menu.containerId==tag.getInt("Menu"))menu.chestName=tag.getString("Name");return;}
        if(kind.equals("brazier_state")){var p=Minecraft.getInstance().player;if(p!=null&&p.containerMenu instanceof com.aranaira.arcanearchives.ArchiveMenu menu&&menu.station==3&&menu.containerId==tag.getInt("Menu")){menu.brazierPosition=tag.getLong("Position");menu.brazierDimension=tag.getString("Dimension");}return;}
        if(kind.equals("manifest_locations")){var p=Minecraft.getInstance().player;var level=Minecraft.getInstance().level;if(p!=null&&level!=null&&p.containerMenu instanceof com.aranaira.arcanearchives.ManifestMenu menu&&menu.containerId==tag.getInt("Menu")&&menu.page==tag.getInt("Page")){int slot=tag.getInt("Slot");if(slot<0||slot>=com.aranaira.arcanearchives.ManifestMenu.DISPLAY_SLOTS)return;var stack=StackData.load(tag.getCompound("Stack"),level);if(!ItemStack.isSameItemSameTags(stack,menu.getSlot(slot).getItem()))return;var items=tag.getList("Items",Tag.TAG_STRING);var lines=new ArrayList<String>();for(int i=0;i<Math.min(16,items.size());i++)lines.add(items.getString(i));menu.hoverLocations.put(slot,new com.aranaira.arcanearchives.ManifestMenu.LocationInfo(menu.page,stack,List.copyOf(lines)));}return;}
        if(kind.equals("tracking_visited")){
            var position=BlockPos.of(tag.getLong("Position"));String dimension=tag.getString("Dimension");
            groups.replaceAll((key,group)->new Group(group.template(),group.targets().stream().filter(t->!t.position().equals(position)||!t.dimension().equals(dimension)).toList()));
            groups.values().removeIf(group->group.targets().isEmpty());return;
        }
        if(kind.equals("guidebook")){GuideScreen.open();return;}
        if(kind.equals("tracking_clear")){groups.clear();return;}
        String key=tag.getString("Item");
        if(kind.equals("tracking_remove")){groups.remove(key);return;}
        if(kind.equals("tracking_add")){
            List<Target> list=new ArrayList<>();ListTag targets=tag.getList("Targets",Tag.TAG_COMPOUND);
            for(int i=0;i<targets.size()&&i<1024;i++){var n=targets.getCompound(i);list.add(new Target(n.getString("Dimension"),BlockPos.of(n.getLong("Position")),tag.getString("Label")));}
            if(Minecraft.getInstance().level==null)return;
            ItemStack template=StackData.load(tag.getCompound("Stack"),Minecraft.getInstance().level);
            if(groups.size()<64||groups.containsKey(key))groups.put(key,new Group(template,List.copyOf(list)));
            var mc=Minecraft.getInstance();boolean shift=tag.getBoolean("Shift");
            if(mc.player!=null&&(mc.screen instanceof ArchiveClient.ManifestScreen||tag.getBoolean("CloseAny")&&mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)&&(ClientPreferences.closeWithoutShift?!shift:shift))mc.player.closeContainer();
        }
    }
    public static List<Target> targets(){tick();return groups.values().stream().flatMap(g->g.targets().stream()).distinct().toList();}
    public static boolean matches(ItemStack stack){tick();return !stack.isEmpty()&&groups.values().stream().anyMatch(g->ItemStack.isSameItemSameTags(stack,g.template()));}
    public static String compact(long count){
        if(count<1000)return Long.toString(count);
        if(count<1_000_000)return String.format(Locale.ROOT,"%.1fk",count/1000.0);
        if(count<1_000_000_000)return String.format(Locale.ROOT,"%.1fM",count/1_000_000.0);
        return String.format(Locale.ROOT,"%.1fG",count/1_000_000_000.0);
    }
}
