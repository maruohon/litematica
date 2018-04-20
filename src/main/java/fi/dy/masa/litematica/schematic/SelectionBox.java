package fi.dy.masa.litematica.schematic;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.util.PositionUtils;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;

public class SelectionBox
{
    private BlockPos pos1;
    private BlockPos pos2;
    private BlockPos size = BlockPos.ORIGIN;
    private String name = "Unnamed";

    @Nullable
    public BlockPos getPos1()
    {
        return this.pos1;
    }

    @Nullable
    public BlockPos getPos2()
    {
        return this.pos2;
    }

    public BlockPos getSize()
    {
        return this.size;
    }

    public String getName()
    {
        return this.name;
    }

    public void setPos1(@Nullable BlockPos pos)
    {
        this.pos1 = pos;
        this.updateSize();
    }

    public void setPos2(@Nullable BlockPos pos)
    {
        this.pos2 = pos;
        this.updateSize();
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void rotate(Rotation rotation)
    {
        BlockPos pos = PositionUtils.getTransformedBlockPos(this.getSize(), Mirror.NONE, rotation);
        this.setPos2(this.getPos1().add(pos).add(-1, -1, -1));
    }

    public void mirror(Mirror mirror)
    {
        BlockPos pos = PositionUtils.getTransformedBlockPos(this.getSize(), mirror, Rotation.NONE);
        this.setPos2(this.getPos1().add(pos).add(-1, -1, -1));
    }

    private void updateSize()
    {
        if (this.pos1 != null && this.pos2 != null)
        {
            this.size = this.pos2.subtract(this.pos1).add(1, 1, 1);
        }
        else if (this.pos1 == null && this.pos2 == null)
        {
            this.size = BlockPos.ORIGIN;
        }
        else
        {
            this.size = new BlockPos(1, 1, 1);
        }
    }
}
