package fi.dy.masa.litematica.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

public class ChunkSchematic extends WorldChunk
{
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    private final List<Entity> entityList = new ArrayList<>();
    private final long timeCreated;
    private final int bottomY;
    private final int topY;
    private int entityCount;
    private boolean isEmpty = true;

    public ChunkSchematic(World worldIn, ChunkPos pos)
    {
        super(worldIn, pos);

        this.timeCreated = worldIn.getTime();
        this.bottomY = worldIn.getBottomY();
        this.topY = worldIn.getTopY();
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        int x = pos.getX() & 0xF;
        int y = pos.getY();
        int z = pos.getZ() & 0xF;
        int cy = this.getSectionIndex(y);
        y &= 0xF;

        ChunkSection[] sections = this.getSectionArray();

        if (cy >= 0 && cy < sections.length)
        {
            ChunkSection chunkSection = sections[cy];

            if (!chunkSection.isEmpty())
            {
                return chunkSection.getBlockState(x, y, z);
            }
         }

         return AIR;
    }

    @Override
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving)
    {
        BlockState stateOld = this.getBlockState(pos);
        int y = pos.getY();

        if (stateOld == state || y >= this.topY || y < this.bottomY)
        {
            return null;
        }
        else
        {
            int x = pos.getX() & 15;
            int z = pos.getZ() & 15;
            int cy = this.getSectionIndex(y);

            Block blockNew = state.getBlock();
            Block blockOld = stateOld.getBlock();
            ChunkSection section = this.getSectionArray()[cy];

            if (section.isEmpty() && state.isAir())
            {
                return null;
            }

            y &= 0xF;

            if (state.isAir() == false)
            {
                this.isEmpty = false;
            }

            section.setBlockState(x, y, z, state);

            if (blockOld != blockNew)
            {
                this.getWorld().removeBlockEntity(pos);
            }

            if (section.getBlockState(x, y, z).getBlock() != blockNew)
            {
                return null;
            }
            else
            {
                if (state.hasBlockEntity() && blockNew instanceof BlockEntityProvider)
                {
                    BlockEntity te = this.getBlockEntity(pos, WorldChunk.CreationType.CHECK);

                    if (te == null)
                    {
                        te = ((BlockEntityProvider) blockNew).createBlockEntity(pos, state);

                        if (te != null)
                        {
                            this.getWorld().getWorldChunk(pos).setBlockEntity(te);
                        }
                    }
                }

                this.needsSaving();

                return stateOld;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addEntity(Entity entity)
    {
        this.entityList.add(entity);
        ++this.entityCount;
    }

    public List<Entity> getEntityList()
    {
        return this.entityList;
    }

    public int getEntityCount()
    {
        return this.entityCount;
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
