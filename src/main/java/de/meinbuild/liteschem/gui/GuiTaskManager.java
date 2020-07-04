package de.meinbuild.liteschem.gui;

import de.meinbuild.liteschem.gui.widgets.WidgetListTasks;
import de.meinbuild.liteschem.gui.widgets.WidgetTaskEntry;
import de.meinbuild.liteschem.scheduler.ITask;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiTaskManager extends GuiListBase<ITask, WidgetTaskEntry, WidgetListTasks>
{
    public GuiTaskManager()
    {
        super(12, 30);

        this.title = StringUtils.translate("litematica.gui.title.task_manager");
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

        GuiMainMenu.ButtonListenerChangeMenu.ButtonType type = GuiMainMenu.ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        ButtonGeneric button = new ButtonGeneric(this.width - 10, y, -1, true, StringUtils.translate(type.getLabelKey()));
        this.addButton(button, new GuiMainMenu.ButtonListenerChangeMenu(type, this.getParent()));
    }

    @Override
    protected WidgetListTasks createListWidget(int listX, int listY)
    {
        return new WidgetListTasks(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), null);
    }
}
