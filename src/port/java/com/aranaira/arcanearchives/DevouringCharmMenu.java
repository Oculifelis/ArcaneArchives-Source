package com.aranaira.arcanearchives;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
/** The charm's front consumes items/fluids; its back holds six non-consuming pickup filters. */
public final class DevouringCharmMenu extends AbstractContainerMenu {
    private final ItemStack charm;
    private final Player viewer;
    public int flipped;
    private boolean changing;
    private final SimpleContainer filters=new SimpleContainer(6);
    private final SimpleContainer front=new SimpleContainer(7){@Override public void setChanged(){super.setChanged();consume();}};
    public DevouringCharmMenu(int id,Inventory inv){this(id,inv,ItemStack.EMPTY);}
    public DevouringCharmMenu(int id,Inventory inv,ItemStack charm){
        super(AA.CHARM_MENU.get(),id);this.charm=charm;viewer=inv.player;
        var saved=(charm.isEmpty()?new CompoundTag():StackData.data(charm)).getList("CharmFilters",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(6,saved.size());i++)filters.setItem(i,StackData.load(saved.getCompound(i),viewer.level()));
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addPlayer(inv,c+r*9+9,10+c*18,165+r*18);
        for(int c=0;c<9;c++)addPlayer(inv,c,10+c*18,223);
        addSlot(new Slot(front,0,82,71){@Override public boolean mayPlace(ItemStack s){return CharmFluids.accepts(s);}@Override public boolean isActive(){return flipped==0;}@Override public int getMaxStackSize(){return 1;}});
        for(int i=0;i<6;i++)addSlot(new Slot(front,i+1,64+i%3*18,113+i/3*18){@Override public boolean mayPlace(ItemStack s){return !s.is(AA.item("devouring_charm"));}@Override public boolean isActive(){return flipped==0;}});
        for(int i=0;i<6;i++)addSlot(new Slot(filters,i,58+i%3*24,99+i/3*24){@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}@Override public boolean isActive(){return flipped!=0;}});
        addDataSlot(new DataSlot(){public int get(){return flipped;}public void set(int v){flipped=v;}});
    }
    private void addPlayer(Inventory inv,int index,int x,int y){addSlot(new Slot(inv,index,x,y){@Override public boolean mayPickup(Player p){return !getItem().is(AA.item("devouring_charm"));}});}
    private void consume(){
        if(viewer==null||viewer.level().isClientSide||changing)return;changing=true;
        try{if(!front.getItem(0).isEmpty())front.setItem(0,CharmFluids.drain(front.getItem(0)));for(int i=1;i<7;i++)front.setItem(i,ItemStack.EMPTY);}finally{changing=false;}
    }
    private void save(){if(charm.isEmpty()||viewer.level().isClientSide)return;var tag=StackData.data(charm);var list=new ListTag();for(int i=0;i<6;i++)list.add(StackData.save(filters.getItem(i),viewer.level()));tag.put("CharmFilters",list);StackData.data(charm,tag);viewer.getInventory().setChanged();}
    public static boolean matches(ItemStack charm,ItemStack candidate,Player p){var list=StackData.data(charm).getList("CharmFilters",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(6,list.size());i++){var filter=StackData.load(list.getCompound(i),p.level());if(!filter.isEmpty()&&ItemStack.isSameItemSameTags(filter,candidate))return true;}return false;}
    public static void open(ServerPlayer p,ItemStack s){p.openMenu(new SimpleMenuProvider((id,inv,player)->new DevouringCharmMenu(id,inv,s),Component.literal("Devouring Charm")));}
    @Override public boolean stillValid(Player p){if(charm.isEmpty())return true;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i)==charm)return true;return false;}
    @Override public boolean clickMenuButton(Player p,int button){if(button!=0||!stillValid(p)||p.level().isClientSide)return false;flipped=1-flipped;broadcastChanges();return true;}
    @Override public void clicked(int slot,int button,ClickType type,Player p){
        if(!stillValid(p))return;
        if(slot>=43&&slot<49){if(flipped!=0&&!p.level().isClientSide&&(type==ClickType.PICKUP||type==ClickType.CLONE)){filters.setItem(slot-43,button==2?ItemStack.EMPTY:getCarried().copyWithCount(1));save();broadcastChanges();}return;}
        if(slot>=36&&slot<43&&flipped!=0)return;super.clicked(slot,button,type,p);
    }
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=43||flipped!=0||!stillValid(p))return ItemStack.EMPTY;var slot=getSlot(index);if(!slot.mayPickup(p)||!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var original=stack.copy();
        if(index<36){if(CharmFluids.accepts(stack)&&getSlot(36).getItem().isEmpty()){if(!moveItemStackTo(stack,36,37,false))return ItemStack.EMPTY;}else if(!moveItemStackTo(stack,37,43,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,36,true))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();broadcastChanges();return original;
    }
    @Override public void removed(Player p){save();if(!p.level().isClientSide){var remaining=front.removeItemNoUpdate(0);if(!p.getInventory().add(remaining))p.drop(remaining,false);}super.removed(p);}
}
