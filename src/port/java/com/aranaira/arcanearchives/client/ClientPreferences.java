package com.aranaira.arcanearchives.client;

import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.client.Minecraft;

public final class ClientPreferences {
    public static boolean persistSearch,closeWithoutShift=true,syncJei,showHud=true;
    public static boolean colourblind;
    public static boolean prettyGuis=true,manifestGrid;
    public static boolean trovesDispense=true,defaultNoNew;
    public static int trackingDistance=100;
    public static String lastSearch="";
    private static Path file(){return Minecraft.getInstance().gameDirectory.toPath().resolve("config/arcanearchives-client.json");}
    public static void load(){
        try{
            if(!Files.isRegularFile(file()))return;
            JsonObject j=JsonParser.parseString(Files.readString(file())).getAsJsonObject();
            persistSearch=bool(j,"persistSearch",false);closeWithoutShift=bool(j,"closeWithoutShift",true);syncJei=bool(j,"syncJei",false);showHud=bool(j,"showHud",true);
            colourblind=bool(j,"colourblind",false);
            prettyGuis=bool(j,"prettyGuis",true);manifestGrid=bool(j,"manifestGrid",false);
            trovesDispense=bool(j,"trovesDispense",true);defaultNoNew=bool(j,"defaultNoNew",false);
            trackingDistance=j.has("trackingDistance")?Math.max(16,Math.min(512,j.get("trackingDistance").getAsInt())):100;
        }catch(Exception e){System.getLogger("ArcaneArchives").log(System.Logger.Level.WARNING,"Could not read client preferences",e);}
    }
    private static boolean bool(JsonObject j,String key,boolean fallback){return j.has(key)?j.get(key).getAsBoolean():fallback;}
    public static void save(){
        JsonObject j=new JsonObject();j.addProperty("persistSearch",persistSearch);j.addProperty("closeWithoutShift",closeWithoutShift);j.addProperty("syncJei",syncJei);j.addProperty("showHud",showHud);j.addProperty("trackingDistance",trackingDistance);
        j.addProperty("colourblind",colourblind);
        j.addProperty("prettyGuis",prettyGuis);j.addProperty("manifestGrid",manifestGrid);
        j.addProperty("trovesDispense",trovesDispense);j.addProperty("defaultNoNew",defaultNoNew);
        try{Files.createDirectories(file().getParent());Files.writeString(file(),new GsonBuilder().setPrettyPrinting().create().toJson(j));}
        catch(Exception e){System.getLogger("ArcaneArchives").log(System.Logger.Level.WARNING,"Could not save client preferences",e);}
    }
    public static void sync(){var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Kind","preferences");tag.putBoolean("TrovesDispense",trovesDispense);tag.putBoolean("NoNew",defaultNoNew);com.aranaira.arcanearchives.ArchiveNetwork.query(tag);}
}
