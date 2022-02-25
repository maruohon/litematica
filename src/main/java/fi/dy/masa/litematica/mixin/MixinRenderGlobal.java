package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.world.SchematicWorldRenderingNotifier;

@Mixin(net.minecraft.client.renderer.RenderGlobal.class)
public abstract class MixinRenderGlobal
{
    @Shadow private net.minecraft.client.multiplayer.WorldClient world;

    @Inject(method = "loadRenderers()V", at = @At("RETURN"))
    private void onLoadRenderers(CallbackInfo ci)
    {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (this.world != null && this.world == net.minecraft.client.Minecraft.getMinecraft().world)
        {
            LitematicaRenderer.getInstance().loadRenderers();
        }
    }

    @Inject(method = "notifyBlockUpdate", at = @At("RETURN"))
    private void onNotifyBlockUpdate(
            net.minecraft.world.World worldIn,
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.block.state.IBlockState oldState,
            net.minecraft.block.state.IBlockState newState, int flags, CallbackInfo ci)
    {
        if (oldState != newState)
        {
            SchematicVerifier.markVerifierBlockChanges(pos);

            if (Configs.Visuals.MAIN_RENDERING_TOGGLE.getBooleanValue() &&
                Configs.Visuals.SCHEMATIC_RENDERING.getBooleanValue())
            {
                SchematicWorldRenderingNotifier.markSchematicChunkForRenderUpdate(pos);
            }
        }
    }
}
