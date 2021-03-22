package fi.dy.masa.litematica.gui.widgets;

import fi.dy.masa.litematica.scheduler.ITask;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.DataListWidget;
import fi.dy.masa.malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.render.ShapeRenderUtils;

public class WidgetTaskEntry extends BaseDataListEntryWidget<ITask>
{
    protected final GenericButton removeButton;

    public WidgetTaskEntry(int x, int y, int width, int height, int listIndex, int originalListIndex,
                           ITask task, DataListWidget<ITask> listWidget)
    {
        super(x, y, width, height, listIndex, originalListIndex, task, listWidget);

        this.removeButton = new GenericButton(x + width - 2, y + 1, -1, true, "litematica.gui.button.remove");
        this.removeButton.setActionListener((w, btn) -> {
            if (TaskScheduler.getInstanceClient().removeTask(this.getData()) == false)
            {
                TaskScheduler.getInstanceServer().removeTask(this.getData());
            }

            this.listWidget.refreshEntries();
        });
    }

    @Override
    public void updateSubWidgetsToGeometryChanges()
    {
        super.updateSubWidgetsToGeometryChanges();

        int x = this.getX() + this.getWidth() - 2;
        this.removeButton.setRightX(x);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidget(this.removeButton);
    }

    @Override
    public void renderAt(int x, int y, float z, int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId)
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

        this.renderSubWidgets(x, y, z, mouseX, mouseY, isActiveGui, hoveredWidgetId);

        //RenderUtils.disableItemLighting();
        //GlStateManager.disableLighting();
    }
}
