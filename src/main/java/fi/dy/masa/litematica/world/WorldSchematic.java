package fi.dy.masa.litematica.world;

import fi.dy.masa.litematica.mixin.IMixinWorldClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;

public class WorldSchematic extends WorldClient
{
    public WorldSchematic(NetHandlerPlayClient netHandler, WorldSettings settings, int dimension,
            EnumDifficulty difficulty, Profiler profilerIn)
    {
        super(netHandler, settings, dimension, difficulty, profilerIn);
    }

    @Override
    protected IChunkProvider createChunkProvider()
    {
        ChunkProviderSchematic provider = new ChunkProviderSchematic(this);

        ((IMixinWorldClient) (Object) this).setClientChunkProvider(provider);

        return provider;
    }
}
