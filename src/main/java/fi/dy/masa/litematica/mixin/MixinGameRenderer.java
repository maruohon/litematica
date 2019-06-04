package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public class MixinGameRenderer
{
    private boolean renderCollidingSchematicBlocks;

    @Inject(method = "updateCameraAndRender(FJ)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;updateChunks(J)V", shift = Shift.AFTER))
    private void setupAndUpdate(float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        this.renderCollidingSchematicBlocks = Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue();
        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(partialTicks);
    }

    @Inject(method = "updateCameraAndRender(FJ)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(" +
                     "Lnet/minecraft/util/BlockRenderLayer;DLnet/minecraft/entity/Entity;)I", ordinal = 0, shift = Shift.AFTER))
    private void renderSolid(float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderSolid(this.renderCollidingSchematicBlocks, partialTicks);
    }

    @Inject(method = "updateCameraAndRender(FJ)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(" +
                     "Lnet/minecraft/util/BlockRenderLayer;DLnet/minecraft/entity/Entity;)I", ordinal = 1, shift = Shift.AFTER))
    private void renderCutoutMipped(float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderCutoutMipped(this.renderCollidingSchematicBlocks, partialTicks);
    }

    @Inject(method = "updateCameraAndRender(FJ)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(" +
                     "Lnet/minecraft/util/BlockRenderLayer;DLnet/minecraft/entity/Entity;)I", ordinal = 2, shift = Shift.AFTER))
    private void renderCutout(float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderCutout(this.renderCollidingSchematicBlocks, partialTicks);
    }

    @Inject(method = "updateCameraAndRender(FJ)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(" +
                     "Lnet/minecraft/util/BlockRenderLayer;DLnet/minecraft/entity/Entity;)I", ordinal = 3, shift = Shift.AFTER))
    private void renderTranslucent(float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderTranslucent(this.renderCollidingSchematicBlocks, partialTicks);
    }

    @Inject(method = "updateCameraAndRender(FJ)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderEntities(" +
                     "Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V"))
    private void renderEntities(float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderEntities(partialTicks);
    }
}
