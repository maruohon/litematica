package litematica.scheduler.tasks;

import net.minecraft.world.WorldServer;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.game.wrap.WorldWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.position.LayerRange;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.util.SchematicPlacingUtils;

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
        return this.placement.isValid() && this.placement.isSchematicLoaded() &&
               GameWrap.getClientWorld() != null &&
               GameWrap.getClientPlayer() != null &&
               GameWrap.isSinglePlayer();
    }

    @Override
    public boolean execute()
    {
        WorldServer world = WorldWrap.getServerWorldForClientWorld();

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
