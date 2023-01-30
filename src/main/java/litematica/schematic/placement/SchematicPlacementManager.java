package litematica.schematic.placement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import com.google.common.collect.ArrayListMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import malilib.config.value.LayerMode;
import malilib.listener.EventListener;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.EnabledCondition;
import malilib.util.data.json.JsonUtils;
import malilib.util.game.RayTraceUtils.RayTraceFluidHandling;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameUtils;
import malilib.util.position.ChunkSectionPos;
import malilib.util.position.IntBoundingBox;
import litematica.config.Configs;
import litematica.config.Hotkeys;
import litematica.data.DataManager;
import litematica.data.SchematicHolder;
import litematica.render.LitematicaRenderer;
import litematica.render.OverlayRenderer;
import litematica.render.infohud.StatusInfoRenderer;
import litematica.schematic.ISchematic;
import litematica.schematic.util.SchematicPlacingUtils;
import litematica.schematic.verifier.SchematicVerifierManager;
import litematica.util.Nags;
import litematica.util.PositionUtils;
import litematica.util.RayTraceUtils;
import litematica.util.RayTraceUtils.RayTraceWrapper;
import litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import litematica.util.ReplaceBehavior;
import litematica.world.WorldSchematic;

public class SchematicPlacementManager
{
    protected final List<SchematicPlacement> schematicPlacements = new ArrayList<>();
    protected final Set<SchematicPlacement> allVisibleSchematicPlacements = new HashSet<>();

    protected final Long2ObjectOpenHashMap<List<SchematicPlacement>> placementsTouchingChunk = new Long2ObjectOpenHashMap<>();
    protected final ArrayListMultimap<ChunkSectionPos, PlacementPart> touchedVolumesInSubChunk = ArrayListMultimap.create();

    protected final LongSet chunksToRebuild = new LongOpenHashSet();
    protected final LongSet chunksToUnload = new LongOpenHashSet();
    protected final LongSet chunksPreChange = new LongOpenHashSet();

    protected final List<EventListener> rebuildListeners = new ArrayList<>();
    protected final GridPlacementManager gridManager;
    protected final Supplier<WorldSchematic> worldSupplier;

    @Nullable protected SchematicPlacement selectedPlacement;
    protected int tickCounter;

    public SchematicPlacementManager(Supplier<WorldSchematic> worldSupplier)
    {
        this.worldSupplier = worldSupplier;
        this.gridManager = new GridPlacementManager(this);
    }

    public void clear()
    {
        this.selectedPlacement = null;
        this.gridManager.clear();
        this.allVisibleSchematicPlacements.clear();
        this.schematicPlacements.clear();
        this.placementsTouchingChunk.clear();
        this.touchedVolumesInSubChunk.clear();
        this.chunksPreChange.clear();
        this.chunksToRebuild.clear();
        this.chunksToUnload.clear();

        SchematicHolder.getInstance().clearLoadedSchematics();
    }

    public boolean hasPendingRebuilds()
    {
        return this.chunksToRebuild.isEmpty() == false;
    }

    public boolean hasPendingRebuildForChunk(int chunkX, int chunkZ)
    {
        return this.chunksToRebuild.contains(ChunkPos.asLong(chunkX, chunkZ));
    }

    public void addRebuildListener(EventListener listener)
    {
        if (this.rebuildListeners.contains(listener) == false)
        {
            this.rebuildListeners.add(listener);
        }
    }

