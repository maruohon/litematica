package litematica.schematic.verifier;

import java.util.Comparator;

import malilib.util.position.BlockPos;
import malilib.util.position.PositionUtils;

class BlockPairTypePositionComparator implements Comparator<BlockPairTypePosition>
{
    protected final int referencePosX;
    protected final int referencePosY;
    protected final int referencePosZ;
    protected final boolean closestFirst;

    public BlockPairTypePositionComparator(BlockPos referencePos, boolean closestFirst)
    {
        this.closestFirst = closestFirst;
        this.referencePosX = referencePos.getX();
        this.referencePosY = referencePos.getY();
        this.referencePosZ = referencePos.getZ();
    }

    @Override
    public int compare(BlockPairTypePosition pos1, BlockPairTypePosition pos2)
    {
        double dist1 = this.getSquareDistance(pos1.posLong);
        double dist2 = this.getSquareDistance(pos2.posLong);

        if (dist1 == dist2)
        {
            return 0;
        }

        return dist1 < dist2 == this.closestFirst ? -1 : 1;
    }

    protected double getSquareDistance(long posLong)
    {
        int diffX = PositionUtils.unpackX(posLong) - this.referencePosX;
        int diffY = PositionUtils.unpackY(posLong) - this.referencePosY;
        int diffZ = PositionUtils.unpackZ(posLong) - this.referencePosZ;
        return diffX * diffX + diffY * diffY + diffZ * diffZ;
    }
}
