package com.aranaira.arcanearchives.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Fits the standalone reader/settings without changing the user's GUI scale. */
public abstract class ScaledScreen extends Screen {
    private final int minimumWidth,minimumHeight;
    protected ScaledScreen(Component title,int width,int height){super(title);minimumWidth=width;minimumHeight=height;}
    public float canvasScale(){return Math.min(1f,Math.min((float)width/minimumWidth,(float)height/minimumHeight));}
    protected int canvasWidth(){return Math.round(width/canvasScale());}
    protected int canvasHeight(){return Math.round(height/canvasScale());}
    protected int canvasX(double x){return (int)(x/canvasScale());}
    protected int canvasY(double y){return (int)(y/canvasScale());}
    public double screenX(double x){return x*canvasScale();}
    public double screenY(double y){return y*canvasScale();}
    protected void beginCanvas(GuiGraphicsExtractor g){g.pose().pushMatrix();g.pose().scale(canvasScale(),canvasScale());}
    protected void endCanvas(GuiGraphicsExtractor g){g.pose().popMatrix();}
    protected boolean canvasMouseClicked(double x,double y,int button){return super.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button,0)),false);}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick){double x=event.x(),y=event.y();int button=event.button();return canvasMouseClicked(x/canvasScale(),y/canvasScale(),button);}
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event){return super.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(event.x()/canvasScale(),event.y()/canvasScale(),event.buttonInfo()));}
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy){return super.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(event.x()/canvasScale(),event.y()/canvasScale(),event.buttonInfo()),dx/canvasScale(),dy/canvasScale());}
    @Override public void mouseMoved(double x,double y){super.mouseMoved(x/canvasScale(),y/canvasScale());}
}
