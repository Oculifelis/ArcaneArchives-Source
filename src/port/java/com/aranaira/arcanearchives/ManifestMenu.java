package com.aranaira.arcanearchives;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Virtual display slots never own items. Every transfer revalidates the live network. */
public class ManifestMenu extends AbstractContainerMenu {
    private final Player player;
    public static final int DISPLAY_SLOTS=90;
    private final SimpleContainer display = new SimpleContainer(DISPLAY_SLOTS);
    private List<NetworkData.Entry> entries = List.of();
    private String query = "";
    private java.util.Set<String> localizedItems=java.util.Set.of();
    public final long[] counts=new long[DISPLAY_SLOTS];
    public record LocationInfo(int page,ItemStack template,List<String> lines){}
    public final Map<Integer,LocationInfo> hoverLocations=new HashMap<>();
    public List<String> locationLines(int slot){var info=hoverLocations.get(slot);return info!=null&&info.page()==page&&ItemStack.isSameItemSameComponents(info.template(),display.getItem(slot))?info.lines():List.of();}
    /** Legacy location tracking is the default; retrieval is an explicit additional mode. */
    public int mode,sort,descending;
    public int page, pages=1;
    public ManifestMenu(int id, Inventory inventory) {
        super(AA.MANIFEST_MENU.get(),id); player=inventory.player;
        for(int i=0;i<DISPLAY_SLOTS;i++) addSlot(new Slot(display,i,12+(i%9)*18,30+(i/9)*18) {
            @Override public boolean isActive(){return false;} // Drawn/clipped in the original six-pixel scrolling viewport.
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player p) { return false; }
        });
        for(int r=0;r<3;r++) for(int c=0;c<9;c++) addSlot(new Slot(inventory,c+r*9+9,19+c*18,243+r*18){@Override public boolean isActive(){return mode==1;}});
        for(int c=0;c<9;c++) addSlot(new Slot(inventory,c,19+c*18,301){@Override public boolean isActive(){return mode==1;}});
        addDataSlot(new DataSlot() { public int get(){return page;} public void set(int v){page=v;} });
        addDataSlot(new DataSlot() { public int get(){return pages;} public void set(int v){pages=v;} });
        addDataSlot(new DataSlot(){public int get(){return mode;}public void set(int v){mode=v;}});
        addDataSlot(new DataSlot(){public int get(){return sort;}public void set(int v){sort=v;}});
        addDataSlot(new DataSlot(){public int get(){return descending;}public void set(int v){descending=v;}});
        for(int i=0;i<DISPLAY_SLOTS;i++)for(int part=0;part<4;part++){
            final int index=i,shift=part*16;
            addDataSlot(new DataSlot(){public int get(){return (int)(counts[index]>>>shift)&65535;}public void set(int v){counts[index]=(counts[index]&~(65535L<<shift))|((long)(v&65535)<<shift);}});
        }
        refresh();
    }
    public static void open(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, player)->new ManifestMenu(id,inv),Component.literal("Manifest")));
    }
    public void search(String input) { search(input,java.util.Set.of()); }
    public void search(String input,java.util.Set<String> ids){localizedItems=java.util.Set.copyOf(ids);query=input.substring(0,Math.min(64,input.length())).toLowerCase(Locale.ROOT);page=0;broadcastChanges();}
    public void refresh() {
        if (player.level().isClientSide()) return;
        Comparator<NetworkData.Entry> comparator=sort==0?Comparator.comparing(e->e.stack().getHoverName().getString(),String.CASE_INSENSITIVE_ORDER):Comparator.comparingLong(NetworkData.Entry::count);
        if(descending!=0)comparator=comparator.reversed();
        entries=NetworkData.get(player.level()).entries(((net.minecraft.server.level.ServerLevel)player.level()).getServer(),player.getUUID()).stream()
                .filter(e->matches(e.stack(),query)||localizedItems.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.stack().getItem()).toString())).sorted(comparator).toList();
        pages=Math.max(1,Math.max(0,(entries.size()+8)/9-9)*3+1); page=Math.max(0,Math.min(page,pages-1));
        for(int i=0;i<DISPLAY_SLOTS;i++) {
            int n=page/3*9+i;
            counts[i]=n>=entries.size()?0:entries.get(n).count();
            display.setItem(i,n>=entries.size()?ItemStack.EMPTY:entries.get(n).stack().copyWithCount(1));
        }
    }
    private static boolean matches(ItemStack stack,String query){
        if(query.startsWith("@"))return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().contains(query.substring(1));
        if(query.startsWith("$"))return stack.typeHolder().tags().anyMatch(tag->tag.location().toString().contains(query.substring(1)));
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }
    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
    public void scrollTo(int row){page=Math.max(0,Math.min(pages-1,row));broadcastChanges();}
    public void hover(ServerPlayer p,int slot,int shownPage){
        if(p!=player||!stillValid(p)||shownPage!=page||slot<0||slot>=DISPLAY_SLOTS||display.getItem(slot).isEmpty())return;
        var template=display.getItem(slot);var response=new net.minecraft.nbt.CompoundTag();response.putString("Kind","manifest_locations");response.putInt("Menu",containerId);response.putInt("Page",page);response.putInt("Slot",slot);response.put("Stack",StackData.save(template.copyWithCount(1),p.level()));var lines=new net.minecraft.nbt.ListTag();
        for(var detail:NetworkData.get(p.level()).locationDetails(((net.minecraft.server.level.ServerLevel)p.level()).getServer(),p.getUUID(),template)){var node=detail.node();String dimension=node.dimension().equals(p.level().dimension().identifier())?"":" ["+node.dimension()+"]";lines.add(net.minecraft.nbt.StringTag.valueOf(detail.label()+dimension+" ("+node.pos().toShortString()+"): "+detail.count()));}
        response.put("Items",lines);ArchiveNetwork.view(p,response);
    }
    @Override public boolean clickMenuButton(Player p,int button) {
        if(p!=player||!stillValid(p)||p.level().isClientSide()) return false;
        if(button==0)page=Math.max(0,page-1);
        else if(button==1)page=Math.min(pages-1,page+1);
        else if(button==2){var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Kind","tracking_clear");ArchiveNetwork.view((ServerPlayer)p,tag);}
        else if(button==3)mode=1-mode;
        else if(button==4){sort=1-sort;page=0;}
        else if(button==5){descending=1-descending;page=0;}
        else if(button==6){}
        else return false;
        refresh();broadcastChanges();return true;
    }
    private void track(int index,boolean remove,boolean shift,Player p){
        var stack=display.getItem(index);if(stack.isEmpty())return;
        trackTemplate((ServerPlayer)p,stack,remove,shift,false);
    }
    public static void trackTemplate(ServerPlayer p,ItemStack stack,boolean remove,boolean shift,boolean closeAny){
        var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Kind",remove?"tracking_remove":"tracking_add");
        // Native serialization includes enchantments, potion contents and custom
        // components, so tracking two variants of the same item stays distinct.
        tag.putString("Item",StackData.save(stack.copyWithCount(1),p.level()).toString());
        tag.put("Stack",StackData.save(stack.copyWithCount(1),p.level()));
        tag.putString("Label",stack.getHoverName().getString());
        tag.putBoolean("Shift",shift);
        tag.putBoolean("CloseAny",closeAny);
        var targets=new net.minecraft.nbt.ListTag();
        for(var node:NetworkData.get(p.level()).locations(((net.minecraft.server.level.ServerLevel)p.level()).getServer(),p.getUUID(),stack)){
            var n=new net.minecraft.nbt.CompoundTag();n.putString("Dimension",node.dimension().toString());n.putLong("Position",node.pos().asLong());targets.add(n);
        }
        tag.put("Targets",targets);ArchiveNetwork.view((ServerPlayer)p,tag);
    }
    @Override public boolean stillValid(Player p) {
        if (p != player) return false;
        if (ArchiveActions.hasManifest(p)) return true;
        if (p.level().isClientSide()) return true;
        for (var node:NetworkData.get(p.level()).nodes(p.getUUID())) if(node.dimension().equals(p.level().dimension().identifier())
                && node.pos().distToCenterSqr(p.position())<=64 && p.level().hasChunkAt(node.pos()) && p.level().getBlockState(node.pos()).is(AA.block("lectern_manifest"))) return true;
        return false;
    }
    @Override public void clicked(int slot,int button,ContainerInput type,Player p) {
        if (slot<0 || slot>=DISPLAY_SLOTS) { super.clicked(slot,button,type,p); return; }
        if(p.level().isClientSide() || !stillValid(p)) return;
        if(type!=ContainerInput.PICKUP && type!=ContainerInput.QUICK_MOVE) return;
        if(mode==0){track(slot,button==1,type==ContainerInput.QUICK_MOVE,p);return;}
        NetworkData data=NetworkData.get(p.level());
        ItemStack carried=getCarried();
        if(!carried.isEmpty()) {
            setCarried(data.insert(((net.minecraft.server.level.ServerLevel)p.level()).getServer(),p.getUUID(),carried,null));
        } else if(!display.getItem(slot).isEmpty()) {
            ItemStack selected=display.getItem(slot).copy();
            int amount=button==1?1:selected.getMaxStackSize();
            ItemStack extracted=data.extract(((net.minecraft.server.level.ServerLevel)p.level()).getServer(),p.getUUID(),selected,amount);
            if(type==ContainerInput.QUICK_MOVE) {
                if(!p.getInventory().add(extracted)) {
                    ItemStack rest=data.insert(((net.minecraft.server.level.ServerLevel)p.level()).getServer(),p.getUUID(),extracted,null);
                    if(!rest.isEmpty()) p.drop(rest,false);
                }
            } else setCarried(extracted);
        }
        refresh(); broadcastChanges();
    }
    @Override public ItemStack quickMoveStack(Player p,int index) {
        if(index<DISPLAY_SLOTS || index>=slots.size() || !stillValid(p)||mode!=1) return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        ItemStack previous=slot.getItem().copy();
        ItemStack remainder=NetworkData.get(p.level()).insert(((net.minecraft.server.level.ServerLevel)p.level()).getServer(),p.getUUID(),previous,null);
        slot.set(remainder); refresh(); return previous;
    }
}
