package litematica.scheduler.tasks;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import net.minecraft.block.state.IBlockState;

import malilib.util.position.BlockPos;
import malilib.util.position.ChunkPos;
import malilib.util.position.Direction;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.LayerRange;

public abstract class TaskCountBlocksBase extends TaskProcessChunkBase
{
    protected final Object2LongOpenHashMap<IBlockState> countsTotal = new Object2LongOpenHashMap<>();

    protected TaskCountBlocksBase(String nameOnHud)
    {
        super(nameOnHud);
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        // All surrounding chunks are loaded on the client
        return this.areSurroundingChunksLoaded(pos, this.worldClient, 1);
    }

    protected void countBlocksInChunk(ChunkPos pos)
    {
        BlockPos.MutBlockPos posMutable = new BlockPos.MutBlockPos();

        for (IntBoundingBox bb : this.getBoxesInChunk(pos))
        {
            final int startX = bb.minX;
            final int startY = bb.minY;
            final int startZ = bb.minZ;
            final int endX = bb.maxX;
            final int endY = bb.maxY;
            final int endZ = bb.maxZ;

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        posMutable.set(x, y, z);
                        this.countAtPosition(posMutable);
                    }
                }
            }
        }
    }

    protected void countBlocksInChunkRespectingLayerRange(ChunkPos pos, LayerRange range)
    {
        Direction.Axis axis = range.getAxis();
        BlockPos.MutBlockPos posMutable = new BlockPos.MutBlockPos();

        for (IntBoundingBox bb : this.getBoxesInChunk(pos))
        {
            final int startX = axis == Direction.Axis.X ? Math.max(bb.minX, range.getMinLayerBoundary()) : bb.minX;
            final int startY = axis == Direction.Axis.Y ? Math.max(bb.minY, range.getMinLayerBoundary()) : bb.minY;
            final int startZ = axis == Direction.Axis.Z ? Math.max(bb.minZ, range.getMinLayerBoundary()) : bb.minZ;
            final int endX = axis == Direction.Axis.X ? Math.min(bb.maxX, range.getMaxLayerBoundary()) : bb.maxX;
            final int endY = axis == Direction.Axis.Y ? Math.min(bb.maxY, range.getMaxLayerBoundary()) : bb.maxY;
            final int endZ = axis == Direction.Axis.Z ? Math.min(bb.maxZ, range.getMaxLayerBoundary()) : bb.maxZ;

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        posMutable.set(x, y, z);
                        this.countAtPosition(posMutable);
                    }
                }
            }
        }
    }

    protected abstract void countAtPosition(BlockPos pos);
}
