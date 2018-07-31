package fi.dy.masa.litematica.data;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.data.Placement.RequiredEnabled;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.InfoUtils;
import fi.dy.masa.litematica.util.JsonUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.Vec4f;
import net.minecraft.init.Blocks;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.PlacementSettings;

public class SchematicPlacement
{
    private static final Set<Integer> USED_COLORS = new HashSet<>();
    private static int nextColorIndex;

    private final Map<String, Placement> relativeSubRegionPlacements = new HashMap<>();
    private SchematicVerifier verifier;
    private LitematicaSchematic schematic;
    @Nullable
    private File schematicFile;
    private BlockPos origin;
    private String name;
    private Rotation rotation = Rotation.NONE;
    private Mirror mirror = Mirror.NONE;
    private boolean ignoreEntities;
    private boolean enabled;
    private boolean renderSchematic;
    private boolean regionPlacementsModified;
    private int boxesBBColor;
    private final int subRegionCount;
    private Vec4f boxesBBColorVec = new Vec4f(0xFF, 0xFF, 0xFF);
    @Nullable
    private String selectedSubRegionName;

    private SchematicPlacement(LitematicaSchematic schematic, BlockPos origin, String name)
    {
        this.schematic = schematic;
        this.schematicFile = schematic.getFile();
        this.origin = origin;
        this.name = name;
        this.subRegionCount = schematic.getSubRegionCount();
    }

