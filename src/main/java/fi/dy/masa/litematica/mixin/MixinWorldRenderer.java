package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.render.LitematicaRenderer;

@Mixin(net.minecraft.client.renderer.WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Shadow
    private net.minecraft.client.world.ClientWorld world;

    @Inject(method = "loadRenderers()V", at = @At("RETURN"))
    private void onLoadRenderers(CallbackInfo ci)
    {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (this.world != null && this.world == net.minecraft.client.Minecraft.getInstance().world)
        {
            LitematicaRenderer.getInstance().loadRenderers();
        }
    }

    @Inject(method = "setupTerrain", at = @At("TAIL"))
    private void onPostSetupTerrain(
            net.minecraft.client.renderer.ActiveRenderInfo camera,
            net.minecraft.client.renderer.culling.ClippingHelper frustum,
            boolean hasForcedFrustum, int frame, boolean spectator, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(frustum);
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", ordinal = 0, shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V"))
    private void renderLayerSolid(
            com.mojang.blaze3d.matrix.MatrixStack matrices,
            float tickDelta, long limitTime, boolean renderBlockOutline,
            net.minecraft.client.renderer.ActiveRenderInfo camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,
            net.minecraft.client.renderer.LightTexture lightmapTextureManager,
            net.minecraft.util.math.vector.Matrix4f matrix4f,
            CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderSolid(matrices, tickDelta);
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", ordinal = 1, shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V"))
    private void renderLayerCutoutMipped(
            com.mojang.blaze3d.matrix.MatrixStack matrices,
            float tickDelta, long limitTime, boolean renderBlockOutline,
            net.minecraft.client.renderer.ActiveRenderInfo camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,
            net.minecraft.client.renderer.LightTexture lightmapTextureManager,
            net.minecraft.util.math.vector.Matrix4f matrix4f,
            CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderCutoutMipped(matrices, tickDelta);
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", ordinal = 2, shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V"))
    private void renderLayerCutout(
            com.mojang.blaze3d.matrix.MatrixStack matrices,
            float tickDelta, long limitTime, boolean renderBlockOutline,
            net.minecraft.client.renderer.ActiveRenderInfo camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,
            net.minecraft.client.renderer.LightTexture lightmapTextureManager,
            net.minecraft.util.math.vector.Matrix4f matrix4f,
            CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderCutout(matrices, tickDelta);
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", ordinal = 3, shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V"))
    private void renderLayerTranslucent(
            com.mojang.blaze3d.matrix.MatrixStack matrices,
            float tickDelta, long limitTime, boolean renderBlockOutline,
            net.minecraft.client.renderer.ActiveRenderInfo camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,
            net.minecraft.client.renderer.LightTexture lightmapTextureManager,
            net.minecraft.util.math.vector.Matrix4f matrix4f,
            CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderTranslucent(matrices, tickDelta);
    }

    @Inject(method = "updateCameraAndRender",
            at = @At(value = "INVOKE_STRING", args = "ldc=blockentities",
                     target = "Lnet/minecraft/profiler/IProfiler;endStartSection(Ljava/lang/String;)V"))
    private void onPostRenderEntities(
            com.mojang.blaze3d.matrix.MatrixStack matrices,
            float tickDelta, long limitTime, boolean renderBlockOutline,
            net.minecraft.client.renderer.ActiveRenderInfo camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,
            net.minecraft.client.renderer.LightTexture lightmapTextureManager,
            net.minecraft.util.math.vector.Matrix4f matrix4f,
            CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderEntities(matrices, tickDelta);
    }

    /*
    @Inject(method = "updateCameraAndRender", at = @At("TAIL"))
    private void onRenderWorldLast(
            com.mojang.blaze3d.matrix.MatrixStack matrices,
            float tickDelta, long limitTime, boolean renderBlockOutline,
            net.minecraft.client.renderer.ActiveRenderInfo camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,
            net.minecraft.client.renderer.LightTexture lightmapTextureManager,
            net.minecraft.client.util.math.Matrix4f matrix4f,
            CallbackInfo ci)
    {
        boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();

        if (Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert &&
            Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() == false)
        {
            LitematicaRenderer.getInstance().renderSchematicWorld(matrices, matrix4f, tickDelta);
        }
    }
    */
}
