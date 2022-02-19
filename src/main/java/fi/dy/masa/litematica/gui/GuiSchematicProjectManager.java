package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicVersions;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVersion;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.projects.SchematicVersion;
import fi.dy.masa.litematica.schematic.util.SchematicUtils;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.ConfirmActionScreen;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.widget.button.BaseButton;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.ButtonActionListener;
import fi.dy.masa.malilib.gui.widget.list.SelectionListener;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.listener.TaskCompletionListener;
import fi.dy.masa.malilib.listener.ConfirmationListener;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicProjectManager extends BaseListScreen<SchematicVersion, WidgetSchematicVersion, WidgetListSchematicVersions>
                                        implements SelectionListener<SchematicVersion>, TaskCompletionListener
{
    private final SchematicProject project;

    public GuiSchematicProjectManager(SchematicProject project)
    {
        super(10, 24, 20, 74);

        this.project = project;
        this.title = StringUtils.translate("litematica.gui.title.schematic_project_manager");
    }

    @Override
    protected void initScreen()
    {
        super.initScreen();

        this.createElements();
    }

    private void createElements()
    {
        int x = 10;
        int y = this.height - 46;

        x += this.createButton(x, y, false, ButtonListener.Type.SAVE_VERSION);
        x += this.createButton(x, y, false, ButtonListener.Type.OPEN_AREA_EDITOR);
        x += this.createButton(x, y, false, ButtonListener.Type.MOVE_ORIGIN);
        x += this.createButton(x, y, false, ButtonListener.Type.PLACE_TO_WORLD);
        x += this.createButton(x, y, false, ButtonListener.Type.DELETE_AREA);

        y += 22;
        x = 10;
        x += this.createButton(x, y, false, ButtonListener.Type.OPEN_PROJECT_BROWSER);
        x += this.createButton(x, y, false, ButtonListener.Type.CLOSE_PROJECT);
    }

    private int createButton(int x, int y, boolean rightAlign, ButtonListener.Type type)
    {
        GenericButton button = new GenericButton(x, y, -1, rightAlign, type.getTranslationKey());
        button.translateAndAddHoverString(type.getHoverText());

        this.addButton(button, new ButtonListener(type, this));

        return button.getWidth() + 2;
    }

    private void reCreateGuiElements()
    {
        this.clearButtons();
        this.clearWidgets();

        this.createElements();
    }

    @Override
    protected SelectionListener<SchematicVersion> getSelectionListener()
    {
        return this;
    }

    @Override
    public void onSelectionChange(@Nullable SchematicVersion entry)
    {
        if (entry != null)
        {
            this.project.switchVersion(entry, true);
            this.getListWidget().refreshEntries();
        }

        this.reCreateGuiElements();
    }

    @Override
    public void onTaskCompleted()
    {
        this.getListWidget().refreshEntries();
    }

    @Override
    protected WidgetListSchematicVersions createListWidget(int listX, int listY)
    {
        return new WidgetListSchematicVersions(listX, listY, this.getListWidth() - 186, this.getListHeight(), this.project, this);
    }

    private static class ButtonListener implements ButtonActionListener
    {
        private final Type type;
        private final GuiSchematicProjectManager gui;

        public ButtonListener(Type type, GuiSchematicProjectManager gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            if (this.type == Type.OPEN_PROJECT_BROWSER)
            {
                GuiSchematicProjectsBrowser gui = new GuiSchematicProjectsBrowser();
                BaseScreen.openScreen(gui);
            }
            else if (this.type == Type.SAVE_VERSION)
            {
                SchematicUtils.saveSchematic(false);
            }
            else if (this.type == Type.OPEN_AREA_EDITOR)
            {
                SelectionManager manager = DataManager.getSelectionManager();

                if (manager.getCurrentSelection() != null)
                {
                    manager.openEditGui(GuiUtils.getCurrentScreen());
                }
            }
            else if (this.type == Type.PLACE_TO_WORLD)
            {
                PlaceToWorldExecutor executor = new PlaceToWorldExecutor();
                String title = "litematica.gui.title.schematic_projects.confirm_place_to_world";
                String msg = "litematica.gui.message.schematic_projects.confirm_place_to_world";
                ConfirmActionScreen gui = new ConfirmActionScreen(320, title, executor, this.gui, msg);
                BaseScreen.openScreen(gui);
            }
            else if (this.type == Type.DELETE_AREA)
            {
                DeleteAreaExecutor executor = new DeleteAreaExecutor();
                String title = "litematica.gui.title.schematic_projects.confirm_delete_area";
                String msg = "litematica.gui.message.schematic_projects.confirm_delete_area";
                ConfirmActionScreen gui = new ConfirmActionScreen(320, title, executor, this.gui, msg);
                BaseScreen.openScreen(gui);
            }
            else if (this.type == Type.MOVE_ORIGIN)
            {
                SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

                if (project != null)
                {
                    project.setOrigin(new BlockPos(this.gui.mc.player));
                    this.gui.reCreateGuiElements();
                }
            }
            else if (this.type == Type.CLOSE_PROJECT)
            {
                DataManager.getSchematicProjectsManager().closeCurrentProject();
                GuiSchematicProjectsBrowser gui = new GuiSchematicProjectsBrowser();
                BaseScreen.openScreen(gui);
            }
        }

        public enum Type
        {
            CLOSE_PROJECT           ("litematica.gui.button.schematic_projects.close_project"),
            DELETE_AREA             ("litematica.gui.button.schematic_projects.delete_area", "litematica.gui.button.hover.schematic_projects.delete_area"),
            MOVE_ORIGIN             ("litematica.gui.button.schematic_projects.move_origin_to_player", "litematica.gui.button.hover.schematic_projects.move_origin_to_player"),
            OPEN_AREA_EDITOR        ("litematica.gui.button.schematic_projects.open_area_editor"),
            OPEN_PROJECT_BROWSER    ("litematica.gui.button.schematic_projects.open_project_browser"),
            PLACE_TO_WORLD          ("litematica.gui.button.schematic_projects.place_to_world", "litematica.gui.button.hover.schematic_projects.place_to_world_warning"),
            SAVE_VERSION            ("litematica.gui.button.schematic_projects.save_version", "litematica.gui.button.hover.schematic_projects.save_new_version");

            private final String translationKey;
            @Nullable private final String hoverText;

            private Type(String label)
            {
                this(label, null);
            }

            private Type(String translationKey, String hoverText)
            {
                this.translationKey = translationKey;
                this.hoverText = hoverText;
            }

            public String getTranslationKey()
            {
                return this.translationKey;
            }

            @Nullable
            public String getHoverText()
            {
                return this.hoverText;
            }
        }
    }

    public static class PlaceToWorldExecutor implements ConfirmationListener
    {
        @Override
        public boolean onActionConfirmed()
        {
            DataManager.getSchematicProjectsManager().pasteCurrentVersionToWorld();
            return true;
        }
    }

    public static class DeleteAreaExecutor implements ConfirmationListener
    {
        @Override
        public boolean onActionConfirmed()
        {
            DataManager.getSchematicProjectsManager().deleteLastSeenArea(Minecraft.getMinecraft());
            return true;
        }
    }
}
