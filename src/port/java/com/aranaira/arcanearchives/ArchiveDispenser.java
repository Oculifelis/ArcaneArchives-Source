package com.aranaira.arcanearchives;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Dispensers choose fill/place from the target, retaining the user's selected mode. */
public final class ArchiveDispenser {
    public static void setup(FMLCommonSetupEvent event){
        event.enqueueWork(()->DispenserBlock.registerBehavior(AA.item("radiant_amphora"),(source,stack)->{
            var direction=source.getBlockState().getValue(DispenserBlock.FACING);
            if(transfer(source.getLevel(),source.getPos().relative(direction),stack)){
                source.getLevel().levelEvent(1000,source.getPos(),0);
                source.getLevel().levelEvent(2000,source.getPos(),direction.get3DDataValue());
            }
            return stack;
        }));
    }
    public static boolean transfer(Level level,BlockPos pos,ItemStack stack){
        var tank=Amphora.boundTank(stack);if(tank==null||!level.hasChunkAt(pos))return false;
        var state=level.getBlockState(pos);
        if(state.canBeReplaced()&&state.getFluidState().isEmpty()){
            FluidStack fluid=tank.tank.drain(1000,FluidAction.SIMULATE);
            if(fluid.getAmount()!=1000||fluid.getFluid()==Fluids.WATER&&level.dimensionType().ultraWarm())return false;
            var placed=fluid.getFluid().defaultFluidState().createLegacyBlock();
            if(placed.isAir()||!level.setBlockAndUpdate(pos,placed))return false;
            tank.tank.drain(1000,FluidAction.EXECUTE);return true;
        }
        var fluid=state.getFluidState();
        if(!fluid.isSource()||!(state.getBlock() instanceof BucketPickup pickup))return false;
        FluidStack input=new FluidStack(fluid.getType(),1000);
        if(tank.tank.fill(input,FluidAction.SIMULATE)!=1000)return false;
        var bucket=pickup.pickupBlock(level,pos,state);
        if(bucket.isEmpty())return false;
        tank.tank.fill(input,FluidAction.EXECUTE);return true;
    }
}
