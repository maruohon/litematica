package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextInput;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IFileBrowserIconProvider;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.gui.wrappers.ButtonWrapper;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

public class WidgetAreaSelectionEntry extends WidgetDirectoryEntry
{
    private final SelectionManager selectionManager;
    private final WidgetAreaSelectionBrowser parent;
    private final List<ButtonWrapper<?>> buttons = new ArrayList<>();
    private int id;
    private int buttonsStartX;

    public WidgetAreaSelectionEntry(int x, int y, int width, int height, float zLevel, boolean isOdd,
            DirectoryEntry entry, SelectionManager selectionManager, Minecraft mc,
            WidgetAreaSelectionBrowser parent, IFileBrowserIconProvider iconProvider)
    {
        super(x, y, width, height, zLevel, isOdd, entry, mc, parent, iconProvider);

        this.selectionManager = selectionManager;
        this.parent = parent;
        this.id = 0;

        int posX = x + width;
        int posY = y + 1;

        // Note: These are placed from right to left

        if (entry.getType() == DirectoryEntryType.FILE && FileType.fromFile(entry.getFullPath()) == FileType.JSON)
        {
            posX = this.createButton(posX, posY, ButtonListener.ButtonType.REMOVE);
            //posX = this.createButton(posX, posY, ButtonListener.ButtonType.CONFIGURE);
            posX = this.createButton(posX, posY, ButtonListener.ButtonType.RENAME);
        }

        this.buttonsStartX = posX;
    }

    private int createButton(int x, int y, ButtonListener.ButtonType type)
    {
        String label = I18n.format(type.getLabelKey());
        int len = Math.max(this.mc.fontRenderer.getStringWidth(label) + 10, 20);
        x -= (len + 2);
        this.addButton(new ButtonGeneric(this.id++, x, y, len, 20, label), new ButtonListener(type, this.selectionManager, this));

        return x;
    }

    private <T extends ButtonBase> void addButton(T button, IButtonActionListener<T> listener)
    {
        this.buttons.add(new ButtonWrapper<>(button, listener));
    }

    @Override
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton)
    {
        for (ButtonWrapper<?> entry : this.buttons)
        {
            if (entry.mousePressed(this.mc, mouseX, mouseY, mouseButton))
            {
                // Don't call super if the button press got handled
                return true;
            }
        }

        return super.onMouseClickedImpl(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX < this.buttonsStartX && super.canSelectAt(mouseX, mouseY, mouseButton);
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        if (this.entry.getType() == DirectoryEntryType.FILE && FileType.fromFile(this.entry.getFullPath()) == FileType.JSON)
        {
            selected = this.entry.getFullPath().getAbsolutePath().equals(this.selectionManager.getCurrentSelectionId());
            super.render(mouseX, mouseY, selected);

            for (int i = 0; i < this.buttons.size(); ++i)
            {
                this.buttons.get(i).draw(this.mc, mouseX, mouseY, 0);
            }
        }
        else
        {
            super.render(mouseX, mouseY, selected);
        }
    }

    @Override
    protected String getDisplayName()
    {
        if (this.entry.getType() == DirectoryEntryType.FILE && FileType.fromFile(this.entry.getFullPath()) == FileType.JSON)
        {
            AreaSelection selection = this.selectionManager.getOrLoadSelectionReadOnly(this.getDirectoryEntry().getFullPath().getAbsolutePath());
            return selection != null ? selection.getName() : "<error>";
        }

        return super.getDisplayName();
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
    {
        List<String> text = new ArrayList<>();
        AreaSelection selection = this.selectionManager.getOrLoadSelection(this.getDirectoryEntry().getFullPath().getAbsolutePath());

        if (selection != null)
        {
            BlockPos o = selection.getOrigin();
            String strOrigin = String.format("x: %d, y: %d, z: %d", o.getX(), o.getY(), o.getZ());
            text.add(I18n.format("litematica.gui.label.area_selection_origin", strOrigin));

            int count = selection.getAllSubRegionBoxes().size();
            text.add(I18n.format("litematica.gui.label.area_selection_box_count", count));
        }

        int offset = 12;

        if (GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.buttonsStartX - offset, this.height))
        {
            this.parent.drawHoveringText(text, mouseX, mouseY);
        }
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final WidgetAreaSelectionEntry widget;
        private final SelectionManager selectionManager;
        private final ButtonType type;

        public ButtonListener(ButtonType type, SelectionManager selectionManager, WidgetAreaSelectionEntry widget)
        {
            this.type = type;
            this.selectionManager = selectionManager;
            this.widget = widget;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            String selectionId = this.widget.getDirectoryEntry().getFullPath().getAbsolutePath();

            if (this.type == ButtonType.RENAME)
            {
                String title = "litematica.gui.title.rename_area_selection";
                AreaSelection selection = this.selectionManager.getSelection(selectionId);
                String name = selection != null ? selection.getName() : "<error>";
                SelectionRenamer renamer = new SelectionRenamer(this.selectionManager, this.widget);
                this.widget.mc.displayGuiScreen(new GuiTextInput(160, title, name, this.widget.parent.getSelectionManagerGui(), renamer));
            }
            else if (this.type == ButtonType.REMOVE)
            {
                this.selectionManager.removeSelection(selectionId);
            }
            else if (this.type == ButtonType.CONFIGURE)
            {
            }

            this.widget.parent.refreshEntries();
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum ButtonType
        {
            RENAME          ("litematica.gui.button.rename"),
            CONFIGURE       ("litematica.gui.button.configure"),
            REMOVE          (TextFormatting.RED.toString() + "-");

            private final String labelKey;

            private ButtonType(String labelKey)
            {
                this.labelKey = labelKey;
            }

            public String getLabelKey()
            {
                return this.labelKey;
            }
        }
    }

    private static class SelectionRenamer implements IStringConsumer
    {
        private final WidgetAreaSelectionEntry widget;
        private final SelectionManager selectionManager;

        public SelectionRenamer(SelectionManager selectionManager, WidgetAreaSelectionEntry widget)
        {
            this.widget = widget;
            this.selectionManager = selectionManager;
        }

        @Override
        public void setString(String string)
        {
            String oldName = this.widget.getDirectoryEntry().getFullPath().getAbsolutePath();
            this.selectionManager.renameSelection(this.widget.getDirectoryEntry().getDirectory(), oldName, string, this.widget.parent.getSelectionManagerGui());
        }
    }
}
