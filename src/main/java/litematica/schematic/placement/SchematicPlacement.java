package litematica.schematic.placement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.util.math.BlockPos;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.FileNameUtils;
import malilib.util.FileUtils;
import malilib.util.StringUtils;
import malilib.util.data.Color4f;
import malilib.util.data.EnabledCondition;
import malilib.util.data.json.JsonUtils;
import malilib.util.position.IntBoundingBox;
import litematica.Litematica;
import litematica.data.DataManager;
import litematica.data.SchematicHolder;
import litematica.materials.MaterialListBase;
import litematica.materials.MaterialListPlacement;
import litematica.schematic.ISchematic;
import litematica.schematic.ISchematicRegion;
import litematica.schematic.verifier.SchematicVerifier;
import litematica.selection.SelectionBox;
import litematica.util.PositionUtils;

public class SchematicPlacement extends BasePlacement
{
    protected static int lastColor;

    protected final Map<String, SubRegionPlacement> subRegionPlacements = new HashMap<>();
    protected final Map<String, SubRegionPlacement> modifiedSubRegions = new HashMap<>();

    @Nullable protected Path schematicFile;
    @Nullable protected ISchematic schematic;
    @Nullable protected IntBoundingBox enclosingBox;
    @Nullable protected GridSettings gridSettings;
    @Nullable protected String placementSaveFile;
    @Nullable protected String selectedSubRegionName;

    @Nullable protected JsonObject materialListData;
    @Nullable protected MaterialListBase materialList;
    @Nullable protected SchematicVerifier verifier;

    protected boolean locked;
    protected boolean regionPlacementsModified;
    protected boolean repeatedPlacement;
    protected boolean shouldBeSaved = true;
    protected boolean valid = true;
    protected int subRegionCount = 1;
    protected long lastSaveTime = -1;

    protected SchematicPlacement(@Nullable Path schematicFile)
    {
        this(schematicFile, BlockPos.ORIGIN, "?", true);
    }

    protected SchematicPlacement(@Nullable Path schematicFile,
                                 BlockPos origin,
                                 String name,
                                 boolean enabled)
    {
        super(name, origin);

        this.schematicFile = schematicFile;
        this.enabled = enabled;

        this.setShouldBeSaved(schematicFile != null);
    }

    protected SchematicPlacement(@Nullable ISchematic schematic,
                                 @Nullable Path schematicFile,
                                 BlockPos origin,
                                 String name,
                                 boolean enabled)
    {
        this(schematicFile, origin, name, enabled);

        this.schematic = schematic;
        this.subRegionCount = schematic != null ? schematic.getSubRegionCount() : 1;
    }

    public boolean isSchematicLoaded()
    {
        return this.schematic != null;
    }

    public boolean isLocked()
    {
        return this.locked;
    }

    public boolean isValid()
    {
        return this.valid;
    }

    public boolean isRepeatedPlacement()
    {
        return this.repeatedPlacement;
    }

    public boolean isSavedToFile()
    {
        return this.placementSaveFile != null;
    }

    public int getSubRegionCount()
    {
        return this.subRegionCount;
    }

    public long getLastSaveTime()
    {
        return this.lastSaveTime;
    }

    /**
     * @return true if this placement should be saved by the SchematicPlacementManager
     * when it saves the list of placements.
     */
    public boolean shouldBeSaved()
    {
        return this.shouldBeSaved;
    }

    public boolean isRegionPlacementModified()
    {
        return this.regionPlacementsModified;
    }

    public boolean isSchematicInMemoryOnly()
    {
        return this.schematicFile == null;
    }

    @Nullable
    public ISchematic getSchematic()
    {
        return this.schematic;
    }

    @Nullable
    public Path getSchematicFile()
    {
        return this.schematicFile;
    }

    @Nullable
    public String getSelectedSubRegionName()
    {
        return this.selectedSubRegionName;
    }

    public GridSettings getGridSettings()
    {
        if (this.gridSettings == null)
        {
            this.gridSettings = new GridSettings();
            this.updateEnclosingBox();
            this.gridSettings.setDefaultSize(PositionUtils.getAreaSizeFromBox(this.enclosingBox));
        }

        return this.gridSettings;
    }

