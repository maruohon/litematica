package fi.dy.masa.litematica.gui.widget.list.entry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.TextInputScreen;
import fi.dy.masa.malilib.gui.icon.FileBrowserIconProvider;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.gui.widget.list.entry.DataListEntryWidgetData;
import fi.dy.masa.malilib.gui.widget.list.entry.DirectoryEntryWidget;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.overlay.message.MessageOutput;
import fi.dy.masa.malilib.util.StringUtils;

public class AreaSelectionEntryWidget extends DirectoryEntryWidget
{
    protected final SelectionManager selectionManager;
    protected final FileType fileType;
    protected final GenericButton configureButton;
    protected final GenericButton copyButton;
    protected final GenericButton removeButton;
    protected final GenericButton renameButton;
    protected final boolean isSelectionEntry;
    protected int buttonsStartX;

    public AreaSelectionEntryWidget(DirectoryEntry entry,
                                    DataListEntryWidgetData constructData,
                                    BaseFileBrowserWidget fileBrowserWidget,
                                    @Nullable FileBrowserIconProvider iconProvider,
                                    SelectionManager selectionManager)
    {
        super(entry, constructData, fileBrowserWidget, iconProvider);

        this.fileType = FileType.fromFileName(this.entry.getFullPath());
        this.selectionManager = selectionManager;
        this.isSelectionEntry = this.fileType == FileType.JSON && entry.getType() == BaseFileBrowserWidget.DirectoryEntryType.FILE;
        this.textOffset.setXOffset(5);

        this.configureButton = GenericButton.create("litematica.button.misc.configure", this::openAreaEditor);
        this.copyButton      = GenericButton.create("litematica.button.misc.copy", this::copySelection);
        this.removeButton    = GenericButton.create("litematica.button.misc.remove.minus", this::removeSelection);
        this.renameButton    = GenericButton.create("litematica.button.misc.rename", this::renameSelection);

        if (this.isSelectionEntry)
        {
            this.addHoverTooltip();
        }
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        if (this.isSelectionEntry)
        {
            this.addWidget(this.configureButton);
            this.addWidget(this.copyButton);
            this.addWidget(this.removeButton);
            this.addWidget(this.renameButton);
        }
    }

    @Override
    public void updateSubWidgetPositions()
    {
        super.updateSubWidgetPositions();

        if (this.isSelectionEntry)
        {
            this.configureButton.centerVerticallyInside(this);
            this.copyButton.centerVerticallyInside(this);
            this.removeButton.centerVerticallyInside(this);
            this.renameButton.centerVerticallyInside(this);

            this.removeButton.setRight(this.getRight() - 2);
            this.renameButton.setRight(this.removeButton.getX() - 2);
            this.copyButton.setRight(this.renameButton.getX() - 2);
            this.configureButton.setRight(this.copyButton.getX() - 2);

            this.buttonsStartX = this.configureButton.getX();
        }
    }

    protected void addHoverTooltip()
    {
        String selectionId = this.getDirectoryEntry().getFullPath().getAbsolutePath();
        AreaSelection selection = this.selectionManager.getOrLoadSelectionReadOnly(selectionId);

        if (selection != null)
        {
            List<String> lines = new ArrayList<>();
            BlockPos o = selection.getExplicitOrigin();
            String key = o == null ? "litematica.hover.area_selection_browser.origin.auto" :
                                     "litematica.hover.area_selection_browser.origin.manual";

            if (o == null)
            {
                o = selection.getEffectiveOrigin();
            }

            lines.add(StringUtils.translate(key, o.getX(), o.getY(), o.getZ()));

            int count = selection.getAllSubRegionBoxes().size();
            lines.add(StringUtils.translate("litematica.hover.area_selection_browser.region_count", count));

            this.getHoverInfoFactory().addStrings(lines);
        }
    }

    @Override
    public boolean canHoverAt(int mouseX, int mouseY, int mouseButton)
    {
        if (this.isSelectionEntry && mouseX >= this.buttonsStartX)
        {
            return false;
        }

        return super.canHoverAt(mouseX, mouseY, mouseButton);
    }

    protected void openAreaEditor()
    {
        String selectionId = this.getDirectoryEntry().getFullPath().getAbsolutePath();
        AreaSelection selection = this.selectionManager.getOrLoadSelection(selectionId);

        if (selection != null)
        {
            /* TODO FIXME malilib refactor
            GuiAreaSelectionEditorNormal gui = new GuiAreaSelectionEditorNormal(selection);
            gui.setParent(GuiUtils.getCurrentScreen());
            gui.setSelectionId(selectionId);
            BaseScreen.openScreen(gui);
            */
        }
    }

    protected void copySelection()
    {
        String selectionId = this.getDirectoryEntry().getFullPath().getAbsolutePath();
        AreaSelection selection = this.selectionManager.getOrLoadSelection(selectionId);

        if (selection != null)
        {
            String title = StringUtils.translate("litematica.title.screen.area_selection_browser.copy_selection", selection.getName());
            BaseScreen.openPopupScreen(new TextInputScreen(title, selection.getName(),
                                                           (str) -> this.renameSelectionUsingName(str, true),
                                                           GuiUtils.getCurrentScreen()));
        }
        else
        {
            MessageDispatcher.error(8000).translate("litematica.message.error.area_selection_browser.failed_to_load");
        }
    }

    protected void renameSelection()
    {
        String selectionId = this.getDirectoryEntry().getFullPath().getAbsolutePath();
        String title = "litematica.title.screen.area_selection_browser.rename_selection";
        AreaSelection selection = this.selectionManager.getOrLoadSelection(selectionId);
        String name = selection != null ? selection.getName() : "<error>";
        BaseScreen.openPopupScreen(new TextInputScreen(title, name,
                                                       (str) -> this.renameSelectionUsingName(str, false),
                                                       GuiUtils.getCurrentScreen()));
    }

    protected boolean renameSelectionUsingName(String name, boolean copy)
    {
        DirectoryEntry entry = this.getDirectoryEntry();
        String selectionId = entry.getFullPath().getAbsolutePath();
        File dir = entry.getDirectory();
        return this.selectionManager.renameSelection(dir, selectionId, name, copy, MessageOutput.MESSAGE_OVERLAY);
    }

    protected void removeSelection()
    {
        String selectionId = this.getDirectoryEntry().getFullPath().getAbsolutePath();
        String current = this.selectionManager.getCurrentNormalSelectionId();

        if (this.selectionManager.removeSelection(selectionId) &&
            this.selectionManager.getSelectionMode() == SelectionMode.NORMAL &&
            selectionId.equals(current))
        {
            GuiUtils.reInitCurrentScreen();
        }

        this.listWidget.refreshEntries();
    }

    /*
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
    */

    @Override
    protected String getDisplayName()
    {
        if (this.isSelectionEntry)
        {
            String selectionId = this.getDirectoryEntry().getFullPath().getAbsolutePath();
            AreaSelection selection = this.selectionManager.getOrLoadSelectionReadOnly(selectionId);
            String prefix = this.entry.getDisplayNamePrefix();
            return selection != null ? (prefix != null ? prefix + selection.getName() : selection.getName()) : "<error>";
        }

        return super.getDisplayName();
    }
}
