package fi.dy.masa.litematica.world;

import fi.dy.masa.litematica.interfaces.IMixinChunkProviderClient;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

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

        ((IMixinChunkProviderClient) (Object) this).getChunkMapping().put(ChunkPos.asLong(chunkX, chunkZ), chunk);
        chunk.markLoaded(true);

        return chunk;
    }
}
