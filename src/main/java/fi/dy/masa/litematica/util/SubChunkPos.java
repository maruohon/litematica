package fi.dy.masa.litematica.util;

import java.util.Comparator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class SubChunkPos extends Vec3i
{
    public SubChunkPos(BlockPos pos)
    {
        this(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    public SubChunkPos(int x, int y, int z)
    {
        super(x, y, z);
    }

    public static class DistanceComparator implements Comparator<SubChunkPos>
    {
        private final SubChunkPos referencePosition;

        public DistanceComparator(SubChunkPos referencePosition)
        {
            this.referencePosition = referencePosition;
        }

        @Override
        public int compare(SubChunkPos pos1, SubChunkPos pos2)
        {
            int x = this.referencePosition.getX();
            int y = this.referencePosition.getY();
            int z = this.referencePosition.getZ();

            double dist1 = pos1.distanceSq(x, y, z);
            double dist2 = pos2.distanceSq(x, y, z);

            return dist1 < dist2 ? -1 : (dist1 > dist2 ? 1 : 0);
        }
    }
}
