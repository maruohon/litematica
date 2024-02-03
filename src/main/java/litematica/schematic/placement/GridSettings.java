package litematica.schematic.placement;

import java.util.Objects;
import com.google.gson.JsonObject;

import malilib.util.data.json.JsonUtils;
import malilib.util.position.Coordinate;
import malilib.util.position.Vec3i;

public class GridSettings
{
    protected Vec3i size = Vec3i.ZERO;
    protected Vec3i defaultSize = Vec3i.ZERO;
    protected Vec3i repeatNegative = Vec3i.ZERO;
    protected Vec3i repeatPositive = Vec3i.ZERO;
    protected boolean enabled;
    protected boolean initialized;

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
        return (this.size.equals(Vec3i.ZERO) || this.size.equals(this.defaultSize)) &&
                this.repeatNegative.equals(Vec3i.ZERO) &&
                this.repeatPositive.equals(Vec3i.ZERO);
    }

    public Vec3i getSize()
    {
        return this.size;
    }

    public Vec3i getDefaultSize()
    {
        return this.defaultSize;
    }

    public Vec3i getRepeatNegative()
    {
        return this.repeatNegative;
    }

    public Vec3i getRepeatPositive()
    {
        return this.repeatPositive;
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

    public void setDefaultSize(Vec3i defaultSize)
    {
        this.defaultSize = defaultSize;
        // If the new default size is larger than the current size, then grow the current size
        this.setSize(this.size);
    }

    public void setSize(Vec3i size)
    {
        // Don't allow shrinking the grid size smaller than the placement enclosing box
        if (size.getX() < this.defaultSize.getX() ||
            size.getY() < this.defaultSize.getY() ||
            size.getZ() < this.defaultSize.getZ())
        {
            int x = Math.max(size.getX(), this.defaultSize.getX());
            int y = Math.max(size.getY(), this.defaultSize.getY());
            int z = Math.max(size.getZ(), this.defaultSize.getZ());
            size = new Vec3i(x, y, z);
        }

        this.size = size;
    }

    public void setRepeatCountNegative(Vec3i repeat)
    {
        this.repeatNegative = repeat;
        this.initialized = true;
    }

    public void setRepeatCountPositive(Vec3i repeat)
    {
        this.repeatPositive = repeat;
        this.initialized = true;
    }

    public void setSize(Coordinate coordinate, int value)
    {
        this.setSize(coordinate.modifyVec3i(value, this.size));
        this.initialized = true;
    }

    public void modifySize(Coordinate coordinate, int amount)
    {
        int oldValue = coordinate.asInt(this.size);
        int newValue = Math.max(1, oldValue + amount);
        this.setSize(coordinate, newValue);
    }

    public GridSettings copy()
    {
        GridSettings copy = new GridSettings();

        copy.repeatNegative = this.repeatNegative;
        copy.repeatPositive = this.repeatPositive;
        copy.size = this.size;
        copy.enabled = this.enabled;

        return copy;
    }

    public void copyFrom(GridSettings other)
    {
        this.repeatNegative = other.repeatNegative;
        this.repeatPositive = other.repeatPositive;
        this.size = other.size;
        this.defaultSize = other.defaultSize;
        this.enabled = other.enabled;
        this.initialized = other.initialized;
    }

    public void fromJson(JsonObject obj)
    {
        this.enabled = JsonUtils.getBoolean(obj, "enabled");
        this.size = JsonUtils.getVec3iOrDefault(obj, "size", this.defaultSize);
        this.repeatNegative = JsonUtils.getVec3iOrDefault(obj, "repeatNegative", Vec3i.ZERO);
        this.repeatPositive = JsonUtils.getVec3iOrDefault(obj, "repeatPositive", Vec3i.ZERO);
        this.initialized = this.isAtDefaultValues() == false;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.addProperty("enabled", this.enabled);
        obj.add("size", JsonUtils.blockPosToJson(this.size));
        obj.add("repeatNegative", JsonUtils.blockPosToJson(this.repeatNegative));
        obj.add("repeatPositive", JsonUtils.blockPosToJson(this.repeatPositive));

        return obj;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) { return true; }
        if (o == null || this.getClass() != o.getClass()) { return false; }

        GridSettings that = (GridSettings) o;

        if (this.enabled != that.enabled) { return false; }
        if (!Objects.equals(this.size, that.size)) { return false; }
        if (!Objects.equals(this.repeatNegative, that.repeatNegative)) { return false; }
        return Objects.equals(this.repeatPositive, that.repeatPositive);
    }

    @Override
    public int hashCode()
    {
        int result = this.size != null ? this.size.hashCode() : 0;
        result = 31 * result + (this.repeatNegative != null ? this.repeatNegative.hashCode() : 0);
        result = 31 * result + (this.repeatPositive != null ? this.repeatPositive.hashCode() : 0);
        result = 31 * result + (this.enabled ? 1 : 0);
        return result;
    }
}
