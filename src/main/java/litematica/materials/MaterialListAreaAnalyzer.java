package litematica.materials;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.StringUtils;
import litematica.scheduler.TaskScheduler;
import litematica.scheduler.tasks.TaskCountBlocksArea;
import litematica.selection.AreaSelection;

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
        return StringUtils.translate("litematica.title.screen.material_list.area_analyzer", this.getName());
    }

    @Override
    public void reCreateMaterialList()
    {
        TaskCountBlocksArea task = new TaskCountBlocksArea(this.selection, this);
        TaskScheduler.getInstanceClient().scheduleTask(task, 20);
        MessageDispatcher.generic(1000).translate("litematica.message.scheduled_task_added");
    }
}
