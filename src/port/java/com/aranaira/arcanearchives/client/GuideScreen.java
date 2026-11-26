package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.AA;
import com.aranaira.arcanearchives.ArchiveLegacyConfig;
import com.google.gson.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.*;

/** The complete source guide remains readable when the optional Patchouli mod is absent. */
public final class GuideScreen extends ScaledScreen {
    private final List<JsonObject> entries=new ArrayList<>();
    private final List<Button> entryButtons=new ArrayList<>();
    private String query="",category="";
    private int indexPage,page,section;
    private final Map<String,List<ItemStack>> ingredientCache=new HashMap<>();
    private final Map<String,int[]> imageSizes=new HashMap<>();
    private JsonObject selected;
    private EditBox search;
    public GuideScreen(){super(Component.translatable("arcanearchives.book.name"),524,328);load();}
    public static void open(){
        try{
            Class<?> apiClass=Class.forName("vazkii.patchouli.api.PatchouliAPI");Object api=apiClass.getMethod("get").invoke(null);
            Class<?> contract=Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI");
            if(!((Boolean)contract.getMethod("isStub").invoke(api))){contract.getMethod("openBookGUI",AA.id("arcarc_guide").getClass()).invoke(api,AA.id("arcarc_guide"));return;}
        }catch(ClassNotFoundException absent){/* Self-contained reader below. */}
        catch(ReflectiveOperationException failure){System.getLogger("ArcaneArchives").log(System.Logger.Level.WARNING,"Patchouli could not open the guide; using the complete built-in reader",failure);}
        Minecraft.getInstance().setScreen(new GuideScreen());
    }
    private void load(){
        var mc=Minecraft.getInstance();String language=mc.getLanguageManager().getSelected();
        var location=AA.id("guidebook/"+language+".json");var resource=mc.getResourceManager().getResource(location);
        if(resource.isEmpty())resource=mc.getResourceManager().getResource(AA.id("guidebook/en_us.json"));
        try(var reader=resource.orElseThrow().openAsReader()){
            for(var value:JsonParser.parseReader(reader).getAsJsonArray())entries.add(value.getAsJsonObject());
        }catch(Exception failure){System.getLogger("ArcaneArchives").log(System.Logger.Level.ERROR,"Could not load guide",failure);}
        if(!entries.isEmpty())selected=entries.get(0);
    }
    private List<JsonObject> filtered(){return entries.stream().filter(e->(ArchiveLegacyConfig.arsenal()||!e.get("category").getAsString().equals("gems"))&&(category.isEmpty()||e.get("category").getAsString().equals(category))&&e.get("name").getAsString().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();}
    @Override protected void init(){
        int left=(canvasWidth()-520)/2,top=(canvasHeight()-324)/2;entryButtons.clear();
        String[] categories={"","blocks","items","gems","concepts"};
        for(int i=0;i<categories.length;i++){
            final String name=categories[i];addRenderableWidget(Button.builder(Component.literal(name.isEmpty()?"All":name),b->{category=name;indexPage=0;updateList();}).bounds(left+12+i*99,top+30,95,20).build());
        }
        search=new EditBox(font,left+12,top+61,158,18,Component.literal("Search guide"));search.setValue(query);search.setResponder(text->{query=text;indexPage=0;updateList();});addRenderableWidget(search);
        for(int i=0;i<10;i++){
            final int n=i;entryButtons.add(addRenderableWidget(Button.builder(Component.empty(),b->{var list=filtered();int at=indexPage*10+n;if(at<list.size()){selected=list.get(at);page=0;section=0;}}).bounds(left+12,top+86+i*19,158,18).build()));
        }
        addRenderableWidget(Button.builder(Component.literal("< Index"),b->{indexPage=Math.max(0,indexPage-1);updateList();}).bounds(left+12,top+285,76,20).build());
        addRenderableWidget(Button.builder(Component.literal("Index >"),b->{if((indexPage+1)*10<filtered().size())indexPage++;updateList();}).bounds(left+94,top+285,76,20).build());
        addRenderableWidget(Button.builder(Component.literal("< Page"),b->{if(page>0)page--;else if(section>0){section--;page=0;page=lastPage();}}).bounds(left+190,top+285,90,20).build());
        addRenderableWidget(Button.builder(Component.literal("Page >"),b->{if(page<lastPage())page++;else if(selected!=null&&section+1<selected.getAsJsonArray("pages").size()){section++;page=0;}}).bounds(left+290,top+285,90,20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(left+408,top+285,98,20).build());updateList();
    }
    private void updateList(){
        var list=filtered();for(int i=0;i<entryButtons.size();i++){int at=indexPage*10+i;var button=entryButtons.get(i);button.visible=at<list.size();if(button.visible)button.setMessage(Component.literal(font.plainSubstrByWidth(list.get(at).get("name").getAsString(),148)));}
    }
    private java.util.List<net.minecraft.util.FormattedCharSequence> lines(){
        return selected==null?List.of():font.split(Component.literal(current().get("text").getAsString()),306);
    }
    private JsonObject current(){if(selected==null)return new JsonObject();var pages=selected.getAsJsonArray("pages");section=Math.max(0,Math.min(section,pages.size()-1));return pages.get(section).getAsJsonObject();}
    public boolean showsImage(){return page==0&&current().has("images")&&!current().getAsJsonArray("images").isEmpty();}
    public boolean showsRecipe(){var p=current();return page==0&&(p.has("crafting")&&!p.getAsJsonArray("crafting").isEmpty()||p.has("gemcutting")&&!p.get("gemcutting").isJsonNull());}
    private int initialMediaHeight(){var p=current();if(p.has("images")&&!p.getAsJsonArray("images").isEmpty())return 128;if(p.has("crafting")&&!p.getAsJsonArray("crafting").isEmpty()||p.has("gemcutting")&&!p.get("gemcutting").isJsonNull())return 68;if(p.has("links")&&!p.getAsJsonArray("links").isEmpty())return p.getAsJsonArray("links").size()*18;return p.has("item")&&!p.get("item").getAsString().isEmpty()?24:0;}
    private int mediaHeight(){return page==0?initialMediaHeight():0;}
    private int capacity(){return Math.max(1,(198-mediaHeight())/9);}
    private int firstCapacity(){return Math.max(1,(198-initialMediaHeight())/9);}
    private int lastPage(){return Math.max(0,(lines().size()-firstCapacity()+21)/22);}
    private int offset(){return page==0?0:firstCapacity()+(page-1)*22;}
    private ItemStack item(String id){var location=ResourceLocation.tryParse(id);return location==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.get(location));}
    private ItemStack ingredient(JsonElement value){
        if(value==null||value.isJsonNull())return ItemStack.EMPTY;if(value.isJsonArray()){var a=value.getAsJsonArray();return a.isEmpty()?ItemStack.EMPTY:ingredient(a.get((int)(minecraft.level.getGameTime()/20%a.size())));}
        var p=value.isJsonObject()?value.getAsJsonObject():null;String id=p==null?value.getAsString():p.has("item")?p.get("item").getAsString():p.has("id")?p.get("id").getAsString():"#"+p.get("tag").getAsString();
        if(!id.startsWith("#"))return item(id);var values=ingredientCache.computeIfAbsent(id,key->{var tag=net.minecraft.tags.TagKey.create(Registries.ITEM,ResourceLocation.tryParse(key.substring(1)));return java.util.stream.StreamSupport.stream(BuiltInRegistries.ITEM.spliterator(),false).filter(i->i.builtInRegistryHolder().is(tag)).map(ItemStack::new).toList();});return values.isEmpty()?ItemStack.EMPTY:values.get((int)(minecraft.level.getGameTime()/20%values.size())).copy();
    }
    private void icon(GuiGraphics g,ItemStack stack,int x,int y,int mouseX,int mouseY){if(stack.isEmpty())return;g.renderItem(stack,x,y);g.renderItemDecorations(font,stack,x,y);if(LegacyGui.hit(mouseX,mouseY,x,y,16,16))g.renderTooltip(font,stack,mouseX,mouseY);}
    private void media(GuiGraphics g,int x,int y,int mouseX,int mouseY){
        if(page!=0)return;var p=current();
        if(showsImage()){
            String id=p.getAsJsonArray("images").get(0).getAsString();var location=ResourceLocation.tryParse(id);var size=imageSizes.computeIfAbsent(id,key->{try(var in=minecraft.getResourceManager().getResource(location).orElseThrow().open()){var bytes=java.nio.ByteBuffer.wrap(in.readNBytes(24));return new int[]{bytes.getInt(16),bytes.getInt(20)};}catch(Exception error){return new int[]{256,256};}});float scale=Math.min(306f/size[0],124f/size[1]);int w=Math.round(size[0]*scale),h=Math.round(size[1]*scale);g.blit(location,x+(306-w)/2,y,0,0,w,h,size[0],size[1],size[0],size[1]);
        }else if(p.has("crafting")&&!p.getAsJsonArray("crafting").isEmpty()){
            var recipes=p.getAsJsonArray("crafting");for(int n=0;n<Math.min(2,recipes.size());n++){var recipe=recipes.get(n).getAsJsonObject();int left=x+n*153;var grid=recipe.getAsJsonArray("grid");for(int i=0;i<9;i++){int sx=left+i%3*18,sy=y+i/3*18;g.fill(sx-1,sy-1,sx+17,sy+17,0xffac9e8c);icon(g,ingredient(grid.get(i)),sx,sy,mouseX,mouseY);}g.drawString(font,"→",left+59,y+20,0xff58402a,false);var output=item(recipe.get("output").getAsString());output.setCount(recipe.get("count").getAsInt());icon(g,output,left+78,y+18,mouseX,mouseY);}
        }else if(p.has("gemcutting")&&!p.get("gemcutting").isJsonNull()){
            var recipe=p.getAsJsonObject("gemcutting");var inputs=recipe.getAsJsonArray("ingredients");for(int i=0;i<inputs.size();i++){var input=inputs.get(i).getAsJsonObject();var stack=ingredient(input);stack.setCount(input.get("count").getAsInt());icon(g,stack,x+i%6*20,y+i/6*20,mouseX,mouseY);}g.drawString(font,"→",x+132,y+20,0xff58402a,false);var output=recipe.getAsJsonObject("output");var stack=item(output.get("item").getAsString());stack.setCount(output.get("count").getAsInt());icon(g,stack,x+152,y+18,mouseX,mouseY);
        }else if(p.has("links")&&!p.getAsJsonArray("links").isEmpty()){
            var links=p.getAsJsonArray("links");for(int i=0;i<links.size();i++){String id=links.get(i).getAsString().replace("arcanearchives:","");var entry=entries.stream().filter(e->e.get("id").getAsString().equals(id)).findFirst();if(entry.isPresent())g.drawString(font,entry.get().get("name").getAsString(),x,y+i*18,0xff35619b,false);}
        }else if(p.has("item"))icon(g,item(p.get("item").getAsString()),x,y,mouseX,mouseY);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        mouseX=canvasX(mouseX);mouseY=canvasY(mouseY);beginCanvas(g);int left=(canvasWidth()-520)/2,top=(canvasHeight()-324)/2;
        g.fill(0,0,canvasWidth(),canvasHeight(),0xb0100818);g.fill(left-2,top-2,left+522,top+326,0xff8b669e);g.fill(left,top,left+520,top+324,0xffeee4d4);
        g.drawString(font,title,left+14,top+11,0xff392347,false);super.render(g,mouseX,mouseY,partial);
        if(selected!=null){
            g.drawString(font,Component.literal(selected.get("name").getAsString()),left+190,top+62,0xff392347,false);
            page=Math.min(page,lastPage());media(g,left+190,top+82,mouseX,mouseY);var lines=lines();int capacity=capacity(),offset=offset();
            for(int i=0;i<capacity&&offset+i<lines.size();i++)g.drawString(font,lines.get(offset+i),left+190,top+82+mediaHeight()+i*9,0xff2e2631,false);
            g.drawString(font,(section+1)+" / "+selected.getAsJsonArray("pages").size()+" · "+(page+1),left+386,top+313,0xff392347,false);
        }
        endCanvas(g);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override protected boolean canvasMouseClicked(double x,double y,int button){if(button==1&&search.isMouseOver(x,y)){search.setValue("");setFocused(search);search.setFocused(true);return true;}if(button==0&&page==0&&current().has("links")){var links=current().getAsJsonArray("links");int left=(canvasWidth()-520)/2,top=(canvasHeight()-324)/2;for(int i=0;i<links.size();i++)if(LegacyGui.hit(x,y,left+190,top+82+i*18,306,16)){String id=links.get(i).getAsString().replace("arcanearchives:","");for(var entry:entries)if(entry.get("id").getAsString().equals(id)){selected=entry;page=0;section=0;return true;}}}return super.canvasMouseClicked(x,y,button);}
}
