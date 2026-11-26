package com.aranaira.arcanearchives;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ArchiveItem extends Item {
    public final String name;
    public ArchiveItem(String name,Properties props) { super(props);this.name=name; }
    @Override public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack,CompoundTag nbt){return name.equals("radiant_amphora")?new AmphoraProvider(stack):super.initCapabilities(stack,nbt);}
    public static void tell(Player p,String text) { p.displayClientMessage(Component.literal(text),false); }
    public boolean handlesBlocks() { return Set.of("scepter_revelation","scepter_manipulation","radiant_key","radiant_amphora","storage_shaped_quartz","devouring_charm","debugorb").contains(name); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack held=player.getItemInHand(hand);
        if(isLetter()){player.startUsingItem(hand);return InteractionResultHolder.consume(held);}
        if(!level.isClientSide) {
            if(name.equals("manifest")) ManifestMenu.open((ServerPlayer)player);
            else if(name.equals("gemsocket"))SocketMenu.open((ServerPlayer)player,held);
            else if(name.equals("devouring_charm"))DevouringCharmMenu.open((ServerPlayer)player,held);
            else if(name.equals("radiant_amphora")) Amphora.use(player,held);
            else if(name.equals("tome_arcana")) {
                var book=new CompoundTag();book.putString("Kind","guidebook");ArchiveNetwork.view((ServerPlayer)player,book);
            }
        }
        return InteractionResultHolder.sidedSuccess(held,level.isClientSide);
    }
    private boolean isLetter(){return Set.of("letter_invitation","letter_resignation","writ_expulsion").contains(name);}
    @Override public int getUseDuration(ItemStack stack){return isLetter()?64:super.getUseDuration(stack);}
    @Override public UseAnim getUseAnimation(ItemStack stack){return isLetter()?UseAnim.BOW:super.getUseAnimation(stack);}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,net.minecraft.world.entity.LivingEntity user){
        if(isLetter()&&!level.isClientSide&&user instanceof ServerPlayer player)triggerLetter(player,stack);
        return stack;
    }
    private void triggerLetter(ServerPlayer player,ItemStack held){Level level=player.level();
            if(name.equals("letter_invitation")) {
                CompoundTag tag=StackData.data(held);
                if(!tag.hasUUID("Issuer")) { tag.putUUID("Issuer",player.getUUID()); StackData.data(held,tag); tell(player,"Invitation signed. Give it to the player you wish to invite."); }
                else if(NetworkData.get(level).join(tag.getUUID("Issuer"),player.getUUID())) { held.shrink(1); tell(player,"Joined the archive network."); }
            } else if(name.equals("letter_resignation")) {
                var tag=StackData.data(held);var data=NetworkData.get(level);
                if(tag.hasUUID("Issuer")&&tag.getUUID("Issuer").equals(player.getUUID())&&!data.network(player.getUUID()).equals(player.getUUID())){
                    data.leave(player.getUUID());held.shrink(1);tell(player,"Left the shared archive network.");
                }
            } else if(name.equals("writ_expulsion")){
                var issuer=StackData.data(held);String target=StackData.customName(held);java.util.UUID uuid=target.isBlank()?null:ArchiveProfiles.uuid((ServerPlayer)player,target);
                if(issuer.hasUUID("Issuer")&&issuer.getUUID("Issuer").equals(player.getUUID())&&uuid!=null&&NetworkData.get(level).expel(player.getUUID(),uuid)){held.shrink(1);tell(player,"Member removed from your archive network: "+target);}
                else tell(player,"Name your own expulsion writ on an anvil with the member's account name, then use it.");
            }
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){return name.equals("raw_quartz")?QuartzInteractions.convert(context):InteractionResult.PASS;}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player p,net.minecraft.world.entity.LivingEntity target,InteractionHand hand){
        if(!name.equals("writ_expulsion")||!(target instanceof Player member))return InteractionResult.PASS;
        if(p.level().isClientSide)return InteractionResult.SUCCESS;
        var tag=StackData.data(stack);if(tag.hasUUID("Issuer")&&tag.getUUID("Issuer").equals(p.getUUID())&&NetworkData.get(p.level()).expel(p.getUUID(),member.getUUID())){stack.shrink(1);tell(p,"Member removed from your archive network.");return InteractionResult.CONSUME;}
        return InteractionResult.FAIL;
    }
    @Override public InteractionResult useOn(UseOnContext ctx) {
        Player p=ctx.getPlayer(); Level level=ctx.getLevel();
        if(p==null) return InteractionResult.PASS;
        if(name.equals("raw_quartz"))return QuartzInteractions.convert(ctx);
        if(!(level.getBlockEntity(ctx.getClickedPos()) instanceof ArchiveBlockEntity tile)) return InteractionResult.PASS;
        if(level.isClientSide) return InteractionResult.SUCCESS;
        if(!tile.allowed(p)) return InteractionResult.FAIL;
        ItemStack held=ctx.getItemInHand();
        if(name.equals("radiant_amphora")&&p.isShiftKeyDown())return Amphora.bind(p,held,tile)?InteractionResult.CONSUME:InteractionResult.FAIL;
        if(name.equals("debugorb")){
            if(!p.getAbilities().instabuild)return InteractionResult.FAIL;
            if(p.isShiftKeyDown()&&tile.kind().equals("radiant_trove")){
                tile.inventory.setStackInSlot(0,tile.inventory.getStackInSlot(0).isEmpty()?new ItemStack(Items.SNOWBALL,tile.inventory.getSlotLimit(0)):ItemStack.EMPTY);
                tile.changed();
            }else tell(p,tile.kind()+" at "+tile.getBlockPos().toShortString()+" | owner "+tile.owner+" | "+tile.inventory.getSlots()+" slots | routing weight "+NetworkData.routingWeight(tile,new ItemStack(Items.SNOWBALL)));
        }else if(name.equals("scepter_revelation")) {
            tell(p,tile.kind()+" | network owner: "+tile.owner);
            long count=0; for(int i=0;i<tile.inventory.getSlots();i++)count+=tile.inventory.getStackInSlot(i).getCount();
            tell(p,"Items: "+count+" | fluid: "+tile.tank.getFluidAmount()+" mB | upgrades: "+tile.upgrades);
        } else if(name.equals("scepter_manipulation")) {
            if(tile.kind().equals("brazier_of_hoarding")){tile.openControls((ServerPlayer)p);}
            else if(Set.of("radiant_trove","radiant_tank").contains(tile.kind())) { UpgradeMenu.open((ServerPlayer)p,tile); }
            else if(tile.kind().equals("radiant_chest")){if(!tile.configureChestDisplay(p,ctx.getClickedFace()))tile.openControls((ServerPlayer)p);}
            else {tile.noNew=!tile.noNew;tell(p,"Accept new item types: "+!tile.noNew);}
            tile.changed();
        } else if(name.equals("radiant_key")) {
            if(!Set.of("radiant_trove","radiant_tank").contains(tile.kind())||tile.locked)return InteractionResult.FAIL;for(int i=0;i<3;i++)if(tile.optionalModules[i].isEmpty()){tile.optionalModules[i]=held.copyWithCount(1);break;}tile.locked=true;if(!p.getAbilities().instabuild)held.shrink(1);tile.changed();tell(p,"Storage reference locked.");
        } else if(name.equals("devouring_charm") && Set.of("radiant_trove","radiant_tank").contains(tile.kind()) && !tile.voiding) {
            for(int i=0;i<3;i++)if(tile.optionalModules[i].isEmpty()){tile.optionalModules[i]=held.copyWithCount(1);break;}tile.voiding=true;if(!p.getAbilities().instabuild)held.shrink(1);tile.changed();tell(p,"Overflow voiding enabled.");
        } else return InteractionResult.PASS;
        return InteractionResult.CONSUME;
    }
}
