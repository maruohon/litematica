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

    public boolean intersects(Box other)
    {
        if ((this.pos1 == null && this.pos2 == null) || (other.pos1 == null && other.pos2 == null))
        {
            return false;
        }

        BlockPos thisPos1 = this.pos1 != null ? this.pos1 : this.pos2;
        BlockPos thisPos2 = this.pos2 != null ? this.pos2 : this.pos1;
        BlockPos otherPos1 = other.pos1 != null ? other.pos1 : other.pos2;
        BlockPos otherPos2 = other.pos2 != null ? other.pos2 : other.pos1;
        int thisMinX = Math.min(thisPos1.getX(), thisPos2.getX());
        int thisMinY = Math.min(thisPos1.getY(), thisPos2.getY());
        int thisMinZ = Math.min(thisPos1.getZ(), thisPos2.getZ());
        int thisMaxX = Math.max(thisPos1.getX(), thisPos2.getX());
        int thisMaxY = Math.max(thisPos1.getY(), thisPos2.getY());
        int thisMaxZ = Math.max(thisPos1.getZ(), thisPos2.getZ());
        int otherMinX = Math.min(otherPos1.getX(), otherPos2.getX());
        int otherMinY = Math.min(otherPos1.getY(), otherPos2.getY());
        int otherMinZ = Math.min(otherPos1.getZ(), otherPos2.getZ());
        int otherMaxX = Math.max(otherPos1.getX(), otherPos2.getX());
        int otherMaxY = Math.max(otherPos1.getY(), otherPos2.getY());
        int otherMaxZ = Math.max(otherPos1.getZ(), otherPos2.getZ());

        if (thisMaxX < otherMinX ||
            thisMaxY < otherMinY ||
            thisMaxZ < otherMinZ ||
            thisMinX > otherMaxX ||
            thisMinY > otherMaxY ||
            thisMinZ > otherMaxZ)
        {
            return false;
        }

        return true;
    }

    @Nullable
    public Box createIntersectingBox(Box other)
    {
        if ((this.pos1 == null && this.pos2 == null) || (other.pos1 == null && other.pos2 == null))
        {
            return null;
        }

        BlockPos thisPos1 = this.pos1 != null ? this.pos1 : this.pos2;
        BlockPos thisPos2 = this.pos2 != null ? this.pos2 : this.pos1;
        BlockPos otherPos1 = other.pos1 != null ? other.pos1 : other.pos2;
        BlockPos otherPos2 = other.pos2 != null ? other.pos2 : other.pos1;
        int thisMinX = Math.min(thisPos1.getX(), thisPos2.getX());
        int thisMinY = Math.min(thisPos1.getY(), thisPos2.getY());
        int thisMinZ = Math.min(thisPos1.getZ(), thisPos2.getZ());
        int thisMaxX = Math.max(thisPos1.getX(), thisPos2.getX());
        int thisMaxY = Math.max(thisPos1.getY(), thisPos2.getY());
        int thisMaxZ = Math.max(thisPos1.getZ(), thisPos2.getZ());
        int otherMinX = Math.min(otherPos1.getX(), otherPos2.getX());
        int otherMinY = Math.min(otherPos1.getY(), otherPos2.getY());
        int otherMinZ = Math.min(otherPos1.getZ(), otherPos2.getZ());
        int otherMaxX = Math.max(otherPos1.getX(), otherPos2.getX());
        int otherMaxY = Math.max(otherPos1.getY(), otherPos2.getY());
        int otherMaxZ = Math.max(otherPos1.getZ(), otherPos2.getZ());

        if (thisMaxX >= otherMinX &&
            thisMaxY >= otherMinY &&
            thisMaxZ >= otherMinZ &&
            thisMinX <= otherMaxX &&
            thisMinY <= otherMaxY &&
            thisMinZ <= otherMaxZ)
        {
            int minX = Math.max(thisMinX, otherMinX);
            int minY = Math.max(thisMinY, otherMinY);
            int minZ = Math.max(thisMinZ, otherMinZ);
            int maxX = Math.min(thisMaxX, otherMaxX);
            int maxY = Math.min(thisMaxY, otherMaxY);
            int maxZ = Math.min(thisMaxZ, otherMaxZ);
            BlockPos pos1 = new BlockPos(minX, minY, minZ);
            BlockPos pos2 = new BlockPos(maxX, maxY, maxZ);

            return new Box(pos1, pos2);
        }

        return null;
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
