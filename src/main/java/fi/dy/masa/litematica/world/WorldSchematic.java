package fi.dy.masa.litematica.world;

import fi.dy.masa.litematica.mixin.IMixinWorldClient;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
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

    @Override
    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags)
    {
        if (pos.getY() < 0 || pos.getY() >= 256)
        {
            return false;
        }
        else
        {
            return this.getChunkFromBlockCoords(pos).setBlockState(pos, newState) != null;
        }
    }

    @Override
    public int getLight(BlockPos pos)
    {
        return 15;
    }

    @Override
    public int getLight(BlockPos pos, boolean checkNeighbors)
    {
        return 15;
    }

    @Override
    public float getLightBrightness(BlockPos pos)
    {
        return 1f;
    }

    @Override
    public int getLightFor(EnumSkyBlock type, BlockPos pos)
    {
        return 15;
    }

    @Override
    public int getLightFromNeighbors(BlockPos pos)
    {
        return 15;
    }

    @Override
    public int getLightFromNeighborsFor(EnumSkyBlock type, BlockPos pos)
    {
        return 15;
    }

    @Override
    public boolean checkLight(BlockPos pos)
    {
        return false;
    }

    @Override
    public boolean checkLightFor(EnumSkyBlock lightType, BlockPos pos)
    {
        return false;
    }
}
