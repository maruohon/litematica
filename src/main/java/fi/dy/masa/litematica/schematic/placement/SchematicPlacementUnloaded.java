package fi.dy.masa.litematica.schematic.placement;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
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
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.gui.interfaces.IMessageConsumer;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.WorldUtils;

public class SchematicPlacementUnloaded
{
    protected static int lastColor;

    @Nullable protected File schematicFile;
    protected final Map<String, SubRegionPlacement> relativeSubRegionPlacements = new HashMap<>();
    protected final GridSettings gridSettings = new GridSettings();

    protected String placementSaveFile;
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
    @Nullable protected Box enclosingBox;
    @Nullable protected String selectedSubRegionName;
    @Nullable protected JsonObject materialListData;

    protected SchematicPlacementUnloaded(@Nullable String storageFile, @Nullable File schematicFile, BlockPos origin, String name, boolean enabled, boolean enableRender)
    {
        this.placementSaveFile = storageFile;
        this.schematicFile = schematicFile;
        this.origin = origin;
        this.name = name;
        this.enabled = enabled;
        this.enableRender = enableRender;

        this.setShouldBeSaved(this.schematicFile != null);
    }

    public boolean isLoaded()
    {
        return false;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean isSavedToFile()
    {
        return this.placementSaveFile != null;
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

    public GridSettings getGridSettings()
    {
        return this.gridSettings;
    }

    public void setSchematicFile(@Nullable File schematicFile)
    {
        this.schematicFile = schematicFile;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setBoxesBBColor(int color)
    {
        this.boxesBBColorVec = Color4f.fromColor(color, 1f);
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
    }

    protected void copyFrom(SchematicPlacementUnloaded other, boolean copyGridSettings)
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
        this.enclosingBox = other.enclosingBox != null ? other.enclosingBox.copy() : null;
        this.selectedSubRegionName = other.selectedSubRegionName;
        this.materialListData = other.materialListData != null ? JsonUtils.deepCopy(other.materialListData) : null;

        if (copyGridSettings)
        {
            this.gridSettings.copyFrom(other.gridSettings);
        }
    }

    public SchematicPlacementUnloaded copyAsUnloaded()
    {
        SchematicPlacementUnloaded placement = new SchematicPlacementUnloaded(null, this.schematicFile, this.origin, this.name, this.enabled, this.enableRender);
        placement.copyFrom(this, true);
        return placement;
    }

    @Nullable
    public SchematicPlacement fullyLoadPlacement()
    {
        if (this.schematicFile != null)
        {
            ISchematic schematic = SchematicHolder.getInstance().getOrLoad(this.schematicFile);

            if (schematic != null)
            {
                SchematicPlacement schematicPlacement = new SchematicPlacement(schematic, this.placementSaveFile, this.schematicFile, this.origin, this.name, this.enabled, this.enableRender);

                schematicPlacement.copyFrom(this, true);
                schematicPlacement.checkAreSubRegionsModified();
                schematicPlacement.updateEnclosingBox();

                return schematicPlacement;
            }
            else
            {
                InfoUtils.printErrorMessage("litematica.error.schematic_load.failed", this.schematicFile.getAbsolutePath());
            }
        }

        return null;
    }

    @Nullable
    public JsonObject toJson()
    {
        if (this.schematicFile != null)
        {
            JsonObject obj = new JsonObject();

            obj.add("schematic", new JsonPrimitive(this.schematicFile.getAbsolutePath()));
            obj.add("name", new JsonPrimitive(this.name));
            obj.add("origin", JsonUtils.blockPosToJson(this.origin));
            obj.add("rotation", new JsonPrimitive(this.rotation.name()));
            obj.add("mirror", new JsonPrimitive(this.mirror.name()));
            obj.add("ignore_entities", new JsonPrimitive(this.ignoreEntities()));
            obj.add("enabled", new JsonPrimitive(this.isEnabled()));
            obj.add("enable_render", new JsonPrimitive(this.enableRender));
            obj.add("render_enclosing_box", new JsonPrimitive(this.shouldRenderEnclosingBox()));
            obj.add("locked", new JsonPrimitive(this.isLocked()));
            obj.add("locked_coords", new JsonPrimitive(this.coordinateLockMask));
            obj.add("bb_color", new JsonPrimitive(this.boxesBBColorVec.intValue));
            obj.add("verifier_type", new JsonPrimitive(this.verifierType.getStringValue()));

            if (this.selectedSubRegionName != null)
            {
                obj.add("selected_region", new JsonPrimitive(this.selectedSubRegionName));
            }

            if (this.placementSaveFile != null)
            {
                obj.add("storage_file", new JsonPrimitive(this.placementSaveFile));
            }

            if (this.materialListData != null)
            {
                obj.add("material_list", this.materialListData);
            }

            if (this.gridSettings.isInitialized())
            {
                obj.add("grid", this.gridSettings.toJson());
            }

            if (this.relativeSubRegionPlacements.isEmpty() == false)
            {
                JsonArray arr = new JsonArray();

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
            BlockPos pos = JsonUtils.blockPosFromJson(obj, "origin");

            if (pos == null)
            {
                LiteModLitematica.logger.warn("Failed to load schematic placement for '{}', invalid origin position", schematicFile.getAbsolutePath());
                return null;
            }

            String name = obj.get("name").getAsString();
            Rotation rotation = Rotation.valueOf(obj.get("rotation").getAsString());
            Mirror mirror = Mirror.valueOf(obj.get("mirror").getAsString());
            boolean enabled = JsonUtils.getBoolean(obj, "enabled");
            boolean enableRender = JsonUtils.getBoolean(obj, "enable_render");

            SchematicPlacementUnloaded schematicPlacement = new SchematicPlacementUnloaded(null, schematicFile, pos, name, enabled, enableRender);
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

            if (JsonUtils.hasString(obj, "storage_file"))
            {
                schematicPlacement.placementSaveFile = JsonUtils.getString(obj, "storage_file");
            }

            if (JsonUtils.hasObject(obj, "grid"))
            {
                schematicPlacement.gridSettings.fromJson(JsonUtils.getNestedObject(obj, "grid", false));
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

    @Nullable
    public static SchematicPlacementUnloaded fromFile(File file)
    {
        JsonElement el = JsonUtils.parseJsonFile(file);

        if (el != null && el.isJsonObject())
        {
            SchematicPlacementUnloaded placement = fromJson(el.getAsJsonObject());

            if (placement != null)
            {
                placement.placementSaveFile = file.getName();
            }

            return placement;
        }

        return null;
    }

    public boolean saveToFileIfChanged(IMessageConsumer feedback)
    {
        if (this.shouldBeSaved == false)
        {
            feedback.addMessage(MessageType.WARNING, "litematica.message.error.schematic_placement.save.should_not_save");
            return false;
        }

        File file;

        if (this.placementSaveFile != null)
        {
            file = new File(getSaveDirectory(), this.placementSaveFile);
        }
        else
        {
            file = this.getAvailableFileName();
        }

        if (file == null)
        {
            feedback.addMessage(MessageType.ERROR, "litematica.message.error.schematic_placement.save.failed_to_get_save_file");
            return false;
        }

        if (this.placementSaveFile == null || file.exists() == false || this.wasModifiedSinceSaved())
        {
            JsonObject obj = this.toJson();

            if (obj != null)
            {
                return this.saveToFile(file, obj, feedback);
            }
            else
            {
                feedback.addMessage(MessageType.ERROR, "litematica.message.error.schematic_placement.save.failed_to_serialize");
                return false;
            }
        }
        else
        {
            feedback.addMessage(MessageType.WARNING, "litematica.message.error.schematic_placement.save.no_changes");
        }

        return true;
    }

    public boolean wasModifiedSinceSaved()
    {
        if (this.placementSaveFile != null)
        {
            File file = new File(getSaveDirectory(), this.placementSaveFile);
            JsonElement el = JsonUtils.parseJsonFile(file);

            if (el != null && el.isJsonObject())
            {
                JsonObject objOther = el.getAsJsonObject();
                JsonObject objThis = this.toJson();

                // Ignore some stuff that doesn't matter
                objOther.remove("enabled");
                objOther.remove("material_list");
                objOther.remove("storage_file");
                objThis.remove("enabled");
                objThis.remove("material_list");
                objThis.remove("storage_file");

                return objOther.equals(objThis) == false;
            }

            return true;
        }

        return false;
    }

    protected boolean saveToFile(File file, JsonObject obj, IMessageConsumer feedback)
    {
        boolean success = JsonUtils.writeJsonToFile(obj, file);

        if (success)
        {
            if (this.placementSaveFile == null)
            {
                this.placementSaveFile = file.getName();
            }

            feedback.addMessage(MessageType.SUCCESS, "litematica.gui.label.schematic_placement.saved_to_file", file.getName());
        }

        return success;
    }

    public static File getSaveDirectory()
    {
        String sep = File.separator;
        String worldAndDimPath = getWorldAndDimPath();
        String path = "litematica" + sep + "placements" + sep + worldAndDimPath;
        File dir = new File(FileUtils.getMinecraftDirectory(), path);

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            InfoUtils.printErrorMessage("Failed to create directory '", dir.getAbsolutePath() + "'");
        }

        return dir;
    }

    public static String getWorldAndDimPath()
    {
        String worldName = StringUtils.getWorldOrServerName();
        net.minecraft.world.World world = net.minecraft.client.Minecraft.getMinecraft().world;
        String path;

        if (worldName == null || world == null)
        {
            path = "__fallback";
        }
        else
        {
            String sep = File.separator;
            String dimStr = "dim_" + WorldUtils.getDimensionId(world);
            path = worldName + sep + dimStr;
        }

        return path;
    }

    @Nullable
    protected File getAvailableFileName()
    {
        if (this.getSchematicFile() == null)
        {
            return null;
        }

        File dir = getSaveDirectory();
        String schName = FileUtils.getNameWithoutExtension(this.getSchematicFile().getName());
        String nameBase = FileUtils.generateSafeFileName(schName);
        int id = 1;
        String name = String.format("%s_%03d.json", nameBase, id);
        File file = new File(dir, name);

        while (file.exists())
        {
            ++id;
            name = String.format("%s_%03d.json", nameBase, id);
            file = new File(dir, name);
        }

        return file;
    }

    protected static int getNextBoxColor()
    {
        int color = Color4f.getColorFromHue(lastColor);
        lastColor += 40;
        return color;
    }
}
