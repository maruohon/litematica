package litematica.gui.widget.list.entry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

import malilib.gui.BaseScreen;
import malilib.gui.icon.DefaultIcons;
import malilib.gui.icon.Icon;
import malilib.gui.widget.IconWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.OnOffButton;
import malilib.gui.widget.list.entry.BaseOrderableListEditEntryWidget;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.overlay.message.MessageDispatcher;
import malilib.overlay.message.MessageHelpers;
import malilib.render.text.StyledTextLine;
import malilib.util.FileNameUtils;
import malilib.util.StringUtils;
import malilib.util.position.BlockPos;
import malilib.util.position.Vec3i;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.gui.SaveSchematicPlacementScreen;
import litematica.gui.SchematicPlacementSettingsScreen;
import litematica.gui.SchematicPlacementsListScreen;
import litematica.gui.util.LitematicaIcons;
import litematica.schematic.ISchematic;
import litematica.schematic.SchematicMetadata;
import litematica.schematic.SchematicType;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;

public class SchematicPlacementEntryWidget extends BaseOrderableListEditEntryWidget<SchematicPlacement>
{
    protected final SchematicPlacement placement;
    protected final SchematicPlacementManager manager;
    protected final SchematicPlacementsListScreen screen;
    protected final GenericButton configureButton;
    protected final GenericButton duplicateButton;
    protected final GenericButton removeButton;
    protected final GenericButton saveToFileButton;
    protected final GenericButton toggleEnabledButton;
    protected final IconWidget lockedIcon;
    protected final IconWidget modificationNoticeIcon;
    protected boolean sortMode;
    protected int buttonsStartX;

