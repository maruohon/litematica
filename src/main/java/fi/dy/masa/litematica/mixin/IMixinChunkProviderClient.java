package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.chunk.Chunk;

@Mixin(ChunkProviderClient.class)
public interface IMixinChunkProviderClient
{
    @Accessor
    Long2ObjectMap<Chunk> getChunkMapping();
}
