package com.aranaira.arcanearchives.client;
import com.aranaira.arcanearchives.*;
import com.mojang.serialization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;

public final class ArchiveItemVisuals{
    public record State(String kind) implements ConditionalItemModelProperty{
        public static final MapCodec<State> CODEC=Codec.STRING.fieldOf("kind").xmap(State::new,State::kind);
        @Override public boolean get(ItemStack stack,ClientLevel level,LivingEntity owner,int seed,ItemDisplayContext context){return state(kind,stack);}
        @Override public MapCodec<State> type(){return CODEC;}
    }
    public record FluidTint() implements ItemTintSource{
        public static final MapCodec<FluidTint> CODEC=MapCodec.unit(new FluidTint());
        @Override public int calculate(ItemStack stack,ClientLevel level,LivingEntity owner){return fluidColor(stack);}
        @Override public MapCodec<FluidTint> type(){return CODEC;}
    }
    public static boolean state(String kind,ItemStack stack){
        return switch(kind){
            case "dun"->stack.getItem() instanceof GemItem&&GemItem.charge(stack)==0;
            case "colourblind"->ClientPreferences.colourblind;
            case "linked"->StackData.data(stack).contains("TankPos");
            case "empty_mode"->StackData.data(stack).getBooleanOr("EmptyMode",false);
            default->false;
        };
    }
    public static int fluidColor(ItemStack stack){
        var id=Identifier.tryParse(StackData.data(stack).getStringOr("LinkedFluid",""));if(id==null)return 0xffffffff;
        var fluid=BuiltInRegistries.FLUID.getValue(id);if(fluid==net.minecraft.world.level.material.Fluids.EMPTY)return 0xffffffff;
        var model=Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        return model.fluidTintSource()==null?0xffffffff:model.fluidTintSource().colorAsStack(new FluidStack(fluid,1000));
    }
}
