package com.aranaira.arcanearchives;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
public final class FleetingAir extends Block {
    public FleetingAir(Properties p){super(p.noCollision().noOcclusion().replaceable());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){level.removeBlock(pos,false);}
}
