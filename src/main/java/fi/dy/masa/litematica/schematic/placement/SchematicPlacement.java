package fi.dy.masa.litematica.schematic.placement;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.ISchematicRegion;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionBox;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.position.Coordinate;
import fi.dy.masa.malilib.util.position.IntBoundingBox;

public class SchematicPlacement extends SchematicPlacementUnloaded
{
    protected final ISchematic schematic;
    protected final int subRegionCount;
    protected boolean isRepeatedPlacement;

    @Nullable protected SchematicVerifier verifier;
    @Nullable protected MaterialListBase materialList;

    protected SchematicPlacement(ISchematic schematic, @Nullable String storageFile, @Nullable File schematicFile, BlockPos origin, String name, boolean enabled)
    {
        super(storageFile, schematicFile, origin, name, enabled);

        this.schematic = schematic;
        this.subRegionCount = schematic.getSubRegionCount();
    }

    public SchematicPlacement copyAsFullyLoaded(boolean isRepeatedPlacement)
    {
        SchematicPlacement copy = new SchematicPlacement(this.schematic, null, this.schematicFile, this.origin, this.name, this.enabled);
        copy.copyFrom(this, ! isRepeatedPlacement);
        copy.isRepeatedPlacement = isRepeatedPlacement;
        return copy;
    }

    @Override
    public boolean isLoaded()
    {
        return true;
    }

    @Override
    public void invalidate()
    {
        super.invalidate();

        if (this.hasVerifier())
        {
            this.getSchematicVerifier().reset();
        }
    }

    public boolean isRepeatedPlacement()
    {
        return this.isRepeatedPlacement;
    }

