package litematica.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import malilib.listener.LayerRangeChangeListener;
import malilib.util.game.wrap.GameWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.ChunkPos;
import malilib.util.position.ChunkSectionPos;
import malilib.util.position.LayerRange;
import litematica.interfaces.IMixinChunkProviderClient;
import litematica.render.LitematicaRenderer;

public class SchematicWorldRenderingNotifier implements LayerRangeChangeListener
{
    public static final SchematicWorldRenderingNotifier INSTANCE = new SchematicWorldRenderingNotifier();

    @Override
    public void updateAll()
    {
        this.updateBetweenY(LayerRange.WORLD_VERTICAL_SIZE_MIN, LayerRange.WORLD_VERTICAL_SIZE_MAX);
    }

    @Override
    public void updateBetweenX(int minX, int maxX)
    {
        World world = SchematicWorldHandler.getSchematicWorld();
        WorldClient clientWorld = GameWrap.getClientWorld();

        if (world != null && clientWorld != null)
        {
            final int xMin = Math.min(minX, maxX);
            final int xMax = Math.max(minX, maxX);
            final int cxMin = (xMin >> 4);
            final int cxMax = (xMax >> 4);
            RenderGlobal rg = LitematicaRenderer.getInstance().getWorldRenderer();
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) clientWorld.getChunkProvider()).getLoadedChunks();

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
        WorldClient clientWorld = GameWrap.getClientWorld();

        if (world != null && clientWorld != null)
        {
            RenderGlobal rg = LitematicaRenderer.getInstance().getWorldRenderer();
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) clientWorld.getChunkProvider()).getLoadedChunks();

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
        WorldClient clientWorld = GameWrap.getClientWorld();

        if (world != null && clientWorld != null)
        {
            final int zMin = Math.min(minZ, maxZ);
            final int zMax = Math.max(minZ, maxZ);
            final int czMin = (zMin >> 4);
            final int czMax = (zMax >> 4);
            RenderGlobal rg = LitematicaRenderer.getInstance().getWorldRenderer();
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) clientWorld.getChunkProvider()).getLoadedChunks();

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

    public static void markSchematicChunkForRenderUpdate(ChunkSectionPos chunkPos)
    {
        World world = SchematicWorldHandler.getSchematicWorld();
        WorldClient clientWorld = GameWrap.getClientWorld();

        if (world != null && clientWorld != null)
        {
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) clientWorld.getChunkProvider()).getLoadedChunks();
            long key = ChunkPos.asLong(chunkPos.getX(), chunkPos.getZ());

            if (schematicChunks.containsKey(key) && clientChunks.containsKey(key))
            {
                RenderGlobal rg = LitematicaRenderer.getInstance().getWorldRenderer();
                rg.markBlockRangeForRenderUpdate((chunkPos.getX() << 4) - 1, (chunkPos.getY() << 4) - 1, (chunkPos.getZ() << 4) - 1,
                                                 (chunkPos.getX() << 4) + 1, (chunkPos.getY() << 4) + 1, (chunkPos.getZ() << 4) + 1);
            }
        }
    }

    public static void markSchematicChunksForRenderUpdate(int chunkX, int chunkZ)
    {
        World world = SchematicWorldHandler.getSchematicWorld();
        WorldClient clientWorld = GameWrap.getClientWorld();

        if (world != null && clientWorld != null)
        {
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) clientWorld.getChunkProvider()).getLoadedChunks();
            long key = ChunkPos.asLong(chunkX, chunkZ);

            if (schematicChunks.containsKey(key) && clientChunks.containsKey(key))
            {
                RenderGlobal rg = LitematicaRenderer.getInstance().getWorldRenderer();
                rg.markBlockRangeForRenderUpdate((chunkX << 4) - 1,   0, (chunkZ << 4) - 1,
                                                 (chunkX << 4) + 1, 255, (chunkZ << 4) + 1);
            }
        }
    }

    public static void markSchematicChunkForRenderUpdate(BlockPos pos)
    {
        World world = SchematicWorldHandler.getSchematicWorld();
        WorldClient clientWorld = GameWrap.getClientWorld();

        if (world != null && clientWorld != null)
        {
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) clientWorld.getChunkProvider()).getLoadedChunks();
            long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);

            if (schematicChunks.containsKey(key) && clientChunks.containsKey(key))
            {
                RenderGlobal rg = LitematicaRenderer.getInstance().getWorldRenderer();
                rg.markBlockRangeForRenderUpdate(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            }
        }
    }
}
