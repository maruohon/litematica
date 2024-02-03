package litematica.gui.widget.list.entry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import malilib.gui.BaseScreen;
import malilib.gui.TextInputScreen;
import malilib.gui.icon.FileBrowserIconProvider;
import malilib.gui.util.GuiUtils;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.BaseFileBrowserWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntryType;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.gui.widget.list.entry.DirectoryEntryWidget;
import malilib.overlay.message.MessageDispatcher;
import malilib.overlay.message.MessageOutput;
import malilib.util.StringUtils;
import malilib.util.data.ResultingStringConsumer;
import malilib.util.position.BlockPos;
import litematica.gui.MultiRegionModeAreaEditorScreen;
import litematica.selection.AreaSelection;
import litematica.selection.AreaSelectionManager;
import litematica.selection.AreaSelectionType;

public class AreaSelectionEntryWidget extends DirectoryEntryWidget
{
    protected final AreaSelectionManager areaSelectionManager;
    protected final String selectionId;
    protected final GenericButton configureButton;
    protected final GenericButton copyButton;
    protected final GenericButton removeButton;
    protected final GenericButton renameButton;
    @Nullable protected AreaSelection selection;
    protected final boolean isSelectionEntry;
    protected boolean isSelected;
    protected int buttonsStartX;

    public AreaSelectionEntryWidget(DirectoryEntry entry,
                                    DataListEntryWidgetData constructData,
                                    BaseFileBrowserWidget fileBrowserWidget,
                                    @Nullable FileBrowserIconProvider iconProvider,
                                    AreaSelectionManager areaSelectionManager)
    {
        super(entry, constructData, fileBrowserWidget, iconProvider);

        Path file = this.data.getFullPath();
        this.selectionId = file.toAbsolutePath().toString();
        // By default, load the selections as read-only, so that they won't unnecessarily get written back to the file
        this.selection = areaSelectionManager.getOrLoadSelectionReadOnly(this.selectionId);
        this.areaSelectionManager = areaSelectionManager;
        this.isSelectionEntry = entry.getType() == DirectoryEntryType.FILE && entry.getName().endsWith(".json");

        this.configureButton = GenericButton.create(16, "litematica.button.misc.configure", this::openAreaEditor);
        this.copyButton      = GenericButton.create(16, "litematica.button.misc.copy", this::copySelection);
        this.removeButton    = GenericButton.create(16, "litematica.button.misc.remove.minus", this::removeSelection);
        this.renameButton    = GenericButton.create(16, "litematica.button.misc.rename", this::renameSelection);

        if (this.isSelectionEntry)
        {
            this.addHoverTooltip();
            this.updateWidgetState();
        }

        this.getBorderRenderer().getHoverSettings().setEnabled(false);
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
            this.isSelected = this.selectionId.equals(this.areaSelectionManager.getCurrentMultiRegionSelectionId());
        }
    }

    @Override
    public boolean canHoverAt(int mouseX, int mouseY)
    {
        if (this.isSelectionEntry && mouseX >= this.buttonsStartX)
        {
            return false;
        }

        return super.canHoverAt(mouseX, mouseY);
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
                int count = this.selection.getBoxCount();
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
                BlockPos o = this.selection.getManualOrigin();
                lines.add(StringUtils.translate(key, o.getX(), o.getY(), o.getZ()));
            }
            else
            {
                String key = "litematica.hover.area_selection_browser.origin.auto";
                BlockPos o = this.selection.getEffectiveOrigin();
                lines.add(StringUtils.translate(key, o.getX(), o.getY(), o.getZ()));
            }

            int count = this.selection.getBoxCount();
            lines.add(StringUtils.translate("litematica.hover.area_selection_browser.box_count", count));

            this.getHoverInfoFactory().addStrings(lines);
        }
    }

    protected void openAreaEditor()
    {
        // Fully load the selection if it's potentially going to be edited
        this.selection = this.areaSelectionManager.getOrLoadSelection(this.selectionId);

        if (this.selection != null)
        {
            BaseScreen.openScreenWithParent(new MultiRegionModeAreaEditorScreen(this.selection));
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
            BaseScreen.openPopupScreenWithCurrentScreenAsParent(new TextInputScreen(title, selectionName, callback));
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
        BaseScreen.openPopupScreenWithCurrentScreenAsParent(new TextInputScreen(title, name, callback));
    }

    protected boolean renameSelectionUsingName(String newName, boolean copy)
    {
        Path dir = this.data.getDirectory();
        return this.areaSelectionManager.renameSelection(dir, this.selectionId, newName, copy, MessageOutput.MESSAGE_OVERLAY);
    }

    protected void removeSelection()
    {
        if (this.areaSelectionManager.removeSelection(this.selectionId) &&
            this.areaSelectionManager.getSelectionMode() == AreaSelectionType.MULTI_REGION &&
            this.selectionId.equals(this.areaSelectionManager.getCurrentMultiRegionSelectionId()))
        {
            GuiUtils.reInitCurrentScreen();
        }
        else
        {
            this.listWidget.refreshEntries();
        }
    }
}
