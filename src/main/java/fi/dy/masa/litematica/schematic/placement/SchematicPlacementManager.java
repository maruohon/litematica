package fi.dy.masa.litematica.schematic.placement;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.infohud.StatusInfoRenderer;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.ISchematicRegion;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.schematic.util.SchematicPlacingUtils;
import fi.dy.masa.litematica.util.IGenericEventListener;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.config.option.HotkeyConfig;
import fi.dy.masa.malilib.config.value.LayerMode;
import fi.dy.masa.malilib.util.consumer.StringConsumer;
import fi.dy.masa.malilib.overlay.message.MessageConsumer;
import fi.dy.masa.malilib.overlay.message.MessageOutput;
import fi.dy.masa.malilib.overlay.message.MessageUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.RayTraceUtils.RayTraceFluidHandling;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.IntBoundingBox;
import fi.dy.masa.malilib.util.position.SubChunkPos;

public class SchematicPlacementManager
{
    private final List<SchematicPlacement> schematicPlacements = new ArrayList<>();
    private final List<SchematicPlacementUnloaded> lightlyLoadedPlacements = new ArrayList<>();
    private final Set<SchematicPlacement> allVisibleSchematicPlacements = new HashSet<>();

    private final HashMultimap<ChunkPos, SchematicPlacement> schematicsTouchingChunk = HashMultimap.create();
    private final ArrayListMultimap<SubChunkPos, PlacementPart> touchedVolumesInSubChunk = ArrayListMultimap.create();

    private final Set<ChunkPos> chunksToRebuild = new HashSet<>();
    private final Set<ChunkPos> chunksToUnload = new HashSet<>();
    private final Set<ChunkPos> chunksPreChange = new HashSet<>();

    private final List<IGenericEventListener> rebuildListeners = new ArrayList<>();

    private final GridPlacementManager gridManager;

    @Nullable
    private SchematicPlacement selectedPlacement;
    private int tickCounter;

    public SchematicPlacementManager()
    {
        this.gridManager = new GridPlacementManager(this);
    }

    public boolean hasPendingRebuilds()
    {
        return this.chunksToRebuild.isEmpty() == false;
    }

    public boolean hasPendingRebuildFor(ChunkPos pos)
    {
        return this.chunksToRebuild.contains(pos);
    }

    public void addRebuildListener(IGenericEventListener listener)
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
            WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

            if (worldSchematic != null)
            {
                for (ChunkPos pos : this.chunksToUnload)
                {
                    this.unloadSchematicChunk(worldSchematic, pos.x, pos.z);
                }
            }

