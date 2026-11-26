package com.aranaira.arcanearchives.client;

import net.minecraft.client.gui.GuiGraphics;
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
    protected void beginCanvas(GuiGraphics g){g.pose().pushPose();g.pose().scale(canvasScale(),canvasScale(),1);}
    protected void endCanvas(GuiGraphics g){g.pose().popPose();}
    protected boolean canvasMouseClicked(double x,double y,int button){return super.mouseClicked(x,y,button);}
    @Override public boolean mouseClicked(double x,double y,int button){return canvasMouseClicked(x/canvasScale(),y/canvasScale(),button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/canvasScale(),y/canvasScale(),button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return super.mouseDragged(x/canvasScale(),y/canvasScale(),button,dx/canvasScale(),dy/canvasScale());}
    @Override public void mouseMoved(double x,double y){super.mouseMoved(x/canvasScale(),y/canvasScale());}
}
