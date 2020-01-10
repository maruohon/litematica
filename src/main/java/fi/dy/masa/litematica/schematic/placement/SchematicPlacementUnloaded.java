package fi.dy.masa.litematica.schematic.placement;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;

public class SchematicPlacementUnloaded
{
    protected static final Set<Integer> USED_COLORS = new HashSet<>();
    protected static int nextColorIndex;

    @Nullable protected final File schematicFile;
    protected final Map<String, SubRegionPlacement> relativeSubRegionPlacements = new HashMap<>();

    protected BlockPos origin;
    protected String name;
    protected Color4f boxesBBColorVec = new Color4f(0xFF, 0xFF, 0xFF);
    protected Rotation rotation = Rotation.NONE;
    protected Mirror mirror = Mirror.NONE;
    protected BlockInfoListType verifierType = BlockInfoListType.ALL;
    protected boolean ignoreEntities;
    protected boolean enabled;
    protected boolean enableRender;
    protected boolean renderEnclosingBox;
    protected boolean regionPlacementsModified;
    protected boolean locked;
    protected boolean shouldBeSaved = true;
    protected int coordinateLockMask;
    protected int boxesBBColor;
    @Nullable protected Box enclosingBox;
    @Nullable protected String selectedSubRegionName;
    @Nullable protected JsonObject materialListData;

    protected SchematicPlacementUnloaded(@Nullable File schematicFile, BlockPos origin, String name, boolean enabled, boolean enableRender)
    {
        this.schematicFile = schematicFile;
        this.origin = origin;
        this.name = name;
        this.enabled = enabled;
        this.enableRender = enableRender;
    }

