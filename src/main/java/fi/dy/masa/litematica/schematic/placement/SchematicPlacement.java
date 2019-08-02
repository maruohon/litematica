package fi.dy.masa.litematica.schematic.placement;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.interfaces.IMessageConsumer;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.PositionUtils.CoordinateType;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class SchematicPlacement
{
    private static final Set<Integer> USED_COLORS = new HashSet<>();
    private static int nextColorIndex;

    private final Map<String, SubRegionPlacement> relativeSubRegionPlacements = new HashMap<>();
    private final int subRegionCount;
    private SchematicVerifier verifier;
    private LitematicaSchematic schematic;
    private BlockPos origin;
    private String name;
    private BlockRotation rotation = BlockRotation.NONE;
    private BlockMirror mirror = BlockMirror.NONE;
    private BlockInfoListType verifierType = BlockInfoListType.ALL;
    private boolean ignoreEntities;
    private boolean enabled;
    private boolean enableRender;
    private boolean renderEnclosingBox;
    private boolean regionPlacementsModified;
    private boolean locked;
    private boolean shouldBeSaved = true;
    private int coordinateLockMask;
    private int boxesBBColor;
    private Color4f boxesBBColorVec = new Color4f(0xFF, 0xFF, 0xFF);
    @Nullable
    private Box enclosingBox;
    @Nullable
    private File schematicFile;
    @Nullable
    private String selectedSubRegionName;
    @Nullable
    private MaterialListBase materialList;

    private SchematicPlacement(LitematicaSchematic schematic, BlockPos origin, String name, boolean enabled, boolean enableRender)
    {
        this.schematic = schematic;
        this.schematicFile = schematic.getFile();
        this.origin = origin;
        this.name = name;
        this.subRegionCount = schematic.getSubRegionCount();
        this.enabled = enabled;
        this.enableRender = enableRender;
    }

    public static SchematicPlacement createFor(LitematicaSchematic schematic, BlockPos origin, String name, boolean enabled, boolean enableRender)
    {
        SchematicPlacement placement = new SchematicPlacement(schematic, origin, name, enabled, enableRender);
        placement.setBoxesBBColorNext();
        placement.resetAllSubRegionsToSchematicValues(InfoUtils.INFO_MESSAGE_CONSUMER);

        return placement;
    }

    /**
     * Creates a placement that can be used for schematic conversions.
     * The origin point will be adjusted such that the actual minimum corner of the sub regions
     * will be at the provided origin point.
     * Also, this placement will not affect the SchematicPlacementManager and cause
     * schematic chunk rebuilds, nor will it affect the rendering related things.
     * @param schematic
     * @param origin
     * @return
     */
    public static SchematicPlacement createForSchematicConversion(LitematicaSchematic schematic, BlockPos origin)
    {
        // Adjust the origin such that the actual sub regions minimum corner is at the provided origin,
        // regardless of where the defined origin point is in relation to the minimum corner.
        Pair<BlockPos, BlockPos> pair = PositionUtils.getEnclosingAreaCorners(schematic.getAreas().values());
        BlockPos originAdjusted = pair != null ? origin.subtract(pair.getLeft()) : origin;

        return createTemporary(schematic, originAdjusted);
    }

    /**
     * Creates a temporary placement which doesn't affect the SchematicPlacementManager
     * @param schematic
     * @param origin
     * @return
     */
    public static SchematicPlacement createTemporary(LitematicaSchematic schematic, BlockPos origin)
    {
        SchematicPlacement placement = new SchematicPlacement(schematic, origin, "?", true, true);
        placement.resetAllSubRegionsToSchematicValues(InfoUtils.INFO_MESSAGE_CONSUMER, false);

        return placement;
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

    public void toggleIgnoreEntities(IMessageConsumer feedback)
    {
        // Marks the currently touched chunks before doing the modification
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        manager.onPrePlacementChange(this);

        this.ignoreEntities = ! this.ignoreEntities;
        this.onModified(manager);
    }

    public void toggleRenderEnclosingBox()
    {
        this.renderEnclosingBox = ! this.renderEnclosingBox;

        if (this.shouldRenderEnclosingBox())
        {
            this.updateEnclosingBox();
        }
    }

    public void toggleLocked()
    {
        this.locked = ! this.locked;
    }

    public void setCoordinateLocked(CoordinateType coord, boolean locked)
    {
        int mask = 0x1 << coord.ordinal();

        if (locked)
        {
            this.coordinateLockMask |= mask;
        }
        else
        {
            this.coordinateLockMask &= ~mask;
        }
    }

    public boolean isCoordinateLocked(CoordinateType coord)
    {
        int mask = 0x1 << coord.ordinal();
        return (this.coordinateLockMask & mask) != 0;
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

    @Nullable
    public Box getEclosingBox()
    {
        return this.enclosingBox;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public SchematicPlacement setBoxesBBColor(int color)
    {
        this.boxesBBColor = color;
        this.boxesBBColorVec = Color4f.fromColor(color, 1f);
        USED_COLORS.add(color);
        return this;
    }

    public BlockPos getOrigin()
    {
        return origin;
    }

    public BlockRotation getRotation()
    {
        return rotation;
    }

    public BlockMirror getMirror()
    {
        return mirror;
    }

    public Color4f getBoxesBBColor()
    {
        return this.boxesBBColorVec;
    }

    public int getSubRegionCount()
    {
        return this.subRegionCount;
    }

    public BlockInfoListType getSchematicVerifierType()
    {
        return this.verifierType;
    }

    public void setSchematicVerifierType(BlockInfoListType type)
    {
        this.verifierType = type;
    }

    public MaterialListBase getMaterialList()
    {
        if (this.materialList == null)
        {
            this.materialList = new MaterialListPlacement(this, true);
        }

        return this.materialList;
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

    public StructurePlacementData getPlacementSettings()
    {
        StructurePlacementData placement = new StructurePlacementData();

        placement.setMirrored(this.mirror);
        placement.setRotation(this.rotation);
        placement.setIgnoreEntities(this.ignoreEntities);

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
    public SubRegionPlacement getSelectedSubRegionPlacement()
    {
        return this.selectedSubRegionName != null ? this.relativeSubRegionPlacements.get(this.selectedSubRegionName) : null;
    }

    @Nullable
    public SubRegionPlacement getRelativeSubRegionPlacement(String areaName)
    {
        return this.relativeSubRegionPlacements.get(areaName);
    }

    public Collection<SubRegionPlacement> getAllSubRegionsPlacements()
    {
        return this.relativeSubRegionPlacements.values();
    }

    public ImmutableMap<String, SubRegionPlacement> getEnabledRelativeSubRegionPlacements()
    {
        ImmutableMap.Builder<String, SubRegionPlacement> builder = ImmutableMap.builder();

        for (Map.Entry<String, SubRegionPlacement> entry : this.relativeSubRegionPlacements.entrySet())
        {
            SubRegionPlacement placement = entry.getValue();

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

    private void updateEnclosingBox()
    {
        if (this.shouldRenderEnclosingBox())
        {
            ImmutableMap<String, Box> boxes = this.getSubRegionBoxes(RequiredEnabled.ANY);
            BlockPos pos1 = null;
            BlockPos pos2 = null;

            for (Box box : boxes.values())
            {
                BlockPos tmp;
                tmp = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());

                if (pos1 == null)
                {
                    pos1 = tmp;
                }
                else if (tmp.getX() < pos1.getX() || tmp.getY() < pos1.getY() || tmp.getZ() < pos1.getZ())
                {
                    pos1 = PositionUtils.getMinCorner(tmp, pos1);
                }

                tmp = PositionUtils.getMaxCorner(box.getPos1(), box.getPos2());

                if (pos2 == null)
                {
                    pos2 = tmp;
                }
                else if (tmp.getX() > pos2.getX() || tmp.getY() > pos2.getY() || tmp.getZ() > pos2.getZ())
                {
                    pos2 = PositionUtils.getMaxCorner(tmp, pos2);
                }
            }

            if (pos1 != null && pos2 != null)
            {
                this.enclosingBox = new Box(pos1, pos2, "Enclosing Box");
            }
        }
    }

    public ImmutableMap<String, Box> getSubRegionBoxes(RequiredEnabled required)
    {
        ImmutableMap.Builder<String, Box> builder = ImmutableMap.builder();
        Map<String, BlockPos> areaSizes = this.schematic.getAreaSizes();

        for (Map.Entry<String, SubRegionPlacement> entry : this.relativeSubRegionPlacements.entrySet())
        {
            String name = entry.getKey();
            BlockPos areaSize = areaSizes.get(name);

            if (areaSize == null)
            {
                Litematica.logger.warn("SchematicPlacement.getSubRegionBoxes(): Size for sub-region '{}' not found in the schematic '{}'", name, this.schematic.getMetadata().getName());
                continue;
            }

            SubRegionPlacement placement = entry.getValue();

            if (placement.matchesRequirement(required))
            {
                BlockPos boxOriginRelative = placement.getPos();
                BlockPos boxOriginAbsolute = PositionUtils.getTransformedBlockPos(boxOriginRelative, this.mirror, this.rotation).add(this.origin);
                BlockPos pos2 = PositionUtils.getRelativeEndPositionFromAreaSize(areaSize);
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

        SubRegionPlacement placement = this.relativeSubRegionPlacements.get(regionName);

        if (placement != null)
        {
            if (placement.matchesRequirement(required))
            {
                BlockPos areaSize = areaSizes.get(regionName);

                if (areaSize != null)
                {
                    BlockPos boxOriginRelative = placement.getPos();
                    BlockPos boxOriginAbsolute = PositionUtils.getTransformedBlockPos(boxOriginRelative, this.mirror, this.rotation).add(this.origin);
                    BlockPos pos2 = PositionUtils.getRelativeEndPositionFromAreaSize(areaSize);
                    pos2 = PositionUtils.getTransformedBlockPos(pos2, this.mirror, this.rotation);
                    pos2 = PositionUtils.getTransformedBlockPos(pos2, placement.getMirror(), placement.getRotation()).add(boxOriginAbsolute);

                    builder.put(regionName, new Box(boxOriginAbsolute, pos2, regionName));
                }
                else
                {
                    Litematica.logger.warn("SchematicPlacement.getSubRegionBoxFor(): Size for sub-region '{}' not found in the schematic '{}'", regionName, this.schematic.getMetadata().getName());
                }
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

    public ImmutableMap<String, IntBoundingBox> getBoxesWithinChunk(int chunkX, int chunkZ)
    {
        ImmutableMap<String, Box> subRegions = this.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
        return PositionUtils.getBoxesWithinChunk(chunkX, chunkZ, subRegions);
    }

    @Nullable
    public IntBoundingBox getBoxWithinChunkForRegion(String regionName, int chunkX, int chunkZ)
    {
        Box box = this.getSubRegionBoxFor(regionName, RequiredEnabled.PLACEMENT_ENABLED).get(regionName);
        return box != null ? PositionUtils.getBoundsWithinChunkForBox(box, chunkX, chunkZ) : null;
    }

    public Set<ChunkPos> getTouchedChunks()
    {
        return PositionUtils.getTouchedChunks(this.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED));
    }

    public Set<ChunkPos> getTouchedChunksForRegion(String regionName)
    {
        return PositionUtils.getTouchedChunks(this.getSubRegionBoxFor(regionName, RequiredEnabled.PLACEMENT_ENABLED));
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
            SubRegionPlacement placement = this.relativeSubRegionPlacements.get(entry.getKey());

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
    public void moveSubRegionTo(String regionName, BlockPos newPos, IStringConsumer feedback)
    {
        if (this.isLocked())
        {
            feedback.setString("litematica.message.placement.cant_modify_is_locked");
            return;
        }

        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            // The input argument position is an absolute position, so need to convert to relative position here
            newPos = newPos.subtract(this.origin);
            // The absolute-based input position needs to be transformed if the entire placement has been rotated or mirrored
            newPos = PositionUtils.getReverseTransformedBlockPos(newPos, this.mirror, this.rotation);

            this.relativeSubRegionPlacements.get(regionName).setPos(newPos);
            this.onModified(regionName, manager);
        }
    }

    public void setSubRegionRotation(String regionName, BlockRotation rotation, IMessageConsumer feedback)
    {
        if (this.isLocked())
        {
            feedback.addMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
            return;
        }

        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.relativeSubRegionPlacements.get(regionName).setRotation(rotation);
            this.onModified(regionName, manager);
        }
    }

    public void setSubRegionMirror(String regionName, BlockMirror mirror, IMessageConsumer feedback)
    {
        if (this.isLocked())
        {
            feedback.addMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
            return;
        }

        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.relativeSubRegionPlacements.get(regionName).setMirror(mirror);
            this.onModified(regionName, manager);
        }
    }

    public void setSubRegionsEnabledState(boolean state, Collection<SubRegionPlacement> subRegions, IMessageConsumer feedback)
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        // Marks the currently touched chunks before doing the modification
        manager.onPrePlacementChange(this);

        for (SubRegionPlacement placement : subRegions)
        {
            // Check that the sub-region is actually from this placement
            placement = this.relativeSubRegionPlacements.get(placement.getName());

            if (placement != null && placement.isEnabled() != state)
            {
                placement.setEnabled(state);
            }
        }

        this.checkAreSubRegionsModified();
        this.onModified(manager);
    }

    public void toggleSubRegionEnabled(String regionName, IMessageConsumer feedback)
    {
        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.relativeSubRegionPlacements.get(regionName).toggleEnabled();
            this.onModified(regionName, manager);
        }
    }

    public void toggleSubRegionIgnoreEntities(String regionName, IMessageConsumer feedback)
    {
        if (this.relativeSubRegionPlacements.containsKey(regionName))
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.relativeSubRegionPlacements.get(regionName).toggleIgnoreEntities();
            this.onModified(regionName, manager);
        }
    }

    public void resetAllSubRegionsToSchematicValues(IStringConsumer feedback)
    {
        this.resetAllSubRegionsToSchematicValues(feedback, true);
    }

    public void resetAllSubRegionsToSchematicValues(IStringConsumer feedback, boolean updatePlacementManager)
    {
        if (this.isLocked())
        {
            feedback.setString("litematica.message.placement.cant_modify_is_locked");
            return;
        }

        if (updatePlacementManager)
        {
            // Marks the currently touched chunks before doing the modification
            DataManager.getSchematicPlacementManager().onPrePlacementChange(this);
        }

        Map<String, BlockPos> areaPositions = this.schematic.getAreaPositions();
        this.relativeSubRegionPlacements.clear();
        this.regionPlacementsModified = false;

        for (Map.Entry<String, BlockPos> entry : areaPositions.entrySet())
        {
            String name = entry.getKey();
            this.relativeSubRegionPlacements.put(name, new SubRegionPlacement(entry.getValue(), name));
        }

        if (updatePlacementManager)
        {
            this.onModified(DataManager.getSchematicPlacementManager());
        }
    }

    public void resetSubRegionToSchematicValues(String regionName, IMessageConsumer feedback)
    {
        if (this.isLocked())
        {
            feedback.addMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
            return;
        }

        BlockPos pos = this.schematic.getSubRegionPosition(regionName);
        SubRegionPlacement placement = this.relativeSubRegionPlacements.get(regionName);

        if (pos != null && placement != null)
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            placement.resetToOriginalValues();
            this.onModified(regionName, manager);
        }
    }

    public void setEnabled(boolean enabled)
    {
        if (enabled != this.enabled)
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
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
        if (render != this.enableRender)
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.enableRender = render;
            this.onModified(manager);
        }
    }

    public void toggleSubRegionRenderingEnabled(String regionName)
    {
        SubRegionPlacement placement = this.relativeSubRegionPlacements.get(regionName);

        if (placement != null)
        {
            placement.toggleRenderingEnabled();
        }
    }

    public SchematicPlacement setOrigin(BlockPos origin, IStringConsumer feedback)
    {
        if (this.isLocked())
        {
            feedback.setString("litematica.message.placement.cant_modify_is_locked");
            return this;
        }

        origin = PositionUtils.getModifiedPartiallyLockedPosition(this.origin, origin, this.coordinateLockMask);

        if (this.origin.equals(origin) == false)
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.origin = origin;
            this.onModified(manager);
        }

        return this;
    }

    public SchematicPlacement setRotation(BlockRotation rotation, IMessageConsumer feedback)
    {
        if (this.isLocked())
        {
            feedback.addMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
            return this;
        }

        if (this.rotation != rotation)
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            manager.onPrePlacementChange(this);

            this.rotation = rotation;
            this.onModified(manager);
        }

        return this;
    }

    public SchematicPlacement setMirror(BlockMirror mirror, IMessageConsumer feedback)
    {
        if (this.isLocked())
        {
            feedback.addMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
            return this;
        }

        if (this.mirror != mirror)
        {
            // Marks the currently touched chunks before doing the modification
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
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
        this.updateEnclosingBox();
        manager.onPostPlacementChange(this);
        OverlayRenderer.getInstance().updatePlacementCache();
    }

    private void onModified(String regionName, SchematicPlacementManager manager)
    {
        this.checkAreSubRegionsModified();
        this.updateEnclosingBox();
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

            if (this.materialList != null)
            {
                obj.add("material_list", this.materialList.toJson());
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
            LitematicaSchematic schematic = SchematicHolder.getInstance().getOrLoad(file);

            if (schematic == null)
            {
                Litematica.logger.warn("Failed to load schematic '{}'", file.getAbsolutePath());
                return null;
            }

            JsonArray posArr = obj.get("origin").getAsJsonArray();

            if (posArr.size() != 3)
            {
                Litematica.logger.warn("Failed to load schematic placement for '{}', invalid origin position", file.getAbsolutePath());
                return null;
            }

            String name = obj.get("name").getAsString();
            BlockPos pos = new BlockPos(posArr.get(0).getAsInt(), posArr.get(1).getAsInt(), posArr.get(2).getAsInt());
            BlockRotation rotation = BlockRotation.valueOf(obj.get("rotation").getAsString());
            BlockMirror mirror = BlockMirror.valueOf(obj.get("mirror").getAsString());
            boolean enabled = JsonUtils.getBoolean(obj, "enabled");
            boolean enableRender = JsonUtils.getBoolean(obj, "enable_render");

            SchematicPlacement schematicPlacement = new SchematicPlacement(schematic, pos, name, enabled, enableRender);
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
                schematicPlacement.materialList = new MaterialListPlacement(schematicPlacement);
                schematicPlacement.materialList.fromJson(JsonUtils.getNestedObject(obj, "material_list", false));
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

            schematicPlacement.checkAreSubRegionsModified();
            schematicPlacement.updateEnclosingBox();

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
