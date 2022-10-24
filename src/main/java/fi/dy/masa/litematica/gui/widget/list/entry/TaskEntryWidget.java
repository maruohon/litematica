package fi.dy.masa.litematica.gui.widget.list.entry;

import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.render.text.StyledTextLine;
import fi.dy.masa.litematica.scheduler.ITask;
import fi.dy.masa.litematica.scheduler.TaskScheduler;

public class TaskEntryWidget extends BaseDataListEntryWidget<ITask>
{
    protected final GenericButton removeButton;

    public TaskEntryWidget(ITask data, DataListEntryWidgetData constructData)
    {
        super(data, constructData);

        this.removeButton = GenericButton.create("litematica.gui.button.remove");
        this.removeButton.setActionListener(() -> {
            if (TaskScheduler.getInstanceClient().removeTask(this.getData()) == false)
            {
                TaskScheduler.getInstanceServer().removeTask(this.getData());
            }

            this.listWidget.refreshEntries();
        });

        this.setText(StyledTextLine.of(data.getDisplayName()));
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidget(this.removeButton);
    }

    @Override
    public void updateSubWidgetPositions()
    {
        super.updateSubWidgetPositions();

        this.removeButton.setRight(this.getRight() - 2);
        this.removeButton.setY(this.getY() + 1);
    }
}
