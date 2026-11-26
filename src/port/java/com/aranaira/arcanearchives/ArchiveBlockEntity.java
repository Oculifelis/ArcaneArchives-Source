package com.aranaira.arcanearchives;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;


import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.*;

public class ArchiveBlockEntity extends BlockEntity implements MenuProvider {
    private static final Random RESONATOR_RANDOM=new Random();
    public UUID owner;
    public int upgrades, upgradeStage, progress;
    public boolean busy;
    public int routingRadius=150;
    public boolean subnetworkOnly;
    public String gctRecipe="arcanearchives:radiant_dust",chestName="";
    public final CompoundTag[] craftingBookmarks={new CompoundTag(),new CompoundTag(),new CompoundTag()};
    public final ItemStack[] optionalModules={ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};
    public boolean resonating,clientSoundActive;
    private long lastAbsorbSound;
    private UUID lastDepositor;
    private long lastDepositAt;
    private ItemStack lastDeposit=ItemStack.EMPTY;
    public boolean voiding, locked, noNew;
    public ItemStack reference = ItemStack.EMPTY;
    public ItemStack displayStack=ItemStack.EMPTY;
    public Direction displayFacing=Direction.NORTH;
    public final BulkInventory inventory;
    public final RoutingInventory router=new RoutingInventory(this);
    public final ArchiveTank tank;
    private net.minecraft.core.HolderLookup.Provider loadRegistries;
    public net.minecraft.core.HolderLookup.Provider registries() { return loadRegistries != null ? loadRegistries : level.registryAccess(); }
    public ArchiveBlockEntity(BlockPos pos, BlockState state) {
        super(AA.ARCHIVE.get(), pos, state);
        inventory = new BulkInventory(this, kind().equals("radiant_chest") ? 54 : kind().equals("radiant_trove") ? 1 : kind().equals("gemcutters_table") ? 18 : 9);
        tank = new ArchiveTank(this);
    }
    public String kind() { return ((ArchiveBlock)getBlockState().getBlock()).name; }
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide()&&owner!=null)NetworkData.get(level).register(this);}
    public int menuStation(){return kind().equals("gemcutters_table")?1:kind().equals("radiant_chest")?2:kind().equals("brazier_of_hoarding")?3:0;}
    public void openControls(net.minecraft.server.level.ServerPlayer player){player.openMenu(this,buffer->buffer.writeVarInt(menuStation()));}
    /** A second click within 300 ms deposits matching stacks from the main inventory. */
    public void deposit(Player player,net.minecraft.world.InteractionHand hand){
        if(hand!=net.minecraft.world.InteractionHand.MAIN_HAND||!allowed(player))return;
        long now=System.currentTimeMillis();boolean twice=player.getUUID().equals(lastDepositor)&&now-lastDepositAt<=300;
        ItemStack held=player.getItemInHand(hand),template=held.isEmpty()&&twice?lastDeposit:held;
        lastDepositor=player.getUUID();lastDepositAt=now;
        if(template.isEmpty()||StackData.data(template).contains("Quark:FavoriteItem"))return;
        lastDeposit=template.copyWithCount(1);
        if(twice){
            for(int i=0;i<36;i++){
                ItemStack candidate=player.getInventory().getItem(i);
                if(!candidate.isEmpty()&&ItemStack.isSameItemSameComponents(candidate,template)&&!StackData.data(candidate).contains("Quark:FavoriteItem"))
                    player.getInventory().setItem(i,router.insertItem(0,candidate,false));
            }
        }else player.setItemInHand(hand,router.insertItem(0,held,false));
        player.containerMenu.broadcastChanges();
    }
    public IItemHandler itemHandler(){return kind().equals("brazier_of_hoarding")?router:inventory;}
    public boolean allowed(Player p) {
        return owner == null || p.getUUID().equals(owner) || (!p.level().isClientSide() && NetworkData.get(p.level()).sameNetwork(owner,p.getUUID()));
    }
    public boolean tryUpgrade(Player player, ItemStack held) {
        if (!allowed(player) || !Set.of("radiant_trove","radiant_tank").contains(kind()) || upgradeStage>=3) return false;
        String expected=upgradeStage==0?"matrix_brace":upgradeStage==1?(kind().equals("radiant_tank")?"containment_field":"material_interface"):"storage_shaped_quartz";
        if (!held.is(AA.item(expected))) return false;
        upgrades+=upgradeStage==0?2:upgradeStage==1?3:4;upgradeStage++;
        
        if(!player.getAbilities().instabuild)held.shrink(1);
        changed();return true;
    }
    public void recalculateUpgrades(){upgrades=upgradeStage==0?0:upgradeStage==1?2:upgradeStage==2?5:9;changed();}
    public boolean configureChestDisplay(Player p,Direction face){if(!kind().equals("radiant_chest")||!allowed(p))return false;var main=p.getMainHandItem();var off=p.getOffhandItem();boolean mainScepter=main.is(AA.item("scepter_manipulation")),offScepter=off.is(AA.item("scepter_manipulation"));if(!mainScepter&&!offScepter)return false;var template=mainScepter?off:main;if(template.isEmpty()&&!p.isShiftKeyDown())return false;displayStack=template.copyWithCount(1);displayFacing=face;changed();return true;}
    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide()){
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
        }
    }
    public void removeContents(){
        if(level==null||level.isClientSide())return;
        NetworkData.get(level).unregister(this);
        if(Set.of("radiant_trove","radiant_tank").contains(kind()))return;
        for(int i=0;i<inventory.getSlots();i++){
            ItemStack stack=inventory.getStackInSlot(i);
            while(!stack.isEmpty())net.minecraft.world.level.block.Block.popResource(level,worldPosition,stack.split(Math.min(stack.getMaxStackSize(),stack.getCount())));
        }
        changed();
    }
    public void tick() {
        if(level!=null&&level.isClientSide()){ViewMessages.blockTick.accept(this);return;}
        if (!(level instanceof ServerLevel server)) return;
        if (server.getGameTime() % 100 == 0 && owner != null) NetworkData.get(level).register(this);
        boolean running=kind().equals("radiant_resonator")&&owner!=null&&server.getServer().getPlayerList().getPlayer(owner)!=null&&level.isEmptyBlock(worldPosition.above());
        if(resonating!=running){resonating=running;changed();}
        if (running) {
            if (++progress > ArchiveConfig.RESONATOR_TICKS.get()) {
                progress = 0; level.setBlockAndUpdate(worldPosition.above(), AA.block("raw_quartz_cluster").defaultBlockState());
                var up=worldPosition.above();var box=new AABB(up.getX()-.9,up.getY()-.9,up.getZ()-.9,up.getX()+.9,up.getY()+.9,up.getZ()+.9);
                for(var cat:server.getEntities((net.minecraft.world.entity.Entity)null,box,e->e.getType()==net.minecraft.world.entity.EntityType.OCELOT||e.getType()==net.minecraft.world.entity.EntityType.CAT)){
                    double dy=RESONATOR_RANDOM.nextFloat()*5,dx=(RESONATOR_RANDOM.nextFloat()-.5)*3,dz=(RESONATOR_RANDOM.nextFloat()-.5)*3;
                    cat.setDeltaMovement(cat.getDeltaMovement().add(dx,dy,dz));
                }
                if(ArchiveConfig.SOUNDS.get()&&ArchiveLegacyConfig.RESONATOR_COMPLETE.get())level.playSound(null,worldPosition,AA.RESONATOR_COMPLETE.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            }
            if(server.getGameTime()%50==0)changed();
            setChanged();
        }
    }
    public void acceptDropped(ItemEntity entity){
        if(level==null||level.isClientSide()||!entity.isAlive()||owner==null||entity.getPersistentData().getBooleanOr("ArcaneRejected",false))return;
        int before=entity.getItem().getCount();ItemStack rest=router.insertItem(0,entity.getItem(),false);
        if(rest.isEmpty())entity.discard();else{
            entity.setItem(rest);entity.getPersistentData().putBoolean("ArcaneRejected",true);entity.setPickUpDelay(20);
            entity.setDeltaMovement(Math.min(.2,(level.getRandom().nextFloat()-.5)*.6),.1,Math.min(.2,(level.getRandom().nextFloat()-.5)*.6));
        }
        long now=System.currentTimeMillis();
        if(ArchiveConfig.SOUNDS.get()&&ArchiveLegacyConfig.BRAZIER_PICKUP.get()&&now-lastAbsorbSound>=300){lastAbsorbSound=now;level.playSound(null,worldPosition,AA.BRAZIER_ABSORB.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);}
    }
    public CompoundTag writeData(CompoundTag tag) {
        if (owner != null) tag.putString("Owner",(owner).toString());
        tag.putInt("Upgrades", upgrades); tag.putInt("UpgradeStage",upgradeStage); tag.putInt("Progress", progress);tag.putInt("RoutingRadius",routingRadius);
        tag.putBoolean("Voiding", voiding); tag.putBoolean("Locked",locked); tag.putBoolean("NoNew",noNew);
        tag.putBoolean("SubnetworkOnly",subnetworkOnly);
        tag.putString("GemcutterRecipe",gctRecipe);tag.putString("ChestName",chestName);
        var bookmarks=new ListTag();for(var bookmark:craftingBookmarks)bookmarks.add(bookmark.copy());tag.put("CraftingBookmarks",bookmarks);
        var modules=new ListTag();for(var module:optionalModules)modules.add(StackData.save(module,registries()));tag.put("OptionalModules",modules);
        tag.putBoolean("Resonating",resonating);
        tag.put("Inventory", inventory.save());
        tag.put("Reference", StackData.save(reference, registries()));
        tag.put("DisplayStack",StackData.save(displayStack,registries()));tag.putInt("DisplayFacing",displayFacing.get3DDataValue());
        tag.put("Tank", tank.save(registries()));
        return tag;
    }
    public void readData(CompoundTag tag) {
        owner = StackData.hasUuid(tag,"Owner") ? StackData.uuid(tag,"Owner") : null;
        upgrades = Math.max(0, Math.min(9, tag.getIntOr("Upgrades",0))); upgradeStage=Math.max(0,Math.min(3,tag.getIntOr("UpgradeStage",0))); progress = tag.getIntOr("Progress",0);
        routingRadius=tag.contains("RoutingRadius")?Math.max(0,Math.min(300,tag.getIntOr("RoutingRadius",0))):150;
        voiding = tag.getBooleanOr("Voiding",false); locked = tag.getBooleanOr("Locked",false); noNew = tag.getBooleanOr("NoNew",false);
        subnetworkOnly=tag.getBooleanOr("SubnetworkOnly",false);
        gctRecipe=tag.contains("GemcutterRecipe")?tag.getStringOr("GemcutterRecipe",""):"arcanearchives:radiant_dust";chestName=tag.getStringOr("ChestName","");
        var bookmarks=tag.getListOrEmpty("CraftingBookmarks");for(int i=0;i<3;i++)craftingBookmarks[i]=i<bookmarks.size()?bookmarks.getCompoundOrEmpty(i).copy():new CompoundTag();
        var modules=tag.getListOrEmpty("OptionalModules");for(int i=0;i<3;i++)optionalModules[i]=i<modules.size()?StackData.load(modules.getCompoundOrEmpty(i),registries()):ItemStack.EMPTY;
        if(modules.isEmpty()&&voiding)optionalModules[0]=new ItemStack(AA.item("devouring_charm"));
        if(modules.isEmpty()&&locked)optionalModules[voiding?1:0]=new ItemStack(AA.item("radiant_key"));
        resonating=tag.getBooleanOr("Resonating",false);
        reference = StackData.load(tag.getCompoundOrEmpty("Reference"), registries());
        displayStack=StackData.load(tag.getCompoundOrEmpty("DisplayStack"),registries());displayFacing=Direction.from3DDataValue(tag.getIntOr("DisplayFacing",0));
        inventory.load(tag.getCompoundOrEmpty("Inventory"));
        tank.load(tag.getCompoundOrEmpty("Tank"),registries());
    }
    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out); out.store("Archive",CompoundTag.CODEC,writeData(new CompoundTag()));
    }
    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input); loadRegistries=input.lookup(); readData(input.read("Archive",CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){removeContents();super.preRemoveSideEffects(pos,state);}
    @Override public Component getDisplayName() { return chestName.isBlank()?getBlockState().getBlock().getName():Component.literal(chestName); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries){var tag=new CompoundTag();tag.put("Archive",writeData(new CompoundTag()));return tag;}
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new ArchiveMenu(id, inv, this); }
}
