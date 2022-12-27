package litematica.gui.widget.list.entry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import net.minecraft.util.math.BlockPos;

import malilib.gui.BaseScreen;
import malilib.gui.icon.Icon;
import malilib.gui.icon.MultiIcon;
import malilib.gui.widget.IconWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.BaseFileBrowserWidget;
import malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.listener.EventListener;
import malilib.render.text.StyledTextLine;
import malilib.util.FileNameUtils;
import malilib.util.StringUtils;
import malilib.util.game.wrap.EntityWrap;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.data.SchematicHolder;
import litematica.gui.SaveConvertSchematicScreen;
import litematica.gui.util.LitematicaIcons;
import litematica.schematic.ISchematic;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;

public class SchematicEntryWidget extends BaseDataListEntryWidget<ISchematic>
{
    protected final GenericButton createPlacementButton;
    protected final GenericButton reloadButton;
    protected final GenericButton saveToFileButton;
    protected final GenericButton unloadButton;
    protected final IconWidget modificationNoticeIcon;
    protected final IconWidget schematicTypeIcon;
    protected int buttonsStartX;

    public SchematicEntryWidget(ISchematic schematic, DataListEntryWidgetData constructData)
    {
        super(schematic, constructData);

        this.modificationNoticeIcon = new IconWidget(LitematicaIcons.NOTICE_EXCLAMATION_11);
        String timeStr = BaseFileBrowserWidget.DATE_FORMAT.format(new Date(schematic.getMetadata().getTimeModified()));
        this.modificationNoticeIcon.translateAndAddHoverString("litematica.hover.schematic_list.modified_on", timeStr);

        if (this.useIconButtons())
        {
            this.createPlacementButton = createIconButton20x20(LitematicaIcons.PLACEMENT,    this::createPlacement);
            this.reloadButton          = createIconButton20x20(LitematicaIcons.RELOAD,       this::reloadFromFile);
            this.saveToFileButton      = createIconButton20x20(LitematicaIcons.SAVE_TO_DISK, this::saveToFile);
            this.unloadButton          = createIconButton20x20(LitematicaIcons.TRASH_CAN,    this::unloadSchematic);
        }
        else
        {
            this.createPlacementButton = GenericButton.create("litematica.button.schematic_list.create_placement", this::createPlacement);
            this.reloadButton          = GenericButton.create("litematica.button.schematic_list.reload",           this::reloadFromFile);
            this.saveToFileButton      = GenericButton.create("litematica.button.schematic_list.save_to_file",     this::saveToFile);
            this.unloadButton          = GenericButton.create("litematica.button.schematic_list.unload",           this::unloadSchematic);
        }

        this.createPlacementButton.translateAndAddHoverString("litematica.hover.button.schematic_list.create_placement");
        this.reloadButton.translateAndAddHoverString("litematica.hover.button.schematic_list.reload_schematic");
        this.saveToFileButton.translateAndAddHoverString("litematica.hover.button.schematic_list.save_to_file");
        this.unloadButton.translateAndAddHoverString("litematica.hover.button.schematic_list.unload");

        this.createPlacementButton.setHoverInfoRequiresShift(true);
        this.reloadButton.setHoverInfoRequiresShift(true);
        this.saveToFileButton.setHoverInfoRequiresShift(true);
        this.unloadButton.setHoverInfoRequiresShift(true);

        Icon icon = schematic.getFile() != null ? schematic.getType().getIcon() : LitematicaIcons.SCHEMATIC_TYPE_MEMORY;
        boolean modified = schematic.getMetadata().wasModifiedSinceSaved();
        this.schematicTypeIcon = new IconWidget(icon);
        this.textOffset.setXOffset(icon.getWidth() + 4);
        this.textSettings.setTextColor(modified ? 0xFFFF9010 : 0xFFFFFFFF);

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, this.isOdd ? 0xA0101010 : 0xA0303030);
        this.getBackgroundRenderer().getHoverSettings().setEnabledAndColor(true, 0xA0707070);
        this.setText(StyledTextLine.of(schematic.getMetadata().getName()));
        this.addHoverInfo(schematic);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidget(this.schematicTypeIcon);
        this.addWidget(this.createPlacementButton);
        this.addWidget(this.reloadButton);
        this.addWidget(this.saveToFileButton);
        this.addWidget(this.unloadButton);

