package fi.dy.masa.litematica.schematic.placement;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.data.IntBoundingBox;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.PositionUtils.CoordinateType;

public class GridSettings
{
    private static final IntBoundingBox NO_REPEAT = new IntBoundingBox(0, 0, 0, 0, 0, 0);

    private IntBoundingBox repeat = NO_REPEAT;
    private Vec3i size = Vec3i.NULL_VECTOR;
    private Vec3i defaultSize = Vec3i.NULL_VECTOR;
    private boolean enabled;
    private boolean initialized;

    public boolean isInitialized()
    {
        return this.initialized;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean isAtDefaultValues()
    {
        return (this.size.equals(Vec3i.NULL_VECTOR) || this.size.equals(this.defaultSize)) && this.repeat.equals(NO_REPEAT);
    }

    public Vec3i getSize()
    {
        return this.size;
    }

    public Vec3i getDefaultSize()
    {
        return this.defaultSize;
    }

    public IntBoundingBox getRepeatCounts()
    {
        return this.repeat;
    }

    public boolean toggleEnabled()
    {
        this.enabled = ! this.enabled;
        this.initialized = true;
        return this.enabled;
    }

    public void resetSize()
    {
        this.setSize(this.defaultSize);
        this.initialized = true;
    }

    public void setDefaultSize(Vec3i size)
    {
        this.defaultSize = size;

        if (size.getX() > this.size.getX() ||
            size.getY() > this.size.getY() ||
            size.getZ() > this.size.getZ())
        {
            int x = Math.max(size.getX(), this.size.getX());
            int y = Math.max(size.getY(), this.size.getY());
            int z = Math.max(size.getZ(), this.size.getZ());
            this.setSize(new Vec3i(x, y, z));
        }
    }

    public void setSize(Vec3i size)
    {
        this.size = size;
    }

    public void setRepeatCounts(IntBoundingBox repeat)
    {
        this.repeat = repeat;
        this.initialized = true;
    }

    public void setSize(CoordinateType coord, int value)
    {
        Vec3i oldSize = this.getSize();
        int defaultValue = PositionUtils.getCoordinate(this.defaultSize, coord);
        // Don't allow shrinking the grid size smaller than the placement enclosing box
        int newValue = Math.max(defaultValue, value);

        this.setSize(PositionUtils.getModifiedPosition(oldSize, newValue, coord));
        this.initialized = true;
    }

    public void modifySize(CoordinateType coord, int amount)
    {
        Vec3i oldSize = this.getSize();
        int oldValue = PositionUtils.getCoordinate(oldSize, coord);
        int newValue = Math.max(1, oldValue + amount);
        this.setSize(coord, newValue);
    }

    public GridSettings copy()
    {
        GridSettings copy = new GridSettings();

        copy.repeat = this.repeat;
        copy.size = this.size;
        copy.enabled = this.enabled;

        return copy;
    }

    public void copyFrom(GridSettings other)
    {
        this.repeat = other.repeat;
        this.size = other.size;
        this.defaultSize = other.defaultSize;
        this.enabled = other.enabled;
        this.initialized = other.initialized;
    }

    public void fromJson(JsonObject obj)
    {
        this.enabled = JsonUtils.getBoolean(obj, "enabled");

        if (JsonUtils.hasArray(obj, "repeat"))
        {
            IntBoundingBox repeat = IntBoundingBox.fromJson(obj.get("repeat").getAsJsonArray());

            if (repeat != null)
            {
                this.repeat = repeat;
            }
        }
        else
        {
            this.repeat = NO_REPEAT;
        }

        BlockPos pos = JsonUtils.blockPosFromJson(obj, "size");

        if (pos != null)
        {
            this.size = pos;
        }
        else
        {
            this.size = this.defaultSize;
        }

        this.initialized = this.isAtDefaultValues() == false;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("enabled", new JsonPrimitive(this.enabled));
        obj.add("repeat", this.repeat.toJson());
        obj.add("size", JsonUtils.blockPosToJson(this.size));

        return obj;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((repeat == null) ? 0 : repeat.hashCode());
        result = prime * result + ((size == null) ? 0 : size.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass())
        {
            return false;
        }

        GridSettings other = (GridSettings) obj;

        if (this.enabled != other.enabled)
        {
            return false;
        }

        if (this.repeat == null)
        {
            if (other.repeat != null)
            {
                return false;
            }
        }
        else if (! this.repeat.equals(other.repeat))
        {
            return false;
        }

        if (this.size == null)
        {
            if (other.size != null)
            {
                return false;
            }
        }
        else if (! this.size.equals(other.size))
        {
            return false;
        }

        return true;
    }
}