            this.chunksToUnload.clear();
        }

        //System.out.printf("processQueuedChunks, size: %d\n", this.chunksToRebuild.size());
        if (this.chunksToRebuild.isEmpty() == false)
        {
            WorldClient worldClient = Minecraft.getMinecraft().world;

            if (worldClient == null)
            {
                this.chunksToRebuild.clear();
                return true;
            }

            WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
            Iterator<ChunkPos> iter = this.chunksToRebuild.iterator();

            while (iter.hasNext())
            {
                if ((System.nanoTime() - DataManager.getClientTickStartTime()) >= 45000000L)
                {
                    break;
                }

                ChunkPos pos = iter.next();

                if (this.schematicsTouchingChunk.containsKey(pos) == false)
                {
                    iter.remove();
                    continue;
                }

                if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() ||
                    worldClient.getChunkProvider().isChunkGeneratedAt(pos.x, pos.z))
                {
                    // Wipe the old chunk if it exists
                    if (worldSchematic.getChunkProvider().isChunkGeneratedAt(pos.x, pos.z))
                    {
                        //System.out.printf("wiping chunk at %s\n", pos);
                        this.unloadSchematicChunk(worldSchematic, pos.x, pos.z);
                    }

                    //System.out.printf("loading chunk at %s\n", pos);
                    worldSchematic.getChunkProvider().loadChunk(pos.x, pos.z);
                }

                if (worldSchematic.getChunkProvider().isChunkGeneratedAt(pos.x, pos.z))
                {
                    //System.out.printf("placing at %s\n", pos);
                    Collection<SchematicPlacement> placements = this.schematicsTouchingChunk.get(pos);

                    if (placements.isEmpty() == false)
                    {
                        for (SchematicPlacement placement : placements)
                        {
                            if (placement.isEnabled())
                            {
                                SchematicPlacingUtils.placeToWorldWithinChunk(placement, pos, worldSchematic, ReplaceBehavior.ALL, false);
                            }
                        }

                        worldSchematic.markBlockRangeForRenderUpdate(pos.x << 4, 0, pos.z << 4, (pos.x << 4) + 15, 256, (pos.z << 4) + 15);
                    }

                    iter.remove();
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
            WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

            if (worldSchematic != null)
            {
                this.unloadSchematicChunk(worldSchematic, chunkX, chunkZ);
                this.chunksToRebuild.add(new ChunkPos(chunkX, chunkZ));
            }
        }
    }

    private void unloadSchematicChunk(WorldSchematic worldSchematic, int chunkX, int chunkZ)
    {
        if (worldSchematic.getChunkProvider().isChunkGeneratedAt(chunkX, chunkZ))
        {
            //System.out.printf("unloading chunk at %d, %d\n", chunkX, chunkZ);
            worldSchematic.markBlockRangeForRenderUpdate((chunkX << 4) - 1, 0, (chunkZ << 4) - 1, (chunkX << 4) + 16, 256, (chunkZ << 4) + 16);
            worldSchematic.getChunkProvider().unloadChunk(chunkX, chunkZ);
        }
    }

    public Set<SchematicPlacement> getVisibleSchematicPlacements()
    {
        return this.allVisibleSchematicPlacements;
    }

    public List<SchematicPlacementUnloaded> getAllSchematicPlacements()
    {
        List<SchematicPlacementUnloaded> list = new ArrayList<>();
        list.addAll(this.schematicPlacements);
        list.addAll(this.lightlyLoadedPlacements);
        return list;
    }

    public List<IntBoundingBox> getTouchedBoxesInSubChunk(SubChunkPos subChunk)
    {
        List<IntBoundingBox> list = new ArrayList<>();

        for (PlacementPart part : this.touchedVolumesInSubChunk.get(subChunk))
        {
            list.add(part.getBox());
        }

        return list;
    }

    public List<PlacementPart> getAllPlacementsTouchingSubChunk(SubChunkPos pos)
    {
        return this.touchedVolumesInSubChunk.get(pos);
    }

    public Set<SubChunkPos> getAllTouchedSubChunks()
    {
        return this.touchedVolumesInSubChunk.keySet();
    }

    public void addSchematicPlacement(SchematicPlacement placement, boolean printMessages)
    {
        this.addSchematicPlacement(placement, printMessages, false);
    }

    public boolean duplicateSelectedPlacement()
    {
        SchematicPlacementUnloaded placement = this.getSelectedSchematicPlacement();

        if (placement != null)
        {
            this.duplicateSchematicPlacement(placement);
            return true;
        }

        return false;
    }

    public void duplicateSchematicPlacement(SchematicPlacementUnloaded placement)
    {
        if (placement.isLoaded())
        {
            SchematicPlacement duplicate = ((SchematicPlacement) placement).copyAsFullyLoaded(false);
            duplicate.setBoundingBoxColorToNext();
            this.addSchematicPlacement(duplicate, false);
            this.setSelectedSchematicPlacement(duplicate);
        }
        else
        {
            this.lightlyLoadedPlacements.add(placement.copyAsUnloaded());
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

    private void addSchematicPlacement(SchematicPlacement placement, boolean printMessages, boolean isLoadFromFile)
    {
        if (this.schematicPlacements.contains(placement) == false)
        {
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
            }

            if (printMessages && isLoadFromFile == false)
            {
                MessageUtils.showGuiMessage(MessageOutput.SUCCESS, StringUtils.translate("litematica.message.schematic_placement_created", placement.getName()));

                if (Configs.InfoOverlays.WARN_DISABLED_RENDERING.getBooleanValue())
                {
                    LayerMode mode = DataManager.getRenderLayerRange().getLayerMode();

                    if (mode != LayerMode.ALL)
                    {
                        MessageUtils.showGuiAndInGameMessage(MessageOutput.WARNING, "litematica.message.warn.layer_mode_currently_at", mode.getDisplayName());
                    }

                    if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() == false)
                    {
                        HotkeyConfig hotkey = Hotkeys.TOGGLE_ALL_RENDERING;
                        String configName = Configs.Visuals.ENABLE_RENDERING.getName();
                        String hotkeyName = hotkey.getName();
                        String hotkeyVal = hotkey.getKeyBind().getKeysDisplayString();

                        MessageUtils.showGuiAndInGameMessage(MessageOutput.WARNING, 8000,
                                                             "litematica.message.warn.main_rendering_disabled", configName, hotkeyName, hotkeyVal);
                    }

                    if (Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() == false)
                    {
                        HotkeyConfig hotkey = Hotkeys.TOGGLE_SCHEMATIC_RENDERING;
                        String configName = Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getName();
                        String hotkeyName = hotkey.getName();
                        String hotkeyVal = hotkey.getKeyBind().getKeysDisplayString();

                        MessageUtils.showGuiAndInGameMessage(MessageOutput.WARNING, 8000,
                                                             "litematica.message.warn.schematic_rendering_disabled", configName, hotkeyName, hotkeyVal);
                    }

                    if (Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue() == false)
                    {
                        HotkeyConfig hotkey = Hotkeys.TOGGLE_SCHEMATIC_BLOCK_RENDERING;
                        String configName = Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getName();
                        String hotkeyName = hotkey.getName();
                        String hotkeyVal = hotkey.getKeyBind().getKeysDisplayString();

                        MessageUtils.showGuiAndInGameMessage(MessageOutput.WARNING, 8000,
                                                             "litematica.message.warn.schematic_blocks_rendering_disabled", configName, hotkeyName, hotkeyVal);
                    }
                }
            }
        }
        else if (printMessages)
        {
            MessageUtils.showGuiAndInGameMessage(MessageOutput.ERROR, "litematica.error.duplicate_schematic_placement");
        }
    }

    public boolean removeSelectedSchematicPlacement()
    {
        SchematicPlacementUnloaded placement = this.getSelectedSchematicPlacement();

        if (placement != null)
        {
            this.removeSchematicPlacement(placement);
            return true;
        }

        return false;
    }

    public boolean removeSchematicPlacement(SchematicPlacementUnloaded placement)
    {
        return this.removeSchematicPlacement(placement, true);
    }

    public boolean removeSchematicPlacement(SchematicPlacementUnloaded placement, boolean update)
    {
        if (this.selectedPlacement == placement)
        {
            this.selectedPlacement = null;
        }

        placement.invalidate();

        if (placement.isLoaded())
        {
            SchematicPlacement loadedPlacement = (SchematicPlacement) placement;
            boolean ret = this.schematicPlacements.remove(loadedPlacement);

            this.gridManager.onPlacementRemoved(loadedPlacement);
            this.removeVisiblePlacement(loadedPlacement);
            this.removeTouchedChunksFor(loadedPlacement);

            if (ret && update)
            {
                this.updateOverlayRendererIfEnabled(loadedPlacement);
            }

            return ret;
        }
        else
        {
            this.lightlyLoadedPlacements.remove(placement);
            return false;
        }
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

        if (placement != null)
        {
            SchematicHolder.getInstance().removeSchematic(placement.getSchematic());
        }
        else
        {
            MessageUtils.showGuiOrInGameMessage(MessageOutput.ERROR, "litematica.message.error.no_placement_selected");
        }
    }

    private void addTouchedChunksFor(SchematicPlacement placement)
    {
        this.addTouchedChunksFor(placement, true);
    }

    void addTouchedChunksFor(SchematicPlacement placement, boolean updateOverlay)
    {
        if (placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
        {
            Set<ChunkPos> chunks = placement.getTouchedChunks();

            for (ChunkPos pos : chunks)
            {
                if (this.schematicsTouchingChunk.containsEntry(pos, placement) == false)
                {
                    this.schematicsTouchingChunk.put(pos, placement);
                    this.updateTouchedBoxesInChunk(pos);
                }

                this.chunksToUnload.remove(pos);
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
        if (placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
        {
            Set<ChunkPos> chunks = placement.getTouchedChunks();

            for (ChunkPos pos : chunks)
            {
                this.schematicsTouchingChunk.remove(pos, placement);
                this.updateTouchedBoxesInChunk(pos);

                if (this.schematicsTouchingChunk.containsKey(pos) == false)
                {
                    this.chunksToUnload.add(pos);
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

    private void onPrePlacementChange(SchematicPlacement placement)
    {
        this.chunksPreChange.clear();
        this.chunksPreChange.addAll(placement.getTouchedChunks());
    }

    private void onPostPlacementChange(SchematicPlacement placement)
    {
        Set<ChunkPos> chunksPost = placement.getTouchedChunks();
        Set<ChunkPos> toRebuild = new HashSet<>(chunksPost);

        //System.out.printf("chunkPre: %s - chunkPost: %s\n", this.chunksPreChange, chunksPost);
        this.chunksPreChange.removeAll(chunksPost);

        for (ChunkPos pos : this.chunksPreChange)
        {
            this.schematicsTouchingChunk.remove(pos, placement);
            this.updateTouchedBoxesInChunk(pos);
            //System.out.printf("removing placement from: %s\n", pos);

            if (this.schematicsTouchingChunk.containsKey(pos) == false)
            {
                //System.out.printf("unloading: %s\n", pos);
                this.chunksToUnload.add(pos);
            }
            else
            {
                //System.out.printf("rebuilding: %s\n", pos);
                toRebuild.add(pos);
            }
        }

        for (ChunkPos pos : chunksPost)
        {
            if (this.schematicsTouchingChunk.containsEntry(pos, placement) == false)
            {
                this.schematicsTouchingChunk.put(pos, placement);
            }

            this.updateTouchedBoxesInChunk(pos);
        }

        this.markChunksForRebuild(toRebuild);
    }

    private void onPlacementModified(SchematicPlacement placement)
    {
        placement.updateEnclosingBox();
        this.onPostPlacementChange(placement);
        this.gridManager.updateGridPlacementsFor(placement);
        OverlayRenderer.getInstance().updatePlacementCache();
    }

    private void onPlacementRegionModified(SchematicPlacement placement)
    {
        placement.checkAreSubRegionsModified();
        this.onPlacementModified(placement);
    }

    private void updateOverlayRendererIfEnabled(SchematicPlacement placement)
    {
        if (placement.isEnabled())
        {
            OverlayRenderer.getInstance().updatePlacementCache();
        }
    }

    public void toggleEnabled(SchematicPlacementUnloaded placement)
    {
        if (placement.isLoaded())
        {
            SchematicPlacement loadedPlacement = (SchematicPlacement) placement;
            this.onPrePlacementChange(loadedPlacement);
            loadedPlacement.toggleEnabled();
            this.onPlacementModified(loadedPlacement);
        }
        else
        {
            SchematicPlacement loadedPlacement = placement.fullyLoadPlacement();

            if (loadedPlacement != null)
            {
                loadedPlacement.toggleEnabled();
                this.addSchematicPlacement(loadedPlacement, false);
                this.lightlyLoadedPlacements.remove(placement);
            }
            else
            {
                MessageUtils.showGuiOrInGameMessage(MessageOutput.ERROR, "litematica.message.error.schematic_placement.load_failed", placement.getName(), placement.getSchematicFile());
            }
        }
    }

    public void toggleIgnoreEntities(SchematicPlacement placement)
    {
        this.onPrePlacementChange(placement);
        placement.toggleIgnoreEntities();
        this.onPlacementModified(placement);
    }

    public void setOrigin(SchematicPlacement placement, BlockPos origin, StringConsumer feedback)
    {
        if (placement.isLocked())
        {
            feedback.consumeString("litematica.message.placement.cant_modify_is_locked");
            return;
        }

        BlockPos oldOrigin = placement.getOrigin();
        BlockPos newOrigin = PositionUtils.getModifiedPartiallyLockedPosition(oldOrigin, origin, placement.coordinateLockMask);

        if (oldOrigin.equals(newOrigin) == false)
        {
            this.onPrePlacementChange(placement);
            placement.setOrigin(newOrigin);
            this.onPlacementModified(placement);
        }
        else if (origin.equals(oldOrigin) == false && placement.coordinateLockMask != 0)
        {
            MessageUtils.showGuiOrInGameMessage(MessageOutput.ERROR, 2000, "litematica.error.schematic_placements.coordinate_locked");
        }
    }

    public void setRotation(SchematicPlacement placement, Rotation rotation, MessageConsumer feedback)
    {
        if (placement.isLocked())
        {
            feedback.addMessage(MessageOutput.ERROR, "litematica.message.placement.cant_modify_is_locked");
            return;
        }

        if (placement.getRotation() != rotation)
        {
            this.onPrePlacementChange(placement);
            placement.setRotation(rotation);
            this.onPlacementModified(placement);
        }
    }

    public void rotateBy(SchematicPlacement placement, Rotation rotation)
    {
        this.setRotation(placement, placement.getRotation().add(rotation), MessageUtils.INGAME_MESSAGE_CONSUMER);
    }

    public void setMirror(SchematicPlacement placement, Mirror mirror, MessageConsumer feedback)
    {
        if (placement.isLocked())
        {
            feedback.addMessage(MessageOutput.ERROR, "litematica.message.placement.cant_modify_is_locked");
            return;
        }

        if (placement.getMirror() != mirror)
        {
            this.onPrePlacementChange(placement);
            placement.setMirror(mirror);
            this.onPlacementModified(placement);
        }
    }

    public void toggleSubRegionEnabled(SchematicPlacement placement, String regionName)
    {
        SubRegionPlacement subPlacement = placement.getRelativeSubRegionPlacement(regionName);

        if (subPlacement != null)
        {
            this.onPrePlacementChange(placement);
            placement.toggleSubRegionEnabled(regionName);
            this.onPlacementRegionModified(placement);
        }
    }

    public void toggleSubRegionIgnoreEntities(SchematicPlacement placement, String regionName)
    {
        SubRegionPlacement subPlacement = placement.getRelativeSubRegionPlacement(regionName);

        if (subPlacement != null)
        {
            this.onPrePlacementChange(placement);
            placement.toggleSubRegionIgnoreEntities(regionName);
            this.onPlacementRegionModified(placement);
        }
    }

    public void setSubRegionsEnabled(SchematicPlacement placement, boolean enabled, Collection<SubRegionPlacement> subRegions)
    {
        this.onPrePlacementChange(placement);
        placement.setSubRegionsEnabledState(enabled, subRegions);
        this.onPlacementRegionModified(placement);
    }

    public void setSubRegionRotation(SchematicPlacement placement, String regionName, Rotation rotation, MessageConsumer feedback)
    {
        if (placement.isLocked())
        {
            feedback.addMessage(MessageOutput.ERROR, "litematica.message.placement.cant_modify_is_locked");
            return;
        }

        SubRegionPlacement subPlacement = placement.getRelativeSubRegionPlacement(regionName);

        if (subPlacement != null)
        {
            this.onPrePlacementChange(placement);
            placement.setSubRegionRotation(regionName, rotation);
            this.onPlacementRegionModified(placement);
        }
    }

    public void setSubRegionMirror(SchematicPlacement placement, String regionName, Mirror mirror, MessageConsumer feedback)
    {
        if (placement.isLocked())
        {
            feedback.addMessage(MessageOutput.ERROR, "litematica.message.placement.cant_modify_is_locked");
            return;
        }

        SubRegionPlacement subPlacement = placement.getRelativeSubRegionPlacement(regionName);

        if (subPlacement != null)
        {
            this.onPrePlacementChange(placement);
            placement.setSubRegionMirror(regionName, mirror);
            this.onPlacementRegionModified(placement);
        }
    }

    public void moveSubRegionTo(SchematicPlacement placement, String regionName, BlockPos newPos, StringConsumer feedback)
    {
        if (placement.isLocked())
        {
            feedback.consumeString("litematica.message.placement.cant_modify_is_locked");
            return;
        }

        SubRegionPlacement subPlacement = placement.getRelativeSubRegionPlacement(regionName);

        if (subPlacement != null)
        {
            this.onPrePlacementChange(placement);
            placement.moveSubRegionTo(regionName, newPos);
            this.onPlacementRegionModified(placement);
        }
    }

    public void resetSubRegionToSchematicValues(SchematicPlacement placement, String regionName, MessageConsumer feedback)
    {
        if (placement.isLocked())
        {
            feedback.addMessage(MessageOutput.ERROR, "litematica.message.placement.cant_modify_is_locked");
            return;
        }

        ISchematicRegion region = placement.getSchematic().getSchematicRegion(regionName);
        SubRegionPlacement subPlacement = placement.getRelativeSubRegionPlacement(regionName);

        if (region != null && subPlacement != null)
        {
            this.onPrePlacementChange(placement);
            placement.resetSubRegionToSchematicValues(regionName);
            this.onPlacementRegionModified(placement);
        }
    }

    public void resetAllSubRegionsToSchematicValues(SchematicPlacement placement, StringConsumer feedback)
    {
        this.resetAllSubRegionsToSchematicValues(placement, feedback, true);
    }

    public void resetAllSubRegionsToSchematicValues(SchematicPlacement placement, StringConsumer feedback, boolean updatePlacementManager)
    {
        if (placement.isLocked())
        {
            feedback.consumeString("litematica.message.placement.cant_modify_is_locked");
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

    public boolean loadPlacementSettings(SchematicPlacement placement, String str, MessageConsumer feedback)
    {
        JsonElement el = JsonUtils.parseJsonFromString(str);

        if (el != null && el.isJsonObject())
        {
            return this.loadPlacementSettings(placement, el.getAsJsonObject(), feedback);
        }

        feedback.addMessage(MessageOutput.ERROR, "litematica.error.schematic_placements.settings_load.invalid_data");

        return false;
    }

    public boolean loadPlacementSettings(SchematicPlacement placement, JsonObject obj, MessageConsumer feedback)
    {
        if (placement.isLocked())
        {
            feedback.addMessage(MessageOutput.ERROR, "litematica.message.placement.cant_modify_is_locked");
            return false;
        }

        this.onPrePlacementChange(placement);

        boolean success = placement.readBaseSettingsFromJson(obj);

        this.onPlacementModified(placement);

        return success;
    }

    private void updateTouchedBoxesInChunk(ChunkPos pos)
    {
        for (int y = 0; y < 16; ++y)
        {
            SubChunkPos subChunk = new SubChunkPos(pos.x, y, pos.z);
            this.touchedVolumesInSubChunk.removeAll(subChunk);
        }

        Collection<SchematicPlacement> placements = this.schematicsTouchingChunk.get(pos);

        if (placements.isEmpty() == false)
        {
            for (SchematicPlacement placement : placements)
            {
                if (placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
                {
                    Map<String, IntBoundingBox> boxMap = placement.getBoxesWithinChunk(pos.x, pos.z);

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
                            this.touchedVolumesInSubChunk.put(new SubChunkPos(pos.x, cy, pos.z), part);
                            //System.out.printf("updateTouchedBoxesInChunk box at %d, %d, %d: %s\n", pos.x, cy, pos.z, bbSub);
                        }
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
        if (placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
        {
            this.markChunksForRebuild(placement.getTouchedChunks());
        }
    }

    private void markChunksForRebuild(Collection<ChunkPos> chunks)
    {
        //System.out.printf("rebuilding %d chunks: %s\n", chunks.size(), chunks);
        this.chunksToRebuild.addAll(chunks);

        for (IGenericEventListener listener : this.rebuildListeners)
        {
            listener.onEvent();
        }
    }

    public void markChunkForRebuild(ChunkPos pos)
    {
        this.chunksToRebuild.add(pos);
    }

    public boolean changeSelection(World world, Entity entity, int maxDistance)
    {
        if (this.schematicPlacements.isEmpty() == false)
        {
            RayTraceWrapper trace = RayTraceUtils.getWrappedRayTraceFromEntity(world, entity, maxDistance);

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

    public void setPositionOfCurrentSelectionToRayTrace(Minecraft mc, double maxDistance)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            RayTraceResult trace = fi.dy.masa.malilib.util.RayTraceUtils.getRayTraceFromEntity(mc.world, entity, RayTraceFluidHandling.NONE, false, maxDistance);

            if (trace.typeOfHit != RayTraceResult.Type.BLOCK)
            {
                return;
            }

            BlockPos pos = trace.getBlockPos();

            // Sneaking puts the position inside the targeted block, not sneaking puts it against the targeted face
            if (mc.player.isSneaking() == false)
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
                MessageUtils.showGuiOrActionBarMessage(MessageOutput.ERROR, "litematica.message.placement.cant_modify_is_locked");
                return;
            }

            boolean movingBox = schematicPlacement.getSelectedSubRegionPlacement() != null;

            if (movingBox)
            {
                this.moveSubRegionTo(schematicPlacement, schematicPlacement.getSelectedSubRegionName(), pos, MessageUtils.INFO_MESSAGE_CONSUMER);

                String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                MessageUtils.showGuiOrActionBarMessage(MessageOutput.SUCCESS, "litematica.message.placement.moved_subregion_to", posStr);
            }
            // Moving the origin point
            else
            {
                BlockPos old = schematicPlacement.getOrigin();
                this.setOrigin(schematicPlacement, pos, MessageUtils.INFO_MESSAGE_CONSUMER);

                if (old.equals(schematicPlacement.getOrigin()) == false)
                {
                    String posStrOld = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
                    String posStrNew = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    MessageUtils.showGuiOrActionBarMessage(MessageOutput.SUCCESS, "litematica.message.placement.moved_placement_origin", posStrOld, posStrNew);
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
                MessageUtils.showGuiOrActionBarMessage(MessageOutput.ERROR, "litematica.message.placement.cant_modify_is_locked");
                return;
            }

            SubRegionPlacement placement = schematicPlacement.getSelectedSubRegionPlacement();

            // Moving a sub-region
            if (placement != null)
            {
                // getPos returns a relative position, but moveSubRegionTo takes an absolute position...
                BlockPos old = PositionUtils.getTransformedBlockPos(placement.getPos(), schematicPlacement.getMirror(), schematicPlacement.getRotation());
                old = old.add(schematicPlacement.getOrigin());

                this.moveSubRegionTo(schematicPlacement, placement.getName(), old.offset(direction, amount), MessageUtils.INFO_MESSAGE_CONSUMER);
            }
            // Moving the origin point
            else
            {
                BlockPos old = schematicPlacement.getOrigin();
                this.setOrigin(schematicPlacement, old.offset(direction, amount), MessageUtils.INFO_MESSAGE_CONSUMER);
            }
        }
    }

    public void clear()
    {
        this.schematicPlacements.clear();
        this.allVisibleSchematicPlacements.clear();
        this.lightlyLoadedPlacements.clear();
        this.gridManager.clear();
        this.selectedPlacement = null;
        this.schematicsTouchingChunk.clear();
        this.touchedVolumesInSubChunk.clear();
        this.chunksPreChange.clear();
        this.chunksToRebuild.clear();
        this.chunksToUnload.clear();

        SchematicHolder.getInstance().clearLoadedSchematics();
    }

    public void loadPlacementFromFile(File file)
    {
        SchematicPlacementUnloaded placement = SchematicPlacementUnloaded.fromFile(file);

        if (placement != null)
        {
            this.loadPlacementFromFile(placement, false);
        }
        else
        {
            MessageUtils.printErrorMessage("litematica.error.schematic_placements.load_fail", file.getName());
        }
    }

    private void loadPlacementFromFile(SchematicPlacementUnloaded placement, boolean isActiveList)
    {
        if (placement != null)
        {
            if (isActiveList == false && this.checkIsAlreadyLoaded(placement))
            {
                MessageUtils.printErrorMessage("litematica.error.schematic_placements.load_fail.already_loaded", placement.getName());
                return;
            }

            if (placement.isEnabled())
            {
                SchematicPlacement loadedPlacement = placement.fullyLoadPlacement();

                if (loadedPlacement != null)
                {
                    this.addSchematicPlacement(loadedPlacement, false, true);
                }
                // Failed to load the schematic, or it did not have a file (that should never happen)
                else
                {
                    placement.enabled = false;
                    this.lightlyLoadedPlacements.add(placement);
                }
            }
            else
            {
                this.lightlyLoadedPlacements.add(placement);
            }

            if (isActiveList == false)
            {
                MessageUtils.showGuiOrInGameMessage(MessageOutput.SUCCESS, "litematica.message.schematic_placement_loaded", placement.getName());
            }
        }
    }

    private boolean checkIsAlreadyLoaded(SchematicPlacementUnloaded placement)
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

        for (SchematicPlacementUnloaded entry : this.lightlyLoadedPlacements)
        {
            if (placement.placementSaveFile.equals(entry.placementSaveFile))
            {
                return true;
            }
        }

        return false;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        List<SchematicPlacementUnloaded> list = this.getAllSchematicPlacements();

        if (list.isEmpty() == false)
        {
            JsonArray arr = new JsonArray();
            int selectedIndex = 0;
            boolean indexValid = false;

            for (SchematicPlacementUnloaded placement : list)
            {
                if (placement.shouldBeSaved() == false)
                {
                    continue;
                }

                JsonObject objPlacement = placement.toJson();

                if (objPlacement != null)
                {
                    arr.add(objPlacement);

                    if (this.selectedPlacement == placement && placement.isEnabled())
                    {
                        indexValid = true;
                    }
                    else if (indexValid == false && placement.isLoaded() && placement.isEnabled())
                    {
                        selectedIndex++;
                    }
                }
            }

            obj.add("placements", arr);

            if (indexValid)
            {
                obj.add("selected", new JsonPrimitive(selectedIndex));
                obj.add("origin_selected", new JsonPrimitive(true));
            }
        }

        return obj;
    }

    public void loadFromJson(JsonObject obj)
    {
        this.clear();

        if (JsonUtils.hasArray(obj, "placements"))
        {
            JsonArray arr = obj.get("placements").getAsJsonArray();
            int index = JsonUtils.hasInteger(obj, "selected") ? obj.get("selected").getAsInt() : -1;
            final int size = arr.size();

            for (int i = 0; i < size; ++i)
            {
                JsonElement el = arr.get(i);

                if (el.isJsonObject())
                {
                    SchematicPlacementUnloaded placement = SchematicPlacementUnloaded.fromJson(el.getAsJsonObject());
                    this.loadPlacementFromFile(placement, true);
                }
                else
                {
                    // Invalid data in the array, don't select an entry
                    index = -1;
                }
            }

            if (index >= 0 && index < this.schematicPlacements.size())
            {
                this.selectedPlacement = this.schematicPlacements.get(index);
            }
        }

        OverlayRenderer.getInstance().updatePlacementCache();
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
