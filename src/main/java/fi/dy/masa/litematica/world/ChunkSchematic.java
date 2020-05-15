package fi.dy.masa.litematica.world;

import java.util.Arrays;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

public class ChunkSchematic extends WorldChunk
{
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    private final long timeCreated;
    private boolean isEmpty = true;

    public ChunkSchematic(World worldIn, ChunkPos pos)
    {
        super(worldIn, pos, new BiomeArray(Util.make(new Biome[BiomeArray.DEFAULT_LENGTH], (biomes) -> { Arrays.fill(biomes, Biomes.PLAINS); })));

        this.timeCreated = worldIn.getTime();
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        int x = pos.getX() & 0xF;
        int y = pos.getY();
        int z = pos.getZ() & 0xF;
        int cy = y >> 4;

        ChunkSection[] sections = this.getSectionArray();

        if (cy >= 0 && cy < sections.length)
        {
            ChunkSection chunkSection = sections[cy];

            if (ChunkSection.isEmpty(chunkSection) == false)
            {
                return chunkSection.getBlockState(x, y & 0xF, z);
            }
         }

         return AIR;
    }

    @Override
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving)
    {
        BlockState stateOld = this.getBlockState(pos);

        if (stateOld == state)
        {
            return null;
        }
        else
        {
            int x = pos.getX() & 15;
            int y = pos.getY();
            int z = pos.getZ() & 15;

            Block blockNew = state.getBlock();
            Block blockOld = stateOld.getBlock();
            ChunkSection section = this.getSectionArray()[y >> 4];

            if (section == EMPTY_SECTION)
            {
                if (state.isAir())
                {
                    return null;
                }

                section = new ChunkSection(y & 0xF0);
                this.getSectionArray()[y >> 4] = section;
            }

            if (state.isAir() == false)
            {
                this.isEmpty = false;
            }

            section.setBlockState(x, y & 0xF, z, state);

            if (blockOld != blockNew)
            {
                this.getWorld().removeBlockEntity(pos);
            }

            if (section.getBlockState(x, y & 0xF, z).getBlock() != blockNew)
            {
                return null;
            }
            else
            {
                if (blockOld.hasBlockEntity())
                {
                    BlockEntity te = this.getBlockEntity(pos, WorldChunk.CreationType.CHECK);

                    if (te != null)
                    {
                        te.resetBlock();
                    }
                }

                if (blockNew.hasBlockEntity() && blockNew instanceof BlockEntityProvider)
                {
                    BlockEntity te = this.getBlockEntity(pos, WorldChunk.CreationType.CHECK);

                    if (te == null)
                    {
                        te = ((BlockEntityProvider) blockNew).createBlockEntity(this.getWorld());
                        this.getWorld().setBlockEntity(pos, te);
                    }

                    if (te != null)
                    {
                        te.resetBlock();
                    }
                }

                this.markDirty();

                return stateOld;
            }
        }
    }

    public long getTimeCreated()
    {
        return this.timeCreated;
    }

    @Override
    public boolean isEmpty()
    {
        return this.isEmpty;
    }
}
