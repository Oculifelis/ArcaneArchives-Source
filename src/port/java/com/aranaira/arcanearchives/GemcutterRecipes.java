package com.aranaira.arcanearchives;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
public final class GemcutterRecipes {
    public static final class Requirement {
        private final String id;private final boolean tag;private final int count;
        private final java.util.function.Predicate<ItemStack> predicate;private final List<ItemStack> examples;
        public Requirement(String id,boolean tag,int count){this(id,tag,count,null,List.of());}
        public Requirement(String id,boolean tag,int count,java.util.function.Predicate<ItemStack> predicate,List<ItemStack> examples){
            if(count<1||count>1000000)throw new IllegalArgumentException("Ingredient count outside1..1000000");
            this.id=id;this.tag=tag;this.count=count;this.predicate=predicate;this.examples=examples.stream().map(ItemStack::copy).toList();
        }
        public String id(){return id;}public boolean tag(){return tag;}public int count(){return count;}
        public boolean test(ItemStack s){return !s.isEmpty()&&(predicate!=null?predicate.test(s):tag?s.is(TagKey.create(Registries.ITEM,new ResourceLocation(id))):BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(id));}
        public List<ItemStack> examples(){return predicate!=null||!examples.isEmpty()?examples.stream().map(ItemStack::copy).toList():java.util.stream.StreamSupport.stream(BuiltInRegistries.ITEM.spliterator(),false).map(ItemStack::new).filter(this::test).toList();}
    }
    public static final class Recipe {
        private final String id,output;private final int count;private final boolean creator;private final List<Requirement> ingredients;private final ItemStack prototype;
        public Recipe(String id,String output,int count,boolean creator,List<Requirement> ingredients){this(id,output,count,creator,ingredients,ItemStack.EMPTY);}
        public Recipe(String id,String output,int count,boolean creator,List<Requirement> ingredients,ItemStack prototype){
            if(count<1||count>64||ingredients.size()>18)throw new IllegalArgumentException("Recipe needs at most18 ingredients and1..64 output items");
            this.id=id;this.output=output;this.count=count;this.creator=creator;this.ingredients=List.copyOf(ingredients);this.prototype=prototype.copy();
        }
        public String id(){return id;}public String output(){return output;}public int count(){return count;}public boolean creator(){return creator;}public List<Requirement> ingredients(){return ingredients;}
        public ItemStack result(){return prototype.isEmpty()?new ItemStack(AA.item(output),count):prototype.copyWithCount(count);}
    }
    public static final List<Recipe> ALL=List.of(
        new Recipe("radiant_dust","radiant_dust",2,false,List.of(new Requirement("arcanearchives:raw_quartz",false,1))),
        new Recipe("shaped_quartz","shaped_quartz",1,false,List.of(new Requirement("arcanearchives:raw_quartz",false,2))),
        new Recipe("manifest","manifest",1,false,List.of(new Requirement("minecraft:paper",false,1),new Requirement("minecraft:black_dye",false,1),new Requirement("arcanearchives:radiant_dust",false,2))),
        new Recipe("letter_invitation","letter_invitation",1,true,List.of(new Requirement("minecraft:paper",false,3),new Requirement("arcanearchives:radiant_dust",false,1),new Requirement("minecraft:light_blue_dye",false,1))),
        new Recipe("letter_resignation","letter_resignation",1,true,List.of(new Requirement("minecraft:paper",false,3),new Requirement("arcanearchives:radiant_dust",false,1),new Requirement("minecraft:pink_dye",false,1))),
        new Recipe("writ_expulsion","writ_expulsion",1,true,List.of(new Requirement("minecraft:paper",false,3),new Requirement("arcanearchives:radiant_dust",false,1),new Requirement("minecraft:red_dye",false,1))),
        new Recipe("scepter_manipulation","scepter_manipulation",1,false,List.of(new Requirement("arcanearchives:scepter_revelation",false,1),new Requirement("arcanearchives:shaped_quartz",false,1))),
        new Recipe("radiant_lantern","radiant_lantern",4,false,List.of(new Requirement("arcanearchives:raw_quartz",false,2),new Requirement("minecraft:gold_nugget",false,1))),
        new Recipe("radiant_trove","radiant_trove",4,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,2),new Requirement("arcanearchives:material_interface",false,1),new Requirement("arcanearchives:radiant_chest",false,1))),
        new Recipe("radiant_tank","radiant_tank",4,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,2),new Requirement("arcanearchives:containment_field",false,1),new Requirement("minecraft:gold_ingot",false,1))),
        new Recipe("monitoring_crystal","monitoring_crystal",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:gold_nugget",false,2),new Requirement("minecraft:stick",false,4))),
        new Recipe("scintillating_inlay","scintillating_inlay",1,false,List.of(new Requirement("arcanearchives:radiant_dust",false,6),new Requirement("minecraft:redstone",false,12),new Requirement("minecraft:gold_ingot",false,1),new Requirement("minecraft:gold_nugget",false,6))),
        new Recipe("brazier_of_hoarding","brazier_of_hoarding",1,false,List.of(new Requirement("arcanearchives:radiant_dust",false,4),new Requirement("minecraft:coal",false,8),new Requirement("minecraft:gold_ingot",false,2),new Requirement("minecraft:logs",true,3))),
        new Recipe("matrix_brace","matrix_brace",1,false,List.of(new Requirement("arcanearchives:scintillating_inlay",false,1),new Requirement("minecraft:gold_ingot",false,2))),
        new Recipe("material_interface","material_interface",1,false,List.of(new Requirement("arcanearchives:scintillating_inlay",false,1),new Requirement("minecraft:gold_ingot",false,1),new Requirement("arcanearchives:shaped_quartz",false,1))),
        new Recipe("containment_field","containment_field",1,false,List.of(new Requirement("arcanearchives:scintillating_inlay",false,1),new Requirement("minecraft:gold_ingot",false,2),new Requirement("arcanearchives:shaped_quartz",false,2))),
        new Recipe("devouring_charm","devouring_charm",4,false,List.of(new Requirement("minecraft:gold_ingot",false,1),new Requirement("minecraft:obsidian",false,2),new Requirement("minecraft:flint_and_steel",false,1))),
        new Recipe("radiant_key","radiant_key",4,true,List.of(new Requirement("minecraft:gold_ingot",false,1),new Requirement("minecraft:gold_nugget",false,3),new Requirement("arcanearchives:shaped_quartz",false,1))),
        new Recipe("radiant_amphora","radiant_amphora",1,false,List.of(new Requirement("arcanearchives:radiant_dust",false,4),new Requirement("minecraft:clay_ball",false,4),new Requirement("minecraft:gold_nugget",false,4))),
        new Recipe("gemsocket","gemsocket",1,false,List.of(new Requirement("arcanearchives:scintillating_inlay",false,1),new Requirement("minecraft:gold_nugget",false,4),new Requirement("minecraft:bowl",false,1),new Requirement("minecraft:leather",false,1))),
        new Recipe("murdergleam","murdergleam",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:yellow_dye",false,4),new Requirement("minecraft:diamond_sword",false,1),new Requirement("minecraft:blaze_powder",false,2))),
        new Recipe("slaughtergleam","slaughtergleam",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:red_dye",false,4),new Requirement("minecraft:diamond",false,1),new Requirement("minecraft:gold_ingot",false,2),new Requirement("minecraft:lapis_block",false,2))),
        new Recipe("agegleam","agegleam",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:green_dye",false,4),new Requirement("minecraft:wheat",false,9),new Requirement("minecraft:carrot",false,9),new Requirement("minecraft:wheat_seeds",false,9))),
        new Recipe("cleansegleam","cleansegleam",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:blue_dye",false,4),new Requirement("minecraft:poisonous_potato",false,1),new Requirement("minecraft:milk_bucket",false,1))),
        new Recipe("switchgleam","switchgleam",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:purple_dye",false,4),new Requirement("minecraft:tripwire_hook",false,1),new Requirement("minecraft:ender_pearl",false,1))),
        new Recipe("salvegleam","salvegleam",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:pink_dye",false,4),new Requirement("minecraft:golden_apple",false,1),new Requirement("minecraft:glistering_melon_slice",false,1),new Requirement("minecraft:golden_carrot",false,1))),
        new Recipe("munchstone","munchstone",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:black_dye",false,4),new Requirement("minecraft:cake",false,1),new Requirement("minecraft:glistering_melon_slice",false,2))),
        new Recipe("orderstone","orderstone",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:pink_dye",false,4),new Requirement("minecraft:anvil",false,1),new Requirement("minecraft:oak_sign",false,30))),
        new Recipe("mindspindle","mindspindle",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:green_dye",false,4),new Requirement("minecraft:enchanted_book",false,1),new Requirement("minecraft:emerald",false,2))),
        new Recipe("elixirspindle","elixirspindle",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:purple_dye",false,4),new Requirement("minecraft:brewing_stand",false,1),new Requirement("minecraft:emerald",false,2),new Requirement("minecraft:redstone",false,4),new Requirement("minecraft:glowstone_dust",false,4))),
        new Recipe("rivertear","rivertear",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:sugar",false,4),new Requirement("minecraft:water_bucket",false,1),new Requirement("minecraft:blue_dye",false,4))),
        new Recipe("mountaintear","mountaintear",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:orange_dye",false,4),new Requirement("minecraft:magma_block",false,4),new Requirement("minecraft:lava_bucket",false,1))),
        new Recipe("parchtear","parchtear",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:black_dye",false,4),new Requirement("minecraft:terracotta",false,4),new Requirement("minecraft:dead_bush",false,4))),
        new Recipe("phoenixway","phoenixway",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:blaze_powder",false,4),new Requirement("minecraft:coal",false,4),new Requirement("minecraft:flint_and_steel",false,1))),
        new Recipe("stormway","stormway",1,false,List.of(new Requirement("arcanearchives:shaped_quartz",false,1),new Requirement("minecraft:yellow_dye",false,4),new Requirement("minecraft:redstone",false,16),new Requirement("minecraft:iron_bars",false,8)))
    );
}
