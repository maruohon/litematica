package fi.dy.masa.litematica.util;

import fi.dy.masa.litematica.interfaces.IMixinChunkProviderClient;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.interfaces.IRangeChangeListener;
import fi.dy.masa.malilib.util.LayerRange;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

public class SchematicWorldRefresher implements IRangeChangeListener
{
    public static final SchematicWorldRefresher INSTANCE = new SchematicWorldRefresher();

    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public void updateAll()
    {
        this.updateBetweenY(LayerRange.WORLD_VERTICAL_SIZE_MIN, LayerRange.WORLD_VERTICAL_SIZE_MAX);
    }

    @Override
    public void updateBetweenX(int minX, int maxX)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            final int xMin = Math.min(minX, maxX);
            final int xMax = Math.max(minX, maxX);
            final int cxMin = (xMin >> 4);
            final int cxMax = (xMax >> 4);
            WorldRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();
            Long2ObjectMap<ChunkSchematic> schematicChunks = world.getChunkProvider().getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) this.mc.world.getChunkProvider()).getLoadedChunks();

            for (Chunk chunk : schematicChunks.values())
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (chunk.x >= cxMin && chunk.x <= cxMax && chunk.isEmpty() == false &&
                    clientChunks.containsKey(ChunkPos.asLong(chunk.x, chunk.z)))
                {
                    minX = Math.max( chunk.x << 4      , xMin);
                    maxX = Math.min((chunk.x << 4) + 15, xMax);
                    renderer.markBlockRangeForRenderUpdate(minX, 0, (chunk.z << 4), maxX, 255, (chunk.z << 4) + 15);
                }
            }
        }
    }

    @Override
    public void updateBetweenY(int minY, int maxY)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            WorldRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();
            Long2ObjectMap<ChunkSchematic> schematicChunks = world.getChunkProvider().getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) this.mc.world.getChunkProvider()).getLoadedChunks();

            for (Chunk chunk : schematicChunks.values())
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (chunk.isEmpty() == false && clientChunks.containsKey(ChunkPos.asLong(chunk.x, chunk.z)))
                {
                    renderer.markBlockRangeForRenderUpdate((chunk.x << 4), minY, (chunk.z << 4), (chunk.x << 4) + 15, maxY, (chunk.z << 4) + 15);
                }
            }
        }
    }

    @Override
    public void updateBetweenZ(int minZ, int maxZ)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            final int zMin = Math.min(minZ, maxZ);
            final int zMax = Math.max(minZ, maxZ);
            final int czMin = (zMin >> 4);
            final int czMax = (zMax >> 4);
            WorldRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();
            Long2ObjectMap<ChunkSchematic> schematicChunks = world.getChunkProvider().getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) this.mc.world.getChunkProvider()).getLoadedChunks();

            for (Chunk chunk : schematicChunks.values())
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (chunk.z >= czMin && chunk.z <= czMax && chunk.isEmpty() == false &&
                    clientChunks.containsKey(ChunkPos.asLong(chunk.x, chunk.z)))
                {
                    minZ = Math.max( chunk.z << 4      , zMin);
                    maxZ = Math.min((chunk.z << 4) + 15, zMax);
                    renderer.markBlockRangeForRenderUpdate((chunk.x << 4), 0, minZ, (chunk.x << 4) + 15, 255, maxZ);
                }
            }
        }
    }

    public void markSchematicChunksForRenderUpdate(ChunkPos chunkPos)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            Long2ObjectMap<ChunkSchematic> schematicChunks = world.getChunkProvider().getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) this.mc.world.getChunkProvider()).getLoadedChunks();
            long key = ChunkPos.asLong(chunkPos.x, chunkPos.z);

            if (schematicChunks.containsKey(key) && clientChunks.containsKey(key))
            {
                WorldRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();
                renderer.markBlockRangeForRenderUpdate(chunkPos.x << 4, 0, chunkPos.z << 4, (chunkPos.x << 4) + 15, 255, (chunkPos.z << 4) + 15);
            }
        }
    }

    public void markSchematicChunkForRenderUpdate(BlockPos pos)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            Long2ObjectMap<ChunkSchematic> schematicChunks = world.getChunkProvider().getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) this.mc.world.getChunkProvider()).getLoadedChunks();
            long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);

            if (schematicChunks.containsKey(key) && clientChunks.containsKey(key))
            {
                WorldRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();
                renderer.markBlockRangeForRenderUpdate(pos.getX(), pos.getY(), pos.getZ(),pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }
}
