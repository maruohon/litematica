package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicVersions;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVersion;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.projects.SchematicVersion;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class GuiSchematicProjectManager extends GuiListBase<SchematicVersion, WidgetSchematicVersion, WidgetListSchematicVersions>
                                        implements ISelectionListener<SchematicVersion>
{
    private final SchematicProject project;

    public GuiSchematicProjectManager(SchematicProject project)
    {
        super(10, 40);

        this.project = project;
        this.title = I18n.format("litematica.gui.title.schematic_project_manager");
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 80;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.createElements();
    }

    private void createElements()
    {
        int x = 10;
        int y = this.height - 36;

        String str = I18n.format("litematica.gui.label.schematic_projects.currently_open_project", this.project.getName());
        int w = this.mc.fontRenderer.getStringWidth(str);
        this.addLabel(x, 26, w, 14, 0xFFFFFFFF, str);

        x += this.createButton(x, y, false, ButtonListener.Type.OPEN_PROJECT_BROWSER);
        x += this.createButton(x, y, false, ButtonListener.Type.MOVE_ORIGIN);
        x += this.createButton(x, y, false, ButtonListener.Type.PLACE_TO_WORLD);
    }

    private int createButton(int x, int y, boolean rightAlign, ButtonListener.Type type)
    {
        ButtonGeneric button = ButtonGeneric.createGeneric(x, y, -1, rightAlign, type.getTranslationKey());
        String hover = type.getHoverText();

        if (hover != null)
        {
            button.setHoverStrings(hover);
        }

        this.addButton(button, new ButtonListener(type, this));

        return button.getButtonWidth() + 4;
    }

    private void reCreateGuiElements()
    {
        this.clearButtons();
        this.clearWidgets();

        this.createElements();
    }

    @Override
    protected ISelectionListener<SchematicVersion> getSelectionListener()
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
    protected WidgetListSchematicVersions createListWidget(int listX, int listY)
    {
        return new WidgetListSchematicVersions(listX, listY, this.getBrowserWidth() - 186, this.getBrowserHeight(), this.zLevel, this.project, this);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final Type type;
        private final GuiSchematicProjectManager gui;

        public ButtonListener(Type type, GuiSchematicProjectManager gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            if (this.type == Type.OPEN_PROJECT_BROWSER)
            {
                GuiSchematicProjectsBrowser gui = new GuiSchematicProjectsBrowser();
                this.gui.mc.displayGuiScreen(gui);
            }
            else if (this.type == Type.PLACE_TO_WORLD)
            {
                DataManager.getSchematicProjectsManager().pasteCurrentVersionToWorld();
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
        }

        public enum Type
        {
            OPEN_PROJECT_BROWSER    ("litematica.gui.button.schematic_projects.open_project_browser"),
            PLACE_TO_WORLD          ("litematica.gui.button.place_to_world", "litematica.gui.button.hover.schematic_projects.place_to_world_warning"),
            MOVE_ORIGIN             ("litematica.gui.button.schematic_projects.move_origin_to_player", "litematica.gui.button.hover.schematic_projects.move_origin_to_player");

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
                return this.hoverText != null ? I18n.format(this.hoverText) : null;
            }
        }
    }
}
