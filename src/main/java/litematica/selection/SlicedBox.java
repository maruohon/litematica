package litematica.selection;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;

public class SlicedBox extends Box
{
    private EnumFacing sliceDirection = EnumFacing.EAST;
    private int sliceStart = 0;
    private int sliceEnd = 1;
    private int sliceRepeatCount;

    public EnumFacing getSliceDirection()
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

    public void setSliceDirection(EnumFacing sliceDirection)
    {
        this.sliceDirection = sliceDirection;
    }

    public void setSliceStart(int sliceStart)
    {
        this.sliceStart = MathHelper.clamp(sliceStart, 0, this.getMaxSliceLength() - 1);
    }

    public void setSliceEnd(int sliceEnd)
    {
        this.sliceEnd = MathHelper.clamp(sliceEnd, 0, this.getMaxSliceLength() - 1);
    }

    public void setSliceRepeatCount(int sliceRepeatCount)
    {
        this.sliceRepeatCount = sliceRepeatCount;
    }
}
