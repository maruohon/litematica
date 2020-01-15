package fi.dy.masa.litematica.schematic.placement;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.JsonUtils;

public class GridSettings
{
    private IntBoundingBox repeat = new IntBoundingBox(0, 0, 0, 0, 0, 0);
    private Vec3i size = Vec3i.NULL_VECTOR;
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

    public Vec3i getSize()
    {
        return this.size;
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

    public void setSize(Vec3i size)
    {
        this.size = size;
        this.initialized = true;
    }

    public void setRepeatCounts(IntBoundingBox repeat)
    {
        this.repeat = repeat;
        this.initialized = true;
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
        this.enabled = other.enabled;
        this.initialized = other.initialized;
    }

    public void fromJson(JsonObject obj)
    {
        if (JsonUtils.hasBoolean(obj, "enabled"))
        {
            this.enabled = JsonUtils.getBoolean(obj, "enabled");
        }

        if (JsonUtils.hasArray(obj, "repeat"))
        {
            IntBoundingBox repeat = IntBoundingBox.fromJson(obj.get("repeat").getAsJsonArray());

            if (repeat != null)
            {
                this.repeat = repeat;
            }
        }

        BlockPos pos = JsonUtils.blockPosFromJson(obj, "size");

        if (pos != null)
        {
            this.size = pos;
        }

        this.initialized = true;
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
