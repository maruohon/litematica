package litematica.schematic.placement;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;

import malilib.util.data.json.JsonUtils;
import litematica.Litematica;

public class SubRegionPlacement extends BasePlacement
{
    protected final BlockPos defaultPos;

    public SubRegionPlacement(BlockPos pos, String name)
    {
        this(pos, pos, name);
    }

    public SubRegionPlacement(BlockPos pos, BlockPos defaultPos, String name)
    {
        this.position = pos;
        this.defaultPos = defaultPos;
        this.name = name;
    }

    public SubRegionPlacement copy()
    {
        SubRegionPlacement copy = new SubRegionPlacement(this.position, this.defaultPos, this.name);

        copy.position = this.position;
        copy.rotation = this.rotation;
        copy.mirror = this.mirror;
        copy.enabled = this.enabled;
        copy.ignoreEntities = this.ignoreEntities;
        copy.coordinateLockMask = this.coordinateLockMask;

        return copy;
    }

    public boolean isRegionPlacementModifiedFromDefault()
    {
        return this.isRegionPlacementModified(this.defaultPos);
    }

    public boolean isRegionPlacementModified(BlockPos defaultPosition)
    {
        return this.enabled == false ||
               this.ignoreEntities ||
               this.mirror != Mirror.NONE ||
               this.rotation != Rotation.NONE ||
               this.position.equals(defaultPosition) == false;
    }

    void resetToOriginalValues()
    {
        this.position = this.defaultPos;
        this.rotation = Rotation.NONE;
        this.mirror = Mirror.NONE;
        this.enabled = true;
        this.ignoreEntities = false;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();
        obj.add("default_pos", JsonUtils.blockPosToJson(this.defaultPos));
        return obj;
    }

    @Nullable
    public static SubRegionPlacement fromJson(JsonObject obj)
    {
        BlockPos pos = JsonUtils.getBlockPos(obj, "pos");
        BlockPos defaultPos = JsonUtils.getBlockPos(obj, "default_pos");
        String name = JsonUtils.getString(obj, "name");

        if (pos == null)
        {
            Litematica.logger.warn("SubRegionPlacement.fromJson(): Error, no position");
            return null;
        }

        if (name == null)
        {
            Litematica.logger.warn("SubRegionPlacement.fromJson(): Error, no region name");
            return null;
        }

        if (defaultPos == null)
        {
            defaultPos = pos;
        }

        SubRegionPlacement placement = new SubRegionPlacement(pos, defaultPos, name);
        placement.enabled = JsonUtils.getBooleanOrDefault(obj, "enabled", true);
        placement.rotation = JsonUtils.getRotation(obj, "rotation");
        placement.mirror = JsonUtils.getMirror(obj, "mirror");
        placement.ignoreEntities = JsonUtils.getBooleanOrDefault(obj, "ignore_entities", false);
        placement.coordinateLockMask = JsonUtils.getIntegerOrDefault(obj, "locked_coords", 0);

        return placement;
    }
}