    public boolean processQueuedChunks()
    {
        if ((++this.tickCounter % 40) == 0)
        {
            this.gridManager.createOrRemoveGridPlacementsForLoadedArea();
        }

        if (this.chunksToUnload.isEmpty() == false)
        {
            WorldSchematic schematicWorld = this.worldSupplier.get();

            if (schematicWorld != null)
            {
                for (long chunkPosLong : this.chunksToUnload)
                {
                    this.unloadSchematicChunk(schematicWorld, chunkPosLong);
                }
            }

            this.chunksToUnload.clear();
        }

        //System.out.printf("processQueuedChunks, size: %d\n", this.chunksToRebuild.size());
        if (this.chunksToRebuild.isEmpty() == false)
        {
            WorldClient clientWorld = GameUtils.getClientWorld();

            if (clientWorld == null)
            {
                this.chunksToRebuild.clear();
                return true;
            }

            WorldSchematic schematicWorld = this.worldSupplier.get();
            LongIterator it = this.chunksToRebuild.iterator();

            while (it.hasNext())
            {
                if ((System.nanoTime() - DataManager.getClientTickStartTime()) >= 50000000L)
                {
                    break;
                }

                long chunkPosLong = it.next();

                if (this.placementsTouchingChunk.containsKey(chunkPosLong) == false)
                {
                    it.remove();
                    continue;
                }

                int chunkX = malilib.util.position.PositionUtils.getChunkPosX(chunkPosLong);
                int chunkZ = malilib.util.position.PositionUtils.getChunkPosZ(chunkPosLong);

                if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() ||
                    clientWorld.getChunkProvider().isChunkGeneratedAt(chunkX, chunkZ))
                {
                    // Wipe the old chunk if it exists
                    if (schematicWorld.getChunkProvider().isChunkGeneratedAt(chunkX, chunkZ))
                    {
                        //System.out.printf("wiping chunk at %s\n", pos);
                        this.unloadSchematicChunk(schematicWorld, chunkX, chunkZ);
                    }

                    //System.out.printf("loading chunk at %s\n", pos);
                    schematicWorld.getChunkProvider().loadChunk(chunkX, chunkZ);
                }

                if (schematicWorld.getChunkProvider().isChunkGeneratedAt(chunkX, chunkZ))
                {
                    //System.out.printf("placing at %s\n", pos);
                    List<SchematicPlacement> placements = this.placementsTouchingChunk.get(chunkPosLong);

                    if (placements != null)
                    {
                        for (SchematicPlacement placement : placements)
                        {
                            if (placement.isEnabled() && placement.isSchematicLoaded())
                            {
                                SchematicPlacingUtils.placeToWorldWithinChunk(placement, new ChunkPos(chunkX, chunkZ),
                                                                              schematicWorld, ReplaceBehavior.ALL, false);
                            }
                        }

                        schematicWorld.markBlockRangeForRenderUpdate( chunkX << 4      ,   0,  chunkZ << 4,
                                                                     (chunkX << 4) + 15, 256, (chunkZ << 4) + 15);
                    }

                    it.remove();
                }
            }

            LitematicaRenderer.getInstance().getWorldRenderer().markNeedsUpdate();

            return this.chunksToRebuild.isEmpty();
        }

