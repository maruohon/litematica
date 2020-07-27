package fi.dy.masa.litematica.scheduler.tasks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import fi.dy.masa.malilib.util.data.IntBoundingBox;
import fi.dy.masa.malilib.util.position.LayerRange;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

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
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

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
                        posMutable.setPos(x, y, z);
                        this.countAtPosition(posMutable);
                    }
                }
            }
        }
    }

    protected void countBlocksInChunkRespectingLayerRange(ChunkPos pos, LayerRange range)
    {
        EnumFacing.Axis axis = range.getAxis();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (IntBoundingBox bb : this.getBoxesInChunk(pos))
        {
            final int startX = axis == EnumFacing.Axis.X ? Math.max(bb.minX, range.getLayerMin()) : bb.minX;
            final int startY = axis == EnumFacing.Axis.Y ? Math.max(bb.minY, range.getLayerMin()) : bb.minY;
            final int startZ = axis == EnumFacing.Axis.Z ? Math.max(bb.minZ, range.getLayerMin()) : bb.minZ;
            final int endX = axis == EnumFacing.Axis.X ? Math.min(bb.maxX, range.getLayerMax()) : bb.maxX;
            final int endY = axis == EnumFacing.Axis.Y ? Math.min(bb.maxY, range.getLayerMax()) : bb.maxY;
            final int endZ = axis == EnumFacing.Axis.Z ? Math.min(bb.maxZ, range.getLayerMax()) : bb.maxZ;

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        posMutable.setPos(x, y, z);
                        this.countAtPosition(posMutable);
                    }
                }
            }
        }
    }

    protected abstract void countAtPosition(BlockPos pos);
}