    public static SchematicPlacement createFor(LitematicaSchematic schematic, BlockPos origin, String name)
    {
        SchematicPlacement placement = new SchematicPlacement(schematic, origin, name);
        placement.setBoxesBBColorNext();
        placement.resetAllSubRegionsToSchematicValues();

        return placement;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean getRenderSchematic()
    {
        return this.isEnabled() && this.renderSchematic;
    }

    public boolean matchesRequirement(RequiredEnabled required)
    {
        if (required == RequiredEnabled.ANY)
        {
            return true;
        }

        if (required == RequiredEnabled.PLACEMENT_ENABLED)
        {
            return this.isEnabled();
        }

        return this.isEnabled() && this.renderSchematic;
    }

    public boolean isRegionPlacementModified()
    {
        return this.regionPlacementsModified;
    }

    public boolean ignoreEntities()
    {
        return this.ignoreEntities;
    }

    public void setIgnoreEntities(boolean ignoreEntities)
    {
        this.ignoreEntities = ignoreEntities;
    }

    public String getName()
    {
        return this.name;
    }

    public LitematicaSchematic getSchematic()
    {
        return schematic;
    }

    @Nullable
    public File getSchematicFile()
    {
        return this.schematicFile;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public SchematicPlacement setBoxesBBColor(int color)
    {
        this.boxesBBColor = color;
        this.boxesBBColorVec = new Vec4f(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f);
        USED_COLORS.add(color);
        return this;
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

    public Vec4f getBoxesBBColor()
    {
        return this.boxesBBColorVec;
    }

    public int getSubRegionCount()
    {
        return this.subRegionCount;
    }

    public boolean hasVerifier()
    {
        return this.verifier != null;
    }

    public SchematicVerifier getSchematicVerifier()
    {
        if (this.verifier == null)
        {
            this.verifier = new SchematicVerifier();
        }

        return this.verifier;
    }

    public PlacementSettings getPlacementSettings()
    {
        PlacementSettings placement = new PlacementSettings();

        placement.setMirror(this.mirror);
        placement.setRotation(this.rotation);
        placement.setIgnoreEntities(this.ignoreEntities);
        placement.setReplacedBlock(Blocks.STRUCTURE_VOID);

        return placement;
    }

    @Nullable
    public String getSelectedSubRegionName()
    {
        return this.selectedSubRegionName;
    }

    public void setSelectedSubRegionName(@Nullable String name)
    {
        this.selectedSubRegionName = name;
    }

    @Nullable
    public Placement getSelectedSubRegionPlacement()
    {
        return this.selectedSubRegionName != null ? this.relativeSubRegionPlacements.get(this.selectedSubRegionName) : null;
    }

    @Nullable
    public Placement getRelativeSubRegionPlacement(String areaName)
    {
        return this.relativeSubRegionPlacements.get(areaName);
    }

    public Collection<Placement> getAllSubRegionsPlacements()
    {
        return this.relativeSubRegionPlacements.values();
    }

    public ImmutableMap<String, Placement> getEnabledRelativeSubRegionPlacements()
    {
        ImmutableMap.Builder<String, Placement> builder = ImmutableMap.builder();

        for (Map.Entry<String, Placement> entry : this.relativeSubRegionPlacements.entrySet())
        {
            Placement placement = entry.getValue();

            if (placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
            {
                builder.put(entry.getKey(), entry.getValue());
            }
        }

        return builder.build();
    }

    /*
    public ImmutableMap<String, Box> getAllSubRegionBoxes()
    {
        return this.getSubRegionBoxes(RequiredEnabled.ANY);
    }
    */

    public ImmutableMap<String, Box> getSubRegionBoxes(RequiredEnabled required)
    {
        ImmutableMap.Builder<String, Box> builder = ImmutableMap.builder();
        Map<String, BlockPos> areaSizes = this.schematic.getAreaSizes();

        for (Map.Entry<String, Placement> entry : this.relativeSubRegionPlacements.entrySet())
        {
            String name = entry.getKey();
            Placement placement = entry.getValue();

            if (placement.matchesRequirement(required))
            {
                BlockPos boxOriginRelative = placement.getPos();
                BlockPos boxOriginAbsolute = PositionUtils.getTransformedBlockPos(boxOriginRelative, this.mirror, this.rotation).add(this.origin);
                BlockPos pos2 = PositionUtils.getRelativeEndPositionFromAreaSize(areaSizes.get(name));
                pos2 = PositionUtils.getTransformedBlockPos(pos2, this.mirror, this.rotation);
                pos2 = PositionUtils.getTransformedBlockPos(pos2, placement.getMirror(), placement.getRotation()).add(boxOriginAbsolute);

                builder.put(name, new Box(boxOriginAbsolute, pos2, name));
            }
        }

        return builder.build();
    }

    public ImmutableMap<String, Box> getSubRegionBoxFor(String regionName, RequiredEnabled required)
    {
        ImmutableMap.Builder<String, Box> builder = ImmutableMap.builder();
        Map<String, BlockPos> areaSizes = this.schematic.getAreaSizes();

        Placement placement = this.relativeSubRegionPlacements.get(regionName);

        if (placement != null)
        {
            if (placement.matchesRequirement(required))
            {
                BlockPos boxOriginRelative = placement.getPos();
                BlockPos boxOriginAbsolute = PositionUtils.getTransformedBlockPos(boxOriginRelative, this.mirror, this.rotation).add(this.origin);
                BlockPos pos2 = PositionUtils.getRelativeEndPositionFromAreaSize(areaSizes.get(regionName));
                pos2 = PositionUtils.getTransformedBlockPos(pos2, this.mirror, this.rotation);
                pos2 = PositionUtils.getTransformedBlockPos(pos2, placement.getMirror(), placement.getRotation()).add(boxOriginAbsolute);

                builder.put(regionName, new Box(boxOriginAbsolute, pos2, regionName));
            }
        }

        return builder.build();
    }

    public Set<String> getRegionsTouchingChunk(int chunkX, int chunkZ)
    {
        ImmutableMap<String, Box> map = this.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
        final int chunkXMin = chunkX << 4;
        final int chunkZMin = chunkZ << 4;
        final int chunkXMax = chunkXMin + 15;
        final int chunkZMax = chunkZMin + 15;
        Set<String> set = new HashSet<>();

        for (Map.Entry<String, Box> entry : map.entrySet())
        {
            Box box = entry.getValue();
            final int boxXMin = Math.min(box.getPos1().getX(), box.getPos2().getX());
            final int boxZMin = Math.min(box.getPos1().getZ(), box.getPos2().getZ());
            final int boxXMax = Math.max(box.getPos1().getX(), box.getPos2().getX());
            final int boxZMax = Math.max(box.getPos1().getZ(), box.getPos2().getZ());

            boolean notOverlapping = boxXMin > chunkXMax || boxZMin > chunkZMax || boxXMax < chunkXMin || boxZMax < chunkZMin;

            if (notOverlapping == false)
            {
                set.add(entry.getKey());
            }
        }

        return set;
    }

    public Map<String, StructureBoundingBox> getBoxesWithinChunk(int chunkX, int chunkZ)
    {
        ImmutableMap<String, Box> map = this.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
        Map<String, StructureBoundingBox> mapOut = new HashMap<>();

        for (Map.Entry<String, Box> entry : map.entrySet())
        {
            Box box = entry.getValue();
            StructureBoundingBox bb = box != null ? PositionUtils.getBoundsWithinChunkForBox(box, chunkX, chunkZ) : null;

            if (bb != null)
            {
                mapOut.put(entry.getKey(), bb);
            }
        }

        return mapOut;
    }

    @Nullable
    public StructureBoundingBox getBoxWithinChunkForRegion(String regionName, int chunkX, int chunkZ)
    {
        Box box = this.getSubRegionBoxFor(regionName, RequiredEnabled.PLACEMENT_ENABLED).get(regionName);
        return box != null ? PositionUtils.getBoundsWithinChunkForBox(box, chunkX, chunkZ) : null;
    }

    public Set<ChunkPos> getTouchedChunks()
    {
        return this.getTouchedChunks(this.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED));
    }

    public Set<ChunkPos> getTouchedChunksForRegion(String regionName)
    {
        return this.getTouchedChunks(this.getSubRegionBoxFor(regionName, RequiredEnabled.PLACEMENT_ENABLED));
    }

    private Set<ChunkPos> getTouchedChunks(ImmutableMap<String, Box> boxes)
    {
        Set<ChunkPos> set = new HashSet<>();

        for (Box box : boxes.values())
        {
            final int boxXMin = Math.min(box.getPos1().getX(), box.getPos2().getX()) >> 4;
            final int boxZMin = Math.min(box.getPos1().getZ(), box.getPos2().getZ()) >> 4;
            final int boxXMax = Math.max(box.getPos1().getX(), box.getPos2().getX()) >> 4;
            final int boxZMax = Math.max(box.getPos1().getZ(), box.getPos2().getZ()) >> 4;

            for (int cz = boxZMin; cz <= boxZMax; ++cz)
            {
                for (int cx = boxXMin; cx <= boxXMax; ++cx)
                {
                    set.add(new ChunkPos(cx, cz));
                }
            }
        }

        return set;
    }

    private void checkAreSubRegionsModified()
    {
        Map<String, BlockPos> areaPositions = this.schematic.getAreaPositions();

        if (areaPositions.size() != this.relativeSubRegionPlacements.size())
        {
            this.regionPlacementsModified = true;
            return;
        }

        for (Map.Entry<String, BlockPos> entry : areaPositions.entrySet())
        {
            Placement placement = this.relativeSubRegionPlacements.get(entry.getKey());

            if (placement == null || placement.isRegionPlacementModified(entry.getValue()))
            {
                this.regionPlacementsModified = true;
                return;
            }
        }

        this.regionPlacementsModified = false;
    }

    /**
     * Moves the sub-region to the given <b>absolute</b> position.
     * @param regionName
     * @param newPos
     */
    public void moveSubRegionTo(String regionName, BlockPos newPos)
    {
        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getInstance().getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            // The input argument position is an absolute position, so need to convert to relative position here
            newPos = newPos.subtract(this.origin);
            // The absolute-based input position needs to be transformed if the entire placement has been rotated or mirrored
            newPos = PositionUtils.getReverseTransformedBlockPos(newPos, this.mirror, this.rotation);

            this.relativeSubRegionPlacements.get(regionName).setPos(newPos);
            this.checkAreSubRegionsModified();
            this.onModified(regionName, manager);
        }
    }

    public void setSubRegionRotation(String regionName, Rotation rotation)
    {
        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getInstance().getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.relativeSubRegionPlacements.get(regionName).setRotation(rotation);
            this.checkAreSubRegionsModified();
            this.onModified(regionName, manager);
        }
    }

    public void setSubRegionMirror(String regionName, Mirror mirror)
    {
        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getInstance().getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.relativeSubRegionPlacements.get(regionName).setMirror(mirror);
            this.checkAreSubRegionsModified();
            this.onModified(regionName, manager);
        }
    }

