package litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collection;
import com.google.common.collect.ArrayListMultimap;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.game.wrap.WorldWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.position.ChunkPos;
import malilib.util.position.LayerRange;
import litematica.config.Configs;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.util.SchematicPlacingUtils;
import litematica.util.value.ReplaceBehavior;
import litematica.world.SchematicWorldHandler;
import litematica.world.WorldSchematic;

public class TaskPasteSchematicPerChunkDirect extends TaskPasteSchematicPerChunkBase
{
    private final ArrayListMultimap<ChunkPos, SchematicPlacement> placementsPerChunk = ArrayListMultimap.create();
    private final ReplaceBehavior replace;

    public TaskPasteSchematicPerChunkDirect(Collection<SchematicPlacement> placements, LayerRange range, boolean changedBlocksOnly)
    {
        super(placements, range, changedBlocksOnly);

        this.replace = Configs.Generic.PASTE_REPLACE_BEHAVIOR.getValue();
    }

    @Override
    protected void onChunkAddedForHandling(ChunkPos pos, SchematicPlacement placement)
    {
        super.onChunkAddedForHandling(pos, placement);

        this.placementsPerChunk.put(pos, placement);
    }

    @Override
    public boolean canExecute()
    {
        if (super.canExecute() == false || GameWrap.isSinglePlayer() == false)
        {
            return false;
        }

        World world = WorldWrap.getBestWorld();
        return world != null && world.isRemote == false;
    }

    @Override
    public boolean execute()
    {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        WorldClient worldClient = GameWrap.getClientWorld();
        World world = WorldWrap.getBestWorld();
        int processed = 0;

        this.sortChunkList();

        for (int chunkIndex = 0; chunkIndex < this.chunks.size(); ++chunkIndex)
        {
            ChunkPos pos = this.chunks.get(chunkIndex);

            if (this.canProcessChunk(pos, worldSchematic, worldClient))
            {
                // New list to avoid CME
                ArrayList<SchematicPlacement> placements = new ArrayList<>(this.placementsPerChunk.get(pos));

                for (SchematicPlacement placement : placements)
                {
                    if (placement.isSchematicLoaded() == false ||
                        placement.isValid() == false ||
                        SchematicPlacingUtils.placeToWorldWithinChunk(placement, pos, world, this.replace, false))
                    {
                        this.placementsPerChunk.remove(pos, placement);
                        ++processed;
                    }
                }

                if (this.placementsPerChunk.containsKey(pos) == false)
                {
                    this.chunks.remove(chunkIndex);
                    --chunkIndex;
                }
            }
        }

        if (this.chunks.isEmpty())
        {
            this.finished = true;
            return true;
        }

        if (processed > 0)
        {
            this.updateInfoHudLines();
        }

        return false;
    }

    @Override
    public void stop()
    {
        if (this.finished)
        {
            MessageDispatcher.success().screenOrActionbar().translate("litematica.message.schematic_placements_pasted");
        }
        else
        {
            MessageDispatcher.error().screenOrActionbar().translate("litematica.message.error.schematic_paste_failed");
        }

        super.stop();
    }
}
