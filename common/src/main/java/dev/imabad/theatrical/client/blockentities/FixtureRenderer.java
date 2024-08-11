package dev.imabad.theatrical.client.blockentities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.MovingLightBlock;
import dev.imabad.theatrical.client.LazyRenderers;
import dev.imabad.theatrical.client.TheatricalRenderTypes;
import dev.imabad.theatrical.config.TheatricalConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Optional;

public abstract class FixtureRenderer<T extends BaseLightBlockEntity> implements BlockEntityRenderer<T> {

    public record FixtureRenderContext(Fixture fixtureType, Direction facing, boolean isFlipped, boolean isHanging,
                                       int prevPan, int pan, int prevTilt, int tilt,
                                       Optional<BlockState> supportingStructure, float intensity, float prevIntensity,
                                       int prevRed, int red, int prevGreen, int green, int prevBlue, int blue,
                                       BlockPos pos, int focus, double distance){}

    private final Double beamOpacity = TheatricalConfig.INSTANCE.CLIENT.beamOpacity;

    public FixtureRenderer(BlockEntityRendererProvider.Context context) {
    }
    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.cutout());
        BlockState blockState = blockEntity.getBlockState();
        boolean isFlipped = blockEntity.isUpsideDown();
        boolean isHanging = ((HangableBlock) blockState.getBlock()).isHanging(blockEntity.getLevel(), blockEntity.getBlockPos());
        Direction facing = blockState.getValue(MovingLightBlock.FACING);
        FixtureRenderContext fixtureRenderContext = new FixtureRenderContext(blockEntity.getFixture(), facing, isFlipped, isHanging, blockEntity.getPrevPan(),
                blockEntity.getPan(), blockEntity.getPrevTilt(), blockEntity.getTilt(), BaseLightBlockEntity
                .getSupportingStructure(blockEntity.getLevel(), blockEntity.getBlockPos(), blockState), blockEntity.getIntensity(),
                blockEntity.getPrevIntensity(), blockEntity.getPrevRed(), blockEntity.getRed(), blockEntity.getPrevGreen(), blockEntity.getGreen(),
                blockEntity.getPrevBlue(), blockEntity.getBlue(), blockEntity.getBlockPos(), blockEntity.getFocus(), blockEntity.getDistance());
        renderModel(fixtureRenderContext, poseStack, vertexConsumer, partialTick, blockState, packedLight, packedOverlay);
        beforeRenderBeam(fixtureRenderContext, poseStack, vertexConsumer, multiBufferSource,  partialTick, blockState, packedLight, packedOverlay);
        VertexConsumer linesB = multiBufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, linesB, AABB.ofSize(new Vec3(0, 0, 0), .1d, .1d, .1d), 1, 0, 1, 1);
        if(!shouldRenderBeam(fixtureRenderContext)){
            LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
                @Override
                public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
                    poseStack.pushPose();
                    Vec3 offset = Vec3.atLowerCornerOf(blockEntity.getBlockPos()).subtract(camera.getPosition());
                    poseStack.translate(offset.x, offset.y, offset.z);
                    preparePoseStack(fixtureRenderContext, poseStack, partialTick, blockState);
                    VertexConsumer beamConsumer = bufferSource.getBuffer(TheatricalRenderTypes.BEAM);
                    poseStack.translate(blockEntity.getFixture().getBeamStartPosition()[0], blockEntity.getFixture().getBeamStartPosition()[1], blockEntity.getFixture().getBeamStartPosition()[2]);
                    float intensity = (blockEntity.getPrevIntensity() + ((blockEntity.getIntensity()) - blockEntity.getPrevIntensity()) * partialTick);
                    int color = blockEntity.calculatePartialColour(partialTick);
                    renderLightBeam(beamConsumer, poseStack, fixtureRenderContext, partialTick, (float) ((intensity * beamOpacity) / 255f), blockEntity.getFixture().getBeamWidth(), (float) blockEntity.getDistance(), color);

                    poseStack.popPose();
                }

                @Override
                public Vec3 getPos(float partialTick) {
                    return blockEntity.getBlockPos().getCenter();
                }
            });
        }
        poseStack.popPose();
    }

    public abstract void renderModel(FixtureRenderContext fixtureRenderContext, PoseStack poseStack, VertexConsumer vertexConsumer, float partialTicks, BlockState blockState, int packedLight, int packedOverlay);

    public abstract void preparePoseStack(FixtureRenderContext fixtureRenderContext, PoseStack poseStack, float partialTicks, BlockState blockState);

    public void beforeRenderBeam(FixtureRenderContext fixtureRenderContext, PoseStack poseStack, VertexConsumer vertexConsumer,
                                 MultiBufferSource multiBufferSource,  float partialTicks, BlockState blockstate, int packedLight,
                                 int packedOverlay) {}

    public static boolean shouldRenderBeam(FixtureRenderContext context){
        return context.intensity() > 0 && context.fixtureType().hasBeam();
    }

    protected static void minecraftRenderModel(PoseStack poseStack, VertexConsumer vertexConsumer, BlockState blockState, BakedModel model, int packedLight, int packedOverlay){
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), vertexConsumer, blockState, model, 1, 1, 1, packedLight, packedOverlay);
    }

    public static void renderLightBeam(VertexConsumer builder, PoseStack stack, FixtureRenderContext context, float partialTicks, float alpha, float beamSize, float length, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (alpha * 255);
        Matrix4f m = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        float endMultiplier = beamSize * context.focus();
        addVertex(builder, m, normal, r, g, b, 0, beamSize * endMultiplier, beamSize * endMultiplier, -length);
        addVertex(builder, m, normal, r, g, b, a,  beamSize, beamSize, 0);
        addVertex(builder, m, normal, r, g, b, a, beamSize, -beamSize, 0);
        addVertex(builder, m, normal, r, g, b, 0,beamSize * endMultiplier, -beamSize * endMultiplier, -length);

        addVertex(builder, m, normal, r, g, b, 0, -beamSize * endMultiplier, -beamSize * endMultiplier, -length);
        addVertex(builder, m, normal, r, g, b, a, -beamSize, -beamSize, 0);
        addVertex(builder, m, normal, r, g, b, a, -beamSize, beamSize, 0);
        addVertex(builder, m, normal, r, g, b, 0, -beamSize * endMultiplier, beamSize * endMultiplier, -length);

        addVertex(builder, m, normal, r, g, b, 0, -beamSize * endMultiplier, beamSize * endMultiplier, -length);
        addVertex(builder, m, normal, r, g, b, a, -beamSize, beamSize, 0);
        addVertex(builder, m, normal, r, g, b, a, beamSize, beamSize, 0);
        addVertex(builder, m, normal, r, g, b, 0, beamSize * endMultiplier, beamSize * endMultiplier, -length);

        addVertex(builder, m, normal, r, g, b, 0, beamSize * endMultiplier, -beamSize * endMultiplier, -length);
        addVertex(builder, m, normal, r, g, b, a, beamSize, -beamSize, 0);
        addVertex(builder, m, normal, r, g, b, a, -beamSize, -beamSize, 0);
        addVertex(builder, m, normal, r, g, b, 0, -beamSize * endMultiplier, -beamSize * endMultiplier, -length);
    }

    protected static void addVertex(VertexConsumer builder, Matrix4f matrix4f, Matrix3f matrix3f, int r, int g, int b, int a, float x, float y, float z) {
        builder.vertex(matrix4f, x, y, z).color(r, g, b, a).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return TheatricalConfig.INSTANCE.CLIENT.renderDistance;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPos) {
        return true;
    }
}
