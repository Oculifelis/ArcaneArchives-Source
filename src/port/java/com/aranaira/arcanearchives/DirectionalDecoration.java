package com.aranaira.arcanearchives;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
public final class DirectionalDecoration extends Block {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.FACING;
    private final boolean sliver;
    public DirectionalDecoration(String name,Properties p){
        super(p.lightLevel(s->15).strength(name.equals("quartz_sliver")?0:1.4f));sliver=name.equals("quartz_sliver");
        registerDefaultState(stateDefinition.any().setValue(FACING,sliver?Direction.DOWN:Direction.UP));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace());}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        Direction facing=s.getValue(FACING);
        if(sliver)return switch(facing){
            case UP->Block.box(6.4,0,6.4,9.6,8,9.6);case DOWN->Block.box(6.4,8,6.4,9.6,16,9.6);
            case SOUTH->Block.box(6.4,6.4,0,9.6,9.6,8);case NORTH->Block.box(6.4,6.4,8,9.6,9.6,16);
            case EAST->Block.box(0,6.4,6.4,8,9.6,9.6);case WEST->Block.box(8,6.4,6.4,16,9.6,9.6);
        };
        return switch(facing.getAxis()){
            case Y->Block.box(5.6,0,5.6,10.4,16,10.4);
            case X->Block.box(0,5.6,5.6,16,10.4,10.4);
            case Z->Block.box(5.6,5.6,0,10.4,10.4,16);
        };
    }
}