        if (this.getData().getMetadata().wasModifiedSinceSaved())
        {
            this.addWidget(this.modificationNoticeIcon);
        }
    }

    @Override
    public void updateSubWidgetPositions()
    {
        super.updateSubWidgetPositions();

        this.schematicTypeIcon.centerVerticallyInside(this);
        this.modificationNoticeIcon.centerVerticallyInside(this);
        this.createPlacementButton.centerVerticallyInside(this);
        this.reloadButton.centerVerticallyInside(this);
        this.saveToFileButton.centerVerticallyInside(this);
        this.unloadButton.centerVerticallyInside(this);

        this.schematicTypeIcon.setX(this.getX() + 2);
        this.unloadButton.setRight(this.getRight() - 2);
        this.reloadButton.setRight(this.unloadButton.getX() - 1);
        this.saveToFileButton.setRight(this.reloadButton.getX() - 1);
        this.createPlacementButton.setRight(this.saveToFileButton.getX() - 1);
        this.modificationNoticeIcon.setRight(this.createPlacementButton.getX() - 2);

        this.buttonsStartX = this.modificationNoticeIcon.getX() - 1;
    }

    @Override
    public boolean canHoverAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX <= this.buttonsStartX && super.canHoverAt(mouseX, mouseY, mouseButton);
    }

    public static GenericButton createIconButton20x20(MultiIcon icon, EventListener listener)
    {
        GenericButton button = GenericButton.create(20, 20, icon);
        button.setRenderButtonBackgroundTexture(true);
        button.setActionListener(listener);
        return button;
    }

    protected void addHoverInfo(ISchematic schematic)
    {
        List<String> lines = new ArrayList<>();
        Path schematicFile = schematic.getFile();
        String fileName = schematicFile != null ? schematicFile.getFileName().toString() :
                          StringUtils.translate("litematica.hover.schematic_list.in_memory_only");

        lines.add(StringUtils.translate("litematica.hover.schematic_list.schematic_name", schematic.getMetadata().getName()));
        lines.add(StringUtils.translate("litematica.hover.schematic_list.schematic_file", fileName));
        lines.add(StringUtils.translate("litematica.hover.schematic_list.schematic_type", schematic.getType().getDisplayName()));

        if (schematic.getMetadata().wasModifiedSinceSaved())
        {
            String timeStr = BaseFileBrowserWidget.DATE_FORMAT.format(new Date(schematic.getMetadata().getTimeModified()));
            lines.add(StringUtils.translate("litematica.hover.schematic_list.modified_on", timeStr));
        }

        this.getHoverInfoFactory().addStrings(lines);
    }

    protected boolean useIconButtons()
    {
        return Configs.Internal.SCHEMATIC_LIST_ICON_BUTTONS.getBooleanValue();
    }

    protected void createPlacement()
    {
        ISchematic schematic = this.getData();
        BlockPos pos = EntityWrap.getCameraEntityBlockPos();
        String name = schematic.getMetadata().getName();
        boolean createAsEnabled = BaseScreen.isShiftDown() == false;

        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        SchematicPlacement placement = SchematicPlacement.createFor(schematic, pos, name, createAsEnabled);
        manager.addSchematicPlacement(placement, true);
        manager.setSelectedSchematicPlacement(placement);
    }

    protected void reloadFromFile()
    {
        this.getData().readFromFile();
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        manager.getAllPlacementsOfSchematic(this.getData()).forEach(manager::markChunksForRebuild);
    }

    protected void saveToFile()
    {
        BaseScreen.openScreenWithParent(new SaveConvertSchematicScreen(this.getData(), true));
    }

    protected void unloadSchematic()
    {
        SchematicHolder.getInstance().removeSchematic(this.getData());
        this.listWidget.refreshEntries();
    }

    public static boolean schematicSearchFilter(ISchematic entry, List<String> searchTerms)
    {
        String fileName = null;

        if (entry.getFile() != null)
        {
            fileName = entry.getFile().getFileName().toString().toLowerCase(Locale.ROOT);
            fileName = FileNameUtils.getFileNameWithoutExtension(fileName);
        }

        for (String searchTerm : searchTerms)
        {
            if (entry.getMetadata().getName().toLowerCase(Locale.ROOT).contains(searchTerm))
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
