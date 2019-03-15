package fi.dy.masa.litematica.scheduler.tasks;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class TaskSaveSchematic extends TaskBase implements IInfoHudRenderer
{
    private final LitematicaSchematic schematic;
    private final BlockPos origin;
    private final World world;
    private final ImmutableMap<String, Box> subRegions;
    private final Set<ChunkPos> requiredChunks = new HashSet<>();
    private final Set<UUID> existingEntities = new HashSet<>();
    private final boolean isClientWorld;
    @Nullable private final File dir;
    @Nullable private final String fileName;
    private final boolean takeEntities;
    private final boolean overrideFile;
    private boolean finished;

    public TaskSaveSchematic(LitematicaSchematic schematic, AreaSelection area, boolean takeEntities)
    {
        this(null, null, schematic, area, takeEntities, false);
    }

    public TaskSaveSchematic(@Nullable File dir, @Nullable String fileName, LitematicaSchematic schematic, AreaSelection area, boolean takeEntities, boolean overrideFile)
    {
        this.schematic = schematic;
        this.origin = area.getEffectiveOrigin();
        this.subRegions = area.getAllSubRegions();
        this.requiredChunks.addAll(PositionUtils.getTouchedChunksForBoxes(area.getAllSubRegionBoxes()));
        this.dir = dir;
        this.fileName = fileName;
        this.takeEntities = takeEntities;
        this.overrideFile = overrideFile;
        this.world = WorldUtils.getBestWorld(this.mc);
        this.isClientWorld = (this.world == this.mc.world);

        this.updateInfoHudLinesMissingChunks(this.requiredChunks);
        InfoHud.getInstance().addInfoHudRenderer(this, true);
    }

    @Override
    public boolean execute()
    {
        World worldClient = this.mc.world;

        if (this.world != null && worldClient != null)
        {
            Iterator<ChunkPos> iter = this.requiredChunks.iterator();
            int processed = 0;

            while (iter.hasNext())
            {
                ChunkPos pos = iter.next();

                // All neighbor chunks loaded
                if (this.areSurroundingChunksLoaded(pos, worldClient, 1))
                {
                    ImmutableMap<String, StructureBoundingBox> volumes = PositionUtils.getBoxesWithinChunk(pos.x, pos.z, this.subRegions);
                    this.schematic.takeBlocksFromWorldWithinChunk(this.world, pos.x, pos.z, volumes, this.subRegions);

                    if (this.takeEntities)
                    {
                        this.schematic.takeEntitiesFromWorldWithinChunk(this.world, pos.x, pos.z, volumes, this.subRegions, this.existingEntities, this.origin);
                    }

                    iter.remove();
                    processed++;
                }
            }

            if (processed > 0)
            {
                this.updateInfoHudLinesMissingChunks(this.requiredChunks);
            }
        }

        this.finished = this.requiredChunks.isEmpty();

        return this.finished;
    }

    @Override
    public void stop()
    {
        // Multiplayer, just a client world
        if (this.isClientWorld)
        {
            this.onStop();
        }
        // Single player, saving from the integrated server world
        else
        {
            this.mc.addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    TaskSaveSchematic.this.onStop();
                }
            });
        }
    }

    private void onStop()
    {
        if (this.finished)
        {
            long time = (new Date()).getTime();
            this.schematic.getMetadata().setTimeCreated(time);
            this.schematic.getMetadata().setTimeModified(time);
            this.schematic.getMetadata().setTotalBlocks(this.schematic.getTotalBlocks());

            if (this.dir != null)
            {
                if (this.schematic.writeToFile(this.dir, this.fileName, this.overrideFile))
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica.message.schematic_saved_as", this.fileName);
                }
                else
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.schematic_save_failed", this.fileName);
                }
            }
            // In-memory only
            else
            {
                String name = this.schematic.getMetadata().getName();
                SchematicHolder.getInstance().addSchematic(this.schematic, true);
                InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica.message.in_memory_schematic_created", name);
            }

            if (this.completionListener != null)
            {
                this.completionListener.onTaskCompleted();
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.error.schematic_save_interrupted");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);
    }
}
