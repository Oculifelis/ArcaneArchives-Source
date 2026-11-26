package com.aranaira.arcanearchives;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.*;

public class ArchiveMenu extends AbstractContainerMenu {
    private final ArchiveBlockEntity tile;
    private final Player viewer;
    private java.util.List<GemcutterRecipes.Recipe> recipeSnapshot;
    private int snapshotEpoch=ArchiveRecipes.generation();
    private java.util.UUID lastNetwork;
    private final SimpleContainer recipes=new SimpleContainer(8);
    private final SimpleContainer bulkDisplay=new SimpleContainer(54);
    public final int[] counts=new int[54];
    public int station;
    public int radius=150,subnetworkOnly;
    public static final int GCT_OUTPUT=18,GCT_CHOICES=19,GCT_PLAYER=26;
    public int recipePage,recipePages=1,selectedRecipeIndex,recipeAvailability,chestNoNew;
    public String chestName="";
    public long brazierPosition=Long.MIN_VALUE;public String brazierDimension="";
    private boolean sentBrazier;
    private String sentName;
    private java.util.List<ItemStack> lastInputs=java.util.List.of();
    private int lastPage=-1,lastSelected=-1;
    public java.util.List<GemcutterRecipes.Recipe> stationRecipes(){return tile==null?ArchiveRecipes.client():recipeSnapshot;}
    public GemcutterRecipes.Recipe displayedRecipe(int slot){int index=slot==GCT_OUTPUT?selectedRecipeIndex:recipePage*7+slot-GCT_CHOICES;return slot<GCT_OUTPUT||slot>=GCT_PLAYER||index<0||index>=stationRecipes().size()?null:stationRecipes().get(index);}
    public boolean recipeAvailable(int choice){return (recipeAvailability&(1<<choice))!=0;}
    public ArchiveMenu(int id, Inventory inv) { this(id, inv, null); }
    public ArchiveMenu(int id, Inventory inv, ArchiveBlockEntity tile) {
        this(id,inv,tile,tile==null?0:tile.menuStation());
    }
    public static ArchiveMenu client(int id,Inventory inv,int station){return new ArchiveMenu(id,inv,null,station);}
    private ArchiveMenu(int id,Inventory inv,ArchiveBlockEntity tile,int initialStation){
        super(AA.STORAGE_MENU.get(),id); this.tile=tile;viewer=inv.player;recipeSnapshot=ArchiveRecipes.forLevel(viewer.level());recipePages=Math.max(1,(recipeSnapshot.size()+6)/7);
        if(tile!=null)for(int i=0;i<recipeSnapshot.size();i++)if(ArchiveRecipes.key(recipeSnapshot.get(i).id()).equals(tile.gctRecipe)){selectedRecipeIndex=i;recipePage=i/7;break;}
        station=initialStation;
        IItemHandler handler = tile == null ? new ItemStackHandler(station==1?18:station==2?54:9) : tile.inventory;
        addDataSlot(new DataSlot(){public int get(){return station;}public void set(int v){station=v;}});
        addDataSlot(new DataSlot(){public int get(){return tile==null?radius:tile.routingRadius;}public void set(int v){radius=v;}});
        addDataSlot(new DataSlot(){public int get(){return tile==null?subnetworkOnly:tile.subnetworkOnly?1:0;}public void set(int v){subnetworkOnly=v;}});
        addDataSlot(new DataSlot(){public int get(){return recipePage;}public void set(int v){recipePage=v;}});
        addDataSlot(new DataSlot(){public int get(){return recipePages;}public void set(int v){recipePages=v;}});
        addDataSlot(new DataSlot(){public int get(){return selectedRecipeIndex;}public void set(int v){selectedRecipeIndex=v;}});
        addDataSlot(new DataSlot(){public int get(){return recipeAvailability;}public void set(int v){recipeAvailability=v;}});
        addDataSlot(new DataSlot(){public int get(){return tile==null?chestNoNew:tile.noNew?1:0;}public void set(int v){chestNoNew=v;}});
        if(station==3)return;
        if(station==1){
            for(int i=0;i<18;i++)addSlot(new SlotItemHandler(handler,i,23+i%9*18,105+i/9*18));
            addSlot(new Slot(recipes,0,95,18){@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}});
            for(int i=0;i<7;i++)addSlot(new Slot(recipes,i+1,41+i*18,70){@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}});
            addPlayerSlots(inv,23,166);refreshRecipes();return;
        }
        for(int i=0;i<54;i++){final int n=i;addDataSlot(new DataSlot(){public int get(){return counts[n];}public void set(int v){counts[n]=v;}});}
        for (int i = 0; i < 54; i++) {
            final int slot=i;
            if(station==2)addSlot(new Slot(bulkDisplay,i,16+(i%9)*18,16+(i/9)*18){
                @Override public boolean mayPlace(ItemStack s){return false;}
                @Override public boolean mayPickup(Player p){return false;}
            });
            else if (i < handler.getSlots()) addSlot(new SlotItemHandler(handler,i,8+(i%9)*18,18+(i/9)*18));
            else addSlot(new Slot(station==1?recipes:new SimpleContainer(36),Math.max(0,i-18),8+(i%9)*18,18+(i/9)*18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player p) { return false; }
            });
        }
        addPlayerSlots(inv,station==2?16:8,station==2?142:140);
        refreshRecipes();
        refreshBulk();
    }
    protected void addPlayerSlots(Inventory inv,int x,int y) {
        for(int r=0;r<3;r++) for(int c=0;c<9;c++) addSlot(new Slot(inv,c+r*9+9,x+c*18,y+r*18));
        for(int c=0;c<9;c++) addSlot(new Slot(inv,c,x+c*18,y+58));
    }
    @Override public boolean stillValid(Player p) { return tile==null || (!tile.isRemoved() && tile.allowed(p) && p.distanceToSqr(tile.getBlockPos().getX()+0.5,tile.getBlockPos().getY()+0.5,tile.getBlockPos().getZ()+0.5) <= 64); }
    @Override public void removed(Player p){super.removed(p);ArchiveTracking.closed(p,tile);}
    @Override public ItemStack quickMoveStack(Player p,int index) {
        if(index<0 || index>=slots.size()||!stillValid(p)) return ItemStack.EMPTY;
        if(station==1){
            if(index==GCT_OUTPUT){craftQuick(p);return ItemStack.EMPTY;}if(index>=GCT_CHOICES&&index<GCT_PLAYER)return ItemStack.EMPTY;
            Slot source=slots.get(index);ItemStack result=source.getItem().copy();
            if(!moveItemStackTo(source.getItem(),index<18?GCT_PLAYER:0,index<18?slots.size():18,false))return ItemStack.EMPTY;
            if(source.getItem().isEmpty())source.set(ItemStack.EMPTY);else source.setChanged();refreshRecipes();return result;
        }
        if(station==2&&index<54){clicked(index,0,ContainerInput.QUICK_MOVE,p);return ItemStack.EMPTY;}
        if(station==2&&tile!=null&&index>=54&&stillValid(p)){
            Slot source=slots.get(index);ItemStack original=source.getItem().copy();
            ItemStack rest=ItemHandlerHelper.insertItemStacked(tile.inventory,original,false);
            source.set(rest);refreshBulk();return original;
        }
        Slot slot=slots.get(index);
        if(!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original=slot.getItem().copy(), stack=slot.getItem();
        if(!moveItemStackTo(stack,index<54?54:0,index<54?slots.size():54,false)) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }
    @Override public void clicked(int index,int button,ContainerInput type,Player p){
        if(station==2&&index>=0&&index<54){
            if(tile==null||!stillValid(p)||type!=ContainerInput.PICKUP&&type!=ContainerInput.QUICK_MOVE)return;
            ItemStack carried=getCarried();
            if(!carried.isEmpty()){
                int wanted=button==1?1:carried.getCount();ItemStack remainder=tile.inventory.insertItem(index,carried.copyWithCount(wanted),false);
                carried.shrink(wanted-remainder.getCount());setCarried(carried);
            }else{
                ItemStack stored=tile.inventory.getStackInSlot(index);
                int amount=button==1?(int)Math.ceil(Math.min(stored.getCount(),stored.getMaxStackSize())/2.0):stored.getMaxStackSize();
                ItemStack extracted=tile.inventory.extractItem(index,amount,false);
                if(type==ContainerInput.QUICK_MOVE){
                    if(!p.getInventory().add(extracted)){ItemStack rest=tile.inventory.insertItem(index,extracted,false);if(!rest.isEmpty())p.drop(rest,false);}
                }else setCarried(extracted);
            }refreshBulk();broadcastChanges();return;
        }
        if(station!=1||index<GCT_OUTPUT||index>=GCT_PLAYER){super.clicked(index,button,type,p);return;}
        if(p.level().isClientSide()||tile==null||!stillValid(p)||type!=ContainerInput.PICKUP&&type!=ContainerInput.QUICK_MOVE)return;
        if(index>=GCT_CHOICES){int selected=recipePage*7+index-GCT_CHOICES;if(selected<recipeSnapshot.size()){selectedRecipeIndex=selected;tile.gctRecipe=ArchiveRecipes.key(recipeSnapshot.get(selected).id());tile.changed();refreshRecipes();broadcastChanges();}return;}
        if(type==ContainerInput.QUICK_MOVE)craftQuick(p);else{craft(p,false);broadcastChanges();}
    }
    private void craftQuick(Player p){if(tile==null||!stillValid(p))return;for(int batch=0;batch<64;batch++){var before=GemcutterCrafting.inputs(tile,p).stream().map(ItemStack::copy).toList();if(!craft(p,true)||GemcutterCrafting.same(before,GemcutterCrafting.inputs(tile,p)))break;}broadcastChanges();}
    private boolean craft(Player p,boolean quick){
        if(tile==null||!stillValid(p))return false;GemcutterRecipes.Recipe recipe=displayedRecipe(GCT_OUTPUT);if(recipe==null||!GemcutterCrafting.permitted(recipe,p))return false;
        ItemStack result=recipe.result(),carried=getCarried();
        if(recipe.creator()){var tag=StackData.data(result);tag.putString("Issuer",(p.getUUID()).toString());tag.putString("IssuerName",p.getName().getString());StackData.data(result,tag);}
        if(!quick&&!carried.isEmpty()&&(!ItemStack.isSameItemSameComponents(carried,result)||carried.getCount()+result.getCount()>carried.getMaxStackSize()))return false;
        var inputs=GemcutterCrafting.inputs(tile,p);int crafts=1;
        if(quick){int free=0;for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(s.isEmpty())free+=result.getMaxStackSize();else if(ItemStack.isSameItemSameComponents(s,result))free+=Math.max(0,s.getMaxStackSize()-s.getCount());}int upper=free/result.getCount();for(var r:recipe.ingredients())upper=Math.min(upper,1_000_000/r.count());int low=0,high=upper;while(low<high){int mid=(low+high+1)/2;if(GemcutterCrafting.plan(recipe,inputs,mid)==null)high=mid-1;else low=mid;}crafts=low;if(crafts==0)return false;}
        int[] used=GemcutterCrafting.plan(recipe,inputs,crafts);if(used==null)return false;result.setCount(result.getCount()*crafts);
        GemcutterCrafting.consume(tile,p,used);
        if(quick){if(!p.getInventory().add(result))p.drop(result,false);}
        else if(carried.isEmpty())setCarried(result);else carried.grow(result.getCount());
        tile.changed();return true;
    }
    private void refreshBulk(){
        if(station!=2||tile==null)return;
        for(int i=0;i<54;i++){ItemStack s=tile.inventory.getStackInSlot(i);counts[i]=s.getCount();bulkDisplay.setItem(i,s.copyWithCount(1));}
    }
    private void refreshRecipes(){
        if(station!=1||tile==null)return;var inputs=GemcutterCrafting.inputs(tile,viewer);
        if(snapshotEpoch!=ArchiveRecipes.generation()){snapshotEpoch=ArchiveRecipes.generation();recipeSnapshot=ArchiveRecipes.forLevel(viewer.level());recipePages=Math.max(1,(recipeSnapshot.size()+6)/7);selectedRecipeIndex=0;for(int i=0;i<recipeSnapshot.size();i++)if(ArchiveRecipes.key(recipeSnapshot.get(i).id()).equals(tile.gctRecipe)){selectedRecipeIndex=i;break;}recipePage=Math.min(recipePage,recipePages-1);lastPage=-1;}
        var network=NetworkData.get(viewer.level()).network(viewer.getUUID());
        if(lastPage==recipePage&&lastSelected==selectedRecipeIndex&&java.util.Objects.equals(lastNetwork,network)&&GemcutterCrafting.same(lastInputs,inputs))return;lastNetwork=network;
        lastPage=recipePage;lastSelected=selectedRecipeIndex;lastInputs=inputs.stream().map(ItemStack::copy).toList();recipeAvailability=0;
        for(int i=0;i<7;i++){int index=recipePage*7+i;var recipe=index>=recipeSnapshot.size()?null:recipeSnapshot.get(index);recipes.setItem(i+1,recipe==null?ItemStack.EMPTY:recipe.result());if(recipe!=null&&GemcutterCrafting.permitted(recipe,viewer)&&GemcutterCrafting.plan(recipe,inputs)!=null)recipeAvailability|=1<<i;}
        var selected=displayedRecipe(GCT_OUTPUT);recipes.setItem(0,selected!=null&&GemcutterCrafting.permitted(selected,viewer)&&GemcutterCrafting.plan(selected,inputs)!=null?selected.result():ItemStack.EMPTY);
    }
    @Override public void broadcastChanges(){refreshBulk();refreshRecipes();super.broadcastChanges();
        if(station==3&&tile!=null&&!sentBrazier&&viewer instanceof net.minecraft.server.level.ServerPlayer p&&p.containerMenu==this){sentBrazier=true;var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Kind","brazier_state");tag.putInt("Menu",containerId);tag.putLong("Position",tile.getBlockPos().asLong());tag.putString("Dimension",tile.getLevel().dimension().identifier().toString());ArchiveNetwork.view(p,tag);}
        if(station==2&&tile!=null&&viewer instanceof net.minecraft.server.level.ServerPlayer p&&p.containerMenu==this&&!tile.chestName.equals(sentName)){
            sentName=tile.chestName;var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Kind","chest_state");tag.putInt("Menu",containerId);tag.putString("Name",sentName);ArchiveNetwork.view(p,tag);
        }
    }
    public void name(Player p,String name){if(tile!=null&&station==2&&stillValid(p)){tile.chestName=name.replaceAll("[\\p{Cntrl}]","").substring(0,Math.min(64,name.replaceAll("[\\p{Cntrl}]","").length()));tile.changed();NetworkData.get(p.level()).register(tile);broadcastChanges();}}
    public void radius(Player p,int value){if(tile!=null&&station==3&&stillValid(p)&&value>=0&&value<=300){tile.routingRadius=value;tile.changed();broadcastChanges();sendRange(p,false);}}
    public void selectRecipe(Player p,String id){if(tile==null||station!=1||!stillValid(p))return;for(int i=0;i<recipeSnapshot.size();i++)if(ArchiveRecipes.key(recipeSnapshot.get(i).id()).equals(id)){selectedRecipeIndex=i;recipePage=i/7;tile.gctRecipe=id;tile.changed();refreshRecipes();broadcastChanges();break;}}
    @Override public boolean clickMenuButton(Player p,int button){
        if(tile==null||!stillValid(p))return false;
        if(station==1){
            if(button==30)recipePage=(recipePage+recipePages-1)%recipePages;else if(button==31)recipePage=(recipePage+1)%recipePages;
            else if(button==32||button==33){int size=recipeSnapshot.size();for(int step=1;step<size;step++){int i=(selectedRecipeIndex+(button==32?-step:step)+size)%size;var recipe=recipeSnapshot.get(i);if(GemcutterCrafting.permitted(recipe,p)&&GemcutterCrafting.plan(recipe,GemcutterCrafting.inputs(tile,p))!=null){selectRecipe(p,ArchiveRecipes.key(recipe.id()));return true;}}return false;}
            else return false;
            refreshRecipes();broadcastChanges();return true;
        }
        if(station==3){
            if(button==20)tile.routingRadius=Math.max(0,tile.routingRadius-10);
            else if(button==21)tile.routingRadius=Math.min(300,tile.routingRadius+10);
            else if(button==22)tile.subnetworkOnly=!tile.subnetworkOnly;
            else if(button==23){sendRange(p,true);return true;}
            else if(button>=1000&&button<=1300)tile.routingRadius=button-1000;
            else return false;
            tile.changed();broadcastChanges();sendRange(p,false);return true;
        }
        if(!tile.kind().equals("radiant_chest"))return false;
        if(button==10)tile.displayStack=getCarried().copyWithCount(1);
        else if(button==11)tile.displayStack=ItemStack.EMPTY;
        else if(button==12)tile.displayFacing=tile.displayFacing.getAxis().isVertical()?net.minecraft.core.Direction.NORTH:tile.displayFacing.getClockWise();
        else if(button==13)tile.noNew=!tile.noNew;
        else return false;
        tile.changed();return true;
    }
    private void sendRange(Player player,boolean toggle){
        if(!(player instanceof net.minecraft.server.level.ServerPlayer server))return;
        var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Kind",toggle?"range_toggle":"range_update");
        tag.putLong("Position",tile.getBlockPos().asLong());tag.putString("Dimension",player.level().dimension().identifier().toString());
        tag.putInt("Radius",tile.routingRadius);ArchiveNetwork.view(server,tag);
    }
}
