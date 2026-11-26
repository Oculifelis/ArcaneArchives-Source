package com.aranaira.arcanearchives.integration;

import com.aranaira.arcanearchives.*;
import com.aranaira.arcanearchives.client.*;
import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.*;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

@JeiPlugin
public final class ArchiveJeiPlugin implements IModPlugin {
    private List<GemcutterRecipes.Recipe> registered=List.of();
    public static final RecipeType<GemcutterRecipes.Recipe> GCT=RecipeType.create(AA.ID,"gemcutting",GemcutterRecipes.Recipe.class);
    public static final RecipeType<GemcutterRecipes.Recipe> RESONANCE=RecipeType.create(AA.ID,"resonance",GemcutterRecipes.Recipe.class);
    @Override public Identifier getPluginUid(){return AA.id("jei");}
    @Override public void registerCategories(IRecipeCategoryRegistration registration){var helper=registration.getJeiHelpers().getGuiHelper();registration.addRecipeCategories(new Category(helper,false),new Category(helper,true));}
    @Override public void registerRecipes(IRecipeRegistration registration){
        registered=ArchiveRecipes.client();registration.addRecipes(GCT,registered);
        registration.addRecipes(RESONANCE,List.of(new GemcutterRecipes.Recipe("resonance","raw_quartz_cluster",1,false,List.of(new GemcutterRecipes.Requirement("arcanearchives:radiant_resonator",false,1)))));
        registration.addItemStackInfo(new ItemStack(AA.item("radiant_resonator")),Component.literal("Grows Raw Radiant Quartz while the network owner is online. Default: 6,000 ticks. See the world's server configuration."));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration){
        registration.addRecipeCatalyst(new ItemStack(AA.item("gemcutters_table")),GCT);
        registration.addRecipeCatalyst(new ItemStack(AA.item("radiant_resonator")),RESONANCE);
        registration.addRecipeCatalyst(new ItemStack(AA.item("radiant_crafting_table")),RecipeTypes.CRAFTING);
    }
    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration){registration.addRecipeTransferHandler(new Transfer(registration.getTransferHelper()),GCT);registration.addRecipeTransferHandler(PersistentCraftingMenu.class,AA.CRAFTING_MENU.get(),RecipeTypes.CRAFTING,1,9,10,36);}
    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration){
        registration.addGuiContainerHandler(ArchiveClient.ManifestScreen.class,new IGuiContainerHandler<ArchiveClient.ManifestScreen>(){
            @Override public List<Rect2i> getGuiExtraAreas(ArchiveClient.ManifestScreen screen){return List.of();}
        });
        registration.addGuiContainerHandler(ArchiveClient.StorageScreen.class,new IGuiContainerHandler<ArchiveClient.StorageScreen>(){
            @Override public List<Rect2i> getGuiExtraAreas(ArchiveClient.StorageScreen screen){return List.of();}
        });
    }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime){
        if(!ArchiveLegacyConfig.arsenal())runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,GemItem.DEFINITIONS.keySet().stream().map(name->new ItemStack(AA.item(name))).toList());
        ViewMessages.recipes=(oldRecipes,newRecipes)->{runtime.getRecipeManager().hideRecipes(GCT,oldRecipes);runtime.getRecipeManager().addRecipes(GCT,newRecipes);runtime.getRecipeManager().unhideRecipes(GCT,newRecipes);};
        if(registered!=ArchiveRecipes.client())ViewMessages.recipes.accept(registered,ArchiveRecipes.client());
        JeiBridge.read=runtime.getIngredientFilter()::getFilterText;JeiBridge.write=runtime.getIngredientFilter()::setFilterText;
        JeiBridge.hover=()->runtime.getIngredientListOverlay().getIngredientUnderMouse().flatMap(mezz.jei.api.ingredients.ITypedIngredient::getItemStack)
                .or(()->runtime.getBookmarkOverlay().getIngredientUnderMouse().flatMap(mezz.jei.api.ingredients.ITypedIngredient::getItemStack))
                .or(()->runtime.getRecipesGui().getIngredientUnderMouse(mezz.jei.api.constants.VanillaTypes.ITEM_STACK)).orElse(ItemStack.EMPTY);
    }
    @Override public void onRuntimeUnavailable(){JeiBridge.read=()->null;JeiBridge.write=text->{};JeiBridge.hover=()->ItemStack.EMPTY;ViewMessages.recipes=(oldRecipes,newRecipes)->{};}
    private static final class Category implements IRecipeCategory<GemcutterRecipes.Recipe> {
        private final IGuiHelper helper;private final boolean resonance;
        Category(IGuiHelper helper,boolean resonance){this.helper=helper;this.resonance=resonance;}
        @Override public RecipeType<GemcutterRecipes.Recipe> getRecipeType(){return resonance?RESONANCE:GCT;}
        @Override public Component getTitle(){return Component.literal(resonance?"Radiant Resonance":"Gemcutter's Table");}
        @Override public int getWidth(){return 164;}
        @Override public int getHeight(){return 82;}
        @Override public IDrawable getIcon(){return helper.createDrawableItemStack(new ItemStack(AA.item(resonance?"radiant_resonator":"gemcutters_table")));}
        public IDrawable getBackground(){return helper.createBlankDrawable(getWidth(),getHeight());}
        @Override public void setRecipe(IRecipeLayoutBuilder builder,GemcutterRecipes.Recipe recipe,IFocusGroup focuses){
            for(int i=0;i<recipe.ingredients().size();i++){
                var required=recipe.ingredients().get(i);List<ItemStack> alternatives=new ArrayList<>();
                for(var stack:required.examples())alternatives.add(stack.copyWithCount(required.count()));
                builder.addSlot(RecipeIngredientRole.INPUT,2+(i%6)*18,4+(i/6)*20).addItemStacks(alternatives).setBackground(helper.getSlotDrawable(),-1,-1);
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT,140,30).addItemStack(recipe.result()).setBackground(helper.getSlotDrawable(),-1,-1);
            builder.setShapeless();
        }
    }
    private static final class Transfer implements IRecipeTransferHandler<ArchiveMenu,GemcutterRecipes.Recipe> {
        private final IRecipeTransferHandlerHelper helper;
        private Transfer(IRecipeTransferHandlerHelper helper){this.helper=helper;}
        @Override public Class<? extends ArchiveMenu> getContainerClass(){return ArchiveMenu.class;}
        @Override public Optional<MenuType<ArchiveMenu>> getMenuType(){return Optional.of(AA.STORAGE_MENU.get());}
        @Override public RecipeType<GemcutterRecipes.Recipe> getRecipeType(){return GCT;}
        @Override public IRecipeTransferError transferRecipe(ArchiveMenu menu,GemcutterRecipes.Recipe recipe,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,net.minecraft.world.entity.player.Player player,boolean max,boolean transfer){
            if(menu.station!=1)return helper.createInternalError();
            // The legacy station crafts from both inventories; selecting needs no lossy slot rearrangement.
            if(transfer)LegacyGui.textQuery(menu.containerId,"gct_select",ArchiveRecipes.key(recipe.id()));return null;
        }
    }
}
