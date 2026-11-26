package com.aranaira.arcanearchives.mixin;
import com.aranaira.arcanearchives.*;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** NeoForge replaced the old Forge looting-level event with native enchantment effects. */
@Mixin(value=EnchantmentHelper.class,remap=false)
public abstract class SlaughtergleamLootMixin {
    @Inject(method="getEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;)I",at=@At("RETURN"),cancellable=true,remap=false)
    private static void archiveLooting(Holder<Enchantment> enchantment,LivingEntity entity,CallbackInfoReturnable<Integer> result){
        if(!enchantment.is(Enchantments.LOOTING)||!(entity instanceof Player player))return;
        int bonus=0;
        for(ItemStack gem:GemRules.held(player))if(((GemItem)gem.getItem()).name.equals("slaughtergleam")&&GemItem.charge(gem)>0)bonus++;
        if(bonus>0)result.setReturnValue(result.getReturnValueI()+bonus);
    }
}
