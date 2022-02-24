package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetTaskEntry;
import fi.dy.masa.litematica.scheduler.ITask;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.DataListWidget;

public class TaskManagerScreen extends BaseListScreen<DataListWidget<ITask>>
{
    protected final GenericButton mainMenuButton;

    public TaskManagerScreen()
    {
        super(12, 30, 20, 68);

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        this.mainMenuButton = GenericButton.create(type.getLabelKey(), new ButtonListenerChangeMenu(type, this.getParent()));

        this.setTitle("litematica.gui.title.task_manager");
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.mainMenuButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.mainMenuButton.setRight(this.getRight() - 10);
        this.mainMenuButton.setBottom(this.getBottom() - 6);
    }

    protected List<ITask> getAllTasks()
    {
        ArrayList<ITask> list = new ArrayList<>();
        list.addAll(TaskScheduler.getInstanceClient().getAllTasks());
        list.addAll(TaskScheduler.getInstanceServer().getAllTasks());
        return list;
    }

    @Override
    protected DataListWidget<ITask> createListWidget()
    {
        DataListWidget<ITask> listWidget = new DataListWidget<>(this::getAllTasks, true);

        listWidget.setEntryWidgetFactory(WidgetTaskEntry::new);

        return listWidget;
    }
}