    public ISchematic getSchematic()
    {
        return this.schematic;
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

    public Box getEnclosingBox()
    {
        if (this.enclosingBox == null)
        {
            this.updateEnclosingBox();
        }

        return this.enclosingBox;
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

    public List<SubRegionPlacement> getAllSubRegionsPlacements()
    {
        return new ArrayList<>(this.relativeSubRegionPlacements.values());
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

    protected void updateEnclosingBox()
    {
        ImmutableMap<String, SelectionBox> boxes = this.getSubRegionBoxes(RequiredEnabled.ANY);
        BlockPos pos1 = null;
        BlockPos pos2 = null;

        for (SelectionBox box : boxes.values())
        {
            BlockPos tmp;
            tmp = fi.dy.masa.malilib.util.PositionUtils.getMinCorner(box.getPos1(), box.getPos2());

            if (pos1 == null)
            {
                pos1 = tmp;
            }
            else if (tmp.getX() < pos1.getX() || tmp.getY() < pos1.getY() || tmp.getZ() < pos1.getZ())
            {
                pos1 = fi.dy.masa.malilib.util.PositionUtils.getMinCorner(tmp, pos1);
            }

            tmp = fi.dy.masa.malilib.util.PositionUtils.getMaxCorner(box.getPos1(), box.getPos2());

            if (pos2 == null)
            {
                pos2 = tmp;
            }
            else if (tmp.getX() > pos2.getX() || tmp.getY() > pos2.getY() || tmp.getZ() > pos2.getZ())
            {
                pos2 = fi.dy.masa.malilib.util.PositionUtils.getMaxCorner(tmp, pos2);
            }
        }

        if (pos1 != null && pos2 != null)
        {
            this.enclosingBox = new Box(pos1, pos2);
            this.gridSettings.setDefaultSize(PositionUtils.getAreaSizeFromRelativeEndPosition(pos2.subtract(pos1)));
        }
    }

    public ImmutableMap<String, SelectionBox> getSubRegionBoxes(RequiredEnabled required)
    {
        ImmutableMap.Builder<String, SelectionBox> builder = ImmutableMap.builder();
        Map<String, ISchematicRegion> subRegions = this.schematic.getRegions();

        for (Map.Entry<String, SubRegionPlacement> entry : this.relativeSubRegionPlacements.entrySet())
        {
            SubRegionPlacement placement = entry.getValue();

            if (placement.matchesRequirement(required))
            {
                String name = entry.getKey();
                ISchematicRegion region = subRegions.get(name);

                if (region != null)
                {
                    BlockPos boxOriginRelative = placement.getPos();
                    BlockPos boxOriginAbsolute = PositionUtils.getTransformedBlockPos(boxOriginRelative, this.mirror, this.rotation).add(this.origin);
                    BlockPos pos2 = new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(region.getSize()));
                    pos2 = PositionUtils.getTransformedBlockPos(pos2, this.mirror, this.rotation);
                    pos2 = PositionUtils.getTransformedBlockPos(pos2, placement.getMirror(), placement.getRotation()).add(boxOriginAbsolute);

                    builder.put(name, new SelectionBox(boxOriginAbsolute, pos2, name));
                }
                else
                {
                    Litematica.logger.warn("SchematicPlacement.getSubRegionBoxes(): Sub-region '{}' not found in the schematic '{}'", name, this.schematic.getMetadata().getName());
                }
            }
        }

        return builder.build();
    }

    public ImmutableMap<String, SelectionBox> getSubRegionBoxFor(String regionName, RequiredEnabled required)
    {
        ImmutableMap.Builder<String, SelectionBox> builder = ImmutableMap.builder();
        SubRegionPlacement placement = this.relativeSubRegionPlacements.get(regionName);

        if (placement != null && placement.matchesRequirement(required))
        {
            Map<String, ISchematicRegion> subRegions = this.schematic.getRegions();
            ISchematicRegion region = subRegions.get(regionName);

            if (region != null)
            {
                BlockPos boxOriginRelative = placement.getPos();
                BlockPos boxOriginAbsolute = PositionUtils.getTransformedBlockPos(boxOriginRelative, this.mirror, this.rotation).add(this.origin);
                BlockPos pos2 = new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(region.getSize()));
                pos2 = PositionUtils.getTransformedBlockPos(pos2, this.mirror, this.rotation);
                pos2 = PositionUtils.getTransformedBlockPos(pos2, placement.getMirror(), placement.getRotation()).add(boxOriginAbsolute);

                builder.put(regionName, new SelectionBox(boxOriginAbsolute, pos2, regionName));
            }
            else
            {
                Litematica.logger.warn("SchematicPlacement.getSubRegionBoxFor(): Sub-region '{}' not found in the schematic '{}'", regionName, this.schematic.getMetadata().getName());
            }
        }

        return builder.build();
    }

    public Set<String> getRegionsTouchingChunk(int chunkX, int chunkZ)
    {
        ImmutableMap<String, SelectionBox> map = this.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
        final int chunkXMin = chunkX << 4;
        final int chunkZMin = chunkZ << 4;
        final int chunkXMax = chunkXMin + 15;
        final int chunkZMax = chunkZMin + 15;
        Set<String> set = new HashSet<>();

        for (Map.Entry<String, SelectionBox> entry : map.entrySet())
        {
            SelectionBox box = entry.getValue();
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
        ImmutableMap<String, SelectionBox> subRegions = this.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
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

    protected void checkAreSubRegionsModified()
    {
        Map<String, ISchematicRegion> subRegions = this.schematic.getRegions();

        if (subRegions.size() != this.relativeSubRegionPlacements.size())
        {
            this.regionPlacementsModified = true;
            return;
        }

        for (Map.Entry<String, ISchematicRegion> entry : subRegions.entrySet())
        {
            SubRegionPlacement placement = this.relativeSubRegionPlacements.get(entry.getKey());

            if (placement == null || placement.isRegionPlacementModified(entry.getValue().getPosition()))
            {
                this.regionPlacementsModified = true;
                return;
            }
        }

        this.regionPlacementsModified = false;
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

    void toggleEnabled()
    {
        this.enabled = ! this.enabled;
    }

    void toggleIgnoreEntities()
    {
        this.ignoreEntities = ! this.ignoreEntities;
    }

    void setOrigin(BlockPos origin)
    {
        this.origin = origin;
    }

    void setRotation(Rotation rotation)
    {
        this.rotation = rotation;
    }

    void setMirror(Mirror mirror)
    {
        this.mirror = mirror;
    }

    void toggleSubRegionEnabled(String regionName)
    {
        SubRegionPlacement subRegion = this.relativeSubRegionPlacements.get(regionName);

        if (subRegion != null)
        {
            subRegion.toggleEnabled();
        }
    }

    void toggleSubRegionIgnoreEntities(String regionName)
    {
        SubRegionPlacement subRegion = this.relativeSubRegionPlacements.get(regionName);

        if (subRegion != null)
        {
            subRegion.toggleIgnoreEntities();
        }
    }

    void setSubRegionsEnabledState(boolean state, Collection<SubRegionPlacement> subRegions)
    {
        for (SubRegionPlacement subRegion : subRegions)
        {
            // Check that the sub-region is actually from this placement
            subRegion = this.relativeSubRegionPlacements.get(subRegion.getName());

            if (subRegion != null && subRegion.isEnabled() != state)
            {
                subRegion.setEnabled(state);
            }
        }
    }

    void setSubRegionRotation(String regionName, Rotation rotation)
    {
        SubRegionPlacement subRegion = this.relativeSubRegionPlacements.get(regionName);

        if (subRegion != null)
        {
            subRegion.setRotation(rotation);
        }
    }

    void setSubRegionMirror(String regionName, Mirror mirror)
    {
        SubRegionPlacement subRegion = this.relativeSubRegionPlacements.get(regionName);

        if (subRegion != null)
        {
            subRegion.setMirror(mirror);
        }
    }

    /**
     * Moves the sub-region to the given <b>absolute</b> position.
     * @param regionName
     * @param newPos
     */
    void moveSubRegionTo(String regionName, BlockPos newPos)
    {
        SubRegionPlacement subRegion = this.relativeSubRegionPlacements.get(regionName);

        if (subRegion != null)
        {
            // The input argument position is an absolute position, so need to convert to relative position here
            newPos = newPos.subtract(this.origin);
            // The absolute-based input position needs to be transformed if the entire placement has been rotated or mirrored
            newPos = PositionUtils.getReverseTransformedBlockPos(newPos, this.mirror, this.rotation);

            subRegion.setPos(newPos);
        }
    }

    void resetSubRegionToSchematicValues(String regionName)
    {
        SubRegionPlacement placement = this.relativeSubRegionPlacements.get(regionName);

        if (placement != null)
        {
            placement.resetToOriginalValues();
        }
    }

    void resetAllSubRegionsToSchematicValues()
    {
        Map<String, ISchematicRegion> subRegions = this.schematic.getRegions();
        this.relativeSubRegionPlacements.clear();
        this.regionPlacementsModified = false;

        for (Map.Entry<String, ISchematicRegion> entry : subRegions.entrySet())
        {
            String name = entry.getKey();
            this.relativeSubRegionPlacements.put(name, new SubRegionPlacement(entry.getValue().getPosition(), name));
        }
    }

    @Override
    boolean readBaseSettingsFromJson(JsonObject obj)
    {
        if (super.readBaseSettingsFromJson(obj))
        {
            this.checkAreSubRegionsModified();
            return true;
        }

        return false;
    }

    @Override
    @Nullable
    public JsonObject toJson()
    {
        if (this.schematic != null)
        {
            JsonObject obj = super.toJson();

            if (obj != null)
            {
                if (this.materialList != null)
                {
                    obj.add("material_list", this.materialList.toJson());
                }
            }

            return obj;
        }

        return null;
    }

    public static SchematicPlacement createFor(ISchematic schematic, BlockPos origin, String name, boolean enabled)
    {
        return createFor(schematic, origin, name, enabled, true);
    }

    public static SchematicPlacement createFor(ISchematic schematic, BlockPos origin, String name, boolean enabled, boolean offsetToInfrontOfPlayer)
    {
        SchematicPlacement placement = new SchematicPlacement(schematic, null, schematic.getFile(), origin, name, enabled);

        placement.setBoundingBoxColorToNext();
        placement.resetAllSubRegionsToSchematicValues();

        if (offsetToInfrontOfPlayer)
        {
            placement.origin = PositionUtils.getPlacementPositionOffsetToInfrontOfPlayer(origin, placement);
        }

        return placement;
    }
}
