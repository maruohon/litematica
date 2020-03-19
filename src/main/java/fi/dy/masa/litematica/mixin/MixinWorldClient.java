package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.world.SchematicWorldRenderingNotifier;

@Mixin(net.minecraft.client.multiplayer.WorldClient.class)
public abstract class MixinWorldClient extends net.minecraft.world.World
{
    protected MixinWorldClient(
            net.minecraft.world.storage.ISaveHandler saveHandlerIn,
            net.minecraft.world.storage.WorldInfo info,
            net.minecraft.world.WorldProvider providerIn,
            net.minecraft.profiler.Profiler profilerIn, boolean client)
    {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
    }

    @Override
    public boolean setBlockState(net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState newState, int flags)
    {
        boolean success = super.setBlockState(pos, newState, flags);

        if (success)
        {
            SchematicVerifier.markVerifierBlockChanges(pos);

            if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
            {
                SchematicWorldRenderingNotifier.markSchematicChunkForRenderUpdate(pos);
            }
        }

        return success;
    }
}
