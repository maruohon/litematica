package litematica.selection;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import malilib.util.data.json.JsonUtils;
import malilib.util.position.BlockPos;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.Vec3i;
import litematica.util.PositionUtils;

public class CornerDefinedBox
{
    protected BlockPos corner1;
    protected BlockPos corner2;
    protected Vec3i size;

    public CornerDefinedBox()
    {
        this(BlockPos.ORIGIN, BlockPos.ORIGIN);
    }

    public CornerDefinedBox(BlockPos corner1, BlockPos corner2)
    {
        this.corner1 = corner1;
        this.corner2 = corner2;

        this.updateSize();
    }

    public Vec3i getSize()
    {
        return this.size;
    }

    public BlockPos getCorner1()
    {
        return this.corner1;
    }

    public BlockPos getCorner2()
    {
        return this.corner2;
    }

    public void setCorner1(BlockPos pos)
    {
        this.corner1 = pos;
        this.updateSize();
    }

    public void setCorner2(BlockPos pos)
    {
        this.corner2 = pos;
        this.updateSize();
    }

    public BlockPos getCornerPosition(BoxCorner corner)
    {
        return corner.cornerGetter.apply(this);
    }

    protected void setCornerPosition(BoxCorner corner, BlockPos pos)
    {
        corner.cornerSetter.accept(this, pos);
    }

    public IntBoundingBox asIntBoundingBox()
    {
        return IntBoundingBox.createProper(this.corner1, this.corner2);
    }

    public CornerDefinedBox copy()
    {
        return new CornerDefinedBox(this.corner1, this.corner2);
    }

    protected void updateSize()
    {
        this.size = PositionUtils.getAreaSize(this.corner1, this.corner2);
    }

    @Nullable
    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("pos1", JsonUtils.blockPosToJson(this.corner1));
        obj.add("pos2", JsonUtils.blockPosToJson(this.corner2));

        return obj;
    }
}
