package com.aranaira.arcanearchives;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

public final class GemItem extends Item {
    public record Definition(String cut,String color,int normal,int upgraded) {}
    public static final Map<String,Definition> DEFINITIONS=new LinkedHashMap<>();
    static{
        def("slaughtergleam","asscher","red",30,150);def("murdergleam","asscher","yellow",30,150);
        def("agegleam","asscher","green",30,150);def("cleansegleam","asscher","blue",30,150);
        def("switchgleam","asscher","purple",30,150);def("salvegleam","asscher","pink",30,150);
        def("munchstone","oval","black",60,240);def("orderstone","oval","pink",100,400);
        def("mindspindle","pampel","green",800,3600);def("elixirspindle","pampel","purple",5,20);
        def("mountaintear","pendeloque","orange",25,100);def("rivertear","pendeloque","blue",25,100);
        def("parchtear","pendeloque","black",8000,27000);def("phoenixway","trillion","orange",75,300);
        def("stormway","trillion","yellow",30,150);
    }
    private static void def(String id,String cut,String color,int normal,int up){DEFINITIONS.put(id,new Definition(cut,color,normal,up));}
    public final String name;
    public GemItem(String name,Properties properties){super(properties);this.name=name;}
    public static int maximum(ItemStack s){
        GemItem item=(GemItem)s.getItem();Definition d=DEFINITIONS.get(item.name);
        return (StackData.data(s).getInt("upgrades")&2)!=0?d.upgraded():d.normal();
    }
    public static int charge(ItemStack s){CompoundTag t=StackData.data(s);return t.contains("charge")?Math.max(0,t.getInt("charge")):maximum(s);}
    public static void restore(ItemStack s,int amount){CompoundTag t=StackData.data(s);t.putInt("charge",amount<0?maximum(s):Math.min(maximum(s),charge(s)+amount));StackData.data(s,t);SocketMenu.saveGemToParent(s);}
    public static void consume(ItemStack s,int amount){CompoundTag t=StackData.data(s);t.putInt("charge",Math.max(0,charge(s)-amount));StackData.data(s,t);SocketMenu.saveGemToParent(s);}
    public static boolean active(ItemStack s){return StackData.data(s).getBoolean("toggle")&&charge(s)>0;}
    public static void toggle(ItemStack s,boolean value){CompoundTag t=StackData.data(s);t.putBoolean("toggle",value);StackData.data(s,t);SocketMenu.saveGemToParent(s);}
    @Override public boolean isBarVisible(ItemStack stack){return charge(stack)<maximum(stack);}
    @Override public int getBarWidth(ItemStack stack){return Math.round(13f*charge(stack)/maximum(stack));}
    @Override public int getBarColor(ItemStack stack){return 0x9c6ada;}
    @Override public void appendHoverText(ItemStack stack,Level tooltipLevel,List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){
        lines.add(net.minecraft.network.chat.Component.literal("Charge: "+charge(stack)+" / "+maximum(stack)));
        if(Set.of("murdergleam","slaughtergleam","salvegleam","stormway").contains(name))lines.add(net.minecraft.network.chat.Component.literal("Sneak + right-click to toggle"));
    }
    public static boolean recharge(Player p,ItemStack s){
        GemItem item=(GemItem)s.getItem();if(charge(s)>0)return false;
        Item fuel=switch(item.name){case"slaughtergleam"->Items.GOLD_NUGGET;case"munchstone"->Items.BONE_MEAL;case"phoenixway"->Items.GUNPOWDER;case"mindspindle"->Items.BOOK;case"elixirspindle"->Items.NETHER_WART;case"cleansegleam"->Items.MILK_BUCKET;default->null;};
        if(fuel==null)return false;
        for(int i=0;i<p.getInventory().getContainerSize();i++){
            ItemStack found=p.getInventory().getItem(i);
            if(found.is(fuel)){
                int needed=item.name.equals("phoenixway")?3:Set.of("mindspindle","cleansegleam").contains(item.name)?1:5;
                int count=Math.min(needed,found.getCount());
                int gain=switch(item.name){case"mindspindle","cleansegleam"->-1;case"elixirspindle"->count;case"phoenixway"->count*25;default->count*12;};
                found.shrink(count);restore(s,gain);
                if(fuel==Items.MILK_BUCKET){ItemStack bucket=new ItemStack(Items.BUCKET);if(!p.getInventory().add(bucket))p.drop(bucket,false);}
                GemSounds.play(p,item.name);return true;
            }
        }return false;
    }
    @Override public boolean onEntityItemUpdate(ItemStack stack,net.minecraft.world.entity.item.ItemEntity entity){
        if(!entity.level().isClientSide){
            if(name.equals("mountaintear")&&entity.isInLava()||name.equals("rivertear")&&entity.isInWater()||name.equals("stormway")&&entity.level().isRainingAt(entity.blockPosition()))restore(stack,-1);
        }return false;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){
        ItemStack s=p.getItemInHand(hand);
        if(level.isClientSide)return InteractionResultHolder.success(s);
        if(recharge(p,s))return InteractionResultHolder.consume(s);
        if(Set.of("murdergleam","slaughtergleam","salvegleam","stormway").contains(name)&&p.isShiftKeyDown()){
            toggle(s,!active(s));ArchiveItem.tell(p,name+": "+(active(s)?"on":"off"));return InteractionResultHolder.consume(s);
        }
        if(charge(s)<=0)return InteractionResultHolder.fail(s);
        int spent=0;
        if(name.equals("agegleam")){
            for(AgeableMob mob:level.getEntitiesOfClass(AgeableMob.class,p.getBoundingBox().inflate(3.5))){
                if(mob.isBaby()){mob.setAge(mob.getAge()+8000);spent++;}
            }
        }else if(name.equals("cleansegleam")){
            List<LivingEntity> targets=p.isShiftKeyDown()?level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(3.5)):List.of(p);
            for(LivingEntity target:targets){
                spent+=GemRules.cleanse(target,(StackData.data(s).getInt("upgrades")&1)!=0);
            }
        }else if(name.equals("switchgleam")){
            Vec3 start=p.getEyePosition(),end=start.add(p.getLookAngle().scale(40));
            var ray=ProjectileUtil.getEntityHitResult(p,start,end,p.getBoundingBox().expandTowards(p.getLookAngle().scale(40)).inflate(1),e->e instanceof LivingEntity&&e!=p,1600);
            if(ray!=null){Entity target=ray.getEntity();Vec3 old=p.position();p.teleportTo(target.getX(),target.getY(),target.getZ());target.teleportTo(old.x,old.y,old.z);spent=0;}
        }else if(Set.of("rivertear","mountaintear","phoenixway","stormway","parchtear").contains(name)){
            HitResult hit=p.pick(name.equals("stormway")?30:40,1,name.equals("parchtear"));
            if(hit.getType()==HitResult.Type.BLOCK&&hit instanceof BlockHitResult ray){
                BlockPos pos=ray.getBlockPos().relative(ray.getDirection());
                if(level.hasChunkAt(pos)&&level.mayInteract(p,pos)&&p.mayUseItemAt(pos,ray.getDirection(),s)){
                    if(name.equals("parchtear")){
                        BlockPos root=ray.getBlockPos();int radius=p.isShiftKeyDown()?0:1;
                        for(BlockPos at:BlockPos.betweenClosed(root.offset(-radius,-radius,-radius),root.offset(radius,radius,radius))){
                            if(level.hasChunkAt(at)&&!level.getFluidState(at).isEmpty()&&level.mayInteract(p,at)){
                                var state=level.getBlockState(at);
                                if(state.getBlock() instanceof LiquidBlock){level.setBlockAndUpdate(at,AA.block("fake_air").defaultBlockState());level.scheduleTick(at,AA.block("fake_air"),200);spent++;}
                                else if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)&&state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)){level.setBlockAndUpdate(at,state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,false));spent++;}
                            }
                        }
                    }else if(name.equals("stormway")&&level.canSeeSky(pos)){
                        GemRules.lightning(level,pos,p);spent=1;
                    }else if(level.getBlockState(pos).canBeReplaced()){
                        Block result=name.equals("rivertear")?Blocks.WATER:name.equals("mountaintear")?Blocks.LAVA:Blocks.FIRE;
                        if(result!=Blocks.WATER||!level.dimensionType().ultraWarm()){level.setBlockAndUpdate(pos,result.defaultBlockState());spent=1;}
                    }
                }
            }
        }
        if(spent>0)GemSounds.play(p,name);
        if(!p.getAbilities().instabuild)consume(s,spent);
        return InteractionResultHolder.consume(s);
    }
    @Override public InteractionResult useOn(UseOnContext ctx){
        if(!Set.of("orderstone","munchstone").contains(name))return InteractionResult.PASS;
        Player p=ctx.getPlayer();if(p==null)return InteractionResult.PASS;
        Level level=ctx.getLevel();BlockPos pos=ctx.getClickedPos();ItemStack s=ctx.getItemInHand();
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(charge(s)<=0||!level.mayInteract(p,pos))return InteractionResult.FAIL;
        Block block=level.getBlockState(pos).getBlock();int spent=1;
        if(name.equals("munchstone")){
            var state=level.getBlockState(pos);int hunger=MunchstoneFood.value(state);
            if(hunger==0)return InteractionResult.PASS;
            var food=p.getFoodData();int hungerMod=hunger;float saturationMod=1;
            if(food.getFoodLevel()+hunger+hungerMod>20)hungerMod=Math.max(0,20-food.getFoodLevel()-hunger);
            if(food.getSaturationLevel()+1+saturationMod>20)saturationMod=Math.max(0,20-food.getSaturationLevel()-1);
            spent=hungerMod+(int)saturationMod;
            if(spent<=0)return InteractionResult.SUCCESS;
            level.removeBlock(pos,false);food.eat(hunger,1);GemSounds.play(p,name);
        }else{
            Map<Block,Block> transformations=Map.ofEntries(
                Map.entry(Blocks.GRAVEL,Blocks.COBBLESTONE),Map.entry(Blocks.COBBLESTONE,Blocks.STONE),
                Map.entry(Blocks.MOSSY_COBBLESTONE,Blocks.COBBLESTONE),Map.entry(Blocks.SAND,Blocks.COARSE_DIRT),
                Map.entry(Blocks.COARSE_DIRT,Blocks.DIRT),Map.entry(Blocks.DIRT,Blocks.MYCELIUM),Map.entry(Blocks.MYCELIUM,Blocks.PODZOL),
                Map.entry(Blocks.PODZOL,Blocks.GRASS_BLOCK),Map.entry(Blocks.CRACKED_STONE_BRICKS,Blocks.STONE_BRICKS),
                Map.entry(Blocks.MOSSY_STONE_BRICKS,Blocks.STONE_BRICKS),Map.entry(Blocks.STONE_BRICKS,Blocks.CHISELED_STONE_BRICKS),
                Map.entry(Blocks.DAMAGED_ANVIL,Blocks.CHIPPED_ANVIL),Map.entry(Blocks.CHIPPED_ANVIL,Blocks.ANVIL));
            if(block==Blocks.FURNACE&&!level.getBlockState(pos).getValue(FurnaceBlock.LIT)){
                level.setBlockAndUpdate(pos,level.getBlockState(pos).setValue(FurnaceBlock.LIT,true));
                if(!p.getAbilities().instabuild)consume(s,1);return InteractionResult.CONSUME;
            }
            Block replacement=transformations.get(block);if(replacement==null)return InteractionResult.PASS;
            if(block instanceof AnvilBlock)spent=25;else if(block==Blocks.STONE_BRICKS)spent=4;
            var next=replacement.defaultBlockState();
            if(block instanceof AnvilBlock)next=next.setValue(AnvilBlock.FACING,level.getBlockState(pos).getValue(AnvilBlock.FACING));
            level.setBlockAndUpdate(pos,next);
        }
        if(!p.getAbilities().instabuild)consume(s,spent);return InteractionResult.CONSUME;
    }
}
