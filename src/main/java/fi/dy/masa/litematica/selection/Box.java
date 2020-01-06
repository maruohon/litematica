package fi.dy.masa.litematica.selection;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.PositionUtils.CoordinateType;

public class Box
{
    @Nullable protected BlockPos pos1;
    @Nullable protected BlockPos pos2;
    protected Vec3i size = Vec3i.NULL_VECTOR;

    public Box()
    {
        this(BlockPos.ORIGIN, BlockPos.ORIGIN);
    }

    public Box(@Nullable BlockPos pos1, @Nullable BlockPos pos2)
    {
        this.pos1 = pos1;
        this.pos2 = pos2;

        this.updateSize();
    }

    public Box copy()
    {
        Box box = new Box(this.pos1, this.pos2);
        return box;
    }

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

    public Vec3i getSize()
    {
        return this.size;
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

    /*
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
    */

    private void updateSize()
    {
        if (this.pos1 != null && this.pos2 != null)
        {
            this.size = PositionUtils.getAreaSizeFromRelativeEndPosition(this.pos2.subtract(this.pos1));
        }
        else if (this.pos1 == null && this.pos2 == null)
        {
            this.size = new Vec3i(0, 0, 0);
        }
        else
        {
            this.size = new Vec3i(1, 1, 1);
        }
    }

    public BlockPos getPosition(Corner corner)
    {
        return corner == Corner.CORNER_1 ? this.getPos1() : this.getPos2();
    }

    public int getCoordinate(Corner corner, CoordinateType type)
    {
        BlockPos pos = this.getPosition(corner);

        switch (type)
        {
            case X: return pos.getX();
            case Y: return pos.getY();
            case Z: return pos.getZ();
        }

        return 0;
    }

    protected void setPosition(BlockPos pos, Corner corner)
    {
        if (corner == Corner.CORNER_1)
        {
            this.setPos1(pos);
        }
        else if (corner == Corner.CORNER_2)
        {
            this.setPos2(pos);
        }
    }

    public void setCoordinate(int value, Corner corner, CoordinateType type)
    {
        BlockPos pos = this.getPosition(corner);
        pos = PositionUtils.getModifiedPosition(pos, value, type);
        this.setPosition(pos, corner);
    }

    @Nullable
    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.pos1 != null)
        {
            obj.add("pos1", JsonUtils.blockPosToJson(this.pos1));
        }

        if (this.pos2 != null)
        {
            obj.add("pos2", JsonUtils.blockPosToJson(this.pos2));
        }

        return this.pos1 != null || this.pos2 != null ? obj : null;
    }
}
