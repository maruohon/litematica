package litematica.schematic.placement;

import java.util.Objects;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import malilib.util.data.json.JsonUtils;
import malilib.util.position.Coordinate;

public class GridSettings
{
    protected Vec3i size = Vec3i.NULL_VECTOR;
    protected Vec3i defaultSize = Vec3i.NULL_VECTOR;
    protected Vec3i repeatNegative = Vec3i.NULL_VECTOR;
    protected Vec3i repeatPositive = Vec3i.NULL_VECTOR;
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
        return (this.size.equals(Vec3i.NULL_VECTOR) || this.size.equals(this.defaultSize)) &&
                this.repeatNegative.equals(Vec3i.NULL_VECTOR) &&
                this.repeatPositive.equals(Vec3i.NULL_VECTOR);
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
        Vec3i oldSize = this.getSize();
        int defaultValue = coordinate.asInt(this.defaultSize);
        // Don't allow shrinking the grid size smaller than the placement enclosing box
        int newValue = Math.max(defaultValue, value);

        this.setSize(coordinate.modifyVec3i(newValue, oldSize));
        this.initialized = true;
    }

    public void modifySize(Coordinate coordinate, int amount)
    {
        Vec3i oldSize = this.getSize();
        int oldValue = coordinate.asInt(oldSize);
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
        BlockPos pos = JsonUtils.blockPosFromJson(obj, "size");
        BlockPos repNeg = JsonUtils.blockPosFromJson(obj, "repeatNegative");
        BlockPos repPos = JsonUtils.blockPosFromJson(obj, "repeatPositive");

        this.repeatNegative = repNeg != null ? repNeg : Vec3i.NULL_VECTOR;
        this.repeatPositive = repPos != null ? repPos : Vec3i.NULL_VECTOR;
        this.size = pos != null ? pos : this.defaultSize;
        this.initialized = this.isAtDefaultValues() == false;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("enabled", new JsonPrimitive(this.enabled));
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
