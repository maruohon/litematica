package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicVerifier;
import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient extends World
{
    protected MixinWorldClient()
    {
        super(null, null, null, null, null, true);
    }

    @Inject(method = "invalidateRegionAndSetBlock", at = @At("HEAD"))
    private void onInvalidateRegionAndSetBlock(BlockPos pos, IBlockState state, CallbackInfo ci)
    {
        SchematicVerifier verifier = DataManager.getActiveSchematicVerifier();

        if (verifier != null)
        {
            verifier.markBlockChanged(pos);
        }

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue())
        {
            WorldUtils.markSchematicChunkForRenderUpdate(pos);
        }
    }
}
