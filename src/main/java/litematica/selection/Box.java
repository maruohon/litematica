package litematica.selection;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import malilib.util.data.json.JsonUtils;
import malilib.util.position.Coordinate;
import malilib.util.position.IntBoundingBox;
import litematica.util.PositionUtils;
import litematica.util.PositionUtils.Corner;

public class Box
{
    protected BlockPos pos1;
    protected BlockPos pos2;
    protected Vec3i size = new Vec3i(1, 1, 1);

    public Box()
    {
        this(BlockPos.ORIGIN, BlockPos.ORIGIN);
    }

    public Box(BlockPos pos1, BlockPos pos2)
    {
        this.pos1 = pos1;
        this.pos2 = pos2;

        this.updateSize();
    }

    public BlockPos getPos1()
    {
        return this.pos1;
    }

    public BlockPos getPos2()
    {
        return this.pos2;
    }

    public Vec3i getSize()
    {
        return this.size;
    }

    public Box copy()
    {
        return new Box(this.pos1, this.pos2);
    }

    public IntBoundingBox asIntBoundingBox()
    {
        return IntBoundingBox.createProper(this.pos1, this.pos2);
    }

    public void setPos1(BlockPos pos)
    {
        this.pos1 = pos;
        this.updateSize();
    }

    public void setPos2(BlockPos pos)
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

    protected void updateSize()
    {
        this.size = PositionUtils.getAreaSizeFromRelativeEndPosition(this.pos2.subtract(this.pos1));
    }

    public BlockPos getPosition(Corner corner)
    {
        return corner == Corner.CORNER_1 ? this.pos1 : this.pos2;
    }

    public int getCoordinate(Corner corner, Coordinate coordinate)
    {
        return coordinate.asInt(this.getPosition(corner));
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

    public void setCoordinate(int value, Corner corner, Coordinate coordinate)
    {
        BlockPos pos = this.getPosition(corner);
        pos = coordinate.modifyBlockPos(value, pos);
        this.setPosition(pos, corner);
    }

    @Nullable
    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("pos1", JsonUtils.blockPosToJson(this.pos1));
        obj.add("pos2", JsonUtils.blockPosToJson(this.pos2));

        return obj;
    }
}