        return false;
    }

    public void onClientChunkUnload(int chunkX, int chunkZ)
    {
        if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() == false)
        {
            WorldSchematic schematicWorld = this.worldSupplier.get();

            if (schematicWorld != null)
            {
                this.unloadSchematicChunk(schematicWorld, chunkX, chunkZ);
                this.chunksToRebuild.add(ChunkPos.asLong(chunkX, chunkZ));
            }
        }
    }

    protected void unloadSchematicChunk(WorldSchematic worldSchematic, long chunkPosLong)
    {
        int chunkX = malilib.util.position.PositionUtils.getChunkPosX(chunkPosLong);
        int chunkZ = malilib.util.position.PositionUtils.getChunkPosZ(chunkPosLong);
        this.unloadSchematicChunk(worldSchematic, chunkX, chunkZ);
    }

    protected void unloadSchematicChunk(WorldSchematic worldSchematic, int chunkX, int chunkZ)
    {
        if (worldSchematic.getChunkProvider().isChunkGeneratedAt(chunkX, chunkZ))
        {
            //System.out.printf("unloading chunk at %d, %d\n", chunkX, chunkZ);
            worldSchematic.markBlockRangeForRenderUpdate((chunkX << 4) - 1 ,   0, (chunkZ << 4) - 1,
                                                         (chunkX << 4) + 16, 256, (chunkZ << 4) + 16);
            worldSchematic.getChunkProvider().unloadChunk(chunkX, chunkZ);
        }
    }

    public Set<SchematicPlacement> getVisibleSchematicPlacements()
    {
        return this.allVisibleSchematicPlacements;
    }

    public List<SchematicPlacement> getAllSchematicPlacements()
    {
        return this.schematicPlacements;
    }

    public List<IntBoundingBox> getTouchedBoxesInSubChunk(ChunkSectionPos subChunk)
    {
        List<IntBoundingBox> list = new ArrayList<>();

        for (PlacementPart part : this.touchedVolumesInSubChunk.get(subChunk))
        {
            list.add(part.getBox());
        }

        return list;
    }

    public List<PlacementPart> getAllPlacementsTouchingSubChunk(ChunkSectionPos pos)
    {
        return this.touchedVolumesInSubChunk.get(pos);
    }

    public Set<ChunkSectionPos> getAllTouchedSubChunks()
    {
        return this.touchedVolumesInSubChunk.keySet();
    }

    public void reOrderPlacements(List<SchematicPlacement> newList)
    {
        HashSet<SchematicPlacement> set = new HashSet<>(newList);

        // Check that the contents don't change other than on their order
        if (set.size() == this.schematicPlacements.size() &&
            set.containsAll(this.schematicPlacements))
        {
            this.schematicPlacements.clear();
            this.schematicPlacements.addAll(newList);
        }
    }

    public boolean duplicateSelectedPlacement()
    {
        SchematicPlacement placement = this.getSelectedSchematicPlacement();

        if (placement != null)
        {
            this.duplicateSchematicPlacement(placement);
            return true;
        }

        return false;
    }

    public void duplicateSchematicPlacement(SchematicPlacement placement)
    {
        SchematicPlacement duplicate = placement.copy();
        duplicate.setBoundingBoxColorToNext();
        this.addSchematicPlacement(duplicate, false);
    }

    public void updateDependentPlacements(ISchematic schematic, Path newSchematicFile, boolean selectedOnly)
    {
        if (selectedOnly)
        {
            SchematicPlacement placement = this.getSelectedSchematicPlacement();

            if (placement != null && placement.getSchematic() == schematic)
            {
                placement.setSchematicFile(newSchematicFile);
            }
        }
        else
        {
            for (SchematicPlacement placement : this.getAllPlacementsOfSchematic(schematic))
            {
                placement.setSchematicFile(newSchematicFile);
            }
        }
    }

    void addVisiblePlacement(SchematicPlacement placement)
    {
        this.allVisibleSchematicPlacements.add(placement);
    }

    void removeVisiblePlacement(SchematicPlacement placement)
    {
        this.allVisibleSchematicPlacements.remove(placement);
    }

    public void addSchematicPlacement(SchematicPlacement placement, boolean printMessages)
    {
        this.addSchematicPlacement(placement, printMessages, false);
    }

    protected void addSchematicPlacement(SchematicPlacement placement, boolean printMessages, boolean isLoadFromFile)
    {
        if (this.schematicPlacements.contains(placement))
        {
            if (printMessages)
            {
                MessageDispatcher.error("litematica.error.duplicate_schematic_placement");
            }

            return;
        }

        this.schematicPlacements.add(placement);
        this.addVisiblePlacement(placement);
        this.addTouchedChunksFor(placement);

        if (placement.isEnabled() && placement.getGridSettings().isEnabled())
        {
            this.gridManager.updateGridPlacementsFor(placement);
        }

        // Don't enable the overlay every time when switching dimensions via a portal or re-logging
        // (and thus reading the placements from file).
        if (isLoadFromFile == false)
        {
            StatusInfoRenderer.getInstance().startOverrideDelay();

            if (printMessages)
            {
                MessageDispatcher.success("litematica.message.schematic_placement_created", placement.getName());
                this.printRendererDisabledWarningMessages();
            }
        }
    }

    protected void printRendererDisabledWarningMessages()
    {
        if (Configs.InfoOverlays.WARN_DISABLED_RENDERING.getBooleanValue())
        {
            LayerMode mode = DataManager.getRenderLayerRange().getLayerMode();

            if (mode != LayerMode.ALL)
            {
                MessageDispatcher.warning("litematica.message.warn.layer_mode_currently_at", mode.getDisplayName());
            }

            Nags.HELPER.startNag();
            Nags.HELPER.pushNagIfConfigDisabled(Configs.Visuals.MAIN_RENDERING_TOGGLE,      "litematica.message.warn.main_rendering_disabled");
            Nags.HELPER.pushNagIfConfigDisabled(Configs.Visuals.SCHEMATIC_RENDERING,        "litematica.message.warn.schematic_rendering_disabled");
            Nags.HELPER.pushNagIfConfigDisabled(Configs.Visuals.SCHEMATIC_BLOCKS_RENDERING, "litematica.message.warn.schematic_blocks_rendering_disabled");
            Nags.HELPER.buildNag(MessageDispatcher.warning());
        }
    }

    public boolean removeSelectedSchematicPlacement()
    {
        SchematicPlacement placement = this.getSelectedSchematicPlacement();

        if (placement != null && this.removeSchematicPlacement(placement))
        {
            MessageDispatcher.generic("litematica.message.info.selected_placement_removed", placement.getName());
            return true;
        }

        return false;
    }

    public boolean removeSchematicPlacement(SchematicPlacement placement)
    {
        return this.removeSchematicPlacement(placement, true);
    }

    public boolean removeSchematicPlacement(SchematicPlacement placement, boolean update)
    {
        if (this.selectedPlacement == placement)
        {
            this.selectedPlacement = null;
        }

        if (placement.isValid())
        {
            SchematicVerifierManager.INSTANCE.onPlacementRemoved(placement);
        }

        placement.invalidate();

        boolean removed = this.schematicPlacements.remove(placement);

        this.gridManager.onPlacementRemoved(placement);
        this.removeVisiblePlacement(placement);
        this.removeTouchedChunksFor(placement);

        if (removed && update)
        {
            this.updateOverlayRendererIfEnabled(placement);
        }

        return removed;
    }

    public List<SchematicPlacement> getAllPlacementsOfSchematic(ISchematic schematic)
    {
        List<SchematicPlacement> list = new ArrayList<>();

        for (SchematicPlacement placement : this.allVisibleSchematicPlacements)
        {
            if (placement.getSchematic() == schematic)
            {
                list.add(placement);
            }
        }

        return list;
    }

    public void removeAllPlacementsOfSchematic(ISchematic schematic)
    {
        boolean removed = false;

        for (int i = 0; i < this.schematicPlacements.size(); ++i)
        {
            SchematicPlacement placement = this.schematicPlacements.get(i);

            if (placement.getSchematic() == schematic)
            {
                removed |= this.removeSchematicPlacement(placement, false);
                --i;
            }
        }

        if (removed)
        {
            OverlayRenderer.getInstance().updatePlacementCache();
        }
    }

    @Nullable
    public SchematicPlacement getSelectedSchematicPlacement()
    {
        return this.selectedPlacement;
    }

    public void setSelectedSchematicPlacement(@Nullable SchematicPlacement placement)
    {
        if (placement == null || this.allVisibleSchematicPlacements.contains(placement))
        {
            this.selectedPlacement = placement;
            OverlayRenderer.getInstance().updatePlacementCache();
            // Forget the last viewed material list when changing the placement selection
            DataManager.setMaterialList(null);
        }
    }

    public List<SchematicPlacement> getGridPlacementsForBasePlacement(SchematicPlacement basePlacement)
    {
        return this.gridManager.getGridPlacementsForBasePlacement(basePlacement);
    }

    public void unloadCurrentlySelectedSchematic()
    {
        SchematicPlacement placement = this.getSelectedSchematicPlacement();

        if (placement != null && placement.isSchematicLoaded())
        {
            SchematicHolder.getInstance().removeSchematic(placement.getSchematic());
        }
        else
        {
            MessageDispatcher.error("litematica.message.error.no_placement_selected");
        }
    }

    protected void addTouchedChunksFor(SchematicPlacement placement)
    {
        this.addTouchedChunksFor(placement, true);
    }

    protected boolean hasPlacementInChunk(long chunkPosLong, SchematicPlacement placement)
    {
        List<SchematicPlacement> list = this.placementsTouchingChunk.get(chunkPosLong);
        return list != null && list.contains(placement);
    }

    protected void addPlacementToChunk(long chunkPosLong, SchematicPlacement placement)
    {
        this.placementsTouchingChunk.computeIfAbsent(chunkPosLong, cp -> new ArrayList<>()).add(placement);
    }

    protected void removePlacementFromChunk(long chunkPosLong, SchematicPlacement placement)
    {
        List<SchematicPlacement> list = this.placementsTouchingChunk.get(chunkPosLong);

        if (list != null)
        {
            list.remove(placement);

            if (list.isEmpty())
            {
                this.placementsTouchingChunk.remove(chunkPosLong);
            }
        }
    }

    void addTouchedChunksFor(SchematicPlacement placement, boolean updateOverlay)
    {
        if (placement.matchesRequirement(EnabledCondition.ENABLED))
        {
            LongSet chunks = placement.getTouchedChunks();

            for (long chunkPosLong : chunks)
            {
                if (this.hasPlacementInChunk(chunkPosLong, placement) == false)
                {
                    this.addPlacementToChunk(chunkPosLong, placement);
                    this.updateTouchedBoxesInChunk(chunkPosLong);
                }

                this.chunksToUnload.remove(chunkPosLong);
            }

            this.markChunksForRebuild(chunks);

            if (updateOverlay)
            {
                this.updateOverlayRendererIfEnabled(placement);
            }
        }
    }

    void removeTouchedChunksFor(SchematicPlacement placement)
    {
        if (placement.matchesRequirement(EnabledCondition.ENABLED))
        {
            LongSet chunks = placement.getTouchedChunks();

            for (long chunkPosLong : chunks)
            {
                this.removePlacementFromChunk(chunkPosLong, placement);
                this.updateTouchedBoxesInChunk(chunkPosLong);

                if (this.placementsTouchingChunk.containsKey(chunkPosLong) == false)
                {
                    this.chunksToUnload.add(chunkPosLong);
                }
            }

            chunks.removeAll(this.chunksToUnload);

            this.markChunksForRebuild(chunks);
        }
    }

    public void updateGridPlacementsFor(SchematicPlacement basePlacement)
    {
        this.gridManager.updateGridPlacementsFor(basePlacement);
    }

    protected void onPrePlacementChange(SchematicPlacement placement)
    {
        this.chunksPreChange.clear();
        this.chunksPreChange.addAll(placement.getTouchedChunks());
    }

    protected void onPostPlacementChange(SchematicPlacement placement)
    {
        LongSet chunksPost = placement.getTouchedChunks();
        LongSet toRebuild = new LongOpenHashSet(chunksPost);

        //System.out.printf("chunkPre: %s - chunkPost: %s\n", this.chunksPreChange, chunksPost);
        this.chunksPreChange.removeAll(chunksPost);

        for (long chunkPosLong : this.chunksPreChange)
        {
            this.removePlacementFromChunk(chunkPosLong, placement);
            this.updateTouchedBoxesInChunk(chunkPosLong);
            //System.out.printf("removing placement from: %s\n", pos);

            if (this.placementsTouchingChunk.containsKey(chunkPosLong) == false)
            {
                //System.out.printf("unloading: %s\n", pos);
                this.chunksToUnload.add(chunkPosLong);
            }
            else
            {
                //System.out.printf("rebuilding: %s\n", pos);
                toRebuild.add(chunkPosLong);
            }
        }

        for (long chunkPosLong : chunksPost)
        {
            if (this.hasPlacementInChunk(chunkPosLong, placement) == false)
            {
                this.addPlacementToChunk(chunkPosLong, placement);
            }

            this.updateTouchedBoxesInChunk(chunkPosLong);
        }

        this.markChunksForRebuild(toRebuild);
    }

    protected void onPlacementModified(SchematicPlacement placement)
    {
        placement.resetEnclosingBox();
        this.onPostPlacementChange(placement);
        this.gridManager.updateGridPlacementsFor(placement);
        OverlayRenderer.getInstance().updatePlacementCache();
    }

    protected void onPlacementRegionModified(SchematicPlacement placement)
    {
        placement.checkSubRegionsModified();
        this.onPlacementModified(placement);
    }

    protected void updateOverlayRendererIfEnabled(SchematicPlacement placement)
    {
        if (placement.isEnabled())
        {
            OverlayRenderer.getInstance().updatePlacementCache();
        }
    }

    public void toggleEnabled(SchematicPlacement placement)
    {
        if (placement.isSchematicLoaded() == false &&
            placement.fullyLoadPlacement() == false)
        {
            MessageDispatcher.error("litematica.message.error.schematic_placement.load_failed",
                                    placement.getName(), placement.getSchematicFile());
            return;
        }

        this.onPrePlacementChange(placement);
        placement.toggleEnabled();
        this.onPlacementModified(placement);
    }

    public void toggleIgnoreEntities(SchematicPlacement placement)
    {
        this.onPrePlacementChange(placement);
        placement.toggleIgnoreEntities();
        this.onPlacementModified(placement);
    }

    public void setOrigin(SchematicPlacement placement, BlockPos origin)
    {
        if (placement.isLocked())
        {
            this.printLockedErrorMessage();
            return;
        }

        BlockPos oldOrigin = placement.getPosition();
        BlockPos newOrigin = PositionUtils.getModifiedPartiallyLockedPosition(oldOrigin, origin, placement.coordinateLockMask);

        if (oldOrigin.equals(newOrigin) == false)
        {
            this.onPrePlacementChange(placement);
            placement.setOrigin(newOrigin);
            this.onPlacementModified(placement);
        }
        else if (origin.equals(oldOrigin) == false && placement.coordinateLockMask != 0)
        {
            MessageDispatcher.error(2000).translate("litematica.error.schematic_placements.coordinate_locked");
        }
    }

    public void setRotation(SchematicPlacement placement, Rotation rotation)
    {
        if (placement.isLocked())
        {
            this.printLockedErrorMessage();
            return;
        }

        this.onPrePlacementChange(placement);
        placement.setRotation(rotation);
        this.onPlacementModified(placement);
    }

    public void rotateBy(SchematicPlacement placement, Rotation rotation)
    {
        this.setRotation(placement, placement.getRotation().add(rotation));
    }

    public void setMirror(SchematicPlacement placement, Mirror mirror)
    {
        if (placement.isLocked())
        {
            this.printLockedErrorMessage();
            return;
        }

        this.onPrePlacementChange(placement);
        placement.setMirror(mirror);
        this.onPlacementModified(placement);
    }

    protected void modifyPlacementRegion(SchematicPlacement placement,
                                         Consumer<SchematicPlacement> func)
    {
        if (placement.isLocked())
        {
            this.printLockedErrorMessage();
            return;
        }

        this.onPrePlacementChange(placement);
        func.accept(placement);
        placement.resetEnclosingBox();
        this.onPlacementRegionModified(placement);
    }

    protected void modifyPlacementRegion(SchematicPlacement placement,
                                         String regionName,
                                         Consumer<SubRegionPlacement> func,
                                         boolean obeyLock)
    {
        if (obeyLock && placement.isLocked())
        {
            this.printLockedErrorMessage();
            return;
        }

        SubRegionPlacement subPlacement = placement.getSubRegion(regionName);

        if (subPlacement != null)
        {
            this.onPrePlacementChange(placement);
            func.accept(subPlacement);
            placement.resetEnclosingBox();
            this.onPlacementRegionModified(placement);
        }
    }

    public void toggleSubRegionEnabled(SchematicPlacement placement, String regionName)
    {
        this.modifyPlacementRegion(placement, regionName, SubRegionPlacement::toggleEnabled, false);
    }

    public void toggleSubRegionIgnoreEntities(SchematicPlacement placement, String regionName)
    {
        this.modifyPlacementRegion(placement, regionName, SubRegionPlacement::toggleIgnoreEntities, false);
    }

    public void setSubRegionRotation(SchematicPlacement placement, String regionName, Rotation rotation)
    {
        this.modifyPlacementRegion(placement, regionName, reg -> reg.rotation = rotation, true);
    }

    public void setSubRegionMirror(SchematicPlacement placement, String regionName, Mirror mirror)
    {
        this.modifyPlacementRegion(placement, regionName, reg -> reg.mirror = mirror, true);
    }

    public void resetSubRegionToSchematicValues(SchematicPlacement placement, String regionName)
    {
        this.modifyPlacementRegion(placement, p -> p.resetSubRegionToSchematicValues(regionName));
    }

    public void moveSubRegionTo(SchematicPlacement placement, String regionName, BlockPos newPos)
    {
        this.modifyPlacementRegion(placement, p -> p.moveSubRegionTo(regionName, newPos));
    }

    public void setSubRegionsEnabled(SchematicPlacement placement, boolean enabled, Collection<SubRegionPlacement> subRegions)
    {
        this.onPrePlacementChange(placement);
        placement.setSubRegionsEnabledState(enabled, subRegions);
        this.onPlacementRegionModified(placement);
    }

    public void resetAllSubRegionsToSchematicValues(SchematicPlacement placement)
    {
        this.resetAllSubRegionsToSchematicValues(placement, true);
    }

    public void resetAllSubRegionsToSchematicValues(SchematicPlacement placement, boolean updatePlacementManager)
    {
        if (placement.isLocked())
        {
            this.printLockedErrorMessage();
            return;
        }

        if (updatePlacementManager)
        {
            this.onPrePlacementChange(placement);
        }

        placement.resetAllSubRegionsToSchematicValues();

        if (updatePlacementManager)
        {
            this.onPlacementModified(placement);
        }
    }

    public boolean loadPlacementSettings(SchematicPlacement placement, String str)
    {
        JsonElement el = JsonUtils.parseJsonFromString(str);

        if (el != null && el.isJsonObject())
        {
            return this.loadPlacementSettings(placement, el.getAsJsonObject());
        }

        MessageDispatcher.error("litematica.error.schematic_placements.settings_load.invalid_data");

        return false;
    }

    public interface PlacementSettingsLoader
    {
        boolean load(SchematicPlacement placement, JsonObject obj);
    }

    public boolean loadPlacementSettings(SchematicPlacement placement, JsonObject obj)
    {
        return this.loadPlacementSettings(placement, obj, SchematicPlacement::readFromJson);
    }

    public boolean loadPlacementSettingsFromSharedString(SchematicPlacement placement, String str)
    {
        JsonElement el = JsonUtils.parseJsonFromString(str);

        if (el == null || el.isJsonObject() == false)
        {
            MessageDispatcher.error("litematica.error.schematic_placements.settings_load.invalid_data");
            return false;
        }

        return this.loadPlacementSettings(placement, el.getAsJsonObject(), SchematicPlacement::loadFromSharedSettings);
    }

    protected boolean loadPlacementSettings(SchematicPlacement placement, JsonObject obj, PlacementSettingsLoader loader)
    {
        if (placement.isLocked())
        {
            this.printLockedErrorMessage();
            return false;
        }

        this.onPrePlacementChange(placement);

        boolean success = loader.load(placement, obj);

        this.onPlacementModified(placement);

        return success;
    }

    protected void updateTouchedBoxesInChunk(long chunkPosLong)
    {
        int chunkX = malilib.util.position.PositionUtils.getChunkPosX(chunkPosLong);
        int chunkZ = malilib.util.position.PositionUtils.getChunkPosZ(chunkPosLong);

        for (int y = 0; y < 16; ++y)
        {
            ChunkSectionPos subChunk = new ChunkSectionPos(chunkX, y, chunkZ);
            this.touchedVolumesInSubChunk.removeAll(subChunk);
        }

        List<SchematicPlacement> placements = this.placementsTouchingChunk.get(chunkPosLong);

        if (placements == null)
        {
            return;
        }

        for (SchematicPlacement placement : placements)
        {
            if (placement.matchesRequirement(EnabledCondition.ENABLED))
            {
                Map<String, IntBoundingBox> boxMap = placement.getBoxesWithinChunk(chunkX, chunkZ);

                for (Map.Entry<String, IntBoundingBox> entry : boxMap.entrySet())
                {
                    IntBoundingBox bbOrig = entry.getValue();
                    final int startCY = (bbOrig.minY >> 4);
                    final int endCY = (bbOrig.maxY >> 4);

                    for (int cy = startCY; cy <= endCY; ++cy)
                    {
                        int y1 = Math.max((cy << 4)     , bbOrig.minY);
                        int y2 = Math.min((cy << 4) + 15, bbOrig.maxY);

                        IntBoundingBox bbSub = new IntBoundingBox(bbOrig.minX, y1, bbOrig.minZ, bbOrig.maxX, y2, bbOrig.maxZ);
                        PlacementPart part = new PlacementPart(placement, entry.getKey(), bbSub);
                        this.touchedVolumesInSubChunk.put(new ChunkSectionPos(chunkX, cy, chunkZ), part);
                        //System.out.printf("updateTouchedBoxesInChunk box at %d, %d, %d: %s\n", pos.x, cy, pos.z, bbSub);
                    }
                }
            }
        }
    }

    public void markAllPlacementsOfSchematicForRebuild(ISchematic schematic)
    {
        for (SchematicPlacement placement : this.allVisibleSchematicPlacements)
        {
            if (placement.getSchematic() == schematic)
            {
                this.markChunksForRebuild(placement);
            }
        }
    }

    public void markChunksForRebuild(SchematicPlacement placement)
    {
        if (placement.matchesRequirement(EnabledCondition.ENABLED))
        {
            this.markChunksForRebuild(placement.getTouchedChunks());
        }
    }

    protected void markChunksForRebuild(LongSet chunks)
    {
        //System.out.printf("rebuilding %d chunks: %s\n", chunks.size(), chunks);
        this.chunksToRebuild.addAll(chunks);

        for (EventListener listener : this.rebuildListeners)
        {
            listener.onEvent();
        }
    }

    public void markChunkForRebuild(long chunkPosLong)
    {
        this.chunksToRebuild.add(chunkPosLong);
    }

    public boolean changeSelection(World world, Entity cameraEntity, int maxDistance)
    {
        if (this.schematicPlacements.isEmpty() == false)
        {
            RayTraceWrapper trace = RayTraceUtils.getWrappedRayTraceFromEntity(world, cameraEntity, maxDistance);

            SchematicPlacement placement = this.getSelectedSchematicPlacement();

            if (placement != null)
            {
                placement.setSelectedSubRegionName(null);
            }

            if (trace.getHitType() == HitType.PLACEMENT_SUBREGION || trace.getHitType() == HitType.PLACEMENT_ORIGIN)
            {
                this.setSelectedSchematicPlacement(trace.getHitSchematicPlacement());

                boolean selectSubRegion = Hotkeys.SELECTION_GRAB_MODIFIER.getKeyBind().isKeyBindHeld();
                String subRegionName = selectSubRegion ? trace.getHitSchematicPlacementRegionName() : null;
                this.getSelectedSchematicPlacement().setSelectedSubRegionName(subRegionName);

                return true;
            }
            else if (trace.getHitType() == HitType.MISS)
            {
                this.setSelectedSchematicPlacement(null);
                return true;
            }
        }

        return false;
    }

    public void setPositionOfCurrentSelectionToRayTrace(double maxDistance)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            Entity entity = GameUtils.getCameraEntity();
            RayTraceResult trace = malilib.util.game.RayTraceUtils.getRayTraceFromEntity(GameUtils.getClientWorld(), entity, RayTraceFluidHandling.NONE, false, maxDistance);

            if (trace.typeOfHit != RayTraceResult.Type.BLOCK)
            {
                return;
            }

            BlockPos pos = trace.getBlockPos();

            // Sneaking puts the position inside the targeted block, not sneaking puts it against the targeted face
            if (GameUtils.getClientPlayer().isSneaking() == false)
            {
                pos = pos.offset(trace.sideHit);
            }

            this.setPositionOfCurrentSelectionTo(pos);
        }
    }

    public void setPositionOfCurrentSelectionTo(BlockPos pos)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            if (schematicPlacement.isLocked())
            {
                this.printLockedErrorMessage();
                return;
            }

            boolean movingBox = schematicPlacement.getSelectedSubRegionPlacement() != null;

            if (movingBox)
            {
                this.moveSubRegionTo(schematicPlacement, schematicPlacement.getSelectedSubRegionName(), pos);

                MessageDispatcher.success().customHotbar()
                        .translate("litematica.message.placement.moved_subregion_to",
                                   pos.getX(), pos.getY(), pos.getZ());
            }
            // Moving the origin point
            else
            {
                BlockPos old = schematicPlacement.getPosition();
                this.setOrigin(schematicPlacement, pos);

                if (old.equals(schematicPlacement.getPosition()) == false)
                {
                    MessageDispatcher.generic().customHotbar()
                            .translate("litematica.message.placement.moved_placement_origin",
                                       old.getX(), old.getY(), old.getZ(),
                                       pos.getX(), pos.getY(), pos.getZ());
                }
            }
        }
    }

    public void nudgePositionOfCurrentSelection(EnumFacing direction, int amount)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            if (schematicPlacement.isLocked())
            {
                this.printLockedErrorMessage();
                return;
            }

            SubRegionPlacement placement = schematicPlacement.getSelectedSubRegionPlacement();

            // Moving a sub-region
            if (placement != null)
            {
                // getPos returns a relative position, but moveSubRegionTo takes an absolute position...
                BlockPos old = PositionUtils.getTransformedBlockPos(placement.getPosition(), schematicPlacement.getMirror(), schematicPlacement.getRotation());
                old = old.add(schematicPlacement.getPosition());

                this.moveSubRegionTo(schematicPlacement, placement.getName(), old.offset(direction, amount));
            }
            // Moving the origin point
            else
            {
                BlockPos old = schematicPlacement.getPosition();
                this.setOrigin(schematicPlacement, old.offset(direction, amount));
            }
        }
    }

    public void loadPlacementFromFile(Path file)
    {
        SchematicPlacement placement = SchematicPlacement.createFromFile(file);

        if (placement == null)
        {
            MessageDispatcher.error("litematica.error.schematic_placements.load_fail", file.getFileName().toString());
            return;
        }

        if (this.checkIsAlreadyLoaded(placement))
        {
            MessageDispatcher.error("litematica.error.schematic_placements.load_fail.already_loaded",
                                    placement.getName());
            return;
        }

        this.addSchematicPlacement(placement, false, false);
        MessageDispatcher.generic().customHotbar().translate("litematica.message.schematic_placement_loaded", placement.getName());
        this.printRendererDisabledWarningMessages();
    }

    protected boolean checkIsAlreadyLoaded(SchematicPlacement placement)
    {
        if (placement.placementSaveFile == null)
        {
            return false;
        }

        for (SchematicPlacement entry : this.schematicPlacements)
        {
            if (placement.placementSaveFile.equals(entry.placementSaveFile))
            {
                return true;
            }
        }

        return false;
    }

    public void createPlacementForNewlyLoadedSchematic(ISchematic schematic, boolean createAsEnabled)
    {
        BlockPos pos = EntityWrap.getCameraEntityBlockPos();
        String name = schematic.getMetadata().getName();
        SchematicPlacement placement = SchematicPlacement.create(schematic, pos, name, createAsEnabled);

        this.addSchematicPlacement(placement, true);
        this.setSelectedSchematicPlacement(placement);
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        JsonArray arr = JsonUtils.toArray(this.schematicPlacements, SchematicPlacement::toJsonIfShouldSave);
        int selectedIndex = this.selectedPlacement != null ? this.schematicPlacements.indexOf(this.selectedPlacement) : -1;

        if (arr.size() > 0)
        {
            obj.add("placements", arr);
            JsonUtils.addIfNotEqual(obj, "selected", selectedIndex, -1);
        }

        return obj;
    }

    protected void loadPlacementFromJson(JsonObject obj)
    {
        SchematicPlacement placement = SchematicPlacement.createFromJson(obj);

        if (placement != null)
        {
            this.addSchematicPlacement(placement, false, true);
        }
    }

    public void loadFromJson(JsonObject obj)
    {
        this.clear();

        if (JsonUtils.hasArray(obj, "placements"))
        {
            JsonUtils.getArrayElementsIfObjects(obj, "placements", this::loadPlacementFromJson);
            int index = JsonUtils.getIntegerOrDefault(obj, "selected", -1);

            if (index >= 0 && index < this.schematicPlacements.size())
            {
                this.selectedPlacement = this.schematicPlacements.get(index);
            }
        }

        OverlayRenderer.getInstance().updatePlacementCache();
    }

    protected void printLockedErrorMessage()
    {
        MessageDispatcher.error("litematica.message.error.schematic_placement.locked");
    }

    public static class PlacementPart
    {
        private final SchematicPlacement placement;
        private final String subRegionName;
        private final IntBoundingBox bb;

        public PlacementPart(SchematicPlacement placement, String subRegionName, IntBoundingBox bb)
        {
            this.placement = placement;
            this.subRegionName = subRegionName;
            this.bb = bb;
        }

        public SchematicPlacement getPlacement()
        {
            return this.placement;
        }

        public String getSubRegionName()
        {
            return this.subRegionName;
        }

        public IntBoundingBox getBox()
        {
            return this.bb;
        }
    }
}
