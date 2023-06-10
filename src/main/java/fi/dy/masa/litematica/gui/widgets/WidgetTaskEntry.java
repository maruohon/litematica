package fi.dy.masa.litematica.gui.widgets;

import net.minecraft.client.gui.DrawContext;

import fi.dy.masa.litematica.scheduler.ITask;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetTaskEntry extends WidgetListEntryBase<ITask>
{
    private final WidgetListTasks parent;
    private final boolean isOdd;

    public WidgetTaskEntry(int x, int y, int width, int height, boolean isOdd,
            ITask task, int listIndex, WidgetListTasks parent)
    {
        super(x, y, width, height, task, listIndex);

        this.parent = parent;
        this.isOdd = isOdd;

        int posX = x + width;
        ButtonListener listener = new ButtonListener(ButtonListener.Type.REMOVE, this);
        this.addButton(new ButtonGeneric(posX, y + 1, -1, true, StringUtils.translate("litematica.gui.button.remove")), listener);
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, DrawContext drawContext)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        // Draw a lighter background for the hovered and the selected entry
        if (selected || this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0x70FFFFFF);
        }
        else if (this.isOdd)
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0x20FFFFFF);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0x50FFFFFF);
        }

        String name = this.getEntry().getDisplayName();
        this.drawString(this.x + 4, this.y + 7, 0xFFFFFFFF, name, drawContext);

        this.drawSubWidgets(mouseX, mouseY, drawContext);
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final Type type;
        private final WidgetTaskEntry widget;

        public ButtonListener(Type type, WidgetTaskEntry widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == Type.REMOVE)
            {
                ITask task = this.widget.getEntry();

                if (TaskScheduler.getInstanceClient().removeTask(task) == false)
                {
                    TaskScheduler.getInstanceServer().removeTask(task);
                }

                this.widget.parent.refreshEntries();
            }
        }

        public enum Type
        {
            REMOVE;
        }
    }
}