    public IntBoundingBox getEnclosingBox()
    {
        if (this.enclosingBox == null)
        {
            this.updateEnclosingBox();
        }

        return this.enclosingBox;
    }

    public void setSchematicFile(@Nullable Path schematicFile)
    {
        this.schematicFile = schematicFile;
    }

    protected boolean setSchematic(@Nullable ISchematic schematic)
    {
        // Do we want to try to keep any modified subregions between different schematics?
        // That calls for problems if the schematics are actually different, as in if
        // they have a different set of subregions or some regions are in different locations etc.
        // this.storeModifiedSubRegions();

        this.schematic = schematic;

        if (this.isSchematicLoaded())
        {
            this.schematicFile = schematic.getFile();
            this.configureSubRegions();
            return true;
        }

        return false;
    }

    public boolean loadAndSetSchematicFromFile(Path file)
    {
        this.schematicFile = file;
        this.setSchematic(SchematicHolder.getInstance().getOrLoad(file));

        return this.isSchematicLoaded();
    }

    protected void loadSchematicFromFileIfEnabled()
    {
        if (this.enabled &&
            this.schematicFile != null &&
            this.loadAndSetSchematicFromFile(this.schematicFile) == false)
        {
            this.enabled = false;
        }
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setShouldBeSaved(boolean shouldBeSaved)
    {
        this.shouldBeSaved = shouldBeSaved;
    }

    public void toggleRenderEnclosingBox()
    {
        this.renderEnclosingBox = ! this.renderEnclosingBox;
    }

    public void toggleLocked()
    {
        this.locked = ! this.locked;
    }

    void setOrigin(BlockPos origin)
    {
        this.position = origin;
    }

    public void setSelectedSubRegionName(@Nullable String name)
    {
        this.selectedSubRegionName = name;
    }

    public void invalidate()
    {
        this.valid = false;
    }

    public MaterialListBase getMaterialList()
    {
        if (this.materialList == null)
        {
            if (this.materialListData != null)
            {
                this.materialList = MaterialListPlacement.createFromJson(this.materialListData, this);
            }
            else
            {
                this.materialList = new MaterialListPlacement(this, true);
            }
        }

        return this.materialList;
    }

    @Nullable
    public SubRegionPlacement getSelectedSubRegionPlacement()
    {
        return this.selectedSubRegionName != null ? this.subRegionPlacements.get(this.selectedSubRegionName) : null;
    }

    @Nullable
    public SubRegionPlacement getSubRegion(String areaName)
    {
        return this.subRegionPlacements.get(areaName);
    }

    public List<SubRegionPlacement> getAllSubRegions()
    {
        return new ArrayList<>(this.subRegionPlacements.values());
    }

    public List<SubRegionPlacement> getEnabledSubRegions()
    {
        ArrayList<SubRegionPlacement> list = new ArrayList<>();

        for (SubRegionPlacement region : this.subRegionPlacements.values())
        {
            if (region.matchesRequirement(EnabledCondition.ENABLED))
            {
                list.add(region);
            }
        }

        return list;
    }

    protected SelectionBox getSelectionBoxForRegion(SubRegionPlacement regionPlacement,
                                                    ISchematicRegion schematicRegion)
    {
        BlockPos boxOriginRelative = regionPlacement.getPosition();
        BlockPos boxOriginAbsolute = PositionUtils.getTransformedBlockPos(boxOriginRelative, this.mirror, this.rotation).add(this.position);
        BlockPos pos2 = new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(schematicRegion.getSize()));
        pos2 = PositionUtils.getTransformedBlockPos(pos2, this.mirror, this.rotation);
        pos2 = PositionUtils.getTransformedBlockPos(pos2, regionPlacement.getMirror(), regionPlacement.getRotation()).add(boxOriginAbsolute);

        return new SelectionBox(boxOriginAbsolute, pos2, regionPlacement.getName());
    }

