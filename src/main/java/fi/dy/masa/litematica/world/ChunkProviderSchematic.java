package fi.dy.masa.litematica.world;

import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;

public class ChunkProviderSchematic extends ChunkProviderClient
{
    private final WorldSchematic world;
    private final Long2ObjectMap<ChunkSchematic> loadedChunks = new Long2ObjectOpenHashMap<>(8192);
    private final Chunk blankChunk;

    public ChunkProviderSchematic(WorldSchematic world)
    {
        super(world);

        this.world = world;
        this.blankChunk = new EmptyChunk(world, 0, 0);
    }

    public ChunkSchematic loadChunk(int chunkX, int chunkZ)
    {
        ChunkSchematic chunk = new ChunkSchematic(this.world, chunkX, chunkZ);

        this.loadedChunks.put(ChunkPos.asLong(chunkX, chunkZ), chunk);
        chunk.setLoaded(true);

        return chunk;
    }

    public boolean isChunkLoaded(int chunkX, int chunkZ)
    {
        return this.loadedChunks.containsKey(ChunkPos.asLong(chunkX, chunkZ));
    }

    public Long2ObjectMap<ChunkSchematic> getLoadedChunks()
    {
        return this.loadedChunks;
    }

    @Nullable
    public ChunkSchematic getChunk(int chunkX, int chunkZ)
    {
        return this.loadedChunks.get(ChunkPos.asLong(chunkX, chunkZ));
    }

    @Override
    public Chunk getChunk(int chunkX, int chunkZ, boolean p_186025_3_, boolean fallbackToEmpty)
    {
        ChunkSchematic chunk = this.getChunk(chunkX, chunkZ);
        return chunk == null && fallbackToEmpty ? this.blankChunk : chunk;
    }

    @Override
    public void unloadChunk(int chunkX, int chunkZ)
    {
        ChunkSchematic chunk = this.loadedChunks.remove(ChunkPos.asLong(chunkX, chunkZ));

        if (chunk != null)
        {
            chunk.onUnload();
        }
    }
}
