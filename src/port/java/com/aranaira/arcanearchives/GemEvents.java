package com.aranaira.arcanearchives;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.Mod;
@net.neoforged.fml.common.EventBusSubscriber(modid=AA.ID)
public final class GemEvents {
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){if(!event.getEntity().level().isClientSide())GemRules.tick(event.getEntity());}
    @SubscribeEvent public static void endTick(ServerTickEvent.Post event){GemRules.finishExperience();}
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent event){
        if(event.getEntity() instanceof Player p&&!p.level().isClientSide()){GemRules.incoming(p,event.getSource());if(GemRules.fireProtection(p,event.getSource()))event.setCanceled(true);}
    }
    @SubscribeEvent public static void crit(CriticalHitEvent event){
        if(event.getEntity().level().isClientSide())return;
        for(ItemStack s:GemRules.held(event.getEntity()))if(((GemItem)s.getItem()).name.equals("murdergleam")){
            if(GemItem.active(s)){event.setDamageMultiplier(1.5f);event.setCriticalHit(true);GemItem.consume(s,1);if(GemItem.charge(s)==0)GemItem.toggle(s,false);}
            else if(event.isVanillaCritical()){GemItem.restore(s,3);if(GemItem.charge(s)==GemItem.maximum(s))GemItem.toggle(s,true);}
        }
    }
    @SubscribeEvent public static void xp(PlayerXpEvent.PickupXp event){
        Player p=event.getEntity();if(p.level().isClientSide())return;
        GemRules.beginExperience(event.getOrb());
        for(ItemStack s:GemRules.held(p)){
            int amount=event.getOrb().getValue();
            String name=((GemItem)s.getItem()).name;
            if(name.equals("orderstone"))GemItem.restore(s,amount);
            if(name.equals("mindspindle")&&GemItem.charge(s)>0){GemRules.experience(event.getOrb(),Math.round(amount*1.5f));GemItem.consume(s,amount);}
        }
    }
    @SubscribeEvent public static void death(LivingDeathEvent event){
        if(event.getSource().getEntity() instanceof Player p&&!p.level().isClientSide()){
            for(ItemStack s:GemRules.held(p)){
                String name=((GemItem)s.getItem()).name;
                if(name.equals("slaughtergleam"))GemItem.consume(s,1);
                if(name.equals("salvegleam")&&event.getEntity() instanceof Animal){GemItem.restore(s,(int)event.getEntity().getMaxHealth()/2);if(GemItem.charge(s)==GemItem.maximum(s))GemItem.toggle(s,true);}
            }
        }
    }
    @SubscribeEvent public static void potion(LivingEntityUseItemEvent.Start event){
        if(!(event.getEntity() instanceof Player p)||p.level().isClientSide()||!(event.getItem().getItem() instanceof PotionItem))return;
        for(ItemStack s:GemRules.held(p))if(((GemItem)s.getItem()).name.equals("elixirspindle")&&GemItem.charge(s)>0){
            List<MobEffectInstance> effects=GemRules.potionEffects(event.getItem());
            if(!effects.isEmpty()&&effects.stream().noneMatch(e->p.hasEffect(e.getEffect()))){
                effects.forEach(e->p.addEffect(new MobEffectInstance(e)));GemItem.consume(s,1);
            }event.setCanceled(true);return;
        }
    }
    @SubscribeEvent public static void teleport(EntityTeleportEvent.EnderEntity event){
        if(event.getEntity().level().isClientSide())return;
        for(Player p:event.getEntity().level().getEntitiesOfClass(Player.class,event.getEntity().getBoundingBox().inflate(10.5)))
            for(ItemStack s:GemRules.held(p))if(((GemItem)s.getItem()).name.equals("switchgleam")){GemItem.restore(s,3);event.setCanceled(true);}
    }
}
