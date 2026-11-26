package com.aranaira.arcanearchives;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
public final class AmphoraProvider implements ICapabilityProvider{
    private final LazyOptional<IFluidHandlerItem> handler;
    public AmphoraProvider(ItemStack stack){handler=LazyOptional.of(()->new AmphoraHandler(stack));}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side){return capability==ForgeCapabilities.FLUID_HANDLER_ITEM?handler.cast():LazyOptional.empty();}
}
