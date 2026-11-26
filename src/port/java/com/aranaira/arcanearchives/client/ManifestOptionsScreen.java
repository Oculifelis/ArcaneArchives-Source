package com.aranaira.arcanearchives.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ManifestOptionsScreen extends ScaledScreen {
    private final Screen parent;
    public ManifestOptionsScreen(Screen parent){super(Component.literal("Manifest options"),228,296);this.parent=parent;}
    @Override protected void init(){
        int x=canvasWidth()/2-110,y=canvasHeight()/2-144;
        addRenderableWidget(Button.builder(label("Remember search",ClientPreferences.persistSearch),b->{ClientPreferences.persistSearch=!ClientPreferences.persistSearch;b.setMessage(label("Remember search",ClientPreferences.persistSearch));ClientPreferences.save();}).bounds(x,y,220,20).build());
        addRenderableWidget(Button.builder(label("Close unless Shift held",ClientPreferences.closeWithoutShift),b->{ClientPreferences.closeWithoutShift=!ClientPreferences.closeWithoutShift;b.setMessage(label("Close unless Shift held",ClientPreferences.closeWithoutShift));ClientPreferences.save();}).bounds(x,y+24,220,20).build());
        addRenderableWidget(Button.builder(label("Synchronize JEI search",ClientPreferences.syncJei),b->{ClientPreferences.syncJei=!ClientPreferences.syncJei;b.setMessage(label("Synchronize JEI search",ClientPreferences.syncJei));ClientPreferences.save();}).bounds(x,y+48,220,20).build());
        addRenderableWidget(Button.builder(Component.literal("Tracking distance: "+ClientPreferences.trackingDistance),b->{ClientPreferences.trackingDistance=ClientPreferences.trackingDistance>=300?50:ClientPreferences.trackingDistance+50;b.setMessage(Component.literal("Tracking distance: "+ClientPreferences.trackingDistance));ClientPreferences.save();}).bounds(x,y+72,220,20).build());
        addRenderableWidget(Button.builder(label("Gem and storage HUD",ClientPreferences.showHud),b->{ClientPreferences.showHud=!ClientPreferences.showHud;b.setMessage(label("Gem and storage HUD",ClientPreferences.showHud));ClientPreferences.save();}).bounds(x,y+96,220,20).build());
        addRenderableWidget(Button.builder(label("Colourblind gem symbols",ClientPreferences.colourblind),b->{ClientPreferences.colourblind=!ClientPreferences.colourblind;b.setMessage(label("Colourblind gem symbols",ClientPreferences.colourblind));ClientPreferences.save();}).bounds(x,y+120,220,20).build());
        addRenderableWidget(Button.builder(label("Original ornate GUIs",ClientPreferences.prettyGuis),b->{ClientPreferences.prettyGuis=!ClientPreferences.prettyGuis;b.setMessage(label("Original ornate GUIs",ClientPreferences.prettyGuis));ClientPreferences.save();}).bounds(x,y+144,220,20).build());
        addRenderableWidget(Button.builder(label("Manifest slot borders",ClientPreferences.manifestGrid),b->{ClientPreferences.manifestGrid=!ClientPreferences.manifestGrid;b.setMessage(label("Manifest slot borders",ClientPreferences.manifestGrid));ClientPreferences.save();}).bounds(x,y+168,220,20).build());
        if(parent instanceof LegacyScreens.Manifest screen)addRenderableWidget(Button.builder(Component.literal("Manifest clicks: "+(screen.getMenu().mode==0?"track":"retrieve")),b->{int mode=screen.getMenu().mode; minecraft.gameMode.handleInventoryButtonClick(screen.getMenu().containerId,3);b.setMessage(Component.literal("Manifest clicks: "+(mode==0?"retrieve":"track")));}).bounds(x,y+192,220,20).build());
        addRenderableWidget(Button.builder(label("New Chests: no new items",ClientPreferences.defaultNoNew),b->{ClientPreferences.defaultNoNew=!ClientPreferences.defaultNoNew;b.setMessage(label("New Chests: no new items",ClientPreferences.defaultNoNew));ClientPreferences.save();ClientPreferences.sync();}).bounds(x,y+216,220,20).build());
        addRenderableWidget(Button.builder(label("Trove left click gives one",ClientPreferences.trovesDispense),b->{ClientPreferences.trovesDispense=!ClientPreferences.trovesDispense;b.setMessage(label("Trove left click gives one",ClientPreferences.trovesDispense));ClientPreferences.save();ClientPreferences.sync();}).bounds(x,y+240,220,20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(x,y+268,220,20).build());
    }
    private static Component label(String text,boolean value){return Component.literal(text+": "+(value?"on":"off"));}
    @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){beginCanvas(g);super.extractRenderState(g,canvasX(mouseX),canvasY(mouseY),partial);endCanvas(g);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
