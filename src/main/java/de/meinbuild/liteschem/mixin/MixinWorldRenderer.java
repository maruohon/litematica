package de.meinbuild.liteschem.mixin;

import de.meinbuild.liteschem.render.LitematicaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;

@Mixin(net.minecraft.client.render.WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Shadow
    private net.minecraft.client.world.ClientWorld world;

    @Inject(method = "reload()V", at = @At("RETURN"))
    private void onLoadRenderers(CallbackInfo ci)
    {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (this.world != null && this.world == net.minecraft.client.MinecraftClient.getInstance().world)
        {
            LitematicaRenderer.getInstance().loadRenderers();
        }
    }

    @Inject(method = "setupTerrain", at = @At("TAIL"))
    private void onPostSetupTerrain(
            net.minecraft.client.render.Camera camera,
            net.minecraft.client.render.Frustum frustum,
            boolean hasForcedFrustum, int frame, boolean spectator, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(frustum);
    }

    @Inject(method = "renderLayer", at = @At("TAIL"))
    private void onRenderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double x, double y, double z, CallbackInfo ci)
    {
        if (renderLayer == RenderLayer.getSolid())
        {
            LitematicaRenderer.getInstance().piecewiseRenderSolid(matrixStack);
        }
        else if (renderLayer == RenderLayer.getCutoutMipped())
        {
            LitematicaRenderer.getInstance().piecewiseRenderCutoutMipped(matrixStack);
        }
        else if (renderLayer == RenderLayer.getCutout())
        {
            LitematicaRenderer.getInstance().piecewiseRenderCutout(matrixStack);
        }
        else if (renderLayer == RenderLayer.getTranslucent())
        {
            LitematicaRenderer.getInstance().piecewiseRenderTranslucent(matrixStack);
        }
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE_STRING", args = "ldc=blockentities",
                     target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V"))
    private void onPostRenderEntities(
            net.minecraft.client.util.math.MatrixStack matrices,
            float tickDelta, long limitTime, boolean renderBlockOutline,
            net.minecraft.client.render.Camera camera,
            net.minecraft.client.render.GameRenderer gameRenderer,
            net.minecraft.client.render.LightmapTextureManager lightmapTextureManager,
            net.minecraft.util.math.Matrix4f matrix4f,
            CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderEntities(matrices, tickDelta);
    }

    /*
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderWorldLast(
            net.minecraft.client.util.math.MatrixStack matrices,
            float tickDelta, long limitTime, boolean renderBlockOutline,
            net.minecraft.client.render.Camera camera,
            net.minecraft.client.render.GameRenderer gameRenderer,
            net.minecraft.client.render.LightmapTextureManager lightmapTextureManager,
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
