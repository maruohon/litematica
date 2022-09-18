package fi.dy.masa.litematica.schematic.placement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.collect.ArrayListMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.infohud.StatusInfoRenderer;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkBase;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkCommand;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkDirect;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicSetblockToMcfunction;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.util.SchematicPlacingUtils;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IConfirmationListener;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.LayerMode;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.SubChunkPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class SchematicPlacementManager
{
    private final List<SchematicPlacement> schematicPlacements = new ArrayList<>();
    private final ArrayListMultimap<ChunkPos, SchematicPlacement> schematicsTouchingChunk = ArrayListMultimap.create();
    private final ArrayListMultimap<SubChunkPos, PlacementPart> touchedVolumesInSubChunk = ArrayListMultimap.create();
    private final Long2ObjectOpenHashMap<Set<SubChunkPos>> touchedSubChunksInChunks = new Long2ObjectOpenHashMap<>();
    private final Set<ChunkPos> chunksToRebuild = new HashSet<>();
    private final Set<ChunkPos> chunkRebuildQueue = new HashSet<>();
    private final LongOpenHashSet chunksToUnload = new LongOpenHashSet();
    private final Set<ChunkPos> chunksPreChange = new HashSet<>();
    private final List<SubChunkPos> visibleSubChunks = new ArrayList<>();
    private SubChunkPos lastVisibleSubChunksSortPos = new SubChunkPos(0, 0, 0);
    private boolean visibleSubChunksNeedsUpdate;

    @Nullable
    private SchematicPlacement selectedPlacement;

    public boolean hasPendingRebuilds()
    {
        return this.chunksToRebuild.isEmpty() == false;
    }

    public boolean hasPendingRebuildFor(ChunkPos pos)
    {
        return this.chunksToRebuild.contains(pos);
    }

    public void setVisibleSubChunksNeedsUpdate()
    {
        this.visibleSubChunksNeedsUpdate = true;
    }

    public void processQueuedChunks()
    {
        if (this.chunksToUnload.isEmpty() == false)
        {
            WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

            if (worldSchematic != null)
            {
                for (long posLong : this.chunksToUnload)
                {
                    this.unloadSchematicChunk(worldSchematic, ChunkPos.getPackedX(posLong), ChunkPos.getPackedZ(posLong));
                }
            }

            this.chunksToUnload.clear();
        }

        //System.out.printf("processQueuedChunks, size: %d\n", this.chunksToRebuild.size());
        if (this.chunkRebuildQueue.isEmpty() == false)
        {
            ClientWorld worldClient = MinecraftClient.getInstance().world;

            if (worldClient == null)
            {
                this.chunksToRebuild.clear();
                this.chunkRebuildQueue.clear();
                return;
            }

            WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
            Iterator<ChunkPos> queueIterator = this.chunkRebuildQueue.iterator();

            while (queueIterator.hasNext())
            {
                if ((System.nanoTime() - DataManager.getClientTickStartTime()) >= 50000000L)
                {
                    break;
                }

                ChunkPos pos = queueIterator.next();

                if (this.schematicsTouchingChunk.containsKey(pos) == false)
                {
                    queueIterator.remove();
                    this.chunksToRebuild.remove(pos);
                    continue;
                }

                if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() ||
                    WorldUtils.isClientChunkLoaded(worldClient, pos.x, pos.z))
                {
                    // Wipe the old chunk if it exists
                    this.unloadSchematicChunk(worldSchematic, pos.x, pos.z);

                    //System.out.printf("loading chunk at %s\n", pos);
                    worldSchematic.getChunkProvider().loadChunk(pos.x, pos.z);
                    this.visibleSubChunksNeedsUpdate = true;
                }

                if (worldSchematic.getChunkProvider().isChunkLoaded(pos.x, pos.z))
                {
                    //System.out.printf("placing at %s\n", pos);
                    Collection<SchematicPlacement> placements = this.schematicsTouchingChunk.get(pos);

                    if (placements.isEmpty() == false)
                    {
                        for (SchematicPlacement placement : placements)
                        {
                            if (placement.isEnabled())
                            {
                                SchematicPlacingUtils.placeToWorldWithinChunk(worldSchematic, pos, placement, ReplaceBehavior.WITH_NON_AIR, false);
                            }
                        }

                        worldSchematic.scheduleChunkRenders(pos.x, pos.z);
                    }

                    this.chunksToRebuild.remove(pos);
                }

                queueIterator.remove();
            }

            LitematicaRenderer.getInstance().getWorldRenderer().markNeedsUpdate();
        }
    }

    public void onClientChunkLoad(int chunkX, int chunkZ)
    {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        this.chunkRebuildQueue.add(pos);
    }

    public void onClientChunkUnload(int chunkX, int chunkZ)
    {
        if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() == false)
        {
            this.chunksToUnload.add(ChunkPos.toLong(chunkX, chunkZ));
        }
    }

    private void unloadSchematicChunk(WorldSchematic worldSchematic, int chunkX, int chunkZ)
    {
        if (worldSchematic.getChunkProvider().isChunkLoaded(chunkX, chunkZ))
        {
            //System.out.printf("unloading chunk at %d, %d\n", chunkX, chunkZ);
            worldSchematic.getChunkProvider().unloadChunk(chunkX, chunkZ);
            worldSchematic.scheduleChunkRenders(chunkX, chunkZ);
            this.visibleSubChunksNeedsUpdate = true;
        }
    }

    public List<SubChunkPos> getLastVisibleSubChunks()
    {
        return this.visibleSubChunks;
    }

    public List<SubChunkPos> getAndUpdateVisibleSubChunks(SubChunkPos viewSubChunk)
    {
        if (this.visibleSubChunksNeedsUpdate)
        {
            this.visibleSubChunks.clear();
            WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
            LayerRange range = DataManager.getRenderLayerRange();

            if (worldSchematic != null)
            {
                for (long posLong : worldSchematic.getChunkManager().getLoadedChunks().keySet())
                {
                    Set<SubChunkPos> subChunks = this.touchedSubChunksInChunks.get(posLong);

                    if (subChunks != null)
                    {
                        for (SubChunkPos subChunk : subChunks)
                        {
                            if (range.intersects(subChunk))
                            {
                                this.visibleSubChunks.add(subChunk);
                            }
                        }
                    }
                }

                this.visibleSubChunks.sort(new SubChunkPos.DistanceComparator(viewSubChunk));
                this.lastVisibleSubChunksSortPos = viewSubChunk;
            }

            this.visibleSubChunksNeedsUpdate = false;
        }
        else if (viewSubChunk.equals(this.lastVisibleSubChunksSortPos) == false)
        {
            this.visibleSubChunks.sort(new SubChunkPos.DistanceComparator(viewSubChunk));
            this.lastVisibleSubChunksSortPos = viewSubChunk;
        }

        return this.visibleSubChunks;
    }

    public List<SchematicPlacement> getAllSchematicsPlacements()
    {
        return this.schematicPlacements;
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
        if (this.schematicPlacements.contains(placement) == false)
        {
            this.schematicPlacements.add(placement);
            this.addTouchedChunksFor(placement);
            StatusInfoRenderer.getInstance().startOverrideDelay();

            if (printMessages)
            {
                InfoUtils.showGuiMessage(MessageType.SUCCESS, StringUtils.translate("litematica.message.schematic_placement_created", placement.getName()));

                if (Configs.InfoOverlays.WARN_DISABLED_RENDERING.getBooleanValue())
                {
                    LayerMode mode = DataManager.getRenderLayerRange().getLayerMode();

                    if (mode != LayerMode.ALL)
                    {
                        InfoUtils.showGuiAndInGameMessage(MessageType.WARNING, "litematica.message.warn.layer_mode_currently_at", mode.getDisplayName());
                    }

                    if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() == false)
                    {
                        ConfigHotkey hotkey = Hotkeys.TOGGLE_ALL_RENDERING;
                        String configName = Configs.Visuals.ENABLE_RENDERING.getName();
                        String hotkeyName = hotkey.getName();
                        String hotkeyVal = hotkey.getKeybind().getKeysDisplayString();

                        InfoUtils.showGuiAndInGameMessage(MessageType.WARNING, 8000,
                                "litematica.message.warn.main_rendering_disabled", configName, hotkeyName, hotkeyVal);
                    }

                    if (Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() == false)
                    {
                        ConfigHotkey hotkey = Hotkeys.TOGGLE_SCHEMATIC_RENDERING;
                        String configName = Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getName();
                        String hotkeyName = hotkey.getName();
                        String hotkeyVal = hotkey.getKeybind().getKeysDisplayString();

                        InfoUtils.showGuiAndInGameMessage(MessageType.WARNING, 8000,
                                "litematica.message.warn.schematic_rendering_disabled", configName, hotkeyName, hotkeyVal);
                    }

                    if (Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue() == false)
                    {
                        ConfigHotkey hotkey = Hotkeys.TOGGLE_SCHEMATIC_BLOCK_RENDERING;
                        String configName = Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getName();
                        String hotkeyName = hotkey.getName();
                        String hotkeyVal = hotkey.getKeybind().getKeysDisplayString();

                        InfoUtils.showGuiAndInGameMessage(MessageType.WARNING, 8000,
                                "litematica.message.warn.schematic_blocks_rendering_disabled", configName, hotkeyName, hotkeyVal);
                    }
                }
            }
        }
        else if (printMessages)
        {
            InfoUtils.showGuiAndInGameMessage(MessageType.ERROR, "litematica.error.duplicate_schematic_placement");
        }
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

        boolean ret = this.schematicPlacements.remove(placement);
        this.removeTouchedChunksFor(placement);

        if (ret)
        {
            placement.onRemoved();

            if (update)
            {
                this.onPlacementModified(placement);
            }
        }

        return ret;
    }

    public List<SchematicPlacement> getAllPlacementsOfSchematic(LitematicaSchematic schematic)
    {
        List<SchematicPlacement> list = new ArrayList<>();

        for (SchematicPlacement placement : this.schematicPlacements)
        {
            if (placement.getSchematic() == schematic)
            {
                list.add(placement);
            }
        }

        return list;
    }

    public void removeAllPlacementsOfSchematic(LitematicaSchematic schematic)
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
        if (placement == null || this.schematicPlacements.contains(placement))
        {
            this.selectedPlacement = placement;
            OverlayRenderer.getInstance().updatePlacementCache();
            // Forget the last viewed material list when changing the placement selection
            DataManager.setMaterialList(null);
        }
    }

    private void addTouchedChunksFor(SchematicPlacement placement)
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

                this.chunksToUnload.remove(pos.toLong());
            }

            this.markChunksForRebuild(placement);
            this.onPlacementModified(placement);
        }
    }

    private void removeTouchedChunksFor(SchematicPlacement placement)
    {
        if (placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
        {
            Set<ChunkPos> chunks = placement.getTouchedChunks();
            Iterator<ChunkPos> it = chunks.iterator();

            while (it.hasNext())
            {
                ChunkPos pos = it.next();
                this.schematicsTouchingChunk.remove(pos, placement);
                this.updateTouchedBoxesInChunk(pos);

                if (this.schematicsTouchingChunk.containsKey(pos) == false)
                {
                    this.chunksToUnload.add(pos.toLong());
                    it.remove();
                }
            }

            this.markChunksForRebuild(chunks);
        }
    }

    void onPrePlacementChange(SchematicPlacement placement)
    {
        this.chunksPreChange.clear();
        this.chunksPreChange.addAll(placement.getTouchedChunks());
    }

    void onPostPlacementChange(SchematicPlacement placement)
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
                this.chunksToUnload.add(pos.toLong());
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
        this.onPlacementModified(placement);
    }

    private void updateTouchedBoxesInChunk(ChunkPos pos)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
        int minChunkY = world != null ? world.getBottomSectionCoord() : -4;
        int maxChunkY = world != null ? world.getTopSectionCoord() : 19;

        for (int y = minChunkY; y < maxChunkY; ++y)
        {
            SubChunkPos subChunk = new SubChunkPos(pos.x, y, pos.z);
            this.touchedVolumesInSubChunk.removeAll(subChunk);
        }

        long posLong = pos.toLong();
        this.touchedSubChunksInChunks.remove(posLong);

        Collection<SchematicPlacement> placements = this.schematicsTouchingChunk.get(pos);

        if (placements.isEmpty() == false)
        {
            Set<SubChunkPos> subChunks = new HashSet<>();

            for (SchematicPlacement placement : placements)
            {
                if (placement.matchesRequirement(RequiredEnabled.RENDERING_ENABLED))
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
                            SubChunkPos subPos = new SubChunkPos(pos.x, cy, pos.z);
                            this.touchedVolumesInSubChunk.put(subPos, part);
                            subChunks.add(subPos);
                            //System.out.printf("updateTouchedBoxesInChunk box at %d, %d, %d: %s\n", pos.x, cy, pos.z, bbSub);
                        }
                    }
                }
            }

            this.touchedSubChunksInChunks.put(posLong, subChunks);
        }
    }

    public void markAllPlacementsOfSchematicForRebuild(LitematicaSchematic schematic)
    {
        for (int i = 0; i < this.schematicPlacements.size(); ++i)
        {
            SchematicPlacement placement = this.schematicPlacements.get(i);

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

    void markChunksForRebuild(Collection<ChunkPos> chunks)
    {
        //System.out.printf("rebuilding %d chunks: %s\n", chunks.size(), chunks);
        this.chunksToRebuild.addAll(chunks);
        this.chunkRebuildQueue.addAll(chunks);
    }

    public void markChunkForRebuild(ChunkPos pos)
    {
        this.chunksToRebuild.add(pos);
        this.chunkRebuildQueue.add(pos);
    }

    private void onPlacementModified(SchematicPlacement placement)
    {
        if (placement.isEnabled())
        {
            OverlayRenderer.getInstance().updatePlacementCache();
        }
    }

    public boolean changeSelection(World world, Entity entity, int maxDistance)
    {
        if (this.schematicPlacements.size() > 0)
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

                boolean selectSubRegion = Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld();
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

    public void setPositionOfCurrentSelectionToRayTrace(MinecraftClient mc, double maxDistance)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            HitResult trace = RayTraceUtils.getRayTraceFromEntity(mc.world, entity, false, maxDistance);

            if (trace.getType() != HitResult.Type.BLOCK)
            {
                return;
            }

            BlockPos pos = ((BlockHitResult) trace).getBlockPos();

            // Sneaking puts the position inside the targeted block, not sneaking puts it against the targeted face
            if (mc.player.isSneaking() == false)
            {
                pos = pos.offset(((BlockHitResult) trace).getSide());
            }

            this.setPositionOfCurrentSelectionTo(pos, mc);
        }
    }

    public void setPositionOfCurrentSelectionTo(BlockPos pos, MinecraftClient mc)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            if (schematicPlacement.isLocked())
            {
                InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
                return;
            }

            boolean movingBox = schematicPlacement.getSelectedSubRegionPlacement() != null;

            if (movingBox)
            {
                schematicPlacement.moveSubRegionTo(schematicPlacement.getSelectedSubRegionName(), pos, InfoUtils.INFO_MESSAGE_CONSUMER);

                String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.placement.moved_subregion_to", posStr);
            }
            // Moving the origin point
            else
            {
                BlockPos old = schematicPlacement.getOrigin();
                schematicPlacement.setOrigin(pos, InfoUtils.INFO_MESSAGE_CONSUMER);

                if (old.equals(schematicPlacement.getOrigin()) == false)
                {
                    String posStrOld = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
                    String posStrNew = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.placement.moved_placement_origin", posStrOld, posStrNew);
                }
            }
        }
    }

    public void nudgePositionOfCurrentSelection(Direction direction, int amount)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            if (schematicPlacement.isLocked())
            {
                InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
                return;
            }

            SubRegionPlacement placement = schematicPlacement.getSelectedSubRegionPlacement();

            // Moving a sub-region
            if (placement != null)
            {
                // getPos returns a relative position, but moveSubRegionTo takes an absolute position...
                BlockPos old = PositionUtils.getTransformedBlockPos(placement.getPos(), schematicPlacement.getMirror(), schematicPlacement.getRotation());
                old = old.add(schematicPlacement.getOrigin());

                schematicPlacement.moveSubRegionTo(placement.getName(), old.offset(direction, amount), InfoUtils.INFO_MESSAGE_CONSUMER);
            }
            // Moving the origin point
            else
            {
                BlockPos old = schematicPlacement.getOrigin();
                schematicPlacement.setOrigin(old.offset(direction, amount), InfoUtils.INFO_MESSAGE_CONSUMER);
            }
        }
    }

    public void pasteCurrentPlacementToWorld(MinecraftClient mc)
    {
        this.pastePlacementToWorld(this.getSelectedSchematicPlacement(), mc);
    }

    public void pastePlacementToWorld(final SchematicPlacement schematicPlacement, MinecraftClient mc)
    {
        this.pastePlacementToWorld(schematicPlacement, true, mc);
    }

    public void pastePlacementToWorld(final SchematicPlacement schematicPlacement, boolean changedBlocksOnly, MinecraftClient mc)
    {
        this.pastePlacementToWorld(schematicPlacement, changedBlocksOnly, true, mc);
    }

    private static class PasteToCommandsListener implements IConfirmationListener
    {
        private final SchematicPlacement schematicPlacement;
        private final boolean changedBlocksOnly;

        public PasteToCommandsListener(SchematicPlacement schematicPlacement, boolean changedBlocksOnly)
        {
            this.schematicPlacement = schematicPlacement;
            this.changedBlocksOnly = changedBlocksOnly;
        }

        @Override
        public boolean onActionConfirmed()
        {
            LayerRange range = DataManager.getRenderLayerRange();
            TaskPasteSchematicSetblockToMcfunction task = new TaskPasteSchematicSetblockToMcfunction(Collections.singletonList(this.schematicPlacement), range, this.changedBlocksOnly);
            TaskScheduler.getInstanceClient().scheduleTask(task, 1);
            return true;
        }

        @Override
        public boolean onActionCancelled()
        {
            return true;
        }
    }

    public void pastePlacementToWorld(final SchematicPlacement schematicPlacement, boolean changedBlocksOnly, boolean printMessage, MinecraftClient mc)
    {
        if (mc.player != null && EntityUtils.isCreativeMode(mc.player))
        {
            if (schematicPlacement != null)
            {
                /*
                if (PositionUtils.isPlacementWithinWorld(mc.world, schematicPlacement, false) == false)
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.placement_paste_outside_world");
                    return;
                }
                */
                LayerRange range = DataManager.getRenderLayerRange();

                if (Configs.Generic.PASTE_TO_MCFUNCTION.getBooleanValue())
                {
                    PasteToCommandsListener cl = new PasteToCommandsListener(schematicPlacement, changedBlocksOnly);
                    GuiConfirmAction screen = new GuiConfirmAction(320, "Confirm paste to command files", cl, null, "Are you sure you want to paste the current placement as setblock commands into command/mcfunction files?");
                    GuiBase.openGui(screen);
                }
                else if (mc.isIntegratedServerRunning())
                {
                    TaskPasteSchematicPerChunkBase task = new TaskPasteSchematicPerChunkDirect(Collections.singletonList(schematicPlacement), range, changedBlocksOnly);
                    TaskScheduler.getInstanceServer().scheduleTask(task, Configs.Generic.COMMAND_TASK_INTERVAL.getIntegerValue());

                    if (printMessage)
                    {
                        InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
                    }
                }
                else
                {
                    TaskPasteSchematicPerChunkBase task = new TaskPasteSchematicPerChunkCommand(Collections.singletonList(schematicPlacement), range, changedBlocksOnly);
                    TaskScheduler.getInstanceClient().scheduleTask(task, Configs.Generic.COMMAND_TASK_INTERVAL.getIntegerValue());

                    if (printMessage)
                    {
                        InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
                    }
                }
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
        }
    }

    public void clear()
    {
        this.schematicPlacements.clear();
        this.selectedPlacement = null;
        this.schematicsTouchingChunk.clear();
        this.touchedVolumesInSubChunk.clear();
        this.touchedSubChunksInChunks.clear();
        this.chunksPreChange.clear();
        this.chunksToRebuild.clear();
        this.chunkRebuildQueue.clear();
        this.chunksToUnload.clear();

        SchematicHolder.getInstance().clearLoadedSchematics();
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.schematicPlacements.size() > 0)
        {
            JsonArray arr = new JsonArray();
            int selectedIndex = 0;
            boolean indexValid = false;

            for (int i = 0; i < this.schematicPlacements.size(); ++i)
            {
                SchematicPlacement placement = this.schematicPlacements.get(i);

                if (placement.shouldBeSaved() == false)
                {
                    continue;
                }

                JsonObject objPlacement = placement.toJson();

                if (objPlacement != null)
                {
                    arr.add(objPlacement);

                    if (this.selectedPlacement == placement)
                    {
                        indexValid = true;
                    }
                    else if (indexValid == false)
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
                    SchematicPlacement placement = SchematicPlacement.fromJson(el.getAsJsonObject());

                    if (placement != null)
                    {
                        this.addSchematicPlacement(placement, false);
                    }
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
