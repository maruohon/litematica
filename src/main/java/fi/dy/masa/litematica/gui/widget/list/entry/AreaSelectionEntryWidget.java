package fi.dy.masa.litematica.gui.widget.list.entry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.TextInputScreen;
import fi.dy.masa.malilib.gui.icon.FileBrowserIconProvider;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntryType;
import fi.dy.masa.malilib.gui.widget.list.entry.DataListEntryWidgetData;
import fi.dy.masa.malilib.gui.widget.list.entry.DirectoryEntryWidget;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.overlay.message.MessageOutput;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.ResultingStringConsumer;
import fi.dy.masa.litematica.gui.NormalModeAreaEditorScreen;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.FileType;

public class AreaSelectionEntryWidget extends DirectoryEntryWidget
{
    protected final SelectionManager selectionManager;
    protected final FileType fileType;
    protected final String selectionId;
    protected final GenericButton configureButton;
    protected final GenericButton copyButton;
    protected final GenericButton removeButton;
    protected final GenericButton renameButton;
    @Nullable protected final AreaSelection selection;
    protected final boolean isSelectionEntry;
    protected boolean isSelected;
    protected int buttonsStartX;

    public AreaSelectionEntryWidget(DirectoryEntry entry,
                                    DataListEntryWidgetData constructData,
                                    BaseFileBrowserWidget fileBrowserWidget,
                                    @Nullable FileBrowserIconProvider iconProvider,
                                    SelectionManager selectionManager)
    {
        super(entry, constructData, fileBrowserWidget, iconProvider);

        File file = this.data.getFullPath();
        this.selectionId = file.getAbsolutePath();
        this.selection = selectionManager.getOrLoadSelection(this.selectionId);
        this.fileType = FileType.fromFileName(file);
        this.selectionManager = selectionManager;
        this.isSelectionEntry = this.fileType == FileType.JSON && entry.getType() == DirectoryEntryType.FILE;
        this.textOffset.setXOffset(5);

        this.configureButton = GenericButton.create(16, "litematica.button.misc.configure", this::openAreaEditor);
        this.copyButton      = GenericButton.create(16, "litematica.button.misc.copy", this::copySelection);
        this.removeButton    = GenericButton.create(16, "litematica.button.misc.remove.minus", this::removeSelection);
        this.renameButton    = GenericButton.create(16, "litematica.button.misc.rename", this::renameSelection);

        if (this.isSelectionEntry)
        {
            this.isSelected = this.selectionId.equals(this.selectionManager.getCurrentNormalSelectionId());
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

    @Override
    public void updateWidgetState()
    {
        super.updateWidgetState();

        if (this.isSelectionEntry)
        {
            this.isSelected = this.selectionId.equals(this.selectionManager.getCurrentNormalSelectionId());
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

    @Override
    public boolean isSelected()
    {
        return this.isSelected;
    }

    @Override
    protected String getDisplayName()
    {
        if (this.isSelectionEntry)
        {
            if (this.selection != null)
            {
                String prefix = this.data.getDisplayNamePrefix();
                String selectionName = this.selection.getName();
                String name = prefix != null ? prefix + selectionName : selectionName;
                int count = this.selection.getAllSubRegionBoxes().size();
                return StringUtils.translate("litematica.label.area_browser.entry_name", name, count);
            }
            else
            {
                return "<error>";
            }
        }

        return super.getDisplayName();
    }

    protected void addHoverTooltip()
    {
        if (this.selection != null)
        {
            List<String> lines = new ArrayList<>();

            if (this.selection.hasManualOrigin())
            {
                String key = "litematica.hover.area_selection_browser.origin.manual";
                BlockPos o = this.selection.getExplicitOrigin();
                lines.add(StringUtils.translate(key, o.getX(), o.getY(), o.getZ()));
            }
            else
            {
                String key = "litematica.hover.area_selection_browser.origin.auto";
                BlockPos o = this.selection.getEffectiveOrigin();
                lines.add(StringUtils.translate(key, o.getX(), o.getY(), o.getZ()));
            }

            int count = this.selection.getAllSubRegionBoxes().size();
            lines.add(StringUtils.translate("litematica.hover.area_selection_browser.region_count", count));

            this.getHoverInfoFactory().addStrings(lines);
        }
    }

    protected void openAreaEditor()
    {
        if (this.selection != null)
        {
            NormalModeAreaEditorScreen gui = new NormalModeAreaEditorScreen(this.selection);
            gui.setParent(GuiUtils.getCurrentScreen());
            BaseScreen.openScreen(gui);
        }
    }

    protected void copySelection()
    {
        if (this.selection != null)
        {
            String titleKey = "litematica.title.screen.area_selection_browser.copy_selection";
            String selectionName = this.selection.getName();
            String title = StringUtils.translate(titleKey, selectionName);
            ResultingStringConsumer callback = (str) -> this.renameSelectionUsingName(str, true);
            TextInputScreen screen = new TextInputScreen(title, selectionName, callback);
            screen.setParent(GuiUtils.getCurrentScreen());
            BaseScreen.openPopupScreen(screen);
        }
        else
        {
            MessageDispatcher.error(8000).translate("litematica.message.error.area_selection_browser.failed_to_load");
        }
    }

    protected void renameSelection()
    {
        String title = "litematica.title.screen.area_selection_browser.rename_selection";
        String name = this.selection != null ? this.selection.getName() : "<error>";
        ResultingStringConsumer callback = (str) -> this.renameSelectionUsingName(str, false);
        TextInputScreen screen = new TextInputScreen(title, name, callback);
        screen.setParent(GuiUtils.getCurrentScreen());
        BaseScreen.openPopupScreen(screen);
    }

    protected boolean renameSelectionUsingName(String name, boolean copy)
    {
        File dir = this.data.getDirectory();
        return this.selectionManager.renameSelection(dir, this.selectionId, name, copy, MessageOutput.MESSAGE_OVERLAY);
    }

    protected void removeSelection()
    {
        if (this.selectionManager.removeSelection(this.selectionId) &&
            this.selectionManager.getSelectionMode() == SelectionMode.MULTI_REGION &&
            this.selectionId.equals(this.selectionManager.getCurrentNormalSelectionId()))
        {
            GuiUtils.reInitCurrentScreen();
        }
        else
        {
            this.listWidget.refreshEntries();
        }
    }
}