    public ImmutableMap<String, SelectionBox> getSubRegionBoxes(EnabledCondition condition)
    {
        if (this.isSchematicLoaded() == false)
        {
            return ImmutableMap.of();
        }

        ImmutableMap.Builder<String, SelectionBox> builder = ImmutableMap.builder();
        Map<String, ISchematicRegion> schematicRegions = this.schematic.getRegions();

        for (SubRegionPlacement regionPlacement : this.subRegionPlacements.values())
        {
            if (regionPlacement.matchesRequirement(condition))
            {
                String regionName = regionPlacement.getName();
                ISchematicRegion schematicRegion = schematicRegions.get(regionName);

                if (schematicRegion != null)
                {
                    builder.put(regionName, this.getSelectionBoxForRegion(regionPlacement, schematicRegion));
                }
                else
                {
                    Litematica.logger.warn("SchematicPlacement.getSubRegionBoxes(): Subregion '{}' not found in the schematic '{}'",
                                           regionName, this.schematic.getMetadata().getName());
                }
            }
        }

        return builder.build();
    }

    @Nullable
    public SelectionBox getSubRegionBox(String regionName, EnabledCondition condition)
    {
        SubRegionPlacement regionPlacement = this.subRegionPlacements.get(regionName);

        if (this.isSchematicLoaded() && regionPlacement != null && regionPlacement.matchesRequirement(condition))
        {
            ISchematicRegion schematicRegion = this.schematic.getRegions().get(regionName);

            if (schematicRegion != null)
            {
                return this.getSelectionBoxForRegion(regionPlacement, schematicRegion);
            }
            else
            {
                Litematica.logger.warn("SchematicPlacement.getSubRegionBoxFor(): Sub-region '{}' not found in the schematic '{}'", regionName, this.schematic.getMetadata().getName());
            }
        }

        return null;
    }

    public ImmutableMap<String, IntBoundingBox> getBoxesWithinChunk(int chunkX, int chunkZ)
    {
        ImmutableMap<String, SelectionBox> subRegions = this.getSubRegionBoxes(EnabledCondition.ENABLED);
        return PositionUtils.getBoxesWithinChunk(chunkX, chunkZ, subRegions);
    }

    public LongSet getTouchedChunks()
    {
        return PositionUtils.getTouchedChunks(this.getSubRegionBoxes(EnabledCondition.ENABLED));
    }

    /*
    protected void storeModifiedSubRegions()
    {
        if (this.isSchematicLoaded() == false)
        {
            return;
        }

        for (Map.Entry<String, ISchematicRegion> entry : this.schematic.getRegions().entrySet())
        {
            String name = entry.getKey();
            SubRegionPlacement region = this.subRegionPlacements.get(name);

            if (region != null && region.isRegionPlacementModified(entry.getValue().getPosition()))
            {
                this.modifiedSubRegions.put(name, region.copy());
            }
        }
    }
    */

    protected void configureModifiedSubRegions()
    {
        for (SubRegionPlacement region : this.modifiedSubRegions.values())
        {
            String name = region.getName();

            if (this.subRegionPlacements.containsKey(name))
            {
                this.subRegionPlacements.put(name, region.copy());
            }
        }
    }

    protected void configureSubRegions()
    {
        if (this.isSchematicLoaded() == false)
        {
            return;
        }

        this.resetAllSubRegionsToSchematicValues();
        this.configureModifiedSubRegions();
        this.checkSubRegionsModified();
        this.subRegionCount = this.schematic.getSubRegionCount();
    }

    protected void checkSubRegionsModified()
    {
        if (this.isSchematicLoaded() == false)
        {
            return;
        }

        Map<String, ISchematicRegion> subRegions = this.schematic.getRegions();

        if (subRegions.size() != this.subRegionPlacements.size())
        {
            this.regionPlacementsModified = true;
            return;
        }

        for (Map.Entry<String, ISchematicRegion> entry : subRegions.entrySet())
        {
            SubRegionPlacement placement = this.subRegionPlacements.get(entry.getKey());

            if (placement == null || placement.isRegionPlacementModified(entry.getValue().getPosition()))
            {
                this.regionPlacementsModified = true;
                return;
            }
        }

        this.regionPlacementsModified = false;
    }

    protected void resetEnclosingBox()
    {
        this.enclosingBox = null;
    }

    protected void updateEnclosingBox()
    {
        ImmutableMap<String, SelectionBox> boxes = this.getSubRegionBoxes(EnabledCondition.ANY);

        if (boxes.isEmpty())
        {
            this.enclosingBox = IntBoundingBox.ORIGIN;
            return;
        }

        this.enclosingBox = PositionUtils.getEnclosingBox(boxes.values());

        if (this.gridSettings != null)
        {
            this.gridSettings.setDefaultSize(PositionUtils.getAreaSizeFromBox(this.enclosingBox));
        }
    }

