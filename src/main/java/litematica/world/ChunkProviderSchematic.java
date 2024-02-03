package litematica.world;

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import malilib.util.position.ChunkPos;
import litematica.interfaces.IMixinChunkProviderClient;

public class ChunkProviderSchematic extends ChunkProviderClient
{
    private final World world;

    public ChunkProviderSchematic(World world)
    {
        super(world);

        this.world = world;
    }

    @Override
    public Chunk loadChunk(int chunkX, int chunkZ)
    {
        Chunk chunk = new ChunkSchematic(this.world, chunkX, chunkZ);

        ((IMixinChunkProviderClient) (Object) this).getLoadedChunks().put(ChunkPos.asLong(chunkX, chunkZ), chunk);
        chunk.markLoaded(true);

        return chunk;
    }
}
