package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.AA;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;

/** Original, unmodified256px atlases; UVs and controls come from the legacy GUI source. */
public final class LegacyGui {
    public static void blit(GuiGraphicsExtractor g,String file,int x,int y,int u,int v,int w,int h){
        String prefix=!ClientPreferences.prettyGuis&&java.util.Set.of("gemcutterstable","radiantchest","radiantcraftingtable","manifest_base","manifest_overlay","player_inv","buttons","single_slot").contains(file)?"simple/":"";
        int size=file.equals("buttons")?16:256;
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,AA.id("textures/gui/"+prefix+file+".png"),x,y,u,v,w,h,size,size);
    }
    public static boolean hit(double x,double y,int left,int top,int width,int height){return x>=left&&y>=top&&x<left+width&&y<top+height;}
    public static void count(GuiGraphicsExtractor g,net.minecraft.client.gui.Font font,String text,int x,int y){g.pose().pushMatrix();g.pose().scale(.75f,.75f);g.text(font,text,(int)((x+17-font.width(text)*.75f)/.75f),(int)((y+9.7778f)/.75f),0xffffffff,true);g.pose().popMatrix();}
    public static String manifestCount(long count){if(count<1000)return Long.toString(count);if(count<1024)return "1K";int unit=(64-Long.numberOfLeadingZeros(count))/10;double adjusted=(double)count/Math.pow(1024,unit);if(adjusted<100)return String.format(java.util.Locale.ROOT,"%.0f%c",adjusted," KMGTPE".charAt(unit));return String.format(java.util.Locale.ROOT,"%.1f%c",(double)count/Math.pow(1024,unit+1)," KMGTPE".charAt(unit+1));}
    public static boolean shift(){var window=Minecraft.getInstance().getWindow();return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,340)||com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,344);}
    public static void query(int menu,String kind,int value){var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("Menu",menu);tag.putString("Kind",kind);tag.putInt("Value",value);com.aranaira.arcanearchives.ArchiveNetwork.query(tag);}
    public static void textQuery(int menu,String kind,String value){var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("Menu",menu);tag.putString("Kind",kind);tag.putString("Value",value);com.aranaira.arcanearchives.ArchiveNetwork.query(tag);}
    public static void hoverQuery(int menu,int page,int slot){var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("Menu",menu);tag.putString("Kind","manifest_hover");tag.putInt("Page",page);tag.putInt("Value",slot);com.aranaira.arcanearchives.ArchiveNetwork.query(tag);}
    public static final class Hotspot extends Button {
        private final boolean label;
        public Hotspot(int x,int y,int w,int h,String message,boolean label,Runnable pressed){super(x,y,w,h,Component.literal(message),button->pressed.run(),DEFAULT_NARRATION);this.label=label;setTooltip(Tooltip.create(Component.literal(message)));}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
            if(label){var font=Minecraft.getInstance().font;g.text(font,getMessage(),getX()+(width-font.width(getMessage()))/2,getY()+(height-8)/2,isHoveredOrFocused()?0xffffe87a:0xfff6e6cc,true);}
        }
    }
}
