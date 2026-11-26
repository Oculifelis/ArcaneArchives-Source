package com.aranaira.arcanearchives;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.*;

public class QuartzCluster extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public QuartzCluster(Properties properties) { super(properties.lightLevel(s -> 15)); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING,c.getClickedFace()); }
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(3.2,0,3.2,12.8,16,12.8); }
}
