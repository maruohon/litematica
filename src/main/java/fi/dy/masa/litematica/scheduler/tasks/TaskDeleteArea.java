package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;

import net.minecraft.init.Blocks;

import malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.litematica.selection.SelectionBox;

public class TaskDeleteArea extends TaskFillArea
{
    public TaskDeleteArea(List<SelectionBox> boxes, boolean removeEntities)
    {
        super(boxes, Blocks.AIR.getDefaultState(), null, removeEntities, "litematica.gui.label.task_name.delete");
    }

    @Override
    protected void printCompletionMessage()
    {
        if (this.finished)
        {
            if (this.printCompletionMessage)
            {
                MessageDispatcher.success().translate("litematica.message.area_cleared");
            }
        }
        else
        {
            MessageDispatcher.error().translate("litematica.message.error.area_deletion_aborted");
        }
    }
}
