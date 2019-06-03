package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.scheduler.ITask;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

public class WidgetListTasks extends WidgetListBase<ITask, WidgetTaskEntry>
{
    public WidgetListTasks(int x, int y, int width, int height,
            @Nullable ISelectionListener<ITask> selectionListener)
    {
        super(x, y, width, height, selectionListener);

        this.browserEntryHeight = 22;
    }

    @Override
    protected Collection<ITask> getAllEntries()
    {
        ArrayList<ITask> list = new ArrayList<>();
        list.addAll(TaskScheduler.getInstanceClient().getAllTasks());
        list.addAll(TaskScheduler.getInstanceServer().getAllTasks());
        return list;
    }

    @Override
    protected WidgetTaskEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, ITask entry)
    {
        return new WidgetTaskEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, entry, listIndex, this);
    }
}
