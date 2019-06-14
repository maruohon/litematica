package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Shadow
    private ClientWorld world;

    @Inject(method = "reload()V", at = @At("RETURN"))
    private void onLoadRenderers(CallbackInfo ci)
    {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (this.world != null && this.world == MinecraftClient.getInstance().world)
        {
            LitematicaRenderer.getInstance().loadRenderers();
        }
    }

    @Inject(method = "setUpTerrain", at = @At("TAIL"))
    private void onPostSetupTerrain(Camera camera, VisibleRegion visibleRegion, int frame, boolean spectator, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(visibleRegion);
    }

    @Inject(method = "renderEntities", at = @At("TAIL"))
    private void onPostRenderEntities(Camera camera, VisibleRegion visibleRegion, float partialTicks, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseRenderEntities(visibleRegion, partialTicks);
    }
}
