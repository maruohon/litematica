package fi.dy.masa.litematica.scheduler.tasks;

import net.minecraft.world.WorldServer;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.game.WorldUtils;
import fi.dy.masa.malilib.util.position.LayerRange;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.util.SchematicPlacingUtils;

public class TaskPasteSchematicDirect extends TaskBase
{
    private final SchematicPlacement placement;
    private final LayerRange range;

    public TaskPasteSchematicDirect(SchematicPlacement placement, LayerRange range)
    {
        this.placement = placement;
        this.range = range;
    }

    @Override
    public boolean canExecute()
    {
        return this.placement.isInvalidated() == false &&
               this.mc.world != null &&
               this.mc.player != null &&
               this.mc.isSingleplayer();
    }

    @Override
    public boolean execute()
    {
        WorldServer world = WorldUtils.getServerWorldForClientWorld(this.mc);

        if (world != null && SchematicPlacingUtils.placeToWorld(this.placement, world, this.range, false))
        {
            this.finished = true;
        }

        return true;
    }

    @Override
    public void stop()
    {
        if (this.finished)
        {
            if (this.printCompletionMessage)
            {
                MessageDispatcher.success().screenOrActionbar().translate("litematica.message.schematic_pasted");
            }
        }
        else
        {
            MessageDispatcher.error().translate("litematica.message.error.schematic_paste_failed");
        }

        super.stop();
    }
}
