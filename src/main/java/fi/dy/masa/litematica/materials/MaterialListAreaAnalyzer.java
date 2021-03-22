package fi.dy.masa.litematica.materials;

import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksArea;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.malilib.message.MessageType;
import fi.dy.masa.malilib.message.MessageUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class MaterialListAreaAnalyzer extends MaterialListBase
{
    private final AreaSelection selection;

    public MaterialListAreaAnalyzer(AreaSelection selection)
    {
        super();

        this.selection = selection;
    }

    @Override
    public String getName()
    {
        return this.selection.getName();
    }

    @Override
    public String getTitle()
    {
        return StringUtils.translate("litematica.gui.title.material_list.area_analyzer", this.getName());
    }

    @Override
    public void reCreateMaterialList()
    {
        TaskCountBlocksArea task = new TaskCountBlocksArea(this.selection, this);
        TaskScheduler.getInstanceClient().scheduleTask(task, 20);
        MessageUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
    }
}
