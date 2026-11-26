package com.aranaira.arcanearchives;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
public final class GemRules {
    private static final java.util.Map<net.minecraft.world.entity.ExperienceOrb,Integer> XP=new java.util.WeakHashMap<>();
    private static void orbValue(net.minecraft.world.entity.ExperienceOrb orb,int value){orb.setValue(value);}
    public static void beginExperience(net.minecraft.world.entity.ExperienceOrb orb){var previous=XP.remove(orb);if(previous!=null)orbValue(orb,previous);}
    public static void experience(net.minecraft.world.entity.ExperienceOrb orb,int value){XP.putIfAbsent(orb,orb.getValue());orbValue(orb,value);}
    /** Modern merged orbs represent several pickups. Only the current pickup gets the bonus. */
    public static void finishExperience(){XP.forEach(GemRules::orbValue);XP.clear();}
    public static List<MobEffectInstance> potionEffects(ItemStack stack){return java.util.stream.StreamSupport.stream(stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY).getAllEffects().spliterator(),false).toList();}
    public static boolean beneficial(MobEffectInstance effect){return effect.getEffect().value().isBeneficial();}
    public static int cleanse(LivingEntity target,boolean advanced){
        int cost=0;
        for(var effect:new ArrayList<>(target.getActiveEffects())){
            boolean basic=effect.getEffect().equals(MobEffects.HUNGER)||effect.getEffect().equals(MobEffects.NAUSEA)||effect.getEffect().equals(MobEffects.POISON);
            if(basic||advanced&&!beneficial(effect)){target.removeEffect(effect.getEffect());cost=Math.max(cost,basic?1:3);}
        }return cost;
    }
    public static void lightning(Level level,BlockPos pos,Player cause){
        LightningBolt bolt=new LightningBolt(EntityType.LIGHTNING_BOLT,level);
        bolt.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);if(cause instanceof ServerPlayer p)bolt.setCause(p);level.addFreshEntity(bolt);
    }
    public static List<ItemStack> held(Player p){
        List<ItemStack> result=new ArrayList<>();
        List<ItemStack> equipped=new ArrayList<>(List.of(p.getMainHandItem(),p.getOffhandItem()));
        equipped.addAll(com.aranaira.arcanearchives.integration.CuriosBridge.equipped(p));
        Set<ItemStack> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        for(ItemStack stack:equipped){
            if(!seen.add(stack))continue;
            if(stack.getItem() instanceof GemItem)result.add(stack);
            else if(stack.is(AA.item("gemsocket"))){
                ItemStack gem=SocketMenu.equippedGem(stack,p);
                if(gem.getItem() instanceof GemItem)result.add(gem);
            }
        }
        return result;
    }
    public static void tick(Player p){
        if(p.tickCount%20==0){
            for(int i=0;i<36;i++){var stack=p.getInventory().getItem(i);if(stack.is(AA.item("radiant_amphora")))Amphora.sync(p,stack);}
            if(p.getOffhandItem().is(AA.item("radiant_amphora")))Amphora.sync(p,p.getOffhandItem());
        }
        for(ItemStack s:held(p)){
            GemItem item=(GemItem)s.getItem();
            switch(item.name){
                case"salvegleam"->{if(GemItem.active(s)&&p.getHealth()<p.getMaxHealth()){
                    var tag=StackData.data(s);int pulse=tag.contains("pulse")?tag.getIntOr("pulse",0)-1:0;
                    if(pulse<=0){p.heal(1);GemItem.consume(s,1);pulse=20;if(GemItem.charge(s)==0)GemItem.toggle(s,false);tag=StackData.data(s);}
                    tag.putInt("pulse",pulse);StackData.data(s,tag);SocketMenu.saveGemToParent(s);
                }}
                case"agegleam"->{if(GemItem.charge(s)<GemItem.maximum(s)){CompoundTag t=StackData.data(s);int timer=t.contains("recharge")?t.getIntOr("recharge",0)-1:3600;if(timer<=0){GemItem.restore(s,1);timer=3600;t=StackData.data(s);}t.putInt("recharge",timer);StackData.data(s,t);SocketMenu.saveGemToParent(s);}}
                case"stormway"->{if(GemItem.charge(s)<GemItem.maximum(s)&&p.level().isRaining()){
                    GemItem.restore(s,-1);if(ArchiveConfig.SOUNDS.get())p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
                }}
            }
        }
    }
    public static boolean fireProtection(Player p,DamageSource source){
        for(ItemStack s:held(p))if(((GemItem)s.getItem()).name.equals("phoenixway")&&source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)&&!p.hasEffect(MobEffects.FIRE_RESISTANCE)){
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,600));GemItem.consume(s,12);
            if(ArchiveConfig.SOUNDS.get())p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,net.minecraft.sounds.SoundSource.PLAYERS,1,1);return true;
        }return false;
    }
    private static boolean undead(LivingEntity e){return e.isInvertedHealAndHarm();}
    public static void incoming(Player p,DamageSource source){
        for(ItemStack gem:held(p))if(((GemItem)gem.getItem()).name.equals("stormway")&&GemItem.charge(gem)>0){
            if(source.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT)){
                CompoundTag tag=StackData.data(gem);long last=tag.getLongOr("cooldown",0L),now=System.currentTimeMillis();
                if(last+1000<now){
                    p.addEffect(new MobEffectInstance(MobEffects.SPEED,1200,2));
                    p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,1200));
                    p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,1200));
                    GemItem.consume(gem,6);tag=StackData.data(gem);tag.putLong("cooldown",now);StackData.data(gem,tag);SocketMenu.saveGemToParent(gem);
                }
            }
            if(source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)&&source.getEntity() instanceof Mob mob&&GemItem.active(gem)){
                lightning(p.level(),mob.blockPosition(),p);
                mob.addEffect(new MobEffectInstance(undead(mob)?MobEffects.INSTANT_HEALTH:MobEffects.INSTANT_DAMAGE,1,10));
                GemItem.consume(gem,1);
            }
        }
    }
}
