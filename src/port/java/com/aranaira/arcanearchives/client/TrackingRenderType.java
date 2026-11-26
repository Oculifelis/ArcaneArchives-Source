package com.aranaira.arcanearchives.client;
import com.aranaira.arcanearchives.AA;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
public final class TrackingRenderType {
    public static final RenderType TYPE=RenderType.create("arcanearchives_tracking",RenderSetup.builder(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET).withLocation(AA.id("pipeline/tracking"))
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS,false)).build()).createRenderSetup());
}
