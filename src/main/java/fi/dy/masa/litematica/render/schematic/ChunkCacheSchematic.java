package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nullable;
import net.minecraft.class_4543;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import fi.dy.masa.litematica.world.FakeLightingProvider;

public class ChunkCacheSchematic implements ExtendedBlockView
{
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    private final FakeLightingProvider lightingProvider;
    protected int chunkStartX;
    protected int chunkStartZ;
    protected WorldChunk[][] chunkArray;
    protected boolean empty;
    protected World world;

    public ChunkCacheSchematic(ClientWorld worldIn, BlockPos pos, int expand)
    {
        this.lightingProvider = new FakeLightingProvider();

        this.world = worldIn;
        this.chunkStartX = (pos.getX() - expand) >> 4;
        this.chunkStartZ = (pos.getZ() - expand) >> 4;
        int chunkEndX = (pos.getX() + expand + 15) >> 4;
        int chunkEndZ = (pos.getZ() + expand + 15) >> 4;
        this.chunkArray = new WorldChunk[chunkEndX - this.chunkStartX + 1][chunkEndZ - this.chunkStartZ + 1];
        this.empty = true;

        for (int cx = this.chunkStartX; cx <= chunkEndX; ++cx)
        {
            for (int cz = this.chunkStartZ; cz <= chunkEndZ; ++cz)
            {
                this.chunkArray[cx - this.chunkStartX][cz - this.chunkStartZ] = (WorldChunk) worldIn.getChunk(cx, cz);
            }
        }

        for (int cx = pos.getX() >> 4; cx <= (pos.getX() + 15) >> 4; ++cx)
        {
            for (int cz = pos.getZ() >> 4; cz <= (pos.getZ() + 15) >> 4; ++cz)
            {
                WorldChunk chunk = this.chunkArray[cx - this.chunkStartX][cz - this.chunkStartZ];

                if (chunk != null && chunk.method_12228(pos.getY(), pos.getY() + 15) == false) // isEmptyBetween
                {
                    this.empty = false;
                    break;
                }
            }
        }
    }

    public boolean isEmpty()
    {
        return this.empty;
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        if (pos.getY() >= 0 && pos.getY() < 256)
        {
            int cx = (pos.getX() >> 4) - this.chunkStartX;
            int cz = (pos.getZ() >> 4) - this.chunkStartZ;

            if (cx >= 0 && cx < this.chunkArray.length &&
                cz >= 0 && cz < this.chunkArray[cx].length)
            {
                Chunk chunk = this.chunkArray[cx][cz];

                if (chunk != null)
                {
                    return chunk.getBlockState(pos);
                }
            }
        }

        return AIR;
    }

    @Override
    public Biome getBiome(BlockPos pos)
    {
        return Biomes.THE_END;
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos)
    {
        return this.getBlockEntity(pos, WorldChunk.CreationType.CHECK);
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos, WorldChunk.CreationType type)
    {
        int i = (pos.getX() >> 4) - this.chunkStartX;
        int j = (pos.getZ() >> 4) - this.chunkStartZ;

        return this.chunkArray[i][j].getBlockEntity(pos, type);
    }

    @Override
    public int getLightLevel(LightType var1, BlockPos var2)
    {
        return 15;
    }

    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        // TODO change when fluids become separate
        return this.getBlockState(pos).getFluidState();
    }

    @Override
    public class_4543 method_22385()
    {
        return null;
    }

    @Override
    public LightingProvider method_22336()
    {
        return this.lightingProvider;
    }
}
