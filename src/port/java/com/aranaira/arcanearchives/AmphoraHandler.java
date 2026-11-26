package com.aranaira.arcanearchives;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** The item never owns fluid; authoritative operations delegate to the linked tank. */
public final class AmphoraHandler implements IFluidHandlerItem{
    private final ItemStack container;
    public AmphoraHandler(ItemStack container){this.container=container;}
    @Override public ItemStack getContainer(){return container;}
    @Override public int getTanks(){return 1;}
    @Override public FluidStack getFluidInTank(int index){
        if(index!=0)return FluidStack.EMPTY;var tile=Amphora.boundTank(container);if(tile!=null)return tile.tank.getFluid().copy();
        var tag=StackData.data(container);var id=ResourceLocation.tryParse(tag.getString("LinkedFluid"));
        if(id==null)return FluidStack.EMPTY;var fluid=BuiltInRegistries.FLUID.get(id);
        return fluid==net.minecraft.world.level.material.Fluids.EMPTY?FluidStack.EMPTY:new FluidStack(fluid,tag.getInt("LinkedAmount"));
    }
    @Override public int getTankCapacity(int index){var tile=Amphora.boundTank(container);return index!=0?0:tile==null?StackData.data(container).getInt("LinkedCapacity"):tile.tank.getCapacity();}
    @Override public boolean isFluidValid(int index,FluidStack fluid){var tile=Amphora.boundTank(container);return index==0&&tile!=null&&tile.tank.isFluidValid(fluid);}
    @Override public int fill(FluidStack fluid,FluidAction action){var tile=Amphora.boundTank(container);return tile==null||StackData.data(container).getBoolean("EmptyMode")?0:tile.tank.fill(fluid,action);}
    @Override public FluidStack drain(FluidStack fluid,FluidAction action){var tile=Amphora.boundTank(container);return tile==null||!StackData.data(container).getBoolean("EmptyMode")?FluidStack.EMPTY:tile.tank.drain(fluid,action);}
    @Override public FluidStack drain(int amount,FluidAction action){var tile=Amphora.boundTank(container);return tile==null||!StackData.data(container).getBoolean("EmptyMode")?FluidStack.EMPTY:tile.tank.drain(amount,action);}
}
