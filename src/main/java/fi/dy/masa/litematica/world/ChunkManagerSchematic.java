package fi.dy.masa.litematica.world;

import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class ChunkManagerSchematic extends ChunkManager
{
    private final WorldSchematic world;
    private final Long2ObjectMap<ChunkSchematic> loadedChunks = new Long2ObjectOpenHashMap<>(8192);
    private final ChunkSchematic blankChunk;
    private final LightingProvider lightingProvider;

    public ChunkManagerSchematic(WorldSchematic world)
    {
        this.world = world;
        this.blankChunk = new ChunkSchematic(world, new ChunkPos(0, 0));
        this.lightingProvider = new LightingProvider(this, true, world.getDimension().hasSkyLight());
    }

    @Override
    public WorldSchematic getWorld()
    {
        return this.world;
    }

    public void loadChunk(int chunkX, int chunkZ)
    {
        ChunkSchematic chunk = new ChunkSchematic(this.world, new ChunkPos(chunkX, chunkZ));
        this.loadedChunks.put(ChunkPos.toLong(chunkX, chunkZ), chunk);
    }

    @Override
    public boolean isChunkLoaded(int chunkX, int chunkZ)
    {
        return this.loadedChunks.containsKey(ChunkPos.toLong(chunkX, chunkZ));
    }

    @Override
    public String getDebugString()
    {
        return "Schematic Chunk Cache: " + this.getLoadedChunkCount();
    }

    @Override
    public int getLoadedChunkCount()
    {
        return this.loadedChunks.size();
    }

    public Long2ObjectMap<ChunkSchematic> getLoadedChunks()
    {
        return this.loadedChunks;
    }

    @Override
    public WorldChunk getChunk(int chunkX, int chunkZ, ChunkStatus status, boolean fallbackToEmpty)
    {
        ChunkSchematic chunk = this.getChunk(chunkX, chunkZ);
        return chunk == null && fallbackToEmpty ? this.blankChunk : chunk;
    }

    @Override
    public ChunkSchematic getChunk(int chunkX, int chunkZ)
    {
        ChunkSchematic chunk = this.loadedChunks.get(ChunkPos.toLong(chunkX, chunkZ));
        return chunk == null ? this.blankChunk : chunk;
    }

    @Nullable
    public ChunkSchematic getChunkIfExists(int chunkX, int chunkZ)
    {
        return this.loadedChunks.get(ChunkPos.toLong(chunkX, chunkZ));
    }

    public void unloadChunk(int chunkX, int chunkZ)
    {
        ChunkSchematic chunk = this.loadedChunks.remove(ChunkPos.toLong(chunkX, chunkZ));

        if (chunk != null)
        {
            this.world.unloadedEntities(chunk.getEntityCount());
        }
    }

    @Override
    public LightingProvider getLightingProvider()
    {
        return this.lightingProvider;
    }

    @Override
    public void tick(BooleanSupplier shouldKeepTicking, boolean tickChunks)
    {
        // NO-OP
    }
}
