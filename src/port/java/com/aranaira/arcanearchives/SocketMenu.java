package com.aranaira.arcanearchives;
import java.util.*;
import java.lang.ref.WeakReference;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public final class SocketMenu extends AbstractContainerMenu {
    private final ItemStack socket;
    private final Level level;
    public final SimpleContainer gem;
    private record Link(WeakReference<ItemStack> parent,WeakReference<Level> level){}
    private static final Map<ItemStack,Link> PARENTS=Collections.synchronizedMap(new WeakHashMap<>());
    public SocketMenu(int id,Inventory inv){this(id,inv,ItemStack.EMPTY);}
    public SocketMenu(int id,Inventory inv,ItemStack socket){
        super(AA.SOCKET_MENU.get(),id);this.socket=socket;level=inv.player.level();
        gem=new SimpleContainer(1){@Override public void setChanged(){super.setChanged();save();}};
        if(!socket.isEmpty())gem.setItem(0,readGem(socket,inv.player));
        addSlot(new Slot(gem,0,81,3){
            @Override public boolean mayPlace(ItemStack s){return s.getItem() instanceof GemItem;}
            @Override public int getMaxStackSize(){return 1;}
        });
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addInventorySlot(inv,c+r*9+9,10+c*18,57+r*18);
        for(int c=0;c<9;c++)addInventorySlot(inv,c,10+c*18,115);
        addInventorySlot(inv,40,-23,115);
    }
    private void addInventorySlot(Inventory inv,int index,int x,int y){
        addSlot(new Slot(inv,index,x,y){@Override public boolean mayPickup(Player p){return getItem()!=socket;}});
    }
    private void save(){
        if(socket.isEmpty()||gem==null||level.isClientSide)return;
        CompoundTag tag=StackData.data(socket);tag.put("SocketGem",StackData.save(gem.getItem(0),level));StackData.data(socket,tag);
    }
    public static ItemStack readGem(ItemStack socket,Player p){
        CompoundTag tag=StackData.data(socket);
        ItemStack gem=StackData.load(tag.getCompound("SocketGem"),p.level());
        if(!gem.isEmpty())PARENTS.put(gem,new Link(new WeakReference<>(socket),new WeakReference<>(p.level())));
        return gem;
    }
    public static ItemStack equippedGem(ItemStack socket,Player player){
        return player.containerMenu instanceof SocketMenu menu&&menu.socket==socket?menu.gem.getItem(0):readGem(socket,player);
    }
    public static void saveGemToParent(ItemStack gem){
        var ref=PARENTS.get(gem);ItemStack parent=ref==null?null:ref.parent().get();Level level=ref==null?null:ref.level().get();if(parent==null||level==null)return;
        CompoundTag tag=StackData.data(parent);tag.put("SocketGem",StackData.save(gem,level));StackData.data(parent,tag);
    }
    @Override public void removed(Player p){save();super.removed(p);}
    @Override public void broadcastChanges(){
        if(!socket.isEmpty()){
            Player player=((Inventory)slots.get(1).container).player;
            if(!player.level().isClientSide){CompoundTag tag=StackData.data(socket);tag.put("SocketGem",StackData.save(gem.getItem(0),player.level()));StackData.data(socket,tag);}
        }super.broadcastChanges();
    }
    public static void open(ServerPlayer p,ItemStack s){p.openMenu(new SimpleMenuProvider((id,inv,player)->new SocketMenu(id,inv,s),Component.literal("Gem Socket")));}
    @Override public boolean stillValid(Player p){
        if(socket.isEmpty())return true;
        for(int i=0;i<36;i++)if(p.getInventory().getItem(i)==socket)return true;
        return p.getOffhandItem()==socket||com.aranaira.arcanearchives.integration.CuriosBridge.equipped(p).stream().anyMatch(stack->stack==socket);
    }
    @Override public boolean clickMenuButton(Player p,int button){
        if(button!=0||p.level().isClientSide||!stillValid(p))return false;
        var gems=Collections.newSetFromMap(new IdentityHashMap<ItemStack,Boolean>());gems.add(gem.getItem(0));
        for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.getItem() instanceof GemItem)gems.add(s);else if(s.is(AA.item("gemsocket")))gems.add(equippedGem(s,p));}
        for(var s:com.aranaira.arcanearchives.integration.CuriosBridge.equipped(p))if(s.is(AA.item("gemsocket")))gems.add(equippedGem(s,p));
        for(var s:gems)if(s.getItem() instanceof GemItem)GemItem.recharge(p,s);save();broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size()||!stillValid(p))return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.mayPickup(p))return ItemStack.EMPTY;
        ItemStack s=slot.getItem(),copy=s.copy();
        if(!moveItemStackTo(s,index==0?1:0,index==0?slots.size():1,false))return ItemStack.EMPTY;
        if(s.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();broadcastChanges();return copy;
    }
}
