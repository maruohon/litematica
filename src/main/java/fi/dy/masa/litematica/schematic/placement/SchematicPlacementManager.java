package fi.dy.masa.litematica.schematic.placement;

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
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.util.SubChunkPos;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.interfaces.IMessageConsumer;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class SchematicPlacementManager
{
    private final List<SchematicPlacement> schematicPlacements = new ArrayList<>();
    private final HashMultimap<ChunkPos, SchematicPlacement> schematicsTouchingChunk = HashMultimap.create();
    private final ArrayListMultimap<SubChunkPos, PlacementPart> touchedVolumesInSubChunk = ArrayListMultimap.create();
    private final Set<ChunkPos> chunksToRebuild = new HashSet<>();
    private final Set<ChunkPos> chunksToUnload = new HashSet<>();
    private final Set<ChunkPos> chunksPreChange = new HashSet<>();

    @Nullable
    private SchematicPlacement selectedPlacement;

    public boolean processQueuedChunks()
    {
        if (this.chunksToUnload.isEmpty() == false)
        {
            WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

            for (ChunkPos pos : this.chunksToUnload)
            {
                this.unloadSchematicChunk(worldSchematic, pos.x, pos.z);
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
                                placement.getSchematic().placeToWorldWithinChunk(worldSchematic, pos, placement, false);
                            }
                        }

                        worldSchematic.markBlockRangeForRenderUpdate(pos.x << 4, 0, pos.z << 4, (pos.x << 4) + 15, 256, (pos.z << 4) + 15);
                    }

                    iter.remove();
                }
            }

            if (this.chunksToRebuild.isEmpty())
            {
                return true;
            }
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

    public List<SchematicPlacement> getAllSchematicsPlacements()
    {
        return this.schematicPlacements;
    }

    public List<StructureBoundingBox> getTouchedBoxesInSubChunk(SubChunkPos subChunk)
    {
        List<StructureBoundingBox> list = new ArrayList<>();

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

    public void addSchematicPlacement(SchematicPlacement placement, @Nullable IMessageConsumer messageConsumer)
    {
        if (this.schematicPlacements.contains(placement) == false)
        {
            this.schematicPlacements.add(placement);
            this.addTouchedChunksFor(placement);

            if (messageConsumer != null)
            {
                messageConsumer.addMessage(MessageType.SUCCESS, I18n.format("litematica.message.schematic_placement_created", placement.getName()));
            }
        }
        else if (messageConsumer != null)
        {
            messageConsumer.addMessage(MessageType.ERROR, I18n.format("litematica.error.duplicate_schematic_load"));
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

                this.chunksToUnload.remove(pos);
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
        this.onPlacementModified(placement);
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
                if (placement.matchesRequirement(RequiredEnabled.RENDERING_ENABLED))
                {
                    Map<String, StructureBoundingBox> boxMap = placement.getBoxesWithinChunk(pos.x, pos.z);

                    for (Map.Entry<String, StructureBoundingBox> entry : boxMap.entrySet())
                    {
                        StructureBoundingBox bbOrig = entry.getValue();
                        final int startCY = (bbOrig.minY >> 4);
                        final int endCY = (bbOrig.maxY >> 4);

                        for (int cy = startCY; cy <= endCY; ++cy)
                        {
                            int y1 = Math.max((cy << 4)     , bbOrig.minY);
                            int y2 = Math.min((cy << 4) + 15, bbOrig.maxY);

                            StructureBoundingBox bbSub = new StructureBoundingBox(bbOrig.minX, y1, bbOrig.minZ, bbOrig.maxX, y2, bbOrig.maxZ);
                            PlacementPart part = new PlacementPart(placement, entry.getKey(), bbSub);
                            this.touchedVolumesInSubChunk.put(new SubChunkPos(pos.x, cy, pos.z), part);
                            //System.out.printf("updateTouchedBoxesInChunk box at %d, %d, %d: %s\n", pos.x, cy, pos.z, bbSub);
                        }
                    }
                }
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
    }

    public void markChunkForRebuild(ChunkPos pos)
    {
        this.chunksToRebuild.add(pos);
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

    public void setPositionOfCurrentSelectionToRayTrace(Minecraft mc, double maxDistance)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            RayTraceResult trace = RayTraceUtils.getRayTraceFromEntity(mc.world, mc.player, false, maxDistance);

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

            this.setPositionOfCurrentSelectionTo(pos, mc);
        }
    }

    public void setPositionOfCurrentSelectionTo(BlockPos pos, Minecraft mc)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            if (schematicPlacement.isLocked())
            {
                InfoUtils.showMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
                return;
            }

            boolean movingBox = schematicPlacement.getSelectedSubRegionPlacement() != null;

            if (movingBox)
            {
                schematicPlacement.moveSubRegionTo(schematicPlacement.getSelectedSubRegionName(), pos, InfoUtils.INFO_MESSAGE_CONSUMER);

                String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                InfoUtils.showMessage(MessageType.SUCCESS, "litematica.message.placement.moved_subregion_to", posStr);
            }
            // Moving the origin point
            else
            {
                BlockPos old = schematicPlacement.getOrigin();
                schematicPlacement.setOrigin(pos, InfoUtils.INFO_MESSAGE_CONSUMER);
                String posStrOld = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
                String posStrNew = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                InfoUtils.showMessage(MessageType.SUCCESS, "litematica.message.placement.moved_placement_origin", posStrOld, posStrNew);
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
                InfoUtils.showMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
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

    public void pasteCurrentPlacementToWorld(Minecraft mc)
    {
        if (mc.player != null && mc.player.capabilities.isCreativeMode)
        {
            final SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

            if (schematicPlacement != null)
            {
                if (mc.isSingleplayer())
                {
                    final WorldServer world = mc.getIntegratedServer().getWorld(mc.player.getEntityWorld().provider.getDimensionType().getId());
                    final LitematicaSchematic schematic = schematicPlacement.getSchematic();

                    world.addScheduledTask(new Runnable()
                    {
                        public void run()
                        {
                            if (schematic.placeToWorld(world, schematicPlacement, false))
                            {
                                InfoUtils.showMessage(MessageType.SUCCESS, "litematica.message.schematic_pasted");
                            }
                            else
                            {
                                InfoUtils.showMessage(MessageType.ERROR, "litematica.message.error.schematic_paste_failed");
                            }
                        }
                    });

                    InfoUtils.showMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
                }
                else
                {
                    InfoUtils.showMessage(MessageType.ERROR, "litematica.message.error.only_works_in_single_player");
                }
            }
            else
            {
                InfoUtils.showMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
            }
        }
    }

    public void clear()
    {
        this.schematicPlacements.clear();
        this.selectedPlacement = null;
        this.schematicsTouchingChunk.clear();
        this.touchedVolumesInSubChunk.clear();
        this.chunksPreChange.clear();
        this.chunksToRebuild.clear();
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
                        this.addSchematicPlacement(placement, null);
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
        private final StructureBoundingBox bb;

        public PlacementPart(SchematicPlacement placement, String subRegionName, StructureBoundingBox bb)
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

        public StructureBoundingBox getBox()
        {
            return this.bb;
        }
    }
}
