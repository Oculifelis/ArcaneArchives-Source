package com.aranaira.arcanearchives;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
/** Vanilla crafting rules and result handling, backed by a saved nine-slot grid. */
public final class PersistentCraftingMenu extends CraftingMenu {
    private ArchiveBlockEntity tile;
    private boolean closing;
    private final SimpleContainer bookmarks=new SimpleContainer(5);
    private CompoundTag lastCrafted=new CompoundTag();
    public int bookmarkAvailability;
    public PersistentCraftingMenu(int id,Inventory inv){super(id,inv);layout();}
    public PersistentCraftingMenu(int id,Inventory inv,ArchiveBlockEntity t){
        super(id,inv,ContainerLevelAccess.create(t.getLevel(),t.getBlockPos()));layout();tile=t;t.busy=true;closing=true;
        for(int i=0;i<9;i++)getSlot(i+1).set(t.inventory.getStackInSlot(i).copy());
        closing=false;
        slotsChanged(getSlot(1).container);
    }
    private void layout(){
        for(int i=0;i<slots.size();i++){
            int x,y;
            if(i==0){x=136;y=42;}
            else if(i<10){x=24+(i-1)%3*18;y=24+(i-1)/3*18;}
            else if(i<37){x=23+(i-10)%9*18;y=115+(i-10)/9*18;}
            else{x=23+(i-37)*18;y=173;}
            if(i==0){var original=slots.get(i);var result=new ResultSlot(((Inventory)slots.get(10).container).player,(CraftingContainer)slots.get(1).container,original.container,original.getSlotIndex(),x,y);result.index=original.index;slots.set(i,result);}
            else slots.set(i,new PositionedSlot(slots.get(i),x,y));
        }
        for(int i=0;i<3;i++)addSlot(new Slot(bookmarks,i,174,16+i*26){@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}});
        for(int i=3;i<5;i++)addSlot(new Slot(bookmarks,i,-1000,-1000){@Override public boolean isActive(){return false;}@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}});
        addDataSlot(new DataSlot(){public int get(){return bookmarkAvailability;}public void set(int v){bookmarkAvailability=v;}});
    }
    @Override public void broadcastChanges(){
        if(tile!=null&&!closing){Player p=((Inventory)getSlot(10).container).player;bookmarkAvailability=0;for(int i=0;i<3;i++){var tag=tile.craftingBookmarks[i];bookmarks.setItem(i,CraftingBookmarks.display(p,tag));if(CraftingBookmarks.available(p,this,tag))bookmarkAvailability|=1<<i;}bookmarks.setItem(3,getSlot(0).getItem().copy());bookmarks.setItem(4,CraftingBookmarks.display(p,lastCrafted));}super.broadcastChanges();
    }
    @Override public void clicked(int slot,int button,ClickType type,Player p){
        if(!stillValid(p))return;
        if(slot>=46){
            if(slot>=49||tile==null||p.level().isClientSide||type!=ClickType.PICKUP&&type!=ClickType.QUICK_MOVE)return;int index=slot-46;boolean shift=type==ClickType.QUICK_MOVE;
            if(tile.craftingBookmarks[index].isEmpty())tile.craftingBookmarks[index]=(shift?lastCrafted:CraftingBookmarks.capture(p,this)).copy();
            else if(shift)tile.craftingBookmarks[index]=new CompoundTag();
            else if(CraftingBookmarks.craft(p,this,tile.craftingBookmarks[index]))lastCrafted=tile.craftingBookmarks[index].copy();tile.changed();broadcastChanges();return;
        }
        var before=slot==0&&tile!=null?CraftingBookmarks.capture(p,this):new CompoundTag();var grid=CraftingBookmarks.grid(this);super.clicked(slot,button,type,p);if(!before.isEmpty()&&!GemcutterCrafting.same(grid,CraftingBookmarks.grid(this)))lastCrafted=before;
    }
    @Override public MenuType<?> getType(){return AA.CRAFTING_MENU.get();}
    @Override public void slotsChanged(Container container){
        super.slotsChanged(container);
        if(tile!=null&&!closing){
            for(int i=0;i<9;i++)tile.inventory.setStackInSlot(i,getSlot(i+1).getItem().copy());
            tile.changed();
        }
    }
    @Override public boolean stillValid(Player p){return tile==null||!tile.isRemoved()&&tile.allowed(p)&&p.distanceToSqr(tile.getBlockPos().getX()+0.5,tile.getBlockPos().getY()+0.5,tile.getBlockPos().getZ()+0.5)<=64;}
    @Override public void removed(Player p){
        closing=true;
        if(tile!=null&&tile.isRemoved())for(int i=0;i<9;i++)getSlot(i+1).set(ItemStack.EMPTY);
        else if(tile!=null)for(int i=0;i<9;i++){tile.inventory.setStackInSlot(i,getSlot(i+1).getItem().copy());getSlot(i+1).set(ItemStack.EMPTY);}
        super.removed(p);if(tile!=null){tile.busy=false;tile.changed();ArchiveTracking.closed(p,tile);}
    }
}
