package com.aranaira.arcanearchives.integration;

import com.aranaira.arcanearchives.*;
import com.leclowndu93150.thaumaturge.api.infusion.IInfusionStabiliser;
import com.leclowndu93150.thaumaturge.content.crucible.BlockEntityCrucible;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Loaded only when Thaumaturge is present. No Thaumaturge code is bundled. */
public final class ThaumaturgeBridge {
    private ThaumaturgeBridge() {}

    public static Block block(String name, BlockBehaviour.Properties properties) {
        return name.equals("raw_quartz_cluster") ? new Cluster(properties)
                : new Brazier(properties);
    }

    public static boolean interact(Player player, BlockPos pos) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level) || player.isSpectator()
                || !player.mayInteract(level, pos) || !ArchiveLegacyConfig.arsenal()) return false;
        if (!(player.level().getBlockEntity(pos) instanceof BlockEntityCrucible crucible)
                || crucible.getTank().getAmountAsInt(0) != 0) return false;
        boolean hasRivertear = false;
        for (int slot = 0; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).is(AA.item("rivertear"))) {
                hasRivertear = true;
                break;
            }
        }
        if (!hasRivertear) hasRivertear = GemRules.held(player).stream()
                .anyMatch(stack -> stack.is(AA.item("rivertear")));
        if (!hasRivertear) return false;
        // Match the legacy empty-only, no-charge shortcut. The native transaction
        // invokes Thaumaturge's own change/synchronization callback on commit.
        try (var transaction = Transaction.openRoot()) {
            int filled = crucible.getTank().insert(0, FluidResource.of(Fluids.WATER),
                    BlockEntityCrucible.TANK_CAPACITY, transaction);
            if (filled != BlockEntityCrucible.TANK_CAPACITY) return false;
            transaction.commit();
        }
        return true;
    }

    public static final class Cluster extends QuartzCluster implements IInfusionStabiliser {
        public Cluster(Properties properties) { super(properties); }
        @Override public boolean canStabiliseInfusion(Level level, BlockPos pos) { return true; }
        @Override public float getStabilizationAmount(Level level, BlockPos pos) { return 0.15F; }
    }

    public static final class Brazier extends ArchiveBlock implements IInfusionStabiliser {
        public Brazier(Properties properties) { super("brazier_of_hoarding", properties); }
        @Override public boolean canStabiliseInfusion(Level level, BlockPos pos) { return true; }
        @Override public float getStabilizationAmount(Level level, BlockPos pos) { return 0.2F; }
    }
}
