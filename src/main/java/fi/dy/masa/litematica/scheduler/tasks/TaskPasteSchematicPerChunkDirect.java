package fi.dy.masa.litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collection;
import com.google.common.collect.ArrayListMultimap;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.SchematicPlacingUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.WorldUtils;

public class TaskPasteSchematicPerChunkDirect extends TaskPasteSchematicPerChunkBase
{
    private final ArrayListMultimap<ChunkPos, SchematicPlacement> placementsPerChunk = ArrayListMultimap.create();

    public TaskPasteSchematicPerChunkDirect(Collection<SchematicPlacement> placements, LayerRange range, boolean changedBlocksOnly)
    {
        super(placements, range, changedBlocksOnly);
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
        if (super.canExecute() == false || this.mc.isIntegratedServerRunning() == false)
        {
            return false;
        }

        World world = WorldUtils.getBestWorld(this.mc);
        return world != null && world.isClient == false;
    }

    @Override
    public boolean execute()
    {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        ClientWorld worldClient = this.mc.world;
        World world = WorldUtils.getBestWorld(this.mc);
        int processed = 0;
        MinecraftServer server = this.mc.getServer();
        long timeStart = Util.getMeasuringTimeNano();
        long vanillaTickTime = server.lastTickLengths[server.getTicks() % 100];

        this.sortChunkList();

        for (int chunkIndex = 0; chunkIndex < this.chunks.size(); ++chunkIndex)
        {
            ChunkPos pos = this.chunks.get(chunkIndex);
            long currentTime = Util.getMeasuringTimeNano();
            long elapsedTickTime = vanillaTickTime + (currentTime - timeStart);

            if (elapsedTickTime >= 60000000L)
            {
                break;
            }

            if (this.canProcessChunk(pos, worldSchematic, worldClient))
            {
                // New list to avoid CME
                ArrayList<SchematicPlacement> placements = new ArrayList<>(this.placementsPerChunk.get(pos));

                for (SchematicPlacement placement : placements)
                {
                    if (SchematicPlacingUtils.placeToWorldWithinChunk(world, pos, placement, this.replace, false))
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
            InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.schematic_pasted");
        }
        else
        {
            InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.error.schematic_paste_failed");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        super.stop();
    }
}
