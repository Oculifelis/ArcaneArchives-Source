package com.aranaira.arcanearchives;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.fluid.*;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
public final class ArchiveTank extends FluidStacksResourceHandler {
    private final ArchiveBlockEntity parent;
    public final IFluidHandler compat=IFluidHandler.of(this);
    public ArchiveTank(ArchiveBlockEntity p){super(1,16000);parent=p;}
    @Override protected int getCapacity(int i,FluidResource r){return getCapacity();}
    public int getCapacity(){return 16000*(parent.upgrades+1);}
    public int getFluidAmount(){return getAmountAsInt(0);}
    public FluidStack getFluid(){return getResource(0).toStack(getFluidAmount());}
    @Override protected void onContentsChanged(int index,FluidStack before){parent.changed();}
    @Override public int insert(int index,FluidResource resource,int amount,TransactionContext tx){
        boolean discardOverflow=parent.voiding&&getFluidAmount()>=getCapacity()&&getResource(index).equals(resource);
        int filled=super.insert(index,resource,amount,tx);
        return discardOverflow?amount:filled;
    }
    public CompoundTag save(HolderLookup.Provider registries){return (CompoundTag)FluidStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),getFluid()).getOrThrow();}
    public void load(CompoundTag tag,HolderLookup.Provider registries){FluidStack fluid=FluidStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE),tag).getOrThrow();stacks.set(0,fluid);}
}
