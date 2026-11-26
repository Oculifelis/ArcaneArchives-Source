package com.aranaira.arcanearchives;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
public final class CharmFluids {
    public static boolean accepts(ItemStack s){return !s.is(AA.item("radiant_amphora"))&&(s.is(AA.item("parchtear"))||FluidUtil.getFluidHandler(s).isPresent());}
    public static ItemStack drain(ItemStack s){
        if(s.is(AA.item("parchtear"))){GemItem.restore(s,-1);return s;}
        var cap=FluidUtil.getFluidHandler(s).resolve();
        if(cap.isPresent()){var h=cap.get();for(int i=0;i<h.getTanks();i++){var fluid=h.getFluidInTank(i).copy();if(!fluid.isEmpty())h.drain(fluid,FluidAction.EXECUTE);}return h.getContainer();}return s;
    }
}
