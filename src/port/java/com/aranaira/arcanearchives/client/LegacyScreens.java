package com.aranaira.arcanearchives.client;
import com.aranaira.arcanearchives.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;

/** Layouts and UVs recovered from the supplied0.2.2-beta GUI classes and textures. */
public final class LegacyScreens {
    public static class Socket extends AbstractContainerScreen<SocketMenu>{
        public Socket(SocketMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=181;imageHeight=145;}
        @Override protected void init(){super.init();addRenderableWidget(new LegacyGui.Hotspot(leftPos+153,topPos+2,18,18,"Recharge gems",false,()->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)));}
        @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){
            LegacyGui.blit(g,ClientPreferences.prettyGuis?"player_inv":"single_slot",leftPos+(ClientPreferences.prettyGuis?-32:-29),topPos+(ClientPreferences.prettyGuis?106:109),ClientPreferences.prettyGuis?207:32,0,ClientPreferences.prettyGuis?49:28,ClientPreferences.prettyGuis?33:28);
            LegacyGui.blit(g,"player_inv",leftPos,topPos+44,0,0,181,101);
            LegacyGui.blit(g,"player_inv",leftPos+153,topPos+2,238,33,18,18);
            if(ClientPreferences.prettyGuis){LegacyGui.blit(g,"player_inv",leftPos+42,topPos+11,0,101,94,37);LegacyGui.blit(g,"fabrial",leftPos+78,topPos,102,0,22,22);}
            else LegacyGui.blit(g,"single_slot",leftPos+73,topPos-5,0,0,32,32);
        }
        @Override protected void renderLabels(GuiGraphics g,int x,int y){}
        @Override public void render(GuiGraphics g,int x,int y,float p){super.render(g,x,y,p);renderTooltip(g,x,y);}
    }
    public static class Storage extends AbstractContainerScreen<ArchiveMenu>{
        private EditBox name;
        private boolean naming;
        public Storage(ArchiveMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=menu.station==1?206:192;imageHeight=menu.station==1?254:253;}
        @Override protected void init(){
            super.init();
            if(menu.station==1){
                addRenderableWidget(new LegacyGui.Hotspot(leftPos+26,topPos+69,10,18,"Previous recipes",false,()->send(30)));
                addRenderableWidget(new LegacyGui.Hotspot(leftPos+170,topPos+69,10,18,"Next recipes",false,()->send(31)));
            }else if(menu.station==2){
                name=new EditBox(font,leftPos+53,topPos+238,88,12,Component.literal("Archive name"));name.setMaxLength(64);name.setBordered(false);name.setValue(menu.chestName);
                name.setResponder(value->{if(!naming)LegacyGui.textQuery(menu.containerId,"chest_name",value);});addRenderableWidget(name);
                addRenderableWidget(new LegacyGui.Hotspot(leftPos+161,topPos+236,12,12,"Only accept existing item stacks",false,()->send(13)));
            }
        }
        private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
        @Override protected void containerTick(){super.containerTick();if(name!=null&&!name.isFocused()&&!name.getValue().equals(menu.chestName)){naming=true;name.setValue(menu.chestName);naming=false;}}
        @Override protected void renderBg(GuiGraphics g,float p,int x,int y){
            LegacyGui.blit(g,menu.station==1?"gemcutterstable":"radiantchest",leftPos,topPos,0,0,imageWidth,menu.station==1?256:253);
            if(menu.station==2&&menu.chestNoNew!=0){LegacyGui.blit(g,"radiantchest",leftPos+164,topPos+239,234,0,6,6);LegacyGui.blit(g,"radiantchest",leftPos+176,topPos+234,240,0,16,16);}
        }
        private void decorateSlot(GuiGraphics g,Slot slot){
            if(menu.station==1&&slot.index>=ArchiveMenu.GCT_CHOICES&&slot.index<ArchiveMenu.GCT_PLAYER&&!slot.getItem().isEmpty()){
                int choice=slot.index-ArchiveMenu.GCT_CHOICES;
                if(menu.recipePage*7+choice==menu.selectedRecipeIndex)LegacyGui.blit(g,"gemcutterstable",slot.x-2,slot.y-2,206,0,20,20);
                if(!menu.recipeAvailable(choice))g.fill(slot.x,slot.y,slot.x+16,slot.y+16,ClientPreferences.prettyGuis?0xaa1e3340:0x80808080);
            }
            if(menu.station==2&&slot.index<54&&menu.counts[slot.index]>1)LegacyGui.count(g,font,Integer.toString(menu.counts[slot.index]),slot.x,slot.y);
        }
        @Override protected void renderLabels(GuiGraphics g,int x,int y){for(var slot:menu.slots)if(slot.isActive())decorateSlot(g,slot);}
        @Override public boolean mouseScrolled(double x,double y,double scrollX,double delta){if(menu.station==1){send(delta>0?32:33);return true;}return super.mouseScrolled(x,y,scrollX,delta);}
        @Override public void render(GuiGraphics g,int x,int y,float p){super.render(g,x,y,p);renderTooltip(g,x,y);}
        @Override protected void renderTooltip(GuiGraphics g,int x,int y){var lines=hoveredSlot==null?java.util.List.<Component>of():RecipeTooltips.forSlot(menu,hoveredSlot.index);if(lines.isEmpty())super.renderTooltip(g,x,y);else g.renderTooltip(font,lines,java.util.Optional.empty(),x,y);}
        @Override public boolean keyPressed(int key,int scan,int modifiers){if(name!=null&&name.isFocused()&&key!=256)return name.keyPressed(key,scan,modifiers);return super.keyPressed(key,scan,modifiers);}
        @Override public boolean mouseClicked(double x,double y,int button){if(name!=null&&button==1&&name.isMouseOver(x,y)){name.setValue("");setFocused(name);name.setFocused(true);return true;}return super.mouseClicked(x,y,button);}
    }
    public static class Manifest extends AbstractContainerScreen<ManifestMenu>{
        private EditBox search;
        private final String storedJei=JeiBridge.read.get();
        private boolean dragging;
        private boolean initialized;
        private int shownMode=-1;
        private int hoverIndex=-1,hoverPage=-1;private long hoverAt;
        public Manifest(ManifestMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=200;imageHeight=224;}
        @Override protected void init(){
            String previous=search==null?(ClientPreferences.persistSearch?ClientPreferences.lastSearch:""):search.getValue();boolean first=!initialized;super.init();shownMode=menu.mode;
            topPos=Math.max(4,(height-(menu.mode==0?224:338))/2);
            search=new EditBox(font,leftPos+13,topPos+13,89,11,Component.literal("Search"));search.setMaxLength(64);search.setBordered(false);search.setTextColor(0xfff4e7d1);
            search.setValue(previous);search.setResponder(text->{ClientSearch.send(menu.containerId,text);if(ClientPreferences.syncJei)JeiBridge.write.accept(text);});addRenderableWidget(search);if(first&&!previous.isEmpty())ClientSearch.send(menu.containerId,previous);initialized=true;
            addRenderableWidget(new LegacyGui.Hotspot(leftPos+48,topPos+200,88,12,"End Tracking",true,()->send(2)));
            addRenderableWidget(new LegacyGui.Hotspot(leftPos+178,topPos+200,14,14,"Refresh network",false,()->send(6)));
            addRenderableWidget(new LegacyGui.Hotspot(leftPos+158,topPos+200,14,14,"Manifest options",false,()->minecraft.setScreen(new ManifestOptionsScreen(this))));
            addRenderableWidget(new LegacyGui.Hotspot(leftPos+112,topPos+10,14,14,"Name / quantity",false,()->send(4)));
            addRenderableWidget(new LegacyGui.Hotspot(leftPos+130,topPos+10,14,14,"Reverse order",false,()->send(5)));
            addRenderableWidget(new LegacyGui.Hotspot(leftPos+148,topPos+10,14,14,"Synchronize JEI search",false,()->{ClientPreferences.syncJei=!ClientPreferences.syncJei;ClientPreferences.save();}));
        }
        private void send(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
        @Override protected void containerTick(){super.containerTick();if(shownMode!=menu.mode){clearWidgets();init();}if(ClientPreferences.syncJei){String text=JeiBridge.read.get();if(text!=null&&!text.equals(search.getValue()))search.setValue(text);}}
        private int networkSlot(double x,double y){if(!LegacyGui.hit(x,y,leftPos+12,topPos+30,162,162))return -1;return ((int)(y-topPos-30)+(menu.page%3)*6)/18*9+(int)(x-leftPos-12)/18;}
        @Override protected void renderBg(GuiGraphics g,float p,int mouseX,int mouseY){
            LegacyGui.blit(g,"manifest_base",leftPos,topPos,0,0,200,224);
            g.enableScissor(leftPos+11,topPos+29,leftPos+175,topPos+192);int hover=networkSlot(mouseX,mouseY);
            for(int i=0;i<ManifestMenu.DISPLAY_SLOTS;i++){
                var stack=menu.getSlot(i).getItem();if(stack.isEmpty())continue;int x=leftPos+12+i%9*18,y=topPos+30+i/9*18-(menu.page%3)*6;
                if(ClientPreferences.manifestGrid)LegacyGui.blit(g,"manifest_base",x-1,y-1,224,0,18,18);
                if(ArchiveViews.matches(stack))g.fill(x,y,x+16,y+16,0x8086beff);
                g.renderItem(stack,x,y);g.renderItemDecorations(font,stack,x,y,"");if(menu.counts[i]>1){String count=LegacyGui.manifestCount(menu.counts[i]);g.drawString(font,count,x+17-font.width(count),y+9,0xffffffff,true);}
                if(i==hover)g.fill(x,y,x+16,y+16,0x60ffffff);
            }
            g.disableScissor();LegacyGui.blit(g,"manifest_overlay",leftPos,topPos,0,0,200,224);
            LegacyGui.blit(g,"manifest_base",leftPos+112,topPos+10,242,28+menu.sort*14,14,14);
            LegacyGui.blit(g,"manifest_base",leftPos+130,topPos+10,242,56+(menu.descending==0?14:0),14,14);
            LegacyGui.blit(g,"manifest_base",leftPos+148,topPos+10,242,84+(ClientPreferences.syncJei?14:0),14,14);
            LegacyGui.blit(g,"buttons",leftPos+178,topPos+29+(menu.pages<=1?0:menu.page*150/(menu.pages-1)),0,0,12,12);
            if(menu.mode==1)LegacyGui.blit(g,"player_inv",leftPos+9,topPos+230,0,0,181,101);
        }
        @Override protected void renderLabels(GuiGraphics g,int x,int y){}
        @Override public void render(GuiGraphics g,int x,int y,float p){super.render(g,x,y,p);ClientPreferences.lastSearch=search.getValue();renderTooltip(g,x,y);}
        @Override protected void renderTooltip(GuiGraphics g,int x,int y){int index=networkSlot(x,y);if(index>=0){hoveredSlot=menu.getSlot(index);if(!hoveredSlot.getItem().isEmpty()){long now=minecraft.level.getGameTime();if(index!=hoverIndex||menu.page!=hoverPage||now-hoverAt>20){LegacyGui.hoverQuery(menu.containerId,menu.page,index);hoverIndex=index;hoverPage=menu.page;hoverAt=now;}var lines=new java.util.ArrayList<Component>();lines.add(hoveredSlot.getItem().getHoverName());lines.add(Component.literal("Total: "+menu.counts[index]));for(var line:menu.locationLines(index))lines.add(Component.literal(line));lines.add(Component.literal(menu.mode==0?"Left: track; right: stop tracking":"Left: stack; right: one"));g.renderTooltip(font,lines,java.util.Optional.empty(),x,y);}}else super.renderTooltip(g,x,y);}
        @Override public boolean mouseScrolled(double x,double y,double scrollX,double delta){if(LegacyGui.hit(x,y,leftPos+11,topPos+29,182,163)){LegacyGui.query(menu.containerId,"manifest_scroll",menu.page+(delta>0?-1:1));return true;}return super.mouseScrolled(x,y,scrollX,delta);}
        private void drag(double y){LegacyGui.query(menu.containerId,"manifest_scroll",Math.max(0,Math.min(menu.pages-1,(int)Math.round((y-topPos-35)/150.0*(menu.pages-1)))));}
        @Override public boolean mouseClicked(double x,double y,int button){
            if(search.isMouseOver(x,y)){if(button==1){search.setValue("");setFocused(search);search.setFocused(true);return true;}if(button==0&&hasShiftDown()){String text=JeiBridge.read.get();if(text!=null)search.setValue(text);setFocused(search);search.setFocused(true);return true;}}
            if(button==0&&LegacyGui.hit(x,y,leftPos+178,topPos+29,12,162)){dragging=true;drag(y);return true;}
            int index=networkSlot(x,y);if(index>=0){minecraft.gameMode.handleInventoryMouseClick(menu.containerId,index,button,hasShiftDown()?ClickType.QUICK_MOVE:ClickType.PICKUP,minecraft.player);return true;}
            return super.mouseClicked(x,y,button);
        }
        @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(dragging&&button==0){drag(y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
        @Override public boolean mouseReleased(double x,double y,int button){dragging=false;return super.mouseReleased(x,y,button);}
        @Override public boolean keyPressed(int key,int scan,int modifiers){int delta=switch(key){case 265->-1;case 264->1;case 266->-27;case 267->27;default->0;};if(search.isFocused()&&key!=256){if(search.keyPressed(key,scan,modifiers))return true;if(delta==0)return false;}if(delta!=0){LegacyGui.query(menu.containerId,"manifest_scroll",menu.page+delta);return true;}return super.keyPressed(key,scan,modifiers);}
        @Override public void removed(){super.removed();if(ClientPreferences.syncJei&&storedJei!=null&&!storedJei.isEmpty())JeiBridge.write.accept(storedJei);}
    }
    public static class Crafting extends AbstractContainerScreen<PersistentCraftingMenu>{
        private int pointerX,pointerY;
        public Crafting(PersistentCraftingMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=206;imageHeight=203;}
        @Override protected void renderBg(GuiGraphics g,float p,int x,int y){LegacyGui.blit(g,"radiantcraftingtable",leftPos,topPos,0,0,256,256);}
        @Override protected void renderLabels(GuiGraphics g,int x,int y){for(var slot:menu.slots)if(slot.isActive())decorateSlot(g,slot);}
        private void decorateSlot(GuiGraphics g,Slot slot){
            boolean hover=isHovering(slot.x,slot.y,16,16,pointerX,pointerY);
            if(slot.index>=46&&slot.index<49&&hover){if(slot.getItem().isEmpty()){var preview=menu.getSlot(LegacyGui.shift()?50:49).getItem();if(!preview.isEmpty()){g.fill(slot.x,slot.y,slot.x+16,slot.y+16,0x80ffffff);g.renderItem(preview,slot.x,slot.y);g.renderItemDecorations(font,preview,slot.x,slot.y);}}
                else if(!LegacyGui.shift()&&(menu.bookmarkAvailability&(1<<(slot.index-46)))==0){g.fill(slot.x,slot.y,slot.x+16,slot.y+16,0xffbd1c1c);g.renderItem(slot.getItem(),slot.x,slot.y);g.renderItemDecorations(font,slot.getItem(),slot.x,slot.y);}}
        }
        @Override public void render(GuiGraphics g,int x,int y,float p){pointerX=x;pointerY=y;super.render(g,x,y,p);renderTooltip(g,x,y);}
    }
    public static class Charm extends AbstractContainerScreen<DevouringCharmMenu>{
        public Charm(DevouringCharmMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=181;imageHeight=300;}
        @Override protected void init(){super.init();addRenderableWidget(new LegacyGui.Hotspot(leftPos,topPos+60,25,15,"Flip charm",false,()->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)));}
        @Override protected void renderBg(GuiGraphics g,float p,int x,int y){LegacyGui.blit(g,"devouring_charm",leftPos+25,topPos+58,menu.flipped==0?0:126,menu.flipped==0?0:127,130,129);LegacyGui.blit(g,"devouring_charm",leftPos,topPos+60,231,0,25,15);LegacyGui.blit(g,"player_inv",leftPos,topPos+151,0,0,181,101);}
        @Override protected void renderLabels(GuiGraphics g,int x,int y){}
        @Override public void render(GuiGraphics g,int x,int y,float p){super.render(g,x,y,p);renderTooltip(g,x,y);}
    }
    public static class Upgrades extends AbstractContainerScreen<UpgradeMenu>{
        public Upgrades(UpgradeMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=166;}
        @Override protected void renderBg(GuiGraphics g,float p,int x,int y){LegacyGui.blit(g,"player_inv",leftPos,topPos+67,0,0,181,101);LegacyGui.blit(g,"radiant_upgrades",leftPos+49,topPos,0,0,82,32);LegacyGui.blit(g,"radiant_upgrades",leftPos+49,topPos+36,0,32,82,32);}
        @Override protected void renderLabels(GuiGraphics g,int x,int y){}
        @Override public void render(GuiGraphics g,int x,int y,float p){super.render(g,x,y,p);renderTooltip(g,x,y);}
    }
}
