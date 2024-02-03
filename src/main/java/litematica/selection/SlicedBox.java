package litematica.selection;

import malilib.util.MathUtils;
import malilib.util.position.Direction;

public class SlicedBox extends CornerDefinedBox
{
    private Direction sliceDirection = Direction.EAST;
    private int sliceStart = 0;
    private int sliceEnd = 1;
    private int sliceRepeatCount;

    public Direction getSliceDirection()
    {
        return this.sliceDirection;
    }

    /**
     * @return the relative start offset from pos1 along the slice direction axis
     */
    public int getSliceStart()
    {
        return this.sliceStart;
    }

    /**
     * @return the relative end offset (inclusive) from pos1 along the slice direction axis
     */
    public int getSliceEnd()
    {
        return this.sliceEnd;
    }

    public int getSliceLength()
    {
        return Math.abs(this.sliceEnd - this.sliceStart) + 1;
    }

    public int getSliceRepeatCount()
    {
        return this.sliceRepeatCount;
    }

    public int getMaxSliceLength()
    {
        switch (this.sliceDirection.getAxis())
        {
            case X: return this.getSize().getX();
            case Y: return this.getSize().getY();
            case Z: return this.getSize().getZ();
            default: return 1;
        }
    }

    public void setSliceDirection(Direction sliceDirection)
    {
        this.sliceDirection = sliceDirection;
    }

    public void setSliceStart(int sliceStart)
    {
        this.sliceStart = MathUtils.clamp(sliceStart, 0, this.getMaxSliceLength() - 1);
    }

    public void setSliceEnd(int sliceEnd)
    {
        this.sliceEnd = MathUtils.clamp(sliceEnd, 0, this.getMaxSliceLength() - 1);
    }

    public void setSliceRepeatCount(int sliceRepeatCount)
    {
        this.sliceRepeatCount = sliceRepeatCount;
    }
}
