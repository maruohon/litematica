package litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.EntityRenderer;

import malilib.render.RenderContext;
import litematica.config.Configs;
import litematica.render.LitematicaRenderer;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer
{
    private boolean renderCollidingSchematicBlocks;

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;updateChunks(J)V", shift = Shift.AFTER))
    private void setupAndUpdate(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        this.renderCollidingSchematicBlocks = Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue();
        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(partialTicks);
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(" +
                     "Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", ordinal = 0, shift = Shift.AFTER))
    private void renderSolid(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderSolid(this.renderCollidingSchematicBlocks, partialTicks);
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(" +
                     "Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", ordinal = 1, shift = Shift.AFTER))
    private void renderCutoutMipped(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderCutoutMipped(this.renderCollidingSchematicBlocks, partialTicks);
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(" +
                     "Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", ordinal = 2, shift = Shift.AFTER))
    private void renderCutout(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderCutout(this.renderCollidingSchematicBlocks, partialTicks);
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(" +
                     "Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", ordinal = 3, shift = Shift.AFTER))
    private void renderTranslucent(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderTranslucent(this.renderCollidingSchematicBlocks, partialTicks, RenderContext.DUMMY);
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntities(" +
                     "Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V"))
    private void renderEntities(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderEntities(partialTicks);
    }
}
