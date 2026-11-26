package com.aranaira.arcanearchives;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
/** The original two rows: three ordered capacity modules and three optional modules. */
public final class UpgradeMenu extends AbstractContainerMenu {
    private final ArchiveBlockEntity tile;
    private final SimpleContainer modules;
    public UpgradeMenu(int id,Inventory inv){this(id,inv,null);}
    public UpgradeMenu(int id,Inventory inv,ArchiveBlockEntity tile){
        super(AA.UPGRADE_MENU.get(),id);this.tile=tile;
        modules=new SimpleContainer(6){
            @Override public ItemStack getItem(int slot){return tile==null?super.getItem(slot):slot<3?(slot<tile.upgradeStage?new ItemStack(AA.item(upgradeName(tile,slot))):ItemStack.EMPTY):tile.optionalModules[slot-3];}
            @Override public void setItem(int slot,ItemStack s){
                if(tile==null){super.setItem(slot,s);return;}
                if(slot<3){if(s.isEmpty()){if(!canRemove(slot))return;tile.upgradeStage=slot;}else{if(!canPlace(slot,s))return;tile.upgradeStage=slot+1;}tile.recalculateUpgrades();}
                else{if(!s.isEmpty()&&!canPlace(slot,s))return;tile.optionalModules[slot-3]=s.isEmpty()?ItemStack.EMPTY:s.copyWithCount(1);tile.voiding=hasOptional("devouring_charm");tile.locked=hasOptional("radiant_key");if(!tile.locked&&tile.inventory.getStackInSlot(0).isEmpty())tile.reference=ItemStack.EMPTY;tile.changed();}
            }
            @Override public ItemStack removeItem(int slot,int amount){var current=getItem(slot);if(amount<1||!canRemove(slot)||current.isEmpty())return ItemStack.EMPTY;var out=current.copyWithCount(1);setItem(slot,ItemStack.EMPTY);return out;}
            @Override public ItemStack removeItemNoUpdate(int slot){return removeItem(slot,1);}
        };
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,c+r*9+9,10+c*18,80+r*18));
        for(int c=0;c<9;c++)addSlot(new Slot(inv,c,10+c*18,138));
        for(int i=0;i<6;i++){final int n=i;addSlot(new Slot(modules,i,57+i%3*25,8+i/3*36){@Override public boolean mayPlace(ItemStack s){return canPlace(n,s);}@Override public boolean mayPickup(Player p){return canRemove(n);}@Override public int getMaxStackSize(){return 1;}});}
    }
    public static String upgradeName(ArchiveBlockEntity t,int slot){return slot==0?"matrix_brace":slot==1?(t.kind().equals("radiant_tank")?"containment_field":"material_interface"):"storage_shaped_quartz";}
    private boolean hasOptional(String name){for(var s:tile.optionalModules)if(s.is(AA.item(name)))return true;return false;}
    private boolean canPlace(int slot,ItemStack s){if(tile==null)return true;if(slot<3)return slot==tile.upgradeStage&&s.is(AA.item(upgradeName(tile,slot)));return s.is(AA.item("devouring_charm"))&&!hasOptional("devouring_charm")||s.is(AA.item("radiant_key"))&&!hasOptional("radiant_key");}
    private boolean canRemove(int slot){if(tile==null||slot>=3)return true;if(slot!=tile.upgradeStage-1)return false;int multiplier=slot==0?0:slot==1?2:5;return tile.kind().equals("radiant_tank")?tile.tank.getFluidAmount()<=16000*(multiplier+1):tile.inventory.getStackInSlot(0).getCount()<=32768*(multiplier+1);}
    public static void open(ServerPlayer p,ArchiveBlockEntity tile){if(tile.allowed(p))p.openMenu(new SimpleMenuProvider((id,inv,player)->new UpgradeMenu(id,inv,tile),Component.literal("Storage Upgrades")));}
    @Override public boolean stillValid(Player p){return tile==null||!tile.isRemoved()&&tile.allowed(p)&&p.distanceToSqr(tile.getBlockPos().getX()+.5,tile.getBlockPos().getY()+.5,tile.getBlockPos().getZ()+.5)<=64;}
    @Override public void clicked(int slot,int button,ContainerInput type,Player p){if(stillValid(p))super.clicked(slot,button,type,p);}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=42||!stillValid(p))return ItemStack.EMPTY;var slot=getSlot(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;var source=slot.getItem();var copy=source.copy();
        if(index<36){if(!moveItemStackTo(source,36,42,false))return ItemStack.EMPTY;if(source.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();}
        else{var candidate=copy.copy();if(!moveItemStackTo(candidate,0,36,false))return ItemStack.EMPTY;if(candidate.isEmpty())slot.remove(1);}
        broadcastChanges();return copy;
    }
}
