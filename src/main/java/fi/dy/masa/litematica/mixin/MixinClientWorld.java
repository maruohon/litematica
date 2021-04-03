package fi.dy.masa.litematica.mixin;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.profiler.IProfiler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.Dimension;
import net.minecraft.world.DimensionType;
import net.minecraft.world.storage.ServerWorldInfo;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World
{
    protected MixinClientWorld(ServerWorldInfo props, DimensionType dimType,
            BiFunction<World, Dimension, AbstractChunkProvider> func, Supplier<IProfiler> profiler, boolean isClient)
    {
        super(props, dimType, func, profiler, isClient);
    }

    @Inject(method = "setBlockStateWithoutNeighborUpdates", at = @At("HEAD"))
    private void onInvalidateRegionAndSetBlock(BlockPos pos, BlockState state, CallbackInfo ci)
    {
        SchematicVerifier.markVerifierBlockChanges(pos);

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunkForRenderUpdate(pos);
        }
    }
}
