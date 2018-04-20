package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import fi.dy.masa.litematica.config.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.config.gui.button.IButtonActionListener;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.util.DataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

public class GuiSelectionManager extends GuiLitematicaBase
{
    private final List<String> selectionNames = new ArrayList<>();
    private DataManager dataManager;

    public GuiSelectionManager()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world != null)
        {
            this.dataManager = DataManager.getInstance(mc.world);
        }
    }

    @Override
    protected String getTitle()
    {
        return I18n.format("litematica.gui.title.area_selection_manager");
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
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

        if (this.dataManager != null)
        {
            this.selectionNames.addAll(this.dataManager.getAllSelectionNames());
            Collections.sort(this.selectionNames);
        }
    }

    private void updateEntries()
    {
        this.clearButtons();
        this.updateNames();

        if (this.dataManager == null)
        {
            return;
        }

        int xStart = LEFT;
        int x = xStart;
        int y = TOP + 20;
        int nameWidth = 300;
        int id = 0;
        String currentName = this.dataManager.getCurrentSelectionName();
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

            y += button.getButtonHeight() + 2;
        }

        button = new ButtonGeneric(id++, xStart + nameWidth + widthRename + 16, y, 20, 20, BUTTON_LABEL_ADD);
        listener = this.createActionListener(ButtonListener.Type.ADD, "");
        this.addButton(button, listener);
    }

    private ButtonListener createActionListener(ButtonListener.Type type, String name)
    {
        return new ButtonListener(type, name, this.dataManager, this);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiSelectionManager gui;
        private final DataManager dataManager;
        private final Type type;
        private final String name;

        public ButtonListener(Type type, String name, DataManager dataManager, GuiSelectionManager gui)
        {
            this.type = type;
            this.name = name;
            this.dataManager = dataManager;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == Type.SELECT)
            {
                this.dataManager.setCurrentSelection(this.name);
                this.gui.updateEntries();
            }
            else if (this.type == Type.ADD)
            {
                this.dataManager.createNewSelection();
                this.gui.updateEntries();
            }
            else if (this.type == Type.REMOVE)
            {
                this.dataManager.removeSelection(this.name);
                this.gui.updateEntries();

                int size = this.gui.selectionNames.size();

                if (size > 0 && this.name.equals(this.dataManager.getCurrentSelectionName()))
                {
                    this.dataManager.setCurrentSelection(this.gui.selectionNames.get(size - 1));
                    this.gui.updateEntries();
                }
            }
            else if (this.type == Type.RENAME)
            {
                String title = I18n.format("litematica.gui.title.rename_area_selection");
                SelectionRenamer renamer = new SelectionRenamer(this.dataManager, this.name);
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
        private final DataManager dataManager;
        private final String oldName;

        public SelectionRenamer(DataManager dataManager, String oldName)
        {
            this.dataManager = dataManager;
            this.oldName = oldName;
        }

        @Override
        public void setString(String string)
        {
            this.dataManager.renameSelection(this.oldName, string);
        }
    }
}