    /**
     * Moves the subregion to the given <b>world position</b>.
     */
    void moveSubRegionTo(String regionName, BlockPos newPos)
    {
        SubRegionPlacement subRegion = this.subRegionPlacements.get(regionName);

        if (subRegion != null)
        {
            // The input argument position is an absolute position, so need to convert to relative position here
            newPos = newPos.subtract(this.position);
            // The absolute-based input position needs to be transformed if the entire placement has been rotated or mirrored
            newPos = PositionUtils.getReverseTransformedBlockPos(newPos, this.mirror, this.rotation);

            subRegion.setPosition(newPos);
            this.resetEnclosingBox();
        }
    }

    void setSubRegionsEnabledState(boolean state, Collection<SubRegionPlacement> subRegions)
    {
        for (SubRegionPlacement subRegion : subRegions)
        {
            // Check that the subregion is actually from this placement
            subRegion = this.subRegionPlacements.get(subRegion.getName());

            if (subRegion != null)
            {
                subRegion.setEnabled(state);
            }
        }
    }

    void resetSubRegionToSchematicValues(String regionName)
    {
        SubRegionPlacement placement = this.subRegionPlacements.get(regionName);

        if (placement != null)
        {
            placement.resetToOriginalValues();
            this.resetEnclosingBox();
        }
    }

    void resetAllSubRegionsToSchematicValues()
    {
        if (this.isSchematicLoaded() == false)
        {
            return;
        }

        this.subRegionPlacements.clear();
        this.regionPlacementsModified = false;

        for (Map.Entry<String, ISchematicRegion> entry : this.schematic.getRegions().entrySet())
        {
            String name = entry.getKey();
            this.subRegionPlacements.put(name, new SubRegionPlacement(entry.getValue().getPosition(), name));
        }

        this.resetEnclosingBox();
    }

    protected void copyBaseSettingsFrom(SchematicPlacement other)
    {
        this.subRegionPlacements.clear();

        other.subRegionPlacements.forEach((key, value) -> this.subRegionPlacements.put(key, value.copy()));

        this.name = other.name;
        this.position = other.position;
        this.rotation = other.rotation;
        this.mirror = other.mirror;
        this.enabled = other.enabled;
        this.ignoreEntities = other.ignoreEntities;

        this.boundingBoxColor = other.boundingBoxColor;
        this.coordinateLockMask = other.coordinateLockMask;
        this.enclosingBox = other.enclosingBox;
        this.locked = other.locked;
        this.regionPlacementsModified = other.regionPlacementsModified;
        this.renderEnclosingBox = other.renderEnclosingBox;
        this.shouldBeSaved = other.shouldBeSaved;
    }

    protected void copyGridSettingsFrom(SchematicPlacement other)
    {
        if (other.gridSettings != null)
        {
            this.getGridSettings().copyFrom(other.gridSettings);
        }
        else
        {
            this.gridSettings = null;
        }
    }

    public SchematicPlacement copy()
    {
        SchematicPlacement copy = new SchematicPlacement(this.schematic, this.schematicFile,
                                                         this.position, this.name, this.enabled);
        copy.copyBaseSettingsFrom(this);
        copy.copyGridSettingsFrom(this);
        return copy;
    }

    public SchematicPlacement createRepeatedCopy()
    {
        SchematicPlacement copy = new SchematicPlacement(this.schematic, this.schematicFile,
                                                         this.position, this.name, this.enabled);
        copy.copyBaseSettingsFrom(this);
        copy.repeatedPlacement = true;
        return copy;
    }

    public boolean fullyLoadPlacement()
    {
        if (this.schematicFile != null &&
            this.isSchematicLoaded() == false &&
            this.loadAndSetSchematicFromFile(this.schematicFile) == false)
        {
            MessageDispatcher.error().translate("litematica.error.schematic_load.failed",
                                                this.schematicFile.toAbsolutePath().toString());
            return false;
        }

        return true;
    }

    protected void setBoundingBoxColorToNext()
    {
        this.setBoundingBoxColor(getNextBoxColor());
    }

