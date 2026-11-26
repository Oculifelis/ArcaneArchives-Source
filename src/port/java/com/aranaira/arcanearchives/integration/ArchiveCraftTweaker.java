package com.aranaira.arcanearchives.integration;
import com.aranaira.arcanearchives.*;
import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.base.IUndoableAction;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.ingredient.IIngredientWithAmount;
import com.blamejared.crafttweaker.api.item.IItemStack;
import java.util.*;
import net.minecraft.world.item.ItemStack;
import org.openzen.zencode.java.ZenCodeType;

/** Optional modern ZenCode surface retains the legacy mods.arcanearchives.GCT name. */
@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.GCT")
public final class ArchiveCraftTweaker {
    private static final Map<Object,Batch> batches=new WeakHashMap<>();
    private static final class Batch {
        final List<GemcutterRecipes.Recipe> before=ArchiveRecipes.server();final int epoch=ArchiveRecipes.generation();boolean undone;
        void undo(){if(!undone){undone=true;ArchiveRecipes.restore(before,epoch);}}
    }
    @ZenCodeType.Method public static void addRecipe(String name,IItemStack output,IIngredientWithAmount[] inputs){
        var recipe=recipe(name,output,inputs);CraftTweakerAPI.apply(new Edit("Add Gemcutter recipe "+name,()->ArchiveRecipes.add(recipe,false)));
    }
    @ZenCodeType.Method public static void replaceRecipe(String name,IItemStack output,IIngredientWithAmount[] inputs){
        var recipe=recipe(name,output,inputs);CraftTweakerAPI.apply(new Edit("Replace Gemcutter recipe "+name,()->ArchiveRecipes.add(recipe,true)));
    }
    @ZenCodeType.Method public static void removeRecipe(IItemStack output){
        var stack=output.getInternal().copy();CraftTweakerAPI.apply(new Edit("Remove Gemcutter output "+output.getCommandString(),()->ArchiveRecipes.remove(stack)));
    }
    private static GemcutterRecipes.Recipe recipe(String name,IItemStack output,IIngredientWithAmount[] inputs){
        String id=ArchiveRecipes.key(name);ItemStack stack=output.getInternal().copy();
        if(stack.isEmpty())throw new IllegalArgumentException("A Gemcutter output cannot be empty");
        var requirements=new ArrayList<GemcutterRecipes.Requirement>();
        for(int i=0;i<inputs.length;i++){
            var counted=Objects.requireNonNull(inputs[i]);var ingredient=counted.ingredient();
            // CT item ingredients also carry their requested count. Quantity is
            // allocated across the 18 native slots separately from matching.
            var matcher=ingredient instanceof IItemStack stacked?stacked.asImmutable().withAmount(1):ingredient;
            List<ItemStack> examples=Arrays.stream(matcher.getItems()).map(value->value.getInternal().copy()).filter(value->!value.isEmpty()).toList();
            requirements.add(new GemcutterRecipes.Requirement(AA.ID+":custom/ingredient_"+i,false,counted.amount(),value->matcher.matches(IItemStack.of(value)),examples));
        }
        return new GemcutterRecipes.Recipe(id,net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount(),false,requirements,stack);
    }
    private static final class Edit implements IUndoableAction {
        private final String description;private final Runnable operation;
        private Batch batch;
        Edit(String description,Runnable operation){this.description=description;this.operation=operation;}
        @Override public void apply(){batch=batches.computeIfAbsent(CraftTweakerAPI.getScriptRunManager().currentRunInfo(),ignored->new Batch());operation.run();}
        @Override public void undo(){if(batch!=null)batch.undo();}
        @Override public String describe(){return description;}
        @Override public String describeUndo(){return "Undo "+description;}
        @Override public String systemName(){return "Arcane Archives";}
    }
}
