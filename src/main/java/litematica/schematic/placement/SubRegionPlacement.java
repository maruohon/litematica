package litematica.schematic.placement;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import malilib.util.data.json.JsonUtils;
import malilib.util.position.BlockMirror;
import malilib.util.position.BlockPos;
import malilib.util.position.BlockRotation;
import litematica.Litematica;

public class SubRegionPlacement extends BasePlacement
{
    protected final BlockPos defaultPos;
    protected String displayName;

    public SubRegionPlacement(BlockPos pos, String name)
    {
        this(pos, pos, name);
    }

    public SubRegionPlacement(BlockPos pos, BlockPos defaultPos, String name)
    {
        super(name, pos);

        this.defaultPos = defaultPos;
        this.displayName = name;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public SubRegionPlacement copy()
    {
        SubRegionPlacement copy = new SubRegionPlacement(this.position, this.defaultPos, this.name);

        copy.displayName = this.displayName;
        copy.rotation = this.rotation;
        copy.mirror = this.mirror;
        copy.enabled = this.enabled;
        copy.ignoreEntities = this.ignoreEntities;
        copy.renderEnclosingBox = this.renderEnclosingBox;
        copy.coordinateLockMask = this.coordinateLockMask;
        copy.boundingBoxColor = this.boundingBoxColor;

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
               this.mirror != BlockMirror.NONE ||
               this.rotation != BlockRotation.NONE ||
               this.position.equals(defaultPosition) == false;
    }

    protected boolean shouldSave()
    {
        return this.isRegionPlacementModifiedFromDefault() ||
               this.displayName.equals(this.name) == false;
    }

    void resetToOriginalValues()
    {
        this.position = this.defaultPos;
        this.rotation = BlockRotation.NONE;
        this.mirror = BlockMirror.NONE;
        this.enabled = true;
        this.ignoreEntities = false;
    }

    protected static void removeNonImportantSubRegionPropsForSharing(JsonObject obj)
    {
        obj.remove("locked_coords");
        obj.remove("render_enclosing_box");
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();

        if (this.defaultPos.equals(this.position) == false)
        {
            obj.add("default_pos", JsonUtils.blockPosToJson(this.defaultPos));
        }

        if (this.displayName.equals(this.name) == false)
        {
            obj.addProperty("display_name", this.displayName);
        }

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
            Litematica.LOGGER.warn("SubRegionPlacement.fromJson(): Error, no position");
            return null;
        }

        if (name == null)
        {
            Litematica.LOGGER.warn("SubRegionPlacement.fromJson(): Error, no region name");
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
        placement.displayName = JsonUtils.getStringOrDefault(obj, "display_name", placement.name);

        return placement;
    }
}
