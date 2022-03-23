package fi.dy.masa.litematica.task;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.google.common.collect.ImmutableMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import fi.dy.masa.malilib.listener.TaskCompletionListener;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.position.IntBoundingBox;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.scheduler.tasks.TaskProcessChunkBase;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.util.SchematicCreationUtils;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionBox;
import fi.dy.masa.litematica.util.PositionUtils;

public class CreateSchematicTask extends TaskProcessChunkBase
{
    protected final ImmutableMap<String, SelectionBox> subRegions;
    protected final Set<UUID> existingEntities = new HashSet<>();
    protected final ISchematic schematic;
    protected final BlockPos origin;
    protected final boolean ignoreEntities;

    public CreateSchematicTask(ISchematic schematic,
                               AreaSelection area,
                               boolean ignoreEntities,
                               TaskCompletionListener listener)
    {
        super("litematica.hud.task_name.save_schematic");

        this.ignoreEntities = ignoreEntities;
        this.schematic = schematic;
        this.origin = area.getEffectiveOrigin();
        this.subRegions = area.getAllSubRegions();
        this.setCompletionListener(listener);

        this.addPerChunkBoxes(area.getAllSubRegionBoxes());
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
        SchematicCreationUtils.takeBlocksFromWorldWithinChunk(this.schematic, this.world, volumes, this.subRegions);

        if (this.ignoreEntities == false)
        {
            SchematicCreationUtils.takeEntitiesFromWorldWithinChunk(this.schematic, this.world, volumes,
                                                                    this.subRegions, this.existingEntities);
        }

        return true;
    }

    @Override
    protected void onStop()
    {
        if (this.finished == false)
        {
            MessageDispatcher.warning().translate("litematica.message.error.schematic_save_interrupted");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        this.notifyListener();
    }

    /*
    public static class SaveSettings
    {
        public final SimpleBooleanStorage saveBlocks              = new SimpleBooleanStorage(true);
        public final SimpleBooleanStorage saveBlockEntities       = new SimpleBooleanStorage(true);
        public final SimpleBooleanStorage saveEntities            = new SimpleBooleanStorage(true);
        public final SimpleBooleanStorage saveScheduledBlockTicks = new SimpleBooleanStorage(true);
        public final SimpleBooleanStorage saveFromClientWorld     = new SimpleBooleanStorage(true);
        public final SimpleBooleanStorage saveFromSchematicWorld  = new SimpleBooleanStorage(false);
        public final SimpleBooleanStorage exposedBlocksOnly       = new SimpleBooleanStorage(false);
    }
    */
}
