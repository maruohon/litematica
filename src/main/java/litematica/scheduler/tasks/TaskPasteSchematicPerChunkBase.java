package litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import com.google.common.collect.ArrayListMultimap;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.client.multiplayer.WorldClient;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.StringUtils;
import malilib.util.game.wrap.WorldWrap;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.position.ChunkPos;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.LayerRange;
import malilib.util.position.PositionUtils;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.render.infohud.IInfoHudRenderer;
import litematica.render.infohud.InfoHud;
import litematica.schematic.placement.SchematicPlacement;
import litematica.util.PositionUtils.ChunkPosComparator;
import litematica.util.value.ReplaceBehavior;
import litematica.world.SchematicWorldHandler;
import litematica.world.WorldSchematic;

public abstract class TaskPasteSchematicPerChunkBase extends TaskBase implements IInfoHudRenderer
{
    protected final ArrayListMultimap<ChunkPos, IntBoundingBox> boxesInChunks = ArrayListMultimap.create();
    protected final List<ChunkPos> chunks = new ArrayList<>();
    private final HashSet<ChunkPos> individualChunks = new HashSet<>();
    private final Collection<SchematicPlacement> placements;
    private final LayerRange layerRange;
    protected final ChunkPosComparator comparator;
    protected final boolean changedBlockOnly;
    protected final ReplaceBehavior replace;

    public TaskPasteSchematicPerChunkBase(Collection<SchematicPlacement> placements, LayerRange range, boolean changedBlocksOnly)
    {
        this.placements = placements;
        this.layerRange = range;
        this.changedBlockOnly = changedBlocksOnly;
        this.comparator = new ChunkPosComparator();
        this.comparator.setClosestFirst(true);
        this.replace = Configs.Generic.PASTE_REPLACE_BEHAVIOR.getValue();
        this.name = StringUtils.translate("litematica.gui.label.task_name.paste");
    }

    @Override
    public void init()
    {
        for (SchematicPlacement placement : this.placements)
        {
            this.addPlacement(placement, this.layerRange);
        }

        this.chunks.addAll(this.individualChunks);
        this.sortChunkList();

        InfoHud.getInstance().addInfoHudRenderer(this, true);
        this.updateInfoHudLines();
    }

    protected void addPlacement(SchematicPlacement placement, LayerRange range)
    {
        if (placement.isSchematicLoaded() == false)
        {
            MessageDispatcher.error("litematica.message.error.schematic_placement_paste_schematic_not_loaded",
                                    placement.getName());
            return;
        }

        LongSet touchedChunks = placement.getTouchedChunks();

        for (long chunkPosLong : touchedChunks)
        {
            int count = 0;
            int chunkX = PositionUtils.getChunkPosX(chunkPosLong);
            int chunkZ = PositionUtils.getChunkPosZ(chunkPosLong);
            ChunkPos pos = new ChunkPos(chunkX, chunkZ);

            for (IntBoundingBox box : placement.getBoxesWithinChunk(chunkX, chunkZ).values())
            {
                box = range.getClampedBox(box);

                if (box != null)
                {
                    this.boxesInChunks.put(pos, box);
                    ++count;
                }
            }

            if (count > 0)
            {
                this.individualChunks.add(pos);
                this.onChunkAddedForHandling(pos, placement);
            }
        }
    }

    protected void onChunkAddedForHandling(ChunkPos pos, SchematicPlacement placement)
    {
    }

    @Override
    public boolean canExecute()
    {
        return this.chunks.isEmpty() == false &&
               this.mc.world != null &&
               this.mc.player != null &&
               SchematicWorldHandler.getSchematicWorld() != null;
    }

    protected boolean canProcessChunk(ChunkPos pos, WorldSchematic worldSchematic, WorldClient worldClient)
    {
        if (WorldWrap.isClientChunkLoaded(pos.x, pos.z, worldSchematic) == false ||
            DataManager.getSchematicPlacementManager().hasPendingRebuildForChunk(pos.x, pos.z))
        {
            return false;
        }

        // Chunk exists in the schematic world, and all the surrounding chunks are loaded in the client world, good to go
        return this.areSurroundingChunksLoaded(pos, worldClient, 1);
    }

    protected void sortChunkList()
    {
        if (this.chunks.size() > 0)
        {
            this.comparator.setReferencePosition(EntityWrap.getPlayerBlockPos());
            this.chunks.sort(this.comparator);
            this.onChunkListSorted();
        }
    }

    protected void onChunkListSorted()
    {
    }

    protected void updateInfoHudLines()
    {
        List<String> hudLines = new ArrayList<>();
        int maxLines = Math.min(this.chunks.size(), Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue());

        hudLines.add(StringUtils.translate("litematica.title.hud.missing_chunks.schematic_paste", this.chunks.size()));

        for (int i = 0; i < maxLines; ++i)
        {
            ChunkPos pos = this.chunks.get(i);
            hudLines.add(String.format("cx: %5d, cz: %5d (x: %d, z: %d)", pos.x, pos.z, pos.x << 4, pos.z << 4));
        }

        this.infoHudLines = hudLines;
    }
}
