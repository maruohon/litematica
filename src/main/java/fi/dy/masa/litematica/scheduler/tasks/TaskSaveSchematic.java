package fi.dy.masa.litematica.scheduler.tasks;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class TaskSaveSchematic extends TaskProcessChunkBase
{
    private final LitematicaSchematic schematic;
    private final BlockPos origin;
    private final ImmutableMap<String, Box> subRegions;
    private final Set<UUID> existingEntities = new HashSet<>();
    @Nullable private final File dir;
    @Nullable private final String fileName;
    private final boolean takeEntities;
    private final boolean overrideFile;

    public TaskSaveSchematic(LitematicaSchematic schematic, AreaSelection area, boolean takeEntities)
    {
        this(null, null, schematic, area, takeEntities, false);
    }

    public TaskSaveSchematic(@Nullable File dir, @Nullable String fileName, LitematicaSchematic schematic, AreaSelection area, boolean takeEntities, boolean overrideFile)
    {
        super("litematica.gui.label.task_name.save_schematic");

        this.dir = dir;
        this.fileName = fileName;
        this.schematic = schematic;
        this.origin = area.getEffectiveOrigin();
        this.subRegions = area.getAllSubRegions();
        this.takeEntities = takeEntities;
        this.overrideFile = overrideFile;

        this.addBoxesPerChunks(area.getAllSubRegionBoxes());
        this.updateInfoHudLinesMissingChunks(this.requiredChunks);
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        return this.areSurroundingChunksLoaded(pos, this.worldClient, 1);
    }

    @Override
    protected boolean processChunk(ChunkPos pos)
    {
        ImmutableMap<String, IntBoundingBox> volumes = PositionUtils.getBoxesWithinChunk(pos.x, pos.z, this.subRegions);
        this.schematic.takeBlocksFromWorldWithinChunk(this.world, pos.x, pos.z, volumes, this.subRegions);

        if (this.takeEntities)
        {
            this.schematic.takeEntitiesFromWorldWithinChunk(this.world, pos.x, pos.z, volumes, this.subRegions, this.existingEntities, this.origin);
        }

        return true;
    }

    @Override
    protected void onStop()
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
                    if (this.printCompletionMessage)
                    {
                        InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica.message.schematic_saved_as", this.fileName);
                    }
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

                if (this.printCompletionMessage)
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica.message.in_memory_schematic_created", name);
                }
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.error.schematic_save_interrupted");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        this.notifyListener();
    }
}
