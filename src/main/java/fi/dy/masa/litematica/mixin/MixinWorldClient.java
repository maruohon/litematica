package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSavedDataStorage;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient extends World
{
    protected MixinWorldClient(ISaveHandler saveHandler, WorldSavedDataStorage dataStorage, WorldInfo worldInfo, Dimension dimension, Profiler profiler, boolean isRemote)
    {
        super(saveHandler, dataStorage, worldInfo, dimension, profiler, isRemote);
    }

    @Inject(method = "invalidateRegionAndSetBlock", at = @At("HEAD"))
    private void onInvalidateRegionAndSetBlock(BlockPos pos, IBlockState state, CallbackInfo ci)
    {
        SchematicVerifier.markVerifierBlockChanges(pos);

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunkForRenderUpdate(pos);
        }
    }
}
