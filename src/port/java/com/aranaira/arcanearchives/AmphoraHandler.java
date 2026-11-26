package com.aranaira.arcanearchives;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.access.ItemAccess;
public final class AmphoraHandler implements ResourceHandler<FluidResource>{
    private final ItemAccess access;
    public AmphoraHandler(ItemStack stack,ItemAccess access){this.access=access;}
    private ItemStack container(){return access.getResource().toStack();}
    @Override public int size(){return 1;}
    @Override public FluidResource getResource(int index){
        if(index!=0)return FluidResource.EMPTY;var tile=Amphora.boundTank(container());if(tile!=null)return tile.tank.getResource(0);
        var id=Identifier.tryParse(StackData.data(container()).getStringOr("LinkedFluid",""));if(id==null)return FluidResource.EMPTY;
        var fluid=BuiltInRegistries.FLUID.getValue(id);return fluid==net.minecraft.world.level.material.Fluids.EMPTY?FluidResource.EMPTY:FluidResource.of(new FluidStack(fluid,1));
    }
    @Override public long getAmountAsLong(int index){var tile=Amphora.boundTank(container());return index!=0?0:tile==null?StackData.data(container()).getIntOr("LinkedAmount",0):tile.tank.getFluidAmount();}
    @Override public long getCapacityAsLong(int index,FluidResource fluid){var tile=Amphora.boundTank(container());return index!=0?0:tile==null?StackData.data(container()).getIntOr("LinkedCapacity",0):tile.tank.getCapacity();}
    @Override public boolean isValid(int index,FluidResource fluid){var tile=Amphora.boundTank(container());return index==0&&tile!=null&&tile.tank.isValid(0,fluid);}
    @Override public int insert(int index,FluidResource fluid,int amount,TransactionContext transaction){var tile=Amphora.boundTank(container());return index!=0||tile==null||StackData.data(container()).getBooleanOr("EmptyMode",false)?0:tile.tank.insert(0,fluid,amount,transaction);}
    @Override public int extract(int index,FluidResource fluid,int amount,TransactionContext transaction){var tile=Amphora.boundTank(container());return index!=0||tile==null||!StackData.data(container()).getBooleanOr("EmptyMode",false)?0:tile.tank.extract(0,fluid,amount,transaction);}
}
