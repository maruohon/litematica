package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.interfaces.IMixinChunkProviderClient;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.chunk.Chunk;

@Mixin(ChunkProviderClient.class)
public abstract class MixinChunkProviderClient implements IMixinChunkProviderClient
{
    @Shadow
    @Final
    private Long2ObjectMap<Chunk> loadedChunks;

    @Override
    public Long2ObjectMap<Chunk> getLoadedChunks()
    {
        return this.loadedChunks;
    }

    @Inject(method = "unloadChunk", at = @At("RETURN"))
    private void onChunkUnload(int x, int z, CallbackInfo ci)
    {
        if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() == false)
        {
            DataManager.getSchematicPlacementManager().onClientChunkUnload(x, z);
        }
    }
}
