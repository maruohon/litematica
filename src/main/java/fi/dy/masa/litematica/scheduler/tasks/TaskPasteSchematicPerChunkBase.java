package fi.dy.masa.litematica.scheduler.tasks;

import java.util.Collection;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.math.ChunkPos;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.ReplaceBehavior;

public abstract class TaskPasteSchematicPerChunkBase extends TaskProcessChunkMultiPhase
{
    protected final ImmutableList<SchematicPlacement> placements;
    protected final LayerRange layerRange;
    protected final ReplaceBehavior replace;
    protected final boolean changedBlockOnly;
    protected boolean ignoreBlocks;
    protected boolean ignoreEntities;

    public TaskPasteSchematicPerChunkBase(Collection<SchematicPlacement> placements,
                                          LayerRange range,
                                          boolean changedBlocksOnly)
    {
        super("litematica.gui.label.task_name.paste");

        this.placements = ImmutableList.copyOf(placements);
        this.layerRange = range;
        this.changedBlockOnly = changedBlocksOnly;
        this.ignoreEntities = Configs.Generic.PASTE_IGNORE_ENTITIES.getBooleanValue();
        this.replace = (ReplaceBehavior) Configs.Generic.PASTE_REPLACE_BEHAVIOR.getOptionListValue();
    }

    @Override
    public void init()
    {
        for (SchematicPlacement placement : this.placements)
        {
            this.addPlacement(placement, this.layerRange);
        }

        this.pendingChunks.clear();
        this.pendingChunks.addAll(this.boxesInChunks.keySet());
        this.sortChunkList();
    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() && this.schematicWorld != null;
    }

    protected void addPlacement(SchematicPlacement placement, LayerRange range)
    {
        Set<ChunkPos> touchedChunks = placement.getTouchedChunks();


        for (ChunkPos pos : touchedChunks)
        {
            int count = 0;

            for (IntBoundingBox box : placement.getBoxesWithinChunk(pos.x, pos.z).values())
            {
                box = PositionUtils.getClampedBox(box, range);

                if (box != null)
                {
                    // Clamp the box to the world bounds.
                    // This is also important for the fill-based strip generation code to not
                    // overflow the work array bounds.
                    box = PositionUtils.clampBoxToWorldHeightRange(box, this.clientWorld);

                    if (box != null)
                    {
                        this.boxesInChunks.put(pos, box);
                        ++count;
                    }
                }
            }

            if (count > 0)
            {
                this.onChunkAddedForHandling(pos, placement);
            }
        }
    }

    protected void onChunkAddedForHandling(ChunkPos pos, SchematicPlacement placement)
    {
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        if (this.schematicWorld.getChunkProvider().isChunkLoaded(pos.x, pos.z) == false ||
            DataManager.getSchematicPlacementManager().hasPendingRebuildFor(pos))
        {
            return false;
        }

        // Chunk exists in the schematic world, and all the surrounding chunks are loaded in the client world, good to go
        return this.areSurroundingChunksLoaded(pos, this.clientWorld, 1);
    }
}
