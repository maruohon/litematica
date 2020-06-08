package fi.dy.masa.litematica.scheduler.tasks;

import net.minecraft.server.world.ServerWorld;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;

public class TaskPasteSchematicDirect extends TaskBase
{
    private final SchematicPlacement placement;

    public TaskPasteSchematicDirect(SchematicPlacement placement)
    {
        this.placement = placement;
    }

    @Override
    public boolean canExecute()
    {
        return this.mc.world != null && this.mc.player != null && this.mc.isIntegratedServerRunning();
    }

    @Override
    public boolean execute()
    {
        ServerWorld world = this.mc.getServer().getWorld(this.mc.world.getRegistryKey());
        LitematicaSchematic schematic = this.placement.getSchematic();

        if (world != null && schematic.placeToWorld(world, this.placement, false))
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
                InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.schematic_pasted");
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.schematic_paste_failed");
        }

        super.stop();
    }
}
