package fi.dy.masa.litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.common.collect.ArrayListMultimap;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import malilib.util.StringUtils;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.LayerRange;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.PositionUtils.ChunkPosComparator;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

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
        Set<ChunkPos> touchedChunks = placement.getTouchedChunks();

        for (ChunkPos pos : touchedChunks)
        {
            int count = 0;

            for (IntBoundingBox box : placement.getBoxesWithinChunk(pos.x, pos.z).values())
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

    protected boolean canProcessChunk(ChunkPos pos, World worldSchematic, WorldClient worldClient)
    {
        if (worldSchematic.getChunkProvider().isChunkGeneratedAt(pos.x, pos.z) == false ||
            DataManager.getSchematicPlacementManager().hasPendingRebuildFor(pos))
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
            if (this.mc.player != null)
            {
                this.comparator.setReferencePosition(EntityWrap.getEntityBlockPos(this.mc.player));
                this.chunks.sort(this.comparator);
            }

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
