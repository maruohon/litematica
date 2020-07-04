package de.meinbuild.liteschem.mixin;

import java.util.function.Supplier;

import de.meinbuild.liteschem.config.Configs;
import de.meinbuild.liteschem.schematic.verifier.SchematicVerifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import de.meinbuild.liteschem.util.SchematicWorldRefresher;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World
{
    private MixinClientWorld(MutableWorldProperties mutableWorldProperties, RegistryKey<World> registryKey, RegistryKey<DimensionType> registryKey2, DimensionType dimensionType, Supplier<Profiler> supplier, boolean bl, boolean bl2, long l)
    {
        super(mutableWorldProperties, registryKey, registryKey2, dimensionType, supplier, bl, bl2, l);
    }

    @Inject(method = "setBlockStateWithoutNeighborUpdates", at = @At("HEAD"))
    private void onSetBlockStateWithoutNeighborUpdates(BlockPos pos, BlockState state, CallbackInfo ci)
    {
        SchematicVerifier.markVerifierBlockChanges(pos);

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunkForRenderUpdate(pos);
        }
    }
}
