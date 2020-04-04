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

    @Nullable protected String placementSaveFile;
    protected BlockPos origin = BlockPos.ORIGIN;
    protected String name = "?";
    protected Color4f boundingBoxColor = new Color4f(0xFF, 0xFF, 0xFF);
    protected Rotation rotation = Rotation.NONE;
    protected Mirror mirror = Mirror.NONE;
    protected BlockInfoListType verifierType = BlockInfoListType.ALL;
    protected boolean ignoreEntities;
    protected boolean enabled;
    protected boolean renderEnclosingBox;
    protected boolean regionPlacementsModified;
    protected boolean locked;
    protected boolean shouldBeSaved = true;
    protected boolean invalidated;
    protected int coordinateLockMask;
    @Nullable protected Box enclosingBox;
    @Nullable protected String selectedSubRegionName;
    @Nullable protected JsonObject materialListData;

    protected SchematicPlacementUnloaded(@Nullable String storageFile, @Nullable File schematicFile)
    {
        this.placementSaveFile = storageFile;
        this.schematicFile = schematicFile;
    }

    protected SchematicPlacementUnloaded(@Nullable String storageFile, @Nullable File schematicFile, BlockPos origin, String name, boolean enabled)
    {
        this.placementSaveFile = storageFile;
        this.schematicFile = schematicFile;
        this.origin = origin;
        this.name = name;
        this.enabled = enabled;

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

    public boolean isLocked()
    {
        return this.locked;
    }

    public boolean shouldRenderEnclosingBox()
    {
        return this.renderEnclosingBox;
    }

    public boolean isInvalidated()
    {
        return this.invalidated;
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

    public Color4f getBoundingBoxColor()
    {
        return this.boundingBoxColor;
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
                return this.isEnabled();
        }
    }

    public void setSchematicFile(@Nullable File schematicFile)
    {
        this.schematicFile = schematicFile;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setShouldBeSaved(boolean shouldbeSaved)
    {
        this.shouldBeSaved = shouldbeSaved;
    }

    public void invalidate()
    {
        this.invalidated = true;
    }

    public void setBoundingBoxColor(int color)
    {
        this.boundingBoxColor = Color4f.fromColor(color, 1f);
    }

    public void setBoundingBoxColorToNext()
    {
        this.setBoundingBoxColor(getNextBoxColor());
    }

    protected void copyFrom(SchematicPlacementUnloaded other, boolean copyGridSettings)
    {
        this.relativeSubRegionPlacements.clear();
        other.relativeSubRegionPlacements.entrySet().forEach((entry) -> this.relativeSubRegionPlacements.put(entry.getKey(), entry.getValue().copy())); 

        this.origin = other.origin;
        this.name = other.name;
        this.boundingBoxColor = other.boundingBoxColor;
        this.rotation = other.rotation;
        this.mirror = other.mirror;
        this.ignoreEntities = other.ignoreEntities;
        this.enabled = other.enabled;
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
        SchematicPlacementUnloaded placement = new SchematicPlacementUnloaded(null, this.schematicFile, this.origin, this.name, this.enabled);
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
                SchematicPlacement schematicPlacement = new SchematicPlacement(schematic, this.placementSaveFile, this.schematicFile, this.origin, this.name, this.enabled);

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

    boolean readBaseSettingsFromJson(JsonObject obj)
    {
        if (hasBaseSettings(obj))
        {
            BlockPos origin = JsonUtils.blockPosFromJson(obj, "origin");

            if (origin == null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_placements.settings_load.missing_data");
                String name = this.schematicFile != null ? this.schematicFile.getAbsolutePath() : "<null>";
                LiteModLitematica.logger.warn("Failed to load schematic placement for '{}', invalid origin position", name);
                return false;
            }

            this.origin = origin;
            this.rotation = Rotation.NONE;
            this.mirror = Mirror.NONE;
            this.ignoreEntities = JsonUtils.getBoolean(obj, "ignore_entities");

            if (JsonUtils.hasString(obj, "rotation"))
            {
                Rotation rotation = Rotation.valueOf(obj.get("rotation").getAsString());

                if (rotation != null)
                {
                    this.rotation = rotation;
                }
            }

            if (JsonUtils.hasString(obj, "mirror"))
            {
                Mirror mirror = Mirror.valueOf(obj.get("mirror").getAsString());

                if (mirror != null)
                {
                    this.mirror = mirror;
                }
            }

            if (JsonUtils.hasString(obj, "name"))
            {
                String name = obj.get("name").getAsString();

                if (name != null)
                {
                    this.name = name;
                }
            }

            if (JsonUtils.hasObject(obj, "grid"))
            {
                this.gridSettings.fromJson(JsonUtils.getNestedObject(obj, "grid", false));
            }

            if (JsonUtils.hasArray(obj, "placements"))
            {
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
                                this.relativeSubRegionPlacements.put(placementName, placement);
                            }
                        }
                    }
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Writes the most important/basic settings to JSON.
     * If <b>minimal</b> is true, then only non-default rotation, mirror etc. values are included,
     * and the name is not included. This is meant for sharing the settings string with other people.
     * @param minimal
     * @return
     */
    public JsonObject baseSettingsToJson(boolean minimal)
    {
        JsonObject obj = new JsonObject();
        boolean all = minimal == false;

        obj.add("origin", JsonUtils.blockPosToJson(this.origin));

        if (all)
        {
            obj.add("name", new JsonPrimitive(this.name));
        }

        if (this.rotation != Rotation.NONE)
        {
            obj.add("rotation", new JsonPrimitive(this.rotation.name()));
        }

        if (this.mirror != Mirror.NONE)
        {
            obj.add("mirror", new JsonPrimitive(this.mirror.name()));
        }

        if (this.ignoreEntities())
        {
            obj.add("ignore_entities", new JsonPrimitive(this.ignoreEntities()));
        }

        if (this.gridSettings.isInitialized() && this.gridSettings.isAtDefaultValues() == false)
        {
            obj.add("grid", this.gridSettings.toJson());
        }

        if ((all || this.isRegionPlacementModified()) && this.relativeSubRegionPlacements.isEmpty() == false)
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

    @Nullable
    public JsonObject toJson()
    {
        if (this.schematicFile != null)
        {
            JsonObject obj = this.baseSettingsToJson(false);

            obj.add("schematic", new JsonPrimitive(this.schematicFile.getAbsolutePath()));
            obj.add("enabled", new JsonPrimitive(this.isEnabled()));
            obj.add("render_enclosing_box", new JsonPrimitive(this.shouldRenderEnclosingBox()));
            obj.add("locked", new JsonPrimitive(this.isLocked()));
            obj.add("locked_coords", new JsonPrimitive(this.coordinateLockMask));
            obj.add("bb_color", new JsonPrimitive(this.boundingBoxColor.intValue));
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

            return obj;
        }

        // If this placement is for an an in-memory-only Schematic, then there is no point in saving
        // this placement, as the schematic can't be automatically loaded anyway.
        return null;
    }

    protected static boolean hasBaseSettings(JsonObject obj)
    {
        return JsonUtils.hasArray(obj, "origin");
    }

    @Nullable
    public static SchematicPlacementUnloaded fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "schematic") && hasBaseSettings(obj))
        {
            File schematicFile = new File(obj.get("schematic").getAsString());
            SchematicPlacementUnloaded schematicPlacement = new SchematicPlacementUnloaded(null, schematicFile);

            if (schematicPlacement.readBaseSettingsFromJson(obj) == false)
            {
                return null;
            }

            schematicPlacement.enabled = JsonUtils.getBoolean(obj, "enabled");
            schematicPlacement.ignoreEntities = JsonUtils.getBoolean(obj, "ignore_entities");
            schematicPlacement.renderEnclosingBox = JsonUtils.getBoolean(obj, "render_enclosing_box");
            schematicPlacement.coordinateLockMask = JsonUtils.getInteger(obj, "locked_coords");

            if (JsonUtils.hasInteger(obj, "bb_color"))
            {
                schematicPlacement.setBoundingBoxColor(JsonUtils.getInteger(obj, "bb_color"));
            }
            else
            {
                schematicPlacement.setBoundingBoxColorToNext();
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

            schematicPlacement.locked = JsonUtils.getBoolean(obj, "locked");

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
