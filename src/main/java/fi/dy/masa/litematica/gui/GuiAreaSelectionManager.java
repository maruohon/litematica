package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import fi.dy.masa.litematica.config.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.config.gui.button.IButtonActionListener;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.schematic.AreaSelection;
import fi.dy.masa.litematica.util.AreaSelectionManager;
import fi.dy.masa.litematica.util.DataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

public class GuiAreaSelectionManager extends GuiLitematicaBase
{
    private final List<String> selectionNames = new ArrayList<>();
    private AreaSelectionManager selectionManager;

    public GuiAreaSelectionManager()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world != null)
        {
            this.selectionManager = DataManager.getInstance(mc.world).getSelectionManager();
        }
    }

    @Override
    protected String getTitle()
    {
        return I18n.format("litematica.gui.title.area_selection_manager");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.updateEntries();
    }

    private void updateNames()
    {
        this.selectionNames.clear();

        if (this.selectionManager != null)
        {
            this.selectionNames.addAll(this.selectionManager.getAllAreaSelectionNames());
            Collections.sort(this.selectionNames);
        }
    }

    private void updateEntries()
    {
        this.clearButtons();
        this.updateNames();

        if (this.selectionManager == null)
        {
            return;
        }

        int xStart = LEFT;
        int x = xStart;
        int y = TOP + 20;
        int nameWidth = 300;
        int id = 0;
        String currentName = this.selectionManager.getCurrentAreaSelectionName();
        ButtonGeneric button;
        ButtonListener listener;
        String labelRename = I18n.format("litematica.gui.button.rename");
        int widthRename = this.mc.fontRenderer.getStringWidth(labelRename) + 12;

        for (String name : this.selectionNames)
        {
            x = xStart;
            String label = name.equals(currentName) ? TextFormatting.AQUA + name : name;
            button = new ButtonGeneric(id++, x, y, nameWidth, 20, label);
            listener = this.createActionListener(ButtonListener.Type.SELECT, name);
            this.addButton(button, listener);
             x += nameWidth + 8;

            button = new ButtonGeneric(id++, x, y, widthRename, 20, labelRename);
            listener = this.createActionListener(ButtonListener.Type.RENAME, name);
            this.addButton(button, listener);
            x += widthRename + 8;

            button = new ButtonGeneric(id++, x, y, 20, 20, BUTTON_LABEL_REMOVE);
            listener = this.createActionListener(ButtonListener.Type.REMOVE, name);
            this.addButton(button, listener);

            AreaSelection selection = this.selectionManager.getAreaSelection(name);
            int count = selection != null ? selection.getAllSelectionsBoxes().size() : 0;
            label = I18n.format("litematica.gui.label.area_selection_box_count", count);
            int w = this.fontRenderer.getStringWidth(label);
            this.addLabel(id++, x + 28, y, w, 20, WHITE, label);
            y += button.getButtonHeight() + 2;
        }

        button = new ButtonGeneric(id++, xStart + nameWidth + widthRename + 16, y, 20, 20, BUTTON_LABEL_ADD);
        listener = this.createActionListener(ButtonListener.Type.ADD, "");
        this.addButton(button, listener);
    }

    private ButtonListener createActionListener(ButtonListener.Type type, String name)
    {
        return new ButtonListener(type, name, this.selectionManager, this);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiAreaSelectionManager gui;
        private final AreaSelectionManager selectionManager;
        private final Type type;
        private final String name;

        public ButtonListener(Type type, String name, AreaSelectionManager selectionManager, GuiAreaSelectionManager gui)
        {
            this.type = type;
            this.name = name;
            this.selectionManager = selectionManager;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == Type.SELECT)
            {
                this.selectionManager.setCurrentAreaSelection(this.name);
                this.gui.updateEntries();
            }
            else if (this.type == Type.ADD)
            {
                this.selectionManager.createNewAreaSelection();
                this.gui.updateEntries();
            }
            else if (this.type == Type.REMOVE)
            {
                this.selectionManager.removeAreaSelection(this.name);
                this.gui.updateEntries();

                int size = this.gui.selectionNames.size();

                if (size > 0 && this.name.equals(this.selectionManager.getCurrentAreaSelectionName()))
                {
                    this.selectionManager.setCurrentAreaSelection(this.gui.selectionNames.get(size - 1));
                    this.gui.updateEntries();
                }
            }
            else if (this.type == Type.RENAME)
            {
                String title = I18n.format("litematica.gui.title.rename_area_selection");
                SelectionRenamer renamer = new SelectionRenamer(this.selectionManager, this.name);
                this.gui.mc.displayGuiScreen(new GuiTextInput(160, title, this.name, this.gui, renamer));
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum Type
        {
            SELECT,
            ADD,
            RENAME,
            REMOVE;
        }
    }

    private static class SelectionRenamer implements IStringConsumer
    {
        private final AreaSelectionManager selectionManager;
        private final String oldName;

        public SelectionRenamer(AreaSelectionManager selectionManager, String oldName)
        {
            this.selectionManager = selectionManager;
            this.oldName = oldName;
        }

        @Override
        public void setString(String string)
        {
            this.selectionManager.renameAreaSelection(this.oldName, string);
        }
    }
}
