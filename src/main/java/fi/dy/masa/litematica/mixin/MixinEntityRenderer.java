package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.event.RenderEventHandler;
import net.minecraft.client.renderer.EntityRenderer;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer
{
    @Inject(method = "renderWorldPass(IFJ)V", at = @At(
            value = "INVOKE_STRING",
            target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V",
            args = "ldc=hand", shift = Shift.AFTER
        ))
    private void onRenderWorldLast(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci)
    {
        RenderEventHandler.getInstance().onRenderWorldLast(partialTicks);
    }

    @Inject(method = "updateCameraAndRender(FJ)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V",
            shift = Shift.AFTER))
    private void onRenderGameOverlayPost(float partialTicks, long nanoTime, CallbackInfo ci)
    {
        RenderEventHandler.getInstance().onRenderGameOverlayPost(partialTicks);
    }
}
