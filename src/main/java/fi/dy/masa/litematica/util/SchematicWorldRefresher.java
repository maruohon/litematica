package fi.dy.masa.litematica.util;

import fi.dy.masa.litematica.interfaces.IMixinChunkProviderClient;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.interfaces.IRangeChangeListener;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.SubChunkPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class SchematicWorldRefresher implements IRangeChangeListener
{
    public static final SchematicWorldRefresher INSTANCE = new SchematicWorldRefresher();

    @Override
    public void updateAll()
    {
        this.updateBetweenY(LayerRange.WORLD_VERTICAL_SIZE_MIN, LayerRange.WORLD_VERTICAL_SIZE_MAX);
    }

    @Override
    public void updateBetweenX(int minX, int maxX)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            final int xMin = Math.min(minX, maxX);
            final int xMax = Math.max(minX, maxX);
            final int cxMin = (xMin >> 4);
            final int cxMax = (xMax >> 4);
            RenderGlobal rg = LitematicaRenderer.getInstance().getWorldRenderer();
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getMinecraft().world.getChunkProvider()).getLoadedChunks();

            for (Chunk chunk : schematicChunks.values())
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (chunk.x >= cxMin && chunk.x <= cxMax && chunk.isEmpty() == false &&
                    clientChunks.containsKey(ChunkPos.asLong(chunk.x, chunk.z)))
                {
                    minX = Math.max( chunk.x << 4      , xMin);
                    maxX = Math.min((chunk.x << 4) + 15, xMax);
                    rg.markBlockRangeForRenderUpdate(minX, 0, (chunk.z << 4), maxX, 255, (chunk.z << 4) + 15);
                }
            }
        }
    }

    @Override
    public void updateBetweenY(int minY, int maxY)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            RenderGlobal rg = LitematicaRenderer.getInstance().getWorldRenderer();
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getMinecraft().world.getChunkProvider()).getLoadedChunks();

            for (Chunk chunk : schematicChunks.values())
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (chunk.isEmpty() == false && clientChunks.containsKey(ChunkPos.asLong(chunk.x, chunk.z)))
                {
                    rg.markBlockRangeForRenderUpdate((chunk.x << 4) - 1, minY, (chunk.z << 4) - 1, (chunk.x << 4) + 16, maxY, (chunk.z << 4) + 16);
                }
            }
        }
    }

    @Override
    public void updateBetweenZ(int minZ, int maxZ)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            final int zMin = Math.min(minZ, maxZ);
            final int zMax = Math.max(minZ, maxZ);
            final int czMin = (zMin >> 4);
            final int czMax = (zMax >> 4);
            RenderGlobal rg = LitematicaRenderer.getInstance().getWorldRenderer();
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getMinecraft().world.getChunkProvider()).getLoadedChunks();

            for (Chunk chunk : schematicChunks.values())
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (chunk.z >= czMin && chunk.z <= czMax && chunk.isEmpty() == false &&
                    clientChunks.containsKey(ChunkPos.asLong(chunk.x, chunk.z)))
                {
                    minZ = Math.max( chunk.z << 4      , zMin);
                    maxZ = Math.min((chunk.z << 4) + 15, zMax);
                    rg.markBlockRangeForRenderUpdate((chunk.x << 4), 0, minZ, (chunk.x << 4) + 15, 255, maxZ);
                }
            }
        }
    }

    public static void markSchematicChunkForRenderUpdate(SubChunkPos chunkPos)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getMinecraft().world.getChunkProvider()).getLoadedChunks();
            long key = ChunkPos.asLong(chunkPos.getX(), chunkPos.getZ());

            if (schematicChunks.containsKey(key) && clientChunks.containsKey(key))
            {
                RenderGlobal rg = LitematicaRenderer.getInstance().getWorldRenderer();
                rg.markBlockRangeForRenderUpdate((chunkPos.getX() << 4) - 1, (chunkPos.getY() << 4) - 1, (chunkPos.getZ() << 4) - 1,
                                                 (chunkPos.getX() << 4) + 1, (chunkPos.getY() << 4) + 1, (chunkPos.getZ() << 4) + 1);
            }
        }
    }

    public static void markSchematicChunksForRenderUpdate(ChunkPos chunkPos)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getMinecraft().world.getChunkProvider()).getLoadedChunks();
            long key = ChunkPos.asLong(chunkPos.x, chunkPos.z);

            if (schematicChunks.containsKey(key) && clientChunks.containsKey(key))
            {
                RenderGlobal rg = LitematicaRenderer.getInstance().getWorldRenderer();
                rg.markBlockRangeForRenderUpdate((chunkPos.x << 4) - 1,   0, (chunkPos.z << 4) - 1,
                                                 (chunkPos.x << 4) + 1, 255, (chunkPos.z << 4) + 1);
            }
        }
    }

    public static void markSchematicChunkForRenderUpdate(BlockPos pos)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getMinecraft().world.getChunkProvider()).getLoadedChunks();
            long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);

            if (schematicChunks.containsKey(key) && clientChunks.containsKey(key))
            {
                RenderGlobal rg = LitematicaRenderer.getInstance().getWorldRenderer();
                rg.markBlockRangeForRenderUpdate(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            }
        }
    }
}
