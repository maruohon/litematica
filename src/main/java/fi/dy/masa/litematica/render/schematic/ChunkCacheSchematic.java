package fi.dy.masa.litematica.render.schematic;

import fi.dy.masa.litematica.world.FakeLightingProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightSourceView;
import net.minecraft.world.chunk.light.LightingProvider;

import javax.annotation.Nullable;

public class ChunkCacheSchematic implements BlockRenderView, ChunkProvider
{
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    protected final World world;
    protected final ClientWorld worldClient;
    protected final FakeLightingProvider lightingProvider;
    protected int chunkStartX;
    protected int chunkStartZ;
    protected WorldChunk[][] chunkArray;
    protected boolean empty;

    public ChunkCacheSchematic(World worldIn, ClientWorld clientWorld, BlockPos pos, int expand)
    {
        this.world = worldIn;
        this.lightingProvider = new FakeLightingProvider(this);

        this.worldClient = clientWorld;
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
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
                WorldChunk chunk = worldIn.getChunk(cx, cz);
                this.chunkArray[cx - this.chunkStartX][cz - this.chunkStartZ] = chunk;

                if (cx == chunkX && cz == chunkZ &&
                        !chunk.areSectionsEmptyBetween(worldIn.getBottomY(), worldIn.getTopY() - 1))
                {
                    this.empty = false;
                }
            }
        }
    }

    @Override
    public BlockView getWorld()
    {
        return this.world;
    }

    @Override
    public LightSourceView getChunk(int chunkX, int chunkZ)
    {
        return null; // TODO 1.17 this shouldn't be needed since the lighting provider does nothing
    }

    public boolean isEmpty()
    {
        return this.empty;
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
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

        return AIR;
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
    public LightingProvider getLightingProvider()
    {
        return this.lightingProvider;
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver)
    {
        return colorResolver.getColor(this.worldClient.getBiome(pos).value(), pos.getX(), pos.getZ());
    }

    @Override
    public float getBrightness(Direction direction, boolean bl)
    {
        return this.worldClient.getBrightness(direction, bl); // AO brightness on face
    }

    @Override
    public int getHeight()
    {
        return this.world.getHeight();
    }

    @Override
    public int getBottomY()
    {
        return this.world.getBottomY();
    }
}
