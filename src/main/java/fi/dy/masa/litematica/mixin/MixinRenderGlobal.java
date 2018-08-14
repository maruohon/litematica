package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.interfaces.IMixinRenderGlobal;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal implements IMixinRenderGlobal
{
    @Shadow
    private WorldClient world;

    @Shadow
    private ChunkRenderDispatcher renderDispatcher;

    @Inject(method = "loadRenderers()V", at = @At("RETURN"))
    private void onLoadRenderers(CallbackInfo ci)
    {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (this.world != null && this.world == Minecraft.getMinecraft().world)
        {
            LitematicaRenderer.getInstance().loadRenderers();
        }
    }

    @Override
    public ChunkRenderDispatcher getChunkRenderDispatcher()
    {
        return this.renderDispatcher;
    }
}
