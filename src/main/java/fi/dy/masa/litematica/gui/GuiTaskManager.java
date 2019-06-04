package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetListTasks;
import fi.dy.masa.litematica.gui.widgets.WidgetTaskEntry;
import fi.dy.masa.litematica.scheduler.ITask;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.minecraft.client.resources.I18n;

public class GuiTaskManager extends GuiListBase<ITask, WidgetTaskEntry, WidgetListTasks>
{
    public GuiTaskManager()
    {
        super(12, 30);

        this.title = I18n.format("litematica.gui.title.task_manager");
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 68;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int y = this.height - 26;

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        ButtonGeneric button = new ButtonGeneric(this.width - 10, y, -1, true, I18n.format(type.getLabelKey()));
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));
    }

    @Override
    protected WidgetListTasks createListWidget(int listX, int listY)
    {
        return new WidgetListTasks(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), null);
    }
}
