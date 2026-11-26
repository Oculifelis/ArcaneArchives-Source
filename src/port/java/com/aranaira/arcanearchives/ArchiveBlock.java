package com.aranaira.arcanearchives;

import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;

public class ArchiveBlock extends Block implements EntityBlock {
    public final String name;
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public ArchiveBlock(String name, Properties props) { super(props); this.name = name; registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if(name.equals("radiant_resonator")&&!ctx.getLevel().isClientSide&&ctx.getPlayer()!=null
                &&NetworkData.get(ctx.getLevel()).nodes(ctx.getPlayer().getUUID()).stream().filter(n->n.kind().equals("radiant_resonator")).count()>=ArchiveConfig.RESONATOR_LIMIT.get())return null;
        return defaultBlockState().setValue(FACING, name.equals("monitoring_crystal")?ctx.getClickedFace():ctx.getHorizontalDirection().getOpposite());
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){
        return switch(name){
            case"radiant_trove"->Block.box(3.2,0,3.2,12.8,16,12.8);
            case"radiant_tank"->Block.box(1.6,0,1.6,14.4,15.68,14.4);
            case"brazier_of_hoarding"->Block.box(2.4,0,2.4,13.6,12,13.6);
            case"monitoring_crystal"->switch(state.getValue(FACING)){
                case UP->Block.box(5.92,-1.6,5.6,9.76,0.64,10.24);case DOWN->Block.box(5.92,15.04,5.6,9.76,17.6,10.24);
                case SOUTH->Block.box(5.6,5.6,-1.6,9.76,10.24,0.64);case NORTH->Block.box(6.24,5.6,15.04,10.08,10.24,17.6);
                case EAST->Block.box(-1.6,5.6,6.24,0.8,10.08,10.08);case WEST->Block.box(15.2,5.6,5.92,17.6,10.08,9.92);
            };
            default->Shapes.block();
        };
    }
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return name.equals("monitoring_crystal")?Shapes.empty():getShape(s,l,p,c);}
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArchiveBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof ArchiveBlockEntity tile && entity instanceof ServerPlayer player) {
            var carry=StackData.data(stack);
            if(carry.contains("ArchiveCarry"))tile.load(carry.getCompound("ArchiveCarry"));
            else if(name.equals("radiant_chest"))tile.noNew=NetworkData.get(level).defaultNoNew(player.getUUID());
            tile.owner = player.getUUID();
            tile.changed();
            NetworkData.get(level).register(tile);
        }
    }
    @Override public java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){
        if(java.util.Set.of("radiant_trove","radiant_tank").contains(name)){
            var tile=params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
            ItemStack stack=new ItemStack(this);
            if(tile instanceof ArchiveBlockEntity archive){var tag=StackData.data(stack);tag.put("ArchiveCarry",archive.saveWithoutMetadata());StackData.data(stack,tag);}
            return java.util.List.of(stack);
        }
        return super.getDrops(state,params);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ArchiveBlockEntity tile)) return InteractionResult.PASS;
        if(name.equals("radiant_chest")&&tile.allowed(player)&&tile.configureChestDisplay(player,hit.getDirection()))return level.isClientSide?InteractionResult.SUCCESS:InteractionResult.CONSUME;
        if (player.getItemInHand(hand).getItem() instanceof ArchiveItem ai && ai.handlesBlocks()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!tile.allowed(player)) { player.displayClientMessage(Component.literal("This archive belongs to another network."), true); return InteractionResult.CONSUME; }
        if(tile.tryUpgrade(player,player.getItemInHand(hand)))return InteractionResult.CONSUME;
        if (name.equals("radiant_tank")) {
            if (net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, tile.tank)) return InteractionResult.CONSUME;
            player.displayClientMessage(Component.literal(tile.tank.getFluidAmount() + " / " + tile.tank.getCapacity() + " mB"), true);
        } else if (name.equals("radiant_trove")) {
            StorageInteractions.deposit(tile,player,hand);
        } else if (name.equals("lectern_manifest")) {
            ManifestMenu.open((ServerPlayer) player);
        } else if (name.equals("radiant_resonator")) {
            player.displayClientMessage(Component.literal("Resonance: " + tile.progress + " / "+ArchiveConfig.RESONATOR_TICKS.get()+" ticks"), true);
        } else if(name.equals("radiant_crafting_table")){
            if(!tile.busy)player.openMenu(new SimpleMenuProvider((id,inv,p)->new PersistentCraftingMenu(id,inv,tile),tile.getDisplayName()));
        } else if(name.equals("brazier_of_hoarding")){
            tile.deposit(player,hand);
        } else if(name.equals("monitoring_crystal")){
            ManifestMenu.open((ServerPlayer)player);
        } else {
            NetworkHooks.openScreen((ServerPlayer) player, tile, buffer->buffer.writeVarInt(tile.menuStation()));
        }
        return InteractionResult.CONSUME;
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.Entity entity){
        if(name.equals("brazier_of_hoarding")&&entity instanceof net.minecraft.world.entity.item.ItemEntity item&&level.getBlockEntity(pos) instanceof ArchiveBlockEntity tile)tile.acceptDropped(item);
    }
    @Override public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && name.equals("radiant_trove") && level.getBlockEntity(pos) instanceof ArchiveBlockEntity tile && tile.allowed(player)) {
            if(StorageInteractions.canExtract(tile,player))StorageInteractions.extract(tile,player);
        }
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ArchiveBlockEntity tile) {
            tile.removeContents();
        }
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type != AA.ARCHIVE.get() ? null : (l,p,s,t) -> ((ArchiveBlockEntity)t).tick();
    }
    @Override public boolean hasAnalogOutputSignal(BlockState s) { return true; }
    @Override public int getAnalogOutputSignal(BlockState s, Level l, BlockPos p) {
        if (!(l.getBlockEntity(p) instanceof ArchiveBlockEntity t)) return 0;
        if (name.equals("radiant_tank")) return (int) Math.ceil(15.0 * t.tank.getFluidAmount() / t.tank.getCapacity());
        long total = 0, max = 0;
        for (int i = 0; i < t.inventory.getSlots(); i++) { total += t.inventory.getStackInSlot(i).getCount(); max += t.inventory.getSlotLimit(i); }
        return max == 0 ? 0 : (int)Math.ceil(15.0 * total / max);
    }
}