    public boolean wasModifiedSinceSaved()
    {
        if (this.placementSaveFile != null)
        {
            Path file = getSaveDirectory().resolve(this.placementSaveFile);
            JsonElement el = JsonUtils.parseJsonFile(file);

            if (el != null && el.isJsonObject())
            {
                JsonObject objOther = el.getAsJsonObject();
                JsonObject objThis = this.toJson();

                // Ignore some stuff that doesn't matter
                this.removeNonImportantPropsForModifiedSinceSavedCheck(objOther);
                this.removeNonImportantPropsForModifiedSinceSavedCheck(objThis);

                return objOther.equals(objThis) == false;
            }

            return true;
        }

        return false;
    }

    public boolean saveToFileIfChanged()
    {
        if (this.shouldBeSaved == false)
        {
            MessageDispatcher.warning().translate("litematica.message.error.schematic_placement.save.should_not_save");
            return false;
        }

        Path file;

        if (this.placementSaveFile != null)
        {
            file = getSaveDirectory().resolve(this.placementSaveFile);
        }
        else
        {
            file = this.getAvailableFileName();
        }

        if (file == null)
        {
            MessageDispatcher.error().translate("litematica.message.error.schematic_placement.save.failed_to_get_save_file");
            return false;
        }

        //if (this.placementSaveFile == null || Files.exists(file) == false || this.wasModifiedSinceSaved())
        //{
            JsonObject obj = this.toJson();

            if (obj != null)
            {
                return this.saveToFile(file, obj);
            }
            else
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_placement.save.failed_to_serialize");
                return false;
            }
        /*
        }
        else
        {
            MessageDispatcher.warning().translate("litematica.message.error.schematic_placement.save.no_changes");
        }

        return true;
        */
    }

    protected boolean saveToFile(Path file, JsonObject obj)
    {
        obj.addProperty("last_save_time", System.currentTimeMillis());

        if (JsonUtils.writeJsonToFile(obj, file))
        {
            if (this.placementSaveFile == null)
            {
                this.placementSaveFile = file.getFileName().toString();
            }

            MessageDispatcher.generic("litematica.gui.label.schematic_placement.saved_to_file",
                                      file.getFileName().toString());

            return true;
        }

        return false;
    }

    public JsonObject getSettingsShareJson()
    {
        JsonObject obj = this.toJson();
        this.removeNonImportantPlacementPropsForSharing(obj);
        return obj;
    }

    @Nullable
    public JsonObject toJsonIfShouldSave()
    {
        return this.shouldBeSaved ? this.toJson() : null;
    }

    @Override
    @Nullable
    public JsonObject toJson()
    {
        if (this.schematicFile == null)
        {
            // If this placement is for an in-memory-only schematic, then there is no point in saving
            // this placement, as the schematic can't be automatically loaded anyway.
            return null;
        }

        JsonObject obj = super.toJson();

        obj.addProperty("schematic", this.schematicFile.toAbsolutePath().toString());

        JsonUtils.addIfNotEqual(obj, "bb_color", this.boundingBoxColor.intValue, 0);
        JsonUtils.addIfNotEqual(obj, "locked", this.locked, false);
        // The region count is for the lightly loaded placements where it can't be read from the schematic
        JsonUtils.addIfNotEqual(obj, "region_count", this.subRegionCount, 1);
        JsonUtils.addStringIfNotNull(obj, "selected_region", this.selectedSubRegionName);
        JsonUtils.addStringIfNotNull(obj, "storage_file", this.placementSaveFile);

        JsonUtils.addElementIfNotNull(obj, "material_list_data", this.materialListData);

        if (this.gridSettings != null &&
            this.gridSettings.isInitialized() &&
            this.gridSettings.isAtDefaultValues() == false)
        {
            obj.add("grid", this.gridSettings.toJson());
        }

        // FIXME which one is needed?
        if (this.materialList != null)
        {
            obj.add("material_list", this.materialList.toJson());
        }

        JsonArray arr = new JsonArray();

        for (SubRegionPlacement region : this.subRegionPlacements.values())
        {
            if (region.isRegionPlacementModifiedFromDefault())
            {
                arr.add(region.toJson());
            }
        }

        if (arr.size() > 0)
        {
            obj.add("regions", arr);
        }

        return obj;
    }

    protected void removeNonImportantPropsForModifiedSinceSavedCheck(JsonObject obj)
    {
        obj.remove("enabled");
        obj.remove("locked");
        obj.remove("locked_coords");
        obj.remove("material_list");
        obj.remove("render_enclosing_box");
        obj.remove("selected_region");
        obj.remove("storage_file");
        obj.remove("last_save_time");
    }

    protected void removeNonImportantPlacementPropsForSharing(JsonObject obj)
    {
        this.removeNonImportantPropsForModifiedSinceSavedCheck(obj);

        obj.remove("bb_color");
        obj.remove("region_count");
        obj.remove("schematic");

        if (this.isSchematicLoaded() && this.schematic.getMetadata().getName().equals(this.name))
        {
            obj.remove("name");
        }
    }

    protected JsonObject getSharedSettingsPropertiesToLoad(JsonObject objIn)
    {
        JsonObject obj = new JsonObject();

        JsonUtils.copyPropertyIfExists(objIn, obj, "name");
        JsonUtils.copyPropertyIfExists(objIn, obj, "pos");
        JsonUtils.copyPropertyIfExists(objIn, obj, "rotation");
        JsonUtils.copyPropertyIfExists(objIn, obj, "mirror");
        JsonUtils.copyPropertyIfExists(objIn, obj, "ignore_entities");
        JsonUtils.copyPropertyIfExists(objIn, obj, "regions");
        JsonUtils.copyPropertyIfExists(objIn, obj, "grid");

        JsonUtils.copyPropertyIfExists(objIn, obj, "origin"); // backwards compat
        JsonUtils.copyPropertyIfExists(objIn, obj, "placements"); // backwards compat

        return obj;
    }

    protected void readSubRegionFromJson(JsonObject obj)
    {
        SubRegionPlacement placement = SubRegionPlacement.fromJson(obj);

        // Backwards compatibility with old configs/placement save files
        if (placement == null &&
            JsonUtils.hasString(obj, "name") &&
            JsonUtils.hasObject(obj, "placement"))
        {
            placement = SubRegionPlacement.fromJson(obj.get("placement").getAsJsonObject());
        }

        if (placement != null)
        {
            this.modifiedSubRegions.put(placement.getName(), placement);
        }
    }

    public boolean loadFromSharedSettings(JsonObject obj)
    {
        // Only use a fixed subset of the properties, to prevent copy-pasting settings
        // from other people messing up some of the internal values, or setting values
        // that don't really make sense to share.
        obj = this.getSharedSettingsPropertiesToLoad(obj);

        if (this.readFromJson(obj))
        {
            this.configureSubRegions();
            return true;
        }

        return false;
    }

    public boolean readFromJson(JsonObject obj)
    {
        String originKey = obj.has("pos") ? "pos" : "origin";
        BlockPos origin = JsonUtils.getBlockPos(obj, originKey);

        if (origin == null)
        {
            MessageDispatcher.error().translate("litematica.error.schematic_placements.settings_load.missing_data");
            String name = this.schematicFile != null ? this.schematicFile.toAbsolutePath().toString() : "<null>";
            Litematica.logger.warn("Failed to load schematic placement for '{}', invalid origin position", name);
            return false;
        }

        this.position = origin;
        this.name = JsonUtils.getStringOrDefault(obj, "name", this.name);
        this.rotation = JsonUtils.getRotation(obj, "rotation");
        this.mirror = JsonUtils.getMirror(obj, "mirror");
        this.ignoreEntities = JsonUtils.getBooleanOrDefault(obj, "ignore_entities", this.ignoreEntities);
        this.coordinateLockMask = JsonUtils.getIntegerOrDefault(obj, "locked_coords", this.coordinateLockMask);

        this.enabled = JsonUtils.getBooleanOrDefault(obj, "enabled", this.enabled);
        this.lastSaveTime = JsonUtils.getLongOrDefault(obj, "last_save_time", this.lastSaveTime);
        this.locked = JsonUtils.getBooleanOrDefault(obj, "locked", this.locked);
        this.placementSaveFile = JsonUtils.getStringOrDefault(obj, "storage_file", this.placementSaveFile);
        this.renderEnclosingBox = JsonUtils.getBooleanOrDefault(obj, "render_enclosing_box", this.renderEnclosingBox);
        this.selectedSubRegionName = JsonUtils.getStringOrDefault(obj, "selected_region", this.selectedSubRegionName);
        this.subRegionCount = JsonUtils.getIntegerOrDefault(obj, "region_count", this.subRegionCount);

        this.setBoundingBoxColor(JsonUtils.getIntegerOrDefault(obj, "bb_color", this.boundingBoxColor.intValue));
        JsonUtils.getObjectIfExists(obj, "material_list", o -> this.materialListData = o);

        this.modifiedSubRegions.clear();
        JsonUtils.getArrayElementsIfObjects(obj, "regions", this::readSubRegionFromJson);
        // This is for backwards compatibility with older configs
        JsonUtils.getArrayElementsIfObjects(obj, "placements", this::readSubRegionFromJson);

        // Note: This needs to be after reading the sub-regions, so that the enclosing box can be calculated
        // and the grid's default size set correctly.
        JsonUtils.getObjectIfExists(obj, "grid", this.getGridSettings()::fromJson);

        return true;
    }

    public static Path getSaveDirectory()
    {
        String worldName = StringUtils.getWorldOrServerNameOrDefault("__fallback");
        Path dir = DataManager.getDataBaseDirectory("placements").resolve(worldName);

        if (FileUtils.createDirectoriesIfMissing(dir) == false)
        {
            String key = "litematica.message.error.schematic_placement.failed_to_create_directory";
            MessageDispatcher.error().translate(key, dir.toAbsolutePath().toString());
        }

        return dir;
    }

    @Nullable
    protected Path getAvailableFileName()
    {
        if (this.getSchematicFile() == null)
        {
            return null;
        }

        Path dir = getSaveDirectory();
        String schName = FileNameUtils.getFileNameWithoutExtension(this.getSchematicFile().getFileName().toString());
        String nameBase = FileNameUtils.generateSafeFileName(schName);
        int id = 1;
        String name = String.format("%s_%03d.json", nameBase, id);
        Path file = dir.resolve(name);

        while (Files.exists(file))
        {
            ++id;
            name = String.format("%s_%03d.json", nameBase, id);
            file = dir.resolve(name);
        }

        return file;
    }

    protected static int getNextBoxColor()
    {
        int color = Color4f.getColorFromHue(lastColor);
        lastColor += 40;
        return color;
    }

    @Nullable
    public static SchematicPlacement createFromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "schematic"))
        {
            Path schematicFile = Paths.get(JsonUtils.getString(obj, "schematic"));
            SchematicPlacement placement = new SchematicPlacement(schematicFile);

            if (placement.readFromJson(obj) == false)
            {
                return null;
            }

            if (JsonUtils.hasInteger(obj, "bb_color"))
            {
                placement.setBoundingBoxColor(JsonUtils.getInteger(obj, "bb_color"));
            }
            else
            {
                placement.setBoundingBoxColorToNext();
            }

            placement.loadSchematicFromFileIfEnabled();

            return placement;
        }

        return null;
    }

    @Nullable
    public static SchematicPlacement createFromFile(Path file)
    {
        JsonElement el = JsonUtils.parseJsonFile(file);

        if (el != null && el.isJsonObject())
        {
            SchematicPlacement placement = createFromJson(el.getAsJsonObject());

            if (placement != null)
            {
                placement.placementSaveFile = file.getFileName().toString();
            }

            return placement;
        }

        return null;
    }

    public static SchematicPlacement create(ISchematic schematic, BlockPos origin, String name, boolean enabled)
    {
        return create(schematic, origin, name, enabled, true);
    }

    public static SchematicPlacement create(ISchematic schematic, BlockPos origin, String name, boolean enabled,
                                            boolean offsetToInFrontOfPlayer)
    {
        SchematicPlacement placement = new SchematicPlacement(schematic, schematic.getFile(), origin, name, enabled);

        placement.setBoundingBoxColorToNext();
        placement.resetAllSubRegionsToSchematicValues();

        if (offsetToInFrontOfPlayer)
        {
            placement.position = PositionUtils.getPlacementPositionOffsetToInFrontOfPlayer(origin, placement);
        }

        return placement;
    }
}
