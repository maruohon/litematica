package litematica.materials;

import com.google.gson.JsonObject;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.StringUtils;
import litematica.scheduler.TaskScheduler;
import litematica.scheduler.tasks.TaskCountBlocksPlacement;
import litematica.schematic.placement.SchematicPlacement;

public class MaterialListPlacement extends MaterialListBase
{
    private final SchematicPlacement placement;

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
    public boolean isForPlacement()
    {
        return true;
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
        return StringUtils.translate("litematica.title.screen.material_list.placement", this.getName());
    }

    @Override
    public void reCreateMaterialList()
    {
        TaskCountBlocksPlacement task = new TaskCountBlocksPlacement(this.placement, this);
        TaskScheduler.getInstanceClient().scheduleTask(task, 20);
        MessageDispatcher.generic(1000).translate("litematica.message.scheduled_task_added");
    }

    public static MaterialListPlacement createFromJson(JsonObject obj, SchematicPlacement schematicPlacement)
    {
        MaterialListPlacement materialList = new MaterialListPlacement(schematicPlacement, false);
        materialList.fromJson(obj);
        return materialList;
    }
}
