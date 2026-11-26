package com.aranaira.arcanearchives;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.goal.CatSitOnBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/** Legacy tame ocelots became Cats in modern Minecraft. */
@net.neoforged.fml.common.EventBusSubscriber(modid=AA.ID)
public final class ResonatorCats {
    @SubscribeEvent public static void join(EntityJoinLevelEvent event){
        if(!event.getLevel().isClientSide&&event.getEntity() instanceof Cat cat
                &&cat.goalSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal() instanceof Sit))cat.goalSelector.addGoal(6,new Sit(cat));
    }
    public static final class Sit extends CatSitOnBlockGoal {
        public Sit(Cat cat){super(cat,.8);}
        @Override protected boolean isValidTarget(LevelReader level,BlockPos pos){return level.isEmptyBlock(pos.above())&&level.getBlockState(pos).is(AA.block("radiant_resonator"));}
    }
}
