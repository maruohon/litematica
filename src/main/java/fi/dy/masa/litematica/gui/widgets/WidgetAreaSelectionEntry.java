package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.gui.GuiAreaSelectionEditorNormal;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.TextInputScreen;
import fi.dy.masa.malilib.gui.widget.button.BaseButton;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.ButtonActionListener;
import fi.dy.masa.malilib.gui.icon.FileBrowserIconProvider;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.list.entry.DirectoryEntryWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntryType;
import fi.dy.masa.malilib.render.TextRenderUtils;
import fi.dy.masa.malilib.util.data.ResultingStringConsumer;
import fi.dy.masa.malilib.overlay.message.MessageOutput;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetAreaSelectionEntry extends DirectoryEntryWidget
{
    private final SelectionManager selectionManager;
    private final WidgetAreaSelectionBrowser parent;
    private final FileType fileType;
    private final int buttonsStartX;

    public WidgetAreaSelectionEntry(int x, int y, int width, int height, boolean isOdd,
            DirectoryEntry entry, int listIndex, SelectionManager selectionManager,
            WidgetAreaSelectionBrowser parent, FileBrowserIconProvider iconProvider)
    {
        super(x, y, width, height, isOdd, entry, listIndex, parent, iconProvider);

        this.fileType = FileType.fromFileName(this.entry.getFullPath());
        this.selectionManager = selectionManager;
        this.parent = parent;

        int posX = x + width - 2;
        int posY = y + 1;

        // Note: These are placed from right to left

        if (entry.getType() == DirectoryEntryType.FILE && this.fileType == FileType.JSON)
        {
            posX = this.createButton(posX, posY, ButtonListener.ButtonType.REMOVE);
            posX = this.createButton(posX, posY, ButtonListener.ButtonType.RENAME);
            posX = this.createButton(posX, posY, ButtonListener.ButtonType.COPY);
            posX = this.createButton(posX, posY, ButtonListener.ButtonType.CONFIGURE);
        }

        this.buttonsStartX = posX;
    }

    private int createButton(int x, int y, ButtonListener.ButtonType type)
    {
        String label = StringUtils.translate(type.getLabelKey());
        int len = Math.max(this.getStringWidth(label) + 10, 20);
        x -= len;
        this.addButton(new GenericButton(x, y, len, 20, label), new ButtonListener(type, this.selectionManager, this));

        return x - 2;
    }

    @Override
    public boolean canHoverAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX < this.buttonsStartX && super.canHoverAt(mouseX, mouseY, mouseButton);
    }

    @Override
    public void render(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId, boolean selected)
    {
        if (this.entry.getType() == DirectoryEntryType.FILE && this.fileType == FileType.JSON)
        {
            selected = this.entry.getFullPath().getAbsolutePath().equals(this.selectionManager.getCurrentNormalSelectionId());
            super.render(mouseX, mouseY, isActiveGui, hoveredWidgetId, selected);
        }
        else
        {
            super.render(mouseX, mouseY, isActiveGui, hoveredWidgetId, selected);
        }
    }

    @Override
    protected String getDisplayName()
    {
        if (this.entry.getType() == DirectoryEntryType.FILE && this.fileType == FileType.JSON)
        {
            AreaSelection selection = this.selectionManager.getOrLoadSelectionReadOnly(this.getDirectoryEntry().getFullPath().getAbsolutePath());
            String prefix = this.entry.getDisplayNamePrefix();
            return selection != null ? (prefix != null ? prefix + selection.getName() : selection.getName()) : "<error>";
        }

        return super.getDisplayName();
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId)
    {
        List<String> text = new ArrayList<>();
        AreaSelection selection = this.selectionManager.getOrLoadSelectionReadOnly(this.getDirectoryEntry().getFullPath().getAbsolutePath());

        if (selection != null)
        {
            String str;
            BlockPos o = selection.getExplicitOrigin();

            if (o == null)
            {
                o = selection.getEffectiveOrigin();
                str = StringUtils.translate("litematica.gui.label.origin.auto");
            }
            else
            {
                str = StringUtils.translate("litematica.gui.label.origin.manual");
            }

            text.add(StringUtils.translate("litematica.gui.label.area_selection.hover.origin", o.getX(), o.getY(), o.getZ(), str));

            int count = selection.getAllSubRegionBoxes().size();
            text.add(StringUtils.translate("litematica.gui.label.area_selection.hover.box_count", count));
        }

        int offset = 12;

        if (BaseScreen.isMouseOver(mouseX, mouseY, this.getX(), this.getY(), this.buttonsStartX - offset, this.getHeight()))
        {
            TextRenderUtils.renderHoverText(mouseX, mouseY, this.getZ() + 1, text);
        }
    }

    private static class ButtonListener implements ButtonActionListener
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
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            String selectionId = this.widget.getDirectoryEntry().getFullPath().getAbsolutePath();

            if (this.type == ButtonType.RENAME)
            {
                String title = "litematica.gui.title.rename_area_selection";
                AreaSelection selection = this.selectionManager.getOrLoadSelection(selectionId);
                String name = selection != null ? selection.getName() : "<error>";
                SelectionRenamer renamer = new SelectionRenamer(this.selectionManager, this.widget, false);
                BaseScreen.openPopupScreen(new TextInputScreen(title, name, this.widget.parent.getSelectionManagerGui(), renamer));
            }
            else if (this.type == ButtonType.COPY)
            {
                AreaSelection selection = this.selectionManager.getOrLoadSelection(selectionId);

                if (selection != null)
                {
                    String title = StringUtils.translate("litematica.gui.title.copy_area_selection", selection.getName());
                    SelectionRenamer renamer = new SelectionRenamer(this.selectionManager, this.widget, true);
                    BaseScreen.openPopupScreen(new TextInputScreen(title, selection.getName(), this.widget.parent.getSelectionManagerGui(), renamer));
                }
                else
                {
                    this.widget.parent.getSelectionManagerGui().addMessage(MessageOutput.ERROR, "litematica.error.area_selection.failed_to_load");
                }
            }
            else if (this.type == ButtonType.REMOVE)
            {
                String current = this.selectionManager.getCurrentNormalSelectionId();

                if (this.selectionManager.removeSelection(selectionId) &&
                    this.selectionManager.getSelectionMode() == SelectionMode.NORMAL &&
                    selectionId.equals(current))
                {
                    this.widget.parent.getSelectionManagerGui().initGui();
                }

                this.widget.parent.refreshEntries();
            }
            else if (this.type == ButtonType.CONFIGURE)
            {
                AreaSelection selection = this.selectionManager.getOrLoadSelection(selectionId);

                if (selection != null)
                {
                    GuiAreaSelectionEditorNormal gui = new GuiAreaSelectionEditorNormal(selection);
                    gui.setParent(GuiUtils.getCurrentScreen());
                    gui.setSelectionId(selectionId);
                    BaseScreen.openScreen(gui);
                }
            }
        }

        public enum ButtonType
        {
            RENAME          ("litematica.gui.button.rename"),
            COPY            ("litematica.gui.button.copy"),
            CONFIGURE       ("litematica.gui.button.configure"),
            REMOVE          (BaseScreen.TXT_RED + "-");

            private final String labelKey;

            ButtonType(String labelKey)
            {
                this.labelKey = labelKey;
            }

            public String getLabelKey()
            {
                return this.labelKey;
            }
        }
    }

    private static class SelectionRenamer implements ResultingStringConsumer
    {
        private final WidgetAreaSelectionEntry widget;
        private final SelectionManager selectionManager;
        private final boolean copy;

        public SelectionRenamer(SelectionManager selectionManager, WidgetAreaSelectionEntry widget, boolean copy)
        {
            this.widget = widget;
            this.selectionManager = selectionManager;
            this.copy = copy;
        }

        @Override
        public boolean consumeString(String string)
        {
            String selectionId = this.widget.getDirectoryEntry().getFullPath().getAbsolutePath();
            return this.selectionManager.renameSelection(this.widget.getDirectoryEntry().getDirectory(), selectionId, string, this.copy, this.widget.parent.getSelectionManagerGui());
        }
    }
}
