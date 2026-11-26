package com.aranaira.arcanearchives.client;
import com.aranaira.arcanearchives.*;
import java.util.Locale;
import net.minecraft.nbt.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Localized names are client data. The server still owns counts and every transfer. */
public final class ClientSearch {
    public static void send(int menu,String text){
        var tag=new CompoundTag();tag.putString("Kind","search");tag.putInt("Menu",menu);tag.putString("Query",text);
        var ids=new ListTag();String query=text.toLowerCase(Locale.ROOT);
        if(!query.isBlank()&&!query.startsWith("@")&&!query.startsWith("$"))for(var item:BuiltInRegistries.ITEM){
            if(ids.size()>=1024)break;
            if(new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))ids.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
        }
        tag.put("Items",ids);ArchiveNetwork.query(tag);
    }
    public static void track(ItemStack stack){
        var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null||stack.isEmpty())return;
        var tag=new CompoundTag();tag.putString("Kind","track");tag.putInt("Menu",mc.player.containerMenu.containerId);
        tag.put("Stack",StackData.save(stack.copyWithCount(1),mc.level));ArchiveNetwork.query(tag);
    }
}
