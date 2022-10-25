package litematica.gui;

import java.util.ArrayList;
import java.util.List;

import malilib.gui.BaseListScreen;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.DataListWidget;
import litematica.Reference;
import litematica.gui.widget.list.entry.TaskEntryWidget;
import litematica.scheduler.ITask;
import litematica.scheduler.TaskScheduler;

public class TaskManagerScreen extends BaseListScreen<DataListWidget<ITask>>
{
    protected final GenericButton mainMenuButton;

    public TaskManagerScreen()
    {
        super(12, 30, 20, 60);

        this.mainMenuButton = GenericButton.create("litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.setTitle("litematica.title.screen.task_manager", Reference.MOD_VERSION);
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
        listWidget.setDataListEntryWidgetFactory(TaskEntryWidget::new);
        return listWidget;
    }
}
