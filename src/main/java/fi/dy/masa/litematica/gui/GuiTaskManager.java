package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetTaskEntry;
import fi.dy.masa.litematica.scheduler.ITask;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.DataListWidget;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiTaskManager extends BaseListScreen<DataListWidget<ITask>>
{
    public GuiTaskManager()
    {
        super(12, 30, 20, 68);

        this.title = StringUtils.translate("litematica.gui.title.task_manager");
    }

    @Override
    protected void initScreen()
    {
        super.initScreen();

        int y = this.height - 26;

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        GenericButton button = new GenericButton(this.width - 10, y, -1, true, StringUtils.translate(type.getLabelKey()));
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));
    }

    public static Supplier<List<ITask>> getAllTasksSupplier()
    {
        return () -> {
            ArrayList<ITask> list = new ArrayList<>();
            list.addAll(TaskScheduler.getInstanceClient().getAllTasks());
            list.addAll(TaskScheduler.getInstanceServer().getAllTasks());
            return list;
        };
    }

    @Override
    protected DataListWidget<ITask> createListWidget(int listX, int listY, int listWidth, int listHeight)
    {
        DataListWidget<ITask> listWidget = new DataListWidget<>(listX, listY, listWidth, listHeight, getAllTasksSupplier());
        listWidget.setEntryWidgetFactory(WidgetTaskEntry::new);
        listWidget.setFetchFromSupplierOnRefresh(true);
        return listWidget;
    }
}
