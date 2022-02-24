package fi.dy.masa.litematica.gui.widgets;

import fi.dy.masa.litematica.scheduler.ITask;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.malilib.gui.util.ScreenContext;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import fi.dy.masa.malilib.gui.widget.list.entry.DataListEntryWidgetData;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.render.ShapeRenderUtils;

public class WidgetTaskEntry extends BaseDataListEntryWidget<ITask>
{
    protected final GenericButton removeButton;

    public WidgetTaskEntry(ITask data, DataListEntryWidgetData constructData)
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

    @Override
    public void renderAt(int x, int y, float z, ScreenContext ctx)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        int width = this.getWidth();
        int height = this.getHeight();

        // Draw a lighter background for the hovered entry
        if (this.isMouseOver(mouseX, mouseY))
        {
            ShapeRenderUtils.renderRectangle(x, y, z, width, height, 0x70FFFFFF);
        }
        else if (this.isOdd)
        {
            ShapeRenderUtils.renderRectangle(x, y, z, width, height, 0x20FFFFFF);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            ShapeRenderUtils.renderRectangle(x, y, z, width, height, 0x50FFFFFF);
        }

        String name = this.getData().getDisplayName();
        this.drawString(x + 4, y + this.getCenteredTextOffsetY(), z, 0xFFFFFFFF, name);

        this.renderSubWidgets(x, y, z, ctx);

        //RenderUtils.disableItemLighting();
        //GlStateManager.disableLighting();
    }
}
