package com.aranaira.arcanearchives;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Dispensers choose fill/place from the target, retaining the user's selected mode. */
public final class ArchiveDispenser {
    public static void setup(FMLCommonSetupEvent event){
        event.enqueueWork(()->DispenserBlock.registerBehavior(AA.item("radiant_amphora"),(source,stack)->{
            var direction=source.state().getValue(DispenserBlock.FACING);
            if(transfer(source.level(),source.pos().relative(direction),stack)){
                source.level().levelEvent(1000,source.pos(),0);
                source.level().levelEvent(2000,source.pos(),direction.get3DDataValue());
            }
            return stack;
        }));
    }
    public static boolean transfer(Level level,BlockPos pos,ItemStack stack){
        var tank=Amphora.boundTank(stack);if(tank==null||!level.hasChunkAt(pos))return false;
        var state=level.getBlockState(pos);
        if(state.canBeReplaced()&&state.getFluidState().isEmpty()){
            FluidStack fluid=tank.tank.compat.drain(1000,FluidAction.SIMULATE);
            if(fluid.getAmount()!=1000||fluid.getFluid()==Fluids.WATER&&(level.dimension()==Level.NETHER))return false;
            var placed=fluid.getFluid().defaultFluidState().createLegacyBlock();
            if(placed.isAir()||!level.setBlockAndUpdate(pos,placed))return false;
            tank.tank.compat.drain(1000,FluidAction.EXECUTE);return true;
        }
        var fluid=state.getFluidState();
        if(!fluid.isSource()||!(state.getBlock() instanceof BucketPickup pickup))return false;
        FluidStack input=new FluidStack(fluid.getType(),1000);
        if(tank.tank.compat.fill(input,FluidAction.SIMULATE)!=1000)return false;
        var bucket=pickup.pickupBlock(null,level,pos,state);
        if(bucket.isEmpty())return false;
        tank.tank.compat.fill(input,FluidAction.EXECUTE);return true;
    }
}
