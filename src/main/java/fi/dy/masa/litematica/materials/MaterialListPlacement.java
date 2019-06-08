package fi.dy.masa.litematica.materials;

import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class MaterialListPlacement extends MaterialListBase
{
    private final SchematicPlacement placement;

    public MaterialListPlacement(SchematicPlacement placement)
    {
        this(placement, false);
    }

    public MaterialListPlacement(SchematicPlacement placement, boolean reCreate)
    {
        super();

        this.placement = placement;

        if (reCreate)
        {
            this.reCreateMaterialList();
        }
    }

    @Override
    public boolean supportsRenderLayers()
    {
        return true;
    }

    @Override
    public String getName()
    {
        return this.placement.getName();
    }

    @Override
    public String getTitle()
    {
        return StringUtils.translate("litematica.gui.title.material_list.placement", this.getName());
    }

    @Override
    public void reCreateMaterialList()
    {
        TaskCountBlocksPlacement task = new TaskCountBlocksPlacement(this.placement, this);
        TaskScheduler.getInstanceClient().scheduleTask(task, 20);
        InfoUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
    }
}
