package litematica.schematic.placement;

import com.google.gson.JsonObject;

import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.Color4f;
import malilib.util.data.EnabledCondition;
import malilib.util.data.json.JsonUtils;
import malilib.util.position.Coordinate;
import litematica.util.PositionUtils;

public class BasePlacement
{
    protected String name = "?";
    protected BlockPos position = BlockPos.ORIGIN;
    protected Rotation rotation = Rotation.NONE;
    protected Mirror mirror = Mirror.NONE;
    protected Color4f boundingBoxColor = Color4f.WHITE;
    protected boolean enabled = true;
    protected boolean ignoreEntities;
    protected boolean renderEnclosingBox;
    protected int coordinateLockMask;

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

    public Rotation getRotation()
    {
        return this.rotation;
    }

    public Mirror getMirror()
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

    void setRotation(Rotation rotation)
    {
        this.rotation = rotation;
    }

    void setMirror(Mirror mirror)
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

        obj.add("pos", JsonUtils.blockPosToJson(this.position));
        obj.addProperty("name", this.name);

        // Only add the properties that have changed from the default values

        if (this.rotation != Rotation.NONE)     { obj.addProperty("rotation",               this.rotation.name());    }
        if (this.mirror != Mirror.NONE)         { obj.addProperty("mirror",                 this.mirror.name());      }
        if (this.enabled == false)              { obj.addProperty("enabled",                this.enabled);            }
        if (this.ignoreEntities)                { obj.addProperty("ignore_entities",        this.ignoreEntities);     }
        if (this.renderEnclosingBox)            { obj.addProperty("render_enclosing_box",   this.renderEnclosingBox); }
        if (this.coordinateLockMask != 0)       { obj.addProperty("locked_coords",          this.coordinateLockMask); }

        return obj;
    }
}
