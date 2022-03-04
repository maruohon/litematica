package fi.dy.masa.litematica.gui.widget.list.entry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.util.LitematicaIcons;
import fi.dy.masa.litematica.gui.SchematicPlacementsListScreen;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementUnloaded;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.icon.Icon;
import fi.dy.masa.malilib.gui.widget.IconWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.OnOffButton;
import fi.dy.masa.malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import fi.dy.masa.malilib.gui.widget.list.entry.DataListEntryWidgetData;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.overlay.message.MessageHelpers;
import fi.dy.masa.malilib.render.text.StyledTextLine;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class SchematicPlacementEntryWidget extends BaseDataListEntryWidget<SchematicPlacementUnloaded>
{
    protected final SchematicPlacementManager manager;
    protected final SchematicPlacementsListScreen gui;
    @Nullable protected final SchematicPlacement loadedPlacement;
    protected final GenericButton configureButton;
    protected final GenericButton duplicateButton;
    protected final GenericButton removeButton;
    protected final GenericButton saveToFileButton;
    protected final GenericButton toggleEnabledButton;
    protected final IconWidget lockedIcon;
    protected final IconWidget modificationNoticeIcon;
    protected final IconWidget schematicTypeIcon;
    protected int buttonsStartX;

    public SchematicPlacementEntryWidget(SchematicPlacementUnloaded placement,
                                         DataListEntryWidgetData constructData,
                                         SchematicPlacementsListScreen gui)
    {
        super(placement, constructData);

        this.gui = gui;
        this.loadedPlacement = placement.isLoaded() ? (SchematicPlacement) placement : null;
        this.manager = DataManager.getSchematicPlacementManager();

        this.lockedIcon = new IconWidget(LitematicaIcons.LOCK_LOCKED);
        this.modificationNoticeIcon = new IconWidget(LitematicaIcons.NOTICE_EXCLAMATION_11);

        if (this.useIconButtons())
        {
            this.configureButton     = SchematicEntryWidget.createIconButton20x20(LitematicaIcons.CONFIGURATION, this::openConfigurationMenu);
            this.duplicateButton     = SchematicEntryWidget.createIconButton20x20(LitematicaIcons.DUPLICATE,     this::duplicatePlacement);
            this.removeButton        = SchematicEntryWidget.createIconButton20x20(LitematicaIcons.TRASH_CAN,     this::removePlacement);
            this.saveToFileButton    = SchematicEntryWidget.createIconButton20x20(LitematicaIcons.SAVE_TO_DISK,  this::saveToFile);
            this.toggleEnabledButton = OnOffButton.onOff(20, "%s", placement::isEnabled, this::toggleEnabled);
        }
        else
        {
            this.configureButton     = GenericButton.create("litematica.button.misc.configure", this::openConfigurationMenu);
            this.duplicateButton     = GenericButton.create("litematica.button.misc.duplicate", this::duplicatePlacement);
            this.removeButton        = GenericButton.create("litematica.button.misc.remove", this::removePlacement);
            this.saveToFileButton    = GenericButton.create("litematica.button.misc.save", this::saveToFile);
            this.toggleEnabledButton = OnOffButton.onOff(20, "litematica.button.placement_list.placement_enabled", placement::isEnabled, this::toggleEnabled);
        }

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

        Icon icon = this.loadedPlacement != null && this.loadedPlacement.getSchematicFile() != null ? this.loadedPlacement.getSchematic().getType().getIcon() : LitematicaIcons.SCHEMATIC_TYPE_MEMORY;
        this.schematicTypeIcon = new IconWidget(icon);
        this.textOffset.setXOffset(icon.getWidth() + 4);
        this.textSettings.setTextColor(placement.isEnabled() ? 0xFF00FF00 : 0xFFFF0000);

        // boolean placementSelected = this.manager.getSelectedSchematicPlacement() == this.placement;

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, this.isOdd ? 0x70606060 : 0x70909090);
        this.getBackgroundRenderer().getNormalSettings().setEnabled(true);
        this.setText(StyledTextLine.of(placement.getName()));
        this.addHoverInfo(placement);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidget(this.schematicTypeIcon);
        this.addWidget(this.configureButton);
        this.addWidget(this.duplicateButton);
        this.addWidget(this.removeButton);
        this.addWidget(this.saveToFileButton);
        this.addWidget(this.toggleEnabledButton);

        if (this.data.isRegionPlacementModified())
        {
            this.addWidget(this.modificationNoticeIcon);
        }

        if (this.data.isLocked())
        {
            this.addWidget(this.lockedIcon);
        }
    }

    @Override
    public void updateSubWidgetPositions()
    {
        super.updateSubWidgetPositions();

        this.schematicTypeIcon.centerVerticallyInside(this);
        this.modificationNoticeIcon.centerVerticallyInside(this);
        this.lockedIcon.centerVerticallyInside(this);
        this.configureButton.centerVerticallyInside(this);
        this.duplicateButton.centerVerticallyInside(this);
        this.removeButton.centerVerticallyInside(this);
        this.saveToFileButton.centerVerticallyInside(this);
        this.toggleEnabledButton.centerVerticallyInside(this);

        this.schematicTypeIcon.setX(this.getX() + 2);
        this.removeButton.setRight(this.getRight() - 2);
        this.saveToFileButton.setRight(this.removeButton.getX() - 1);
        this.duplicateButton.setRight(this.saveToFileButton.getX() - 1);
        this.toggleEnabledButton.setRight(this.duplicateButton.getX() - 1);
        this.configureButton.setRight(this.toggleEnabledButton.getX() - 1);
        this.modificationNoticeIcon.setRight(this.configureButton.getX() - 2);
        this.lockedIcon.setRight(this.modificationNoticeIcon.getX() - 1);

        this.buttonsStartX = this.lockedIcon.getX() - 1;
    }

    @Override
    protected boolean isSelected()
    {
        return this.manager.getSelectedSchematicPlacement() == this.data;
    }

    @Override
    public boolean canHoverAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX <= this.buttonsStartX && super.canHoverAt(mouseX, mouseY, mouseButton);
    }

    protected void addHoverInfo(SchematicPlacementUnloaded placement)
    {
        File schematicFile = placement.getSchematicFile();
        SchematicMetadata metadata = this.loadedPlacement != null ? this.loadedPlacement.getSchematic().getMetadata() : null;
        String fileName = schematicFile != null ? schematicFile.getName() :
                          StringUtils.translate("litematica.hover.schematic_list.in_memory_only");
        List<String> lines = new ArrayList<>();
        boolean saved = placement.isSavedToFile();

        if (metadata != null)
        {
            lines.add(StringUtils.translate("litematica.hover.schematic_list.schematic_name", metadata.getName()));
        }

        lines.add(StringUtils.translate("litematica.hover.schematic_list.schematic_file", fileName));
        lines.add(StringUtils.translate("litematica.hover.placement_list.is_loaded",
                                        MessageHelpers.getYesNoColored(placement.isLoaded(), false)));

        // Get a cached value, to not query and read the file every rendered frame...
        if (saved && this.gui.getCachedWasModifiedSinceSaved(placement))
        {
            lines.add(StringUtils.translate("litematica.hover.placement_list.saved_to_file.modified"));
        }
        else
        {
            lines.add(StringUtils.translate("litematica.hover.placement_list.saved_to_file.not_modified",
                                            MessageHelpers.getYesNoColored(saved, false)));
        }

        BlockPos o = placement.getOrigin();
        lines.add(StringUtils.translate("litematica.hover.placement_list.origin", o.getX(), o.getY(), o.getZ()));

        if (metadata != null)
        {
            lines.add(StringUtils.translate("litematica.hover.placement_list.sub_region_count",
                                            this.loadedPlacement.getSubRegionCount()));

            Vec3i size = metadata.getEnclosingSize();
            lines.add(StringUtils.translate("litematica.hover.placement_list.enclosing_size",
                                            size.getX(), size.getY(), size.getZ()));
        }

        this.getHoverInfoFactory().addStrings(lines);
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
        /* TODO FIXME malilib refactor
        GuiPlacementConfiguration gui = new GuiPlacementConfiguration((SchematicPlacement) this.widget.placement);
        gui.setParent(this.widget.gui);
        BaseScreen.openScreen(gui);
        */
    }

    protected void removePlacement()
    {
        SchematicPlacementUnloaded placement = this.getData();

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
        if (this.getData().saveToFileIfChanged() == false)
        {
            MessageDispatcher.error("litematica.message.error.placement_list.save_failed");
        }
    }

    protected void toggleEnabled()
    {
        DataManager.getSchematicPlacementManager().toggleEnabled(this.getData());
        this.listWidget.refreshEntries();
    }

    public static boolean placementSearchFilter(SchematicPlacementUnloaded entry, List<String> searchTerms)
    {
        String fileName = null;

        if (entry.getSchematicFile() != null)
        {
            fileName = entry.getSchematicFile().getName().toLowerCase(Locale.ROOT);
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
