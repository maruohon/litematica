package fi.dy.masa.litematica.mixin;

import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World
{
    private MixinClientWorld(MutableWorldProperties properties,
                             RegistryKey<World> registryRef,
                             DynamicRegistryManager manager,
                             RegistryEntry<DimensionType> dimension,
                             Supplier<Profiler> supplier,
                             boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates)
    {
        super(properties, registryRef, manager, dimension, supplier, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void litematica_onHandleBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci)
    {
        SchematicVerifier.markVerifierBlockChanges(pos);

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunkForRenderUpdate(pos);
        }
    }
}
