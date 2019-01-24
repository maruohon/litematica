package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import net.minecraft.client.renderer.EntityRenderer;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer
{
    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;updateChunks(J)V", shift = Shift.AFTER))
    private void setupAndUpdate(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(partialTicks);
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(" +
                     "Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", ordinal = 0, shift = Shift.AFTER))
    private void renderSolid(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderSolid(partialTicks);
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(" +
                     "Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", ordinal = 1, shift = Shift.AFTER))
    private void renderCutoutMipped(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderCutoutMipped(partialTicks);
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(" +
                     "Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", ordinal = 2, shift = Shift.AFTER))
    private void renderCutout(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderCutout(partialTicks);
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(" +
                     "Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", ordinal = 3, shift = Shift.AFTER))
    private void renderTranslucent(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderTranslucent(partialTicks);
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntities(" +
                     "Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V"))
    private void renderEntities(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderEntities(partialTicks);
    }
}