    public SchematicPlacementEntryWidget(SchematicPlacement placement,
                                         DataListEntryWidgetData constructData,
                                         SchematicPlacementsListScreen screen)
    {
        super(placement, constructData);

        this.screen = screen;
        this.placement = placement;
        this.manager = DataManager.getSchematicPlacementManager();

        this.lockedIcon = new IconWidget(DefaultIcons.LOCK_LOCKED);
        this.modificationNoticeIcon = new IconWidget(DefaultIcons.EXCLAMATION_11);
        this.useAddButton = false;
        this.useMoveButtons = false;
        this.useRemoveButton = false;
        this.canReOrder = false;

        if (this.useIconButtons())
        {
            this.configureButton     = SchematicEntryWidget.createIconButton20x20(LitematicaIcons.CONFIGURATION, this::openConfigurationMenu);
            this.duplicateButton     = SchematicEntryWidget.createIconButton20x20(LitematicaIcons.DUPLICATE,     this::duplicatePlacement);
            //this.removeButton        = SchematicEntryWidget.createIconButton20x20(LitematicaIcons.TRASH_CAN,     this::removePlacement);
            this.saveToFileButton    = SchematicEntryWidget.createIconButton20x20(LitematicaIcons.SAVE_TO_DISK,  this::saveToFile);
            this.toggleEnabledButton = OnOffButton.onOff(20, "%s", placement::isEnabled, this::togglePlacementEnabled);
        }
        else
        {
            this.configureButton     = GenericButton.create("litematica.button.schematic_placements_list.configure", this::openConfigurationMenu);
            this.duplicateButton     = GenericButton.create("litematica.button.schematic_placements_list.duplicate", this::duplicatePlacement);
            this.saveToFileButton    = GenericButton.create("litematica.button.schematic_placements_list.save", this::saveToFile);
            this.toggleEnabledButton = OnOffButton.onOff(20, "litematica.button.schematic_placements_list.enabled", placement::isEnabled, this::togglePlacementEnabled);
        }

        this.removeButton = GenericButton.create("litematica.button.schematic_placements_list.remove", this::removePlacement);

        this.lockedIcon.translateAndAddHoverString("litematica.hover.placement_list.icon.placement_locked");
        this.modificationNoticeIcon.translateAndAddHoverString("litematica.hover.placement_list.icon.placement_modified");

        this.configureButton.translateAndAddHoverString("litematica.hover.button.placement_list.configure");
        this.duplicateButton.translateAndAddHoverString("litematica.hover.button.placement_list.duplicate");
        this.removeButton.translateAndAddHoverString("litematica.hover.button.placement_list.remove");
        this.saveToFileButton.translateAndAddHoverString("litematica.hover.button.placement_list.save");
        this.toggleEnabledButton.translateAndAddHoverString("litematica.hover.button.schematic_list.toggle_enabled");

        this.configureButton.setHoverInfoRequiresShift(true);
        this.duplicateButton.setHoverInfoRequiresShift(true);
        this.removeButton.setHoverInfoRequiresShift(true);
        this.saveToFileButton.setHoverInfoRequiresShift(true);
        this.toggleEnabledButton.setHoverInfoRequiresShift(true);

        @Nullable ISchematic schematic = placement.getSchematic();
        Icon icon = DefaultIcons.EXCLAMATION_11;

        if (schematic != null)
        {
            SchematicType<?> type = schematic.getType();
            icon = placement.isSchematicInMemoryOnly() ? type.getInMemoryIcon() : type.getIcon();
        }
        else if (placement.getSchematicFile() != null)
        {
            List<SchematicType<?>> types = SchematicType.getPossibleTypesFromFileName(placement.getSchematicFile());

            if (types.size() > 0)
            {
                icon = types.get(0).getIcon();
            }
        }

        this.iconOffset.setXOffset(3);
        this.textOffset.setXOffset(icon.getWidth() + 6);
        this.setIcon(icon);

        String key = placement.isEnabled() ? "litematica.button.schematic_placement_settings.entry_name.enabled" :
                                             "litematica.button.schematic_placement_settings.entry_name.disabled";
        this.setText(StyledTextLine.translateFirstLine(key, placement.getName()));

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, this.isOdd ? 0xA0101010 : 0xA0303030);
        this.getBackgroundRenderer().getHoverSettings().setEnabledAndColor(true, 0xA0707070);
        this.addHoverInfo(placement);
    }

    public SchematicPlacementEntryWidget setSortMode(boolean sortMode)
    {
        this.sortMode = sortMode;
        this.canReOrder = sortMode;
        return this;
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        if (this.sortMode == false)
        {
            this.addWidget(this.configureButton);
            this.addWidget(this.duplicateButton);
            this.addWidget(this.removeButton);
            this.addWidget(this.saveToFileButton);
            this.addWidget(this.toggleEnabledButton);
            this.addWidgetIf(this.modificationNoticeIcon, this.data.isRegionPlacementModified());
            this.addWidgetIf(this.lockedIcon, this.data.isLocked());
        }
    }

    @Override
    public void updateSubWidgetPositions()
    {
        super.updateSubWidgetPositions();

        if (this.sortMode)
        {
            this.buttonsStartX = this.getRight();
            return;
        }

        this.modificationNoticeIcon.centerVerticallyInside(this);
        this.lockedIcon.centerVerticallyInside(this);
        this.configureButton.centerVerticallyInside(this);
        this.duplicateButton.centerVerticallyInside(this);
        this.removeButton.centerVerticallyInside(this);
        this.saveToFileButton.centerVerticallyInside(this);
        this.toggleEnabledButton.centerVerticallyInside(this);

        this.removeButton.setRight(this.getRight() - 2);
        this.saveToFileButton.setRight(this.removeButton.getX() - 1);
        this.duplicateButton.setRight(this.saveToFileButton.getX() - 1);
        this.configureButton.setRight(this.duplicateButton.getX() - 1);
        this.toggleEnabledButton.setRight(this.configureButton.getX() - 1);

        this.modificationNoticeIcon.setRight(this.toggleEnabledButton.getX() - 2);
        this.lockedIcon.setRight(this.modificationNoticeIcon.getX() - 1);

        this.buttonsStartX = this.lockedIcon.getX() - 1;
    }

    @Override
    protected boolean isSelected()
    {
        return this.manager.getSelectedSchematicPlacement() == this.data;
    }

    @Override
    public boolean canHoverAt(int mouseX, int mouseY)
    {
        return mouseX <= this.buttonsStartX && super.canHoverAt(mouseX, mouseY);
    }

    protected void addHoverInfo(SchematicPlacement placement)
    {
        @Nullable Path schematicFile = placement.getSchematicFile();
        @Nullable ISchematic schematic = placement.getSchematic();
        @Nullable SchematicMetadata metadata = schematic != null ? schematic.getMetadata() : null;
        String fileName = schematicFile != null ? schematicFile.getFileName().toString() :
                          StringUtils.translate("litematica.hover.schematic_list.in_memory_only");
        List<StyledTextLine> lines = new ArrayList<>();
        boolean saved = placement.isSavedToFile();

        StyledTextLine.translate(lines, "litematica.hover.schematic_list.schematic_file", fileName);

        if (metadata != null)
        {
            StyledTextLine.translate(lines, "litematica.hover.schematic_list.schematic_name", metadata.getName());
        }

        BlockPos o = placement.getPosition();
        StyledTextLine.translate(lines, "litematica.hover.placement_list.origin", o.getX(), o.getY(), o.getZ());
        StyledTextLine.translate(lines, "litematica.hover.placement_list.rotation",
                                 placement.getRotation().getDisplayName());
        StyledTextLine.translate(lines, "litematica.hover.placement_list.mirror",
                                 placement.getMirror().getDisplayName());

        if (metadata != null)
        {
            Vec3i size = metadata.getEnclosingSize();
            StyledTextLine.translate(lines, "litematica.hover.placement_list.enclosing_size",
                                     size.getX(), size.getY(), size.getZ());
        }

        StyledTextLine.translate(lines, "litematica.hover.placement_list.sub_region_count",
                                 placement.getSubRegionCount());

        StyledTextLine.translate(lines, "litematica.hover.placement_list.is_loaded",
                                 MessageHelpers.getYesNoColored(placement.isSchematicLoaded(), false));

        // Get a cached value, to not query and read the file every rendered frame...
        if (saved && this.screen.getCachedWasModifiedSinceSaved(placement))
        {
            StyledTextLine.translate(lines, "litematica.hover.placement_list.saved_to_file.modified");
        }
        else
        {
            StyledTextLine.translate(lines, "litematica.hover.placement_list.saved_to_file.not_modified",
                                     MessageHelpers.getYesNoColored(saved, false));
        }

        if (saved)
        {
            StyledTextLine.translate(lines, "litematica.hover.placement_list.saved_to_file.save_file_name",
                                     placement.getSaveFile());
        }

        this.getHoverInfoFactory().setTextLines("hover", lines);
    }

    protected boolean useIconButtons()
    {
        return Configs.Internal.PLACEMENT_LIST_ICON_BUTTONS.getBooleanValue();
    }

    protected void duplicatePlacement()
    {
        DataManager.getSchematicPlacementManager().duplicateSchematicPlacement(this.getData());
        this.listWidget.refreshEntries();
    }

    protected void openConfigurationMenu()
    {
        if (this.placement.isSchematicLoaded())
        {
            BaseScreen.openScreenWithParent(new SchematicPlacementSettingsScreen(this.placement));
        }
        else
        {
            MessageDispatcher.error("litematica.message.error.schematic_placement_configure_schematic_not_loaded",
                                    this.placement.getName());
        }
    }

    protected void removePlacement()
    {
        SchematicPlacement placement = this.getData();

        if (placement.isLocked() && BaseScreen.isShiftDown() == false)
        {
            MessageDispatcher.error("litematica.message.error.placement_list.remove_failed_locked");
        }
        else
        {
            this.manager.removeSchematicPlacement(placement);
            this.listWidget.refreshEntries();
        }
    }

    protected void saveToFile()
    {
        if (BaseScreen.isShiftDown())
        {
            if (this.getData().saveToFileIfChanged() == false)
            {
                MessageDispatcher.error("litematica.message.error.placement_list.save_failed");
            }
        }
        else
        {
            // TODO Manually saving should be allowed even if the placement should not be saved automatically
            //      in the active list (such as in-memory schematic based placements).
            //      However placements for the VCS system probably shouldn't be saved here either?
            //      In that case also don't add the Save button for such placements.
            /*
            if (this.data.shouldBeSaved() == false)
            {
                MessageDispatcher.warning().translate("litematica.message.error.schematic_placement.save.should_not_save");
                return;
            }
            */

            BaseScreen.openScreenWithParent(new SaveSchematicPlacementScreen(this.data));
        }

        this.screen.clearModifiedSinceSavedCache();
        this.listWidget.reCreateListEntryWidgets();
    }

    protected void togglePlacementEnabled()
    {
        DataManager.getSchematicPlacementManager().toggleEnabled(this.getData());
        this.listWidget.reCreateListEntryWidgets();
    }

    public static boolean placementSearchFilter(SchematicPlacement entry, List<String> searchTerms)
    {
        String fileName = null;

        if (entry.getSchematicFile() != null)
        {
            fileName = entry.getSchematicFile().getFileName().toString().toLowerCase(Locale.ROOT);
            fileName = FileNameUtils.getFileNameWithoutExtension(fileName);
        }

        for (String searchTerm : searchTerms)
        {
            if (entry.getName().toLowerCase(Locale.ROOT).contains(searchTerm))
            {
                return true;
            }

            if (fileName != null && fileName.contains(searchTerm))
            {
                return true;
            }
        }

        return false;
    }
}
