package fi.dy.masa.litematica.interfaces;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.chunk.Chunk;

public interface IMixinChunkProviderClient
{
    Long2ObjectMap<Chunk> getLoadedChunks();
}
