package com.aranaira.arcanearchives.client;
import com.aranaira.arcanearchives.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BrazierScreen extends AbstractContainerScreen<ArchiveMenu>{
    private EditBox radius;
    private boolean updating;
    public BrazierScreen(ArchiveMenu menu,Inventory inv,Component title){super(menu,inv,title,98,41);}
    @Override protected void init(){
        super.init();
        radius=new EditBox(font,leftPos+45,topPos+9,36,12,Component.literal("Routing radius"));radius.setBordered(false);
        radius.setMaxLength(3);radius.setFilter(text->text.isEmpty()||text.matches("[0-9]{1,3}"));radius.setValue(Integer.toString(menu.radius));
        radius.setResponder(text->{if(!updating&&!text.isEmpty()){int value=Integer.parseInt(text);if(value<=300)LegacyGui.query(menu.containerId,"brazier_radius",value);}});
        addRenderableWidget(radius);
        addRenderableWidget(new LegacyGui.Hotspot(leftPos+29,topPos+2,8,16,"Decrease radius",false,()->send(20)));
        addRenderableWidget(new LegacyGui.Hotspot(leftPos+85,topPos+2,8,16,"Increase radius",false,()->send(21)));
        addRenderableWidget(new LegacyGui.Hotspot(leftPos+67,topPos+27,12,12,"Route to this owner / entire hive",false,()->send(22)));
        addRenderableWidget(new LegacyGui.Hotspot(leftPos+6,topPos+6,14,14,"Show / hide range",false,()->send(23)));
        update();
    }
    private void send(int button){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,button);}
    private void update(){if(!radius.isFocused()&&!radius.getValue().equals(Integer.toString(menu.radius))){updating=true;radius.setValue(Integer.toString(menu.radius));updating=false;}}
    @Override protected void containerTick(){super.containerTick();update();}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float partial){super.extractBackground(g,x,y,partial);
        LegacyGui.blit(g,"brazier_hoarding",leftPos,topPos,0,0,98,41);
        LegacyGui.blit(g,"brazier_hoarding",leftPos+7,topPos+8,BrazierRange.showing(menu.brazierPosition,menu.brazierDimension)?222:210,0,12,10);
        if(menu.subnetworkOnly!=0){LegacyGui.blit(g,"brazier_hoarding",leftPos+70,topPos+30,234,0,6,6);LegacyGui.blit(g,"brazier_hoarding",leftPos+82,topPos+25,240,0,16,16);}
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int x,int y){}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){super.extractRenderState(g,x,y,partial);}
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){int key=event.key();if(radius.isFocused()&&key!=256)return radius.keyPressed(event);return super.keyPressed(event);}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick){double x=event.x(),y=event.y();int button=event.button();if(button==1&&radius.isMouseOver(x,y)){radius.setValue("");setFocused(radius);radius.setFocused(true);return true;}return super.mouseClicked(event,doubleClick);}
}