    public void toggleSubRegionEnabled(String regionName)
    {
        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getInstance().getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.relativeSubRegionPlacements.get(regionName).toggleEnabled();
            this.checkAreSubRegionsModified();
            this.onModified(regionName, manager);
        }
    }

    public void resetAllSubRegionsToSchematicValues()
    {
        // Marks the currently touched chunks before doing the modification
        SchematicPlacementManager manager = DataManager.getInstance().getSchematicPlacementManager();
        manager.onPrePlacementChange(this);

        Map<String, BlockPos> areaPositions = this.schematic.getAreaPositions();
        this.relativeSubRegionPlacements.clear();
        this.regionPlacementsModified = false;

        for (Map.Entry<String, BlockPos> entry : areaPositions.entrySet())
        {
            String name = entry.getKey();
            this.relativeSubRegionPlacements.put(name, new Placement(entry.getValue(), name));
        }

        this.onModified(manager);
    }

    public void resetSubRegionToSchematicValues(String regionName)
    {
        BlockPos pos = this.schematic.getSubRegionPosition(regionName);
        Placement placement = this.relativeSubRegionPlacements.get(regionName);

        if (pos != null && placement != null)
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getInstance().getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            placement.resetToOriginalValues();
            this.checkAreSubRegionsModified();
            this.onModified(regionName, manager);
        }
    }

    public void setEnabled(boolean enabled)
    {
        if (enabled != this.enabled)
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getInstance().getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.enabled = enabled;
            this.onModified(manager);
        }
    }

    public void toggleEnabled()
    {
        this.setEnabled(! this.enabled);
    }

    public void setRenderSchematic(boolean render)
    {
        if (render != this.renderSchematic)
        {
            this.renderSchematic = render;
        }
    }

    public void toggleSubRegionRenderingEnabled(String regionName)
    {
        Placement placement = this.relativeSubRegionPlacements.get(regionName);

        if (placement != null)
        {
            placement.toggleRenderingEnabled();
        }
    }

    public SchematicPlacement setOrigin(BlockPos origin)
    {
        if (this.origin.equals(origin) == false)
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getInstance().getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.origin = origin;
            this.onModified(manager);
        }

        return this;
    }

    public SchematicPlacement setRotation(Rotation rotation)
    {
        if (this.rotation != rotation)
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getInstance().getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.rotation = rotation;
            this.onModified(manager);
        }

        return this;
    }

    public SchematicPlacement setMirror(Mirror mirror)
    {
        if (this.mirror != mirror)
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getInstance().getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.mirror = mirror;
            this.onModified(manager);
        }

        return this;
    }

    private SchematicPlacement setBoxesBBColorNext()
    {
        return this.setBoxesBBColor(getNextBoxColor());
    }

    private void onModified(SchematicPlacementManager manager)
    {
        manager.onPostPlacementChange(this);
        OverlayRenderer.getInstance().updatePlacementCache();
    }

    private void onModified(String regionName, SchematicPlacementManager manager)
    {
        manager.onPostPlacementChange(this);
        OverlayRenderer.getInstance().updatePlacementCache();
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
        if (this.schematic.getFile() != null)
        {
            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();

            arr.add(this.origin.getX());
            arr.add(this.origin.getY());
            arr.add(this.origin.getZ());

            obj.add("schematic", new JsonPrimitive(this.schematic.getFile().getAbsolutePath()));
            obj.add("name", new JsonPrimitive(this.name));
            obj.add("origin", arr);
            obj.add("rotation", new JsonPrimitive(this.rotation.name()));
            obj.add("mirror", new JsonPrimitive(this.mirror.name()));
            obj.add("ignore_entities", new JsonPrimitive(this.ignoreEntities));
            obj.add("enabled", new JsonPrimitive(this.enabled));
            obj.add("bb_color", new JsonPrimitive(this.boxesBBColor));
            obj.add("render_schematic", new JsonPrimitive(this.renderSchematic));

            if (this.selectedSubRegionName != null)
            {
                obj.add("selected_region", new JsonPrimitive(this.selectedSubRegionName));
            }

            if (this.relativeSubRegionPlacements.isEmpty() == false)
            {
                arr = new JsonArray();

                for (Map.Entry<String, Placement> entry : this.relativeSubRegionPlacements.entrySet())
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
    public static SchematicPlacement fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "schematic") &&
            JsonUtils.hasString(obj, "name") &&
            JsonUtils.hasArray(obj, "origin") &&
            JsonUtils.hasString(obj, "rotation") &&
            JsonUtils.hasString(obj, "mirror") &&
            JsonUtils.hasArray(obj, "placements"))
        {
            File file = new File(obj.get("schematic").getAsString());
            LitematicaSchematic schematic = SchematicHolder.getInstance().getOrLoad(file, InfoUtils.INFO_MESSAGE_CONSUMER);

            if (schematic == null)
            {
                LiteModLitematica.logger.warn("Failed to load schematic '{}'", file.getAbsolutePath());
                return null;
            }

            JsonArray posArr = obj.get("origin").getAsJsonArray();

            if (posArr.size() != 3)
            {
                LiteModLitematica.logger.warn("Failed to load schematic placement for '{}', invalid origin position", file.getAbsolutePath());
                return null;
            }

            String name = obj.get("name").getAsString();
            BlockPos pos = new BlockPos(posArr.get(0).getAsInt(), posArr.get(1).getAsInt(), posArr.get(2).getAsInt());
            Rotation rotation = Rotation.valueOf(obj.get("rotation").getAsString());
            Mirror mirror = Mirror.valueOf(obj.get("mirror").getAsString());
            SchematicPlacement schematicPlacement = new SchematicPlacement(schematic, pos, name);
            schematicPlacement.rotation = rotation;
            schematicPlacement.mirror = mirror;
            schematicPlacement.ignoreEntities = JsonUtils.getBoolean(obj, "ignore_entities");
            schematicPlacement.enabled = JsonUtils.getBoolean(obj, "enabled");
            schematicPlacement.renderSchematic = JsonUtils.getBoolean(obj, "render_schematic");

            if (JsonUtils.hasInteger(obj, "bb_color"))
            {
                schematicPlacement.setBoxesBBColor(JsonUtils.getInteger(obj, "bb_color"));
            }
            else
            {
                schematicPlacement.setBoxesBBColorNext();
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
                        Placement placement = Placement.fromJson(placementObj.get("placement").getAsJsonObject());

                        if (placement != null)
                        {
                            String placementName = placementObj.get("name").getAsString();
                            schematicPlacement.relativeSubRegionPlacements.put(placementName, placement);
                        }
                    }
                }
            }

            schematicPlacement.checkAreSubRegionsModified();

            return schematicPlacement;
        }

        return null;
    }

    private static int getNextBoxColor()
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
