package litematica.schematic.placement;

import com.google.gson.JsonObject;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.Color4f;
import malilib.util.data.EnabledCondition;
import malilib.util.data.json.JsonUtils;
import malilib.util.position.BlockMirror;
import malilib.util.position.BlockPos;
import malilib.util.position.BlockRotation;
import malilib.util.position.Coordinate;
import litematica.util.PositionUtils;

public class BasePlacement
{
    protected String name;
    protected BlockPos position;
    protected BlockRotation rotation = BlockRotation.NONE;
    protected BlockMirror mirror = BlockMirror.NONE;
    protected Color4f boundingBoxColor = Color4f.WHITE;
    protected boolean enabled = true;
    protected boolean ignoreEntities;
    protected boolean renderEnclosingBox;
    protected int coordinateLockMask;

    protected BasePlacement(String name, BlockPos position)
    {
        this.name = name;
        this.position = position;
    }

    public String getName()
    {
        return this.name;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean ignoreEntities()
    {
        return this.ignoreEntities;
    }

    public boolean shouldRenderEnclosingBox()
    {
        return this.renderEnclosingBox;
    }

    public BlockPos getPosition()
    {
        return this.position;
    }

    public BlockRotation getRotation()
    {
        return this.rotation;
    }

    public BlockMirror getMirror()
    {
        return this.mirror;
    }

    public Color4f getBoundingBoxColor()
    {
        return this.boundingBoxColor;
    }

    public void setBoundingBoxColor(int color)
    {
        this.boundingBoxColor = Color4f.fromColor(color, 1f);
    }

    public boolean matchesRequirement(EnabledCondition condition)
    {
        return condition == EnabledCondition.ANY || ((condition == EnabledCondition.ENABLED) == this.enabled);
    }

    void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    void toggleEnabled()
    {
        this.enabled = ! this.enabled;
    }

    void toggleIgnoreEntities()
    {
        this.ignoreEntities = ! this.ignoreEntities;
    }

    void setRotation(BlockRotation rotation)
    {
        this.rotation = rotation;
    }

    void setMirror(BlockMirror mirror)
    {
        this.mirror = mirror;
    }

    void setPosition(BlockPos pos)
    {
        BlockPos newPos = PositionUtils.getModifiedPartiallyLockedPosition(this.position, pos, this.coordinateLockMask);

        if (newPos.equals(this.position) == false)
        {
            this.position = newPos;
        }
        else if (pos.equals(this.position) == false && this.coordinateLockMask != 0)
        {
            MessageDispatcher.error(2000).translate("litematica.error.schematic_placements.coordinate_locked");
        }
    }

    public void setCoordinateLocked(Coordinate coordinate, boolean locked)
    {
        int mask = 0x1 << coordinate.ordinal();

        if (locked)
        {
            this.coordinateLockMask |= mask;
        }
        else
        {
            this.coordinateLockMask &= ~mask;
        }
    }

    public boolean isCoordinateLocked(Coordinate coordinate)
    {
        int mask = 0x1 << coordinate.ordinal();
        return (this.coordinateLockMask & mask) != 0;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.addProperty("name", this.name);
        obj.add("pos", JsonUtils.blockPosToJson(this.position));

        // Only add the properties that have changed from the default values
        JsonUtils.addIfNotEqual(obj, "rotation", this.rotation, BlockRotation.NONE);
        JsonUtils.addIfNotEqual(obj, "mirror", this.mirror, BlockMirror.NONE);
        JsonUtils.addIfNotEqual(obj, "enabled", this.enabled, true);
        JsonUtils.addIfNotEqual(obj, "ignore_entities", this.ignoreEntities, false);
        JsonUtils.addIfNotEqual(obj, "render_enclosing_box", this.renderEnclosingBox, false);
        JsonUtils.addIfNotEqual(obj, "locked_coords", this.coordinateLockMask, 0);

        return obj;
    }
}
