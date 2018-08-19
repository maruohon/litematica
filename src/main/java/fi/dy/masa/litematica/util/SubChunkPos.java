package fi.dy.masa.litematica.util;

import java.util.Comparator;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class SubChunkPos extends Vec3i
{
    public SubChunkPos(int x, int y, int z)
    {
        super(x, y, z);
    }

    public static class DistanceComparator implements Comparator<SubChunkPos>
    {
        private final Vec3d referencePosition;

        public DistanceComparator(Vec3d referencePosition)
        {
            this.referencePosition = referencePosition;
        }

        @Override
        public int compare(SubChunkPos pos1, SubChunkPos pos2)
        {
            double dist1 = pos1.distanceSq(this.referencePosition.x, this.referencePosition.y, this.referencePosition.z);
            double dist2 = pos2.distanceSq(this.referencePosition.x, this.referencePosition.y, this.referencePosition.z);

            return dist1 < dist2 ? -1 : (dist1 > dist2 ? 1 : 0);
        }
    }
}
