package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.block.Blocks;

public class TaskDeleteArea extends TaskFillArea
{
    public TaskDeleteArea(List<Box> boxes, boolean removeEntities)
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
                InfoUtils.showGuiMessage(MessageType.SUCCESS, "litematica.message.area_cleared");
            }
        }
        else
        {
            InfoUtils.showGuiMessage(MessageType.ERROR, "litematica.message.error.area_deletion_aborted");
        }
    }
}
