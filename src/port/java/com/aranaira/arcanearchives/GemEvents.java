package com.aranaira.arcanearchives;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=AA.ID)
public final class GemEvents {
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END&&!event.player.level().isClientSide)GemRules.tick(event.player);}
    @SubscribeEvent public static void endTick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END)return;GemRules.finishExperience();}
    @SubscribeEvent public static void damage(LivingAttackEvent event){
        if(event.getEntity() instanceof Player p&&!p.level().isClientSide){GemRules.incoming(p,event.getSource());if(GemRules.fireProtection(p,event.getSource()))event.setCanceled(true);}
    }
    @SubscribeEvent public static void crit(CriticalHitEvent event){
        if(event.getEntity().level().isClientSide)return;
        for(ItemStack s:GemRules.held(event.getEntity()))if(((GemItem)s.getItem()).name.equals("murdergleam")){
            if(GemItem.active(s)){event.setDamageModifier(1.5f);event.setResult(Event.Result.ALLOW);GemItem.consume(s,1);if(GemItem.charge(s)==0)GemItem.toggle(s,false);}
            else if(event.isVanillaCritical()){GemItem.restore(s,3);if(GemItem.charge(s)==GemItem.maximum(s))GemItem.toggle(s,true);}
        }
    }
    @SubscribeEvent public static void xp(PlayerXpEvent.PickupXp event){
        Player p=event.getEntity();if(p.level().isClientSide)return;
        GemRules.beginExperience(event.getOrb());
        for(ItemStack s:GemRules.held(p)){
            int amount=event.getOrb().getValue();
            String name=((GemItem)s.getItem()).name;
            if(name.equals("orderstone"))GemItem.restore(s,amount);
            if(name.equals("mindspindle")&&GemItem.charge(s)>0){GemRules.experience(event.getOrb(),Math.round(amount*1.5f));GemItem.consume(s,amount);}
        }
    }
    @SubscribeEvent public static void death(LivingDeathEvent event){
        if(event.getSource().getEntity() instanceof Player p&&!p.level().isClientSide){
            for(ItemStack s:GemRules.held(p)){
                String name=((GemItem)s.getItem()).name;
                if(name.equals("slaughtergleam"))GemItem.consume(s,1);
                if(name.equals("salvegleam")&&event.getEntity() instanceof Animal){GemItem.restore(s,(int)event.getEntity().getMaxHealth()/2);if(GemItem.charge(s)==GemItem.maximum(s))GemItem.toggle(s,true);}
            }
        }
    }
    @SubscribeEvent public static void looting(LootingLevelEvent event){
        if(event.getDamageSource()!=null&&event.getDamageSource().getEntity() instanceof Player p)
            for(ItemStack s:GemRules.held(p))if(((GemItem)s.getItem()).name.equals("slaughtergleam")&&GemItem.charge(s)>0)event.setLootingLevel(event.getLootingLevel()+1);
    }
    @SubscribeEvent public static void potion(LivingEntityUseItemEvent.Start event){
        if(!(event.getEntity() instanceof Player p)||p.level().isClientSide||!(event.getItem().getItem() instanceof PotionItem))return;
        for(ItemStack s:GemRules.held(p))if(((GemItem)s.getItem()).name.equals("elixirspindle")&&GemItem.charge(s)>0){
            List<MobEffectInstance> effects=GemRules.potionEffects(event.getItem());
            if(!effects.isEmpty()&&effects.stream().noneMatch(e->p.hasEffect(e.getEffect()))){
                effects.forEach(e->p.addEffect(new MobEffectInstance(e)));GemItem.consume(s,1);
            }event.setCanceled(true);return;
        }
    }
    @SubscribeEvent public static void teleport(EntityTeleportEvent.EnderEntity event){
        if(event.getEntity().level().isClientSide)return;
        for(Player p:event.getEntity().level().getEntitiesOfClass(Player.class,event.getEntity().getBoundingBox().inflate(10.5)))
            for(ItemStack s:GemRules.held(p))if(((GemItem)s.getItem()).name.equals("switchgleam")){GemItem.restore(s,3);event.setCanceled(true);}
    }
}
