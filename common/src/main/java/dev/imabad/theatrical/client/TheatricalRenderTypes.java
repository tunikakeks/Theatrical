package dev.imabad.theatrical.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public class TheatricalRenderTypes {

    public static final RenderType FADER = RenderType.create("TheatricalFader",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .createCompositeState(false));

    public static final RenderType BEAM = RenderType.create(
            "TheatricalBeam",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.translucentState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
    );
}