    public boolean isLoaded()
    {
        return false;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean isRenderingEnabled()
    {
        return this.isEnabled() && this.enableRender;
    }

    public boolean isLocked()
    {
        return this.locked;
    }

    public boolean shouldRenderEnclosingBox()
    {
        return this.renderEnclosingBox;
    }

    /**
     * Returns whether or not this placement should be saved by the SchematicPlacementManager
     * when it saves the list of placements.
     * @return
     */
    public boolean shouldBeSaved()
    {
        return this.shouldBeSaved;
    }

    public void setShouldBeSaved(boolean shouldbeSaved)
    {
        this.shouldBeSaved = shouldbeSaved;
    }

    public boolean matchesRequirement(RequiredEnabled required)
    {
        switch (required)
        {
            case ANY:
                return true;
            case PLACEMENT_ENABLED:
                return this.isEnabled();
            default:
                return this.isEnabled() && this.enableRender;
        }
    }

    public boolean isRegionPlacementModified()
    {
        return this.regionPlacementsModified;
    }

    public boolean ignoreEntities()
    {
        return this.ignoreEntities;
    }

    public String getName()
    {
        return this.name;
    }

    @Nullable
    public File getSchematicFile()
    {
        return this.schematicFile;
    }

    @Nullable
    public Box getEclosingBox()
    {
        return this.enclosingBox;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setBoxesBBColor(int color)
    {
        this.boxesBBColor = color;
        this.boxesBBColorVec = Color4f.fromColor(color, 1f);
        USED_COLORS.add(color);
    }

    public BlockPos getOrigin()
    {
        return origin;
    }

    public Rotation getRotation()
    {
        return rotation;
    }

    public Mirror getMirror()
    {
        return mirror;
    }

    public Color4f getBoxesBBColor()
    {
        return this.boxesBBColorVec;
    }

    protected void setBoxesBBColorNext()
    {
        this.setBoxesBBColor(getNextBoxColor());
    }

    public void onRemoved()
    {
        USED_COLORS.remove(this.boxesBBColor);

        if (USED_COLORS.isEmpty())
        {
            nextColorIndex = 0;
        }
    }

    @Nullable
    public JsonObject toJson()
    {
        if (this.schematicFile != null)
        {
            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();

            arr.add(this.origin.getX());
            arr.add(this.origin.getY());
            arr.add(this.origin.getZ());

            obj.add("schematic", new JsonPrimitive(this.schematicFile.getAbsolutePath()));
            obj.add("name", new JsonPrimitive(this.name));
            obj.add("origin", arr);
            obj.add("rotation", new JsonPrimitive(this.rotation.name()));
            obj.add("mirror", new JsonPrimitive(this.mirror.name()));
            obj.add("ignore_entities", new JsonPrimitive(this.ignoreEntities()));
            obj.add("enabled", new JsonPrimitive(this.isEnabled()));
            obj.add("enable_render", new JsonPrimitive(this.enableRender));
            obj.add("render_enclosing_box", new JsonPrimitive(this.shouldRenderEnclosingBox()));
            obj.add("locked", new JsonPrimitive(this.isLocked()));
            obj.add("locked_coords", new JsonPrimitive(this.coordinateLockMask));
            obj.add("bb_color", new JsonPrimitive(this.boxesBBColor));
            obj.add("verifier_type", new JsonPrimitive(this.verifierType.getStringValue()));

            if (this.selectedSubRegionName != null)
            {
                obj.add("selected_region", new JsonPrimitive(this.selectedSubRegionName));
            }

            if (this.relativeSubRegionPlacements.isEmpty() == false)
            {
                arr = new JsonArray();

                for (Map.Entry<String, SubRegionPlacement> entry : this.relativeSubRegionPlacements.entrySet())
                {
                    JsonObject placementObj = new JsonObject();
                    placementObj.add("name", new JsonPrimitive(entry.getKey()));
                    placementObj.add("placement", entry.getValue().toJson());
                    arr.add(placementObj);
                }

                obj.add("placements", arr);
            }

            return obj;
        }

        // If this placement is for an an in-memory-only Schematic, then there is no point in saving
        // this placement, as the schematic can't be automatically loaded anyway.
        return null;
    }

    protected void copyFrom(SchematicPlacementUnloaded other)
    {
        this.relativeSubRegionPlacements.clear();
        other.relativeSubRegionPlacements.entrySet().forEach((entry) -> this.relativeSubRegionPlacements.put(entry.getKey(), entry.getValue().copy())); 

        this.origin = other.origin;
        this.name = other.name;
        this.boxesBBColorVec = other.boxesBBColorVec;
        this.rotation = other.rotation;
        this.mirror = other.mirror;
        this.ignoreEntities = other.ignoreEntities;
        this.enabled = other.enabled;
        this.enableRender = other.enableRender;
        this.renderEnclosingBox = other.renderEnclosingBox;
        this.regionPlacementsModified = other.regionPlacementsModified;
        this.locked = other.locked;
        this.shouldBeSaved = other.shouldBeSaved;
        this.coordinateLockMask = other.coordinateLockMask;
        this.boxesBBColor = other.boxesBBColor;
        this.enclosingBox = other.enclosingBox != null ? other.enclosingBox.copy() : null;
        this.selectedSubRegionName = other.selectedSubRegionName;
        this.materialListData = other.materialListData != null ? JsonUtils.deepCopy(other.materialListData) : null;
    }

    @Nullable
    public SchematicPlacement fullyLoadPlacement()
    {
        if (this.schematicFile != null)
        {
            ISchematic schematic = SchematicHolder.getInstance().getOrLoad(this.schematicFile);

            if (schematic != null)
            {
                SchematicPlacement schematicPlacement = new SchematicPlacement(schematic, this.schematicFile, this.origin, this.name, this.enabled, this.enableRender);

                schematicPlacement.copyFrom(this);
                schematicPlacement.checkAreSubRegionsModified();
                schematicPlacement.updateEnclosingBox();

                return schematicPlacement;
            }
            else
            {
                InfoUtils.printErrorMessage("", this.schematicFile.getAbsolutePath());
            }
        }

        return null;
    }

    @Nullable
    public static SchematicPlacementUnloaded fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "schematic") &&
            JsonUtils.hasString(obj, "name") &&
            JsonUtils.hasArray(obj, "origin") &&
            JsonUtils.hasString(obj, "rotation") &&
            JsonUtils.hasString(obj, "mirror") &&
            JsonUtils.hasArray(obj, "placements"))
        {
            File schematicFile = new File(obj.get("schematic").getAsString());
            JsonArray posArr = obj.get("origin").getAsJsonArray();

            if (posArr.size() != 3)
            {
                LiteModLitematica.logger.warn("Failed to load schematic placement for '{}', invalid origin position", schematicFile.getAbsolutePath());
                return null;
            }

            String name = obj.get("name").getAsString();
            BlockPos pos = new BlockPos(posArr.get(0).getAsInt(), posArr.get(1).getAsInt(), posArr.get(2).getAsInt());
            Rotation rotation = Rotation.valueOf(obj.get("rotation").getAsString());
            Mirror mirror = Mirror.valueOf(obj.get("mirror").getAsString());
            boolean enabled = JsonUtils.getBoolean(obj, "enabled");
            boolean enableRender = JsonUtils.getBoolean(obj, "enable_render");

            SchematicPlacementUnloaded schematicPlacement = new SchematicPlacementUnloaded(schematicFile, pos, name, enabled, enableRender);
            schematicPlacement.rotation = rotation;
            schematicPlacement.mirror = mirror;
            schematicPlacement.ignoreEntities = JsonUtils.getBoolean(obj, "ignore_entities");
            schematicPlacement.renderEnclosingBox = JsonUtils.getBoolean(obj, "render_enclosing_box");
            schematicPlacement.locked = JsonUtils.getBoolean(obj, "locked");
            schematicPlacement.coordinateLockMask = JsonUtils.getInteger(obj, "locked_coords");

            if (JsonUtils.hasInteger(obj, "bb_color"))
            {
                schematicPlacement.setBoxesBBColor(JsonUtils.getInteger(obj, "bb_color"));
            }
            else
            {
                schematicPlacement.setBoxesBBColorNext();
            }

            if (JsonUtils.hasObject(obj, "material_list"))
            {
                schematicPlacement.materialListData = JsonUtils.getNestedObject(obj, "material_list", false);
            }

            if (JsonUtils.hasString(obj, "verifier_type"))
            {
                schematicPlacement.verifierType = BlockInfoListType.fromStringStatic(JsonUtils.getString(obj, "verifier_type"));
            }

            if (JsonUtils.hasString(obj, "selected_region"))
            {
                schematicPlacement.selectedSubRegionName = JsonUtils.getString(obj, "selected_region");
            }

            JsonArray placementArr = obj.get("placements").getAsJsonArray();

            for (int i = 0; i < placementArr.size(); ++i)
            {
                JsonElement el = placementArr.get(i);

                if (el.isJsonObject())
                {
                    JsonObject placementObj = el.getAsJsonObject();

                    if (JsonUtils.hasString(placementObj, "name") &&
                        JsonUtils.hasObject(placementObj, "placement"))
                    {
                        SubRegionPlacement placement = SubRegionPlacement.fromJson(placementObj.get("placement").getAsJsonObject());

                        if (placement != null)
                        {
                            String placementName = placementObj.get("name").getAsString();
                            schematicPlacement.relativeSubRegionPlacements.put(placementName, placement);
                        }
                    }
                }
            }

            return schematicPlacement;
        }

        return null;
    }

    protected static int getNextBoxColor()
    {
        int length = OverlayRenderer.KELLY_COLORS.length;
        int color = OverlayRenderer.KELLY_COLORS[nextColorIndex];
        nextColorIndex = (nextColorIndex + 1) % length;

        for (int i = 0; i < length; ++i)
        {
            if (USED_COLORS.contains(color) == false)
            {
                return color;
            }

            color = OverlayRenderer.KELLY_COLORS[nextColorIndex];
            nextColorIndex = (nextColorIndex + 1) % length;
        }

        return color;
    }
}
