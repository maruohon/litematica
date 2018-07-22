package fi.dy.masa.litematica.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.Placement.RequiredEnabled;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase.InfoType;
import fi.dy.masa.litematica.gui.interfaces.IMessageConsumer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.JsonUtils;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class SchematicPlacementManager
{
    private final List<SchematicPlacement> schematicPlacements = new ArrayList<>();
    private final HashMultimap<ChunkPos, SchematicPlacement> schematicsTouchingChunk = HashMultimap.create();
    private final Set<ChunkPos> chunksToRebuild = new HashSet<>();
    private final Set<ChunkPos> chunksToUnload = new HashSet<>();
    private final Set<ChunkPos> chunksPreChange = new HashSet<>();
    private boolean rebuildActive;

    @Nullable
    private SchematicPlacement selectedPlacement;

    public boolean processQueuedChunks()
    {
        if (this.rebuildActive && this.chunksToUnload.isEmpty() == false)
        {
            WorldSchematic worldSchematic = SchematicWorldHandler.getInstance().getSchematicWorld();

            for (ChunkPos pos : this.chunksToUnload)
            {
                worldSchematic.getChunkProvider().unloadChunk(pos.x, pos.z);
                worldSchematic.markBlockRangeForRenderUpdate(pos.x << 4, 0, pos.z << 4, (pos.x << 4) + 15, 256, (pos.z << 4) + 15);
            }

            this.chunksToUnload.clear();
        }

        if (this.rebuildActive && this.chunksToRebuild.isEmpty() == false)
        {
            WorldClient worldClient = Minecraft.getMinecraft().world;
            WorldSchematic worldSchematic = SchematicWorldHandler.getInstance().getSchematicWorld();
            Iterator<ChunkPos> iter = this.chunksToRebuild.iterator();

            while (iter.hasNext())
            {
                if ((System.nanoTime() - DataManager.getClientTickStartTime()) >= 50000000L)
                {
                    break;
                }

                ChunkPos pos = iter.next();

                if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() ||
                    worldClient.getChunkProvider().isChunkGeneratedAt(pos.x, pos.z))
                {
                    // Wipe the old chunk if it exists
                    if (worldSchematic.getChunkProvider().isChunkGeneratedAt(pos.x, pos.z))
                    {
                        worldSchematic.getChunkProvider().unloadChunk(pos.x, pos.z);
                    }

                    worldSchematic.getChunkProvider().loadChunk(pos.x, pos.z);
                }

                if (worldSchematic.getChunkProvider().isChunkGeneratedAt(pos.x, pos.z))
                {
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
                }

                // Always remove the entry. If the chunk wasn't loaded, it will get handled when it loads the next time.
                iter.remove();
            }

            if (this.chunksToRebuild.isEmpty())
            {
                this.rebuildActive = false;
                return true;
            }
        }

        return false;
    }

    public List<SchematicPlacement> getAllSchematicsPlacements()
    {
        return this.schematicPlacements;
    }

    public void addSchematicPlacement(SchematicPlacement placement, @Nullable IMessageConsumer messageConsumer)
    {
        if (this.schematicPlacements.contains(placement) == false)
        {
            this.schematicPlacements.add(placement);
            this.addTouchedChunksFor(placement);

            if (messageConsumer != null)
            {
                messageConsumer.addMessage(InfoType.SUCCESS, I18n.format("litematica.message.schematic_placement_created", placement.getName()));
            }
        }
        else if (messageConsumer != null)
        {
            messageConsumer.addMessage(InfoType.ERROR, I18n.format("litematica.error.duplicate_schematic_load"));
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
        }
    }

    private void addTouchedChunksFor(SchematicPlacement placement)
    {
        if (placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
        {
            for (ChunkPos pos : placement.getTouchedChunks())
            {
                if (this.schematicsTouchingChunk.containsEntry(pos, placement) == false)
                {
                    this.schematicsTouchingChunk.put(pos, placement);
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
        Set<ChunkPos> toRebuild = new HashSet<>();
        toRebuild.addAll(chunksPost);
        //System.out.printf("chunkPre: %s - chunkPost: %s\n", this.chunksPreChange, chunksPost);
        this.chunksPreChange.removeAll(chunksPost);

        for (ChunkPos pos : this.chunksPreChange)
        {
            this.schematicsTouchingChunk.remove(pos, placement);
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
        }

        this.markChunksForRebuild(toRebuild);
        this.onPlacementModified(placement);
    }

    void markChunksForRebuild(SchematicPlacement placement)
    {
        if (placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
        {
            this.markChunksForRebuild(placement.getTouchedChunks());
        }
    }

    void markChunksForRebuild(Collection<ChunkPos> chunks)
    {
        //System.out.printf("rebuilding: %s\n", chunks);
        this.chunksToRebuild.addAll(chunks);
        this.rebuildActive = true;
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

            if (trace.getHitType() == HitType.PLACEMENT_SUBREGION)
            {
                this.setSelectedSchematicPlacement(trace.getHitSchematicPlacement());
                this.getSelectedSchematicPlacement().setSelectedSubRegionName(trace.getHitSchematicPlacementRegionName());
                return true;
            }
            else if (trace.getHitType() == HitType.PLACEMENT_ORIGIN)
            {
                this.setSelectedSchematicPlacement(trace.getHitSchematicPlacement());
                this.getSelectedSchematicPlacement().setSelectedSubRegionName(null);
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
            boolean movingBox = schematicPlacement.getSelectedSubRegionPlacement() != null;

            if (movingBox)
            {
                schematicPlacement.moveSubRegionTo(schematicPlacement.getSelectedSubRegionName(), pos);

                String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                StringUtils.printActionbarMessage("litematica.message.placement.moved_subregion_to", posStr);
            }
            // Moving the origin point
            else
            {
                BlockPos old = schematicPlacement.getOrigin();
                schematicPlacement.setOrigin(pos);
                String posStrOld = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
                String posStrNew = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                StringUtils.printActionbarMessage("litematica.message.placement.moved_placement_origin", posStrOld, posStrNew);
            }
        }
    }

    public void nudgePositionOfCurrentSelection(EnumFacing direction, int amount)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            Placement placement = schematicPlacement.getSelectedSubRegionPlacement();

            // Moving a sub-region
            if (placement != null)
            {
                // getPos returns a relative position, but moveSubRegionTo takes an absolute position...
                BlockPos old = placement.getPos().add(schematicPlacement.getOrigin());
                schematicPlacement.moveSubRegionTo(placement.getName(), old.offset(direction, amount));
            }
            // Moving the origin point
            else
            {
                BlockPos old = schematicPlacement.getOrigin();
                schematicPlacement.setOrigin(old.offset(direction, amount));
            }
        }
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
        this.schematicPlacements.clear();
        this.selectedPlacement = null;

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
                        String schematicName = placement.getSchematic().getMetadata().getName();
                        String fileName = placement.getSchematicFile().getName();

                        SchematicHolder.getInstance().addSchematic(placement.getSchematic(), schematicName, fileName);
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
}
