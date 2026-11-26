package com.aranaira.arcanearchives;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
/** The linked tank remains authoritative. No chunk is loaded by using an amphora. */
public final class Amphora {
    public static boolean bind(Player p,ItemStack stack,ArchiveBlockEntity tile){
        if(!tile.kind().equals("radiant_tank")||!tile.allowed(p))return false;
        CompoundTag t=StackData.data(stack);t.putLong("TankPos",tile.getBlockPos().asLong());t.putString("TankDimension",tile.getLevel().dimension().identifier().toString());t.putString("TankOwner",(p.getUUID()).toString());
        StackData.data(stack,t);sync(p,stack);ArchiveItem.tell(p,"Amphora linked to this tank. Sneak-use in air to change fill/empty mode.");return true;
    }
    public static void sync(Player player,ItemStack stack){
        var tag=StackData.data(stack);if(!tag.contains("TankPos"))return;
        var tile=tank(player,stack);
        if(tile!=null&&!StackData.hasUuid(tag,"TankOwner")){tag.putString("TankOwner",(player.getUUID()).toString());StackData.data(stack,tag);}
        syncBound(stack,tile);
    }
    private static void syncBound(ItemStack stack,ArchiveBlockEntity tile){
        var tag=StackData.data(stack);
        String fluid=tile==null||tile.tank.getFluid().isEmpty()?"":net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(tile.tank.getFluid().getFluid()).toString();
        int amount=tile==null?0:tile.tank.getFluidAmount(),capacity=tile==null?0:tile.tank.getCapacity();
        if(!fluid.equals(tag.getStringOr("LinkedFluid",""))||amount!=tag.getIntOr("LinkedAmount",0)||capacity!=tag.getIntOr("LinkedCapacity",0)){
            tag.putString("LinkedFluid",fluid);tag.putInt("LinkedAmount",amount);tag.putInt("LinkedCapacity",capacity);StackData.data(stack,tag);
        }
    }
    /** Automation carries the original binder's authority; it never loads a chunk. */
    public static ArchiveBlockEntity boundTank(ItemStack stack){
        if(stack.isEmpty()||!stack.is(AA.item("radiant_amphora")))return null;
        var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null||!server.isSameThread())return null;
        var tag=StackData.data(stack);if(!StackData.hasUuid(tag,"TankOwner"))return null;
        var dimension=Identifier.tryParse(tag.getStringOr("TankDimension",""));if(dimension==null)return null;
        var level=server.getLevel(ResourceKey.create(Registries.DIMENSION,dimension));var pos=BlockPos.of(tag.getLongOr("TankPos",0L));
        if(level==null||!level.hasChunkAt(pos))return null;
        if(!(level.getBlockEntity(pos) instanceof ArchiveBlockEntity tile)||!tile.kind().equals("radiant_tank")
                ||!NetworkData.get(level).sameNetwork(tile.owner,StackData.uuid(tag,"TankOwner")))return null;
        return tile;
    }
    public static ArchiveBlockEntity tank(Player p,ItemStack stack){
        if(!(p.level() instanceof ServerLevel current))return null;
        CompoundTag t=StackData.data(stack);Identifier dim=Identifier.tryParse(t.getStringOr("TankDimension",""));
        if(dim==null)return null;
        ServerLevel level=current.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,dim));BlockPos pos=BlockPos.of(t.getLongOr("TankPos",0L));
        if(level==null||!level.hasChunkAt(pos))return null;
        return level.getBlockEntity(pos) instanceof ArchiveBlockEntity tile&&tile.kind().equals("radiant_tank")&&tile.allowed(p)?tile:null;
    }
    public static boolean use(Player p,ItemStack stack){
        CompoundTag t=StackData.data(stack);
        if(p.isShiftKeyDown()){boolean empty=!t.getBooleanOr("EmptyMode",false);t.putBoolean("EmptyMode",empty);StackData.data(stack,t);ArchiveItem.tell(p,empty?"Amphora: empty tank into world":"Amphora: fill tank from world");return true;}
        ArchiveBlockEntity tank=tank(p,stack);if(tank==null){ArchiveItem.tell(p,"Linked tank unavailable or access denied.");return false;}
        Level level=p.level();boolean empty=t.getBooleanOr("EmptyMode",false);
        HitResult hit=p.pick(5,1,!empty);if(hit.getType()!=HitResult.Type.BLOCK||!(hit instanceof BlockHitResult ray))return false;
        BlockPos pos=empty?ray.getBlockPos().relative(ray.getDirection()):ray.getBlockPos();
        if(!level.hasChunkAt(pos)||!level.mayInteract(p,pos)||!p.mayUseItemAt(pos,ray.getDirection(),stack))return false;
        if(empty){
            FluidStack fluid=tank.tank.compat.drain(1000,FluidAction.SIMULATE);
            if(fluid.getAmount()!=1000||!level.getBlockState(pos).canBeReplaced())return false;
            if(fluid.getFluid()==net.minecraft.world.level.material.Fluids.WATER&&(level.dimension()==Level.NETHER))return false;
            var placed=fluid.getFluid().defaultFluidState().createLegacyBlock();if(placed.isAir())return false;
            if(level.setBlockAndUpdate(pos,placed)){tank.tank.compat.drain(1000,FluidAction.EXECUTE);return true;}
        }else{
            var state=level.getBlockState(pos);var fluid=state.getFluidState();
            if(!(state.getBlock() instanceof LiquidBlock)||!fluid.isSource())return false;
            FluidStack input=new FluidStack(fluid.getType(),1000);
            if(tank.tank.compat.fill(input,FluidAction.SIMULATE)!=1000)return false;
            if(level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState())){tank.tank.compat.fill(input,FluidAction.EXECUTE);return true;}
        }
        return false;
    }
}
