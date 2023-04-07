package litematica.gui.widget.list.entry;

import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.render.text.StyledTextLine;
import litematica.scheduler.ITask;
import litematica.scheduler.TaskScheduler;

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

        this.setText(StyledTextLine.parseFirstLine(data.getDisplayName()));
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
