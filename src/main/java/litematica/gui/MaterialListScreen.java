package litematica.gui;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import malilib.gui.BaseListScreen;
import malilib.gui.BaseScreen;
import malilib.gui.icon.DefaultIcons;
import malilib.gui.util.GuiUtils;
import malilib.gui.widget.InfoIconWidget;
import malilib.gui.widget.IntegerEditWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.OnOffButton;
import malilib.gui.widget.list.DataListWidget;
import malilib.gui.widget.list.header.ColumnizedDataListHeaderWidget;
import malilib.gui.widget.list.header.DataListHeaderWidget;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.StringUtils;
import malilib.util.datadump.DataDump;
import malilib.util.datadump.DataDump.Format;
import litematica.data.DataManager;
import litematica.gui.widget.list.entry.MaterialListEntryWidget;
import litematica.materials.MaterialCache;
import litematica.materials.MaterialListAreaAnalyzer;
import litematica.materials.MaterialListBase;
import litematica.materials.MaterialListEntry;
import litematica.materials.MaterialListHudRenderer;
import litematica.materials.MaterialListUtils;
import litematica.render.infohud.InfoHud;
import litematica.util.LitematicaDirectories;
import litematica.util.value.BlockInfoListType;

public class MaterialListScreen extends BaseListScreen<DataListWidget<MaterialListEntry>>
{
    protected static int scrollBarPosition;

    protected final MaterialListBase materialList;
    protected final LabelWidget progressLabel;
    protected final InfoIconWidget infoIcon;
    protected final IntegerEditWidget multiplierEditor;
    protected final GenericButton clearCacheButton;
    protected final GenericButton clearIgnoredButton;
    protected final GenericButton exportButton;
    protected final GenericButton hiveAvailableButton;
    protected final GenericButton hudButton;
    protected final GenericButton mainMenuButton;
    protected final GenericButton refreshButton;
    protected final GenericButton scopeButton;

    public MaterialListScreen(MaterialListBase materialList)
    {
        super(10, 59, 20, 82);

        this.materialList = materialList;
        this.materialList.setCompletionListener(this::onRefreshFinished);
        this.shouldRestoreScrollbarPosition = true;

        this.progressLabel = new LabelWidget();
        this.infoIcon = new InfoIconWidget(DefaultIcons.INFO_11, "litematica.hover.material_list.info_widget");

        this.clearCacheButton   = GenericButton.create(18, "litematica.button.material_list.clear_cache", this::clearCache);
        this.clearIgnoredButton = GenericButton.create(18, "litematica.button.material_list.clear_ignored", this::clearIgnored);
        this.exportButton       = GenericButton.create(18, "litematica.button.material_list.export_to_file", this::exportToFile);
        this.mainMenuButton     = GenericButton.create(18, "litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.refreshButton      = GenericButton.create(18, "litematica.button.material_list.refresh", this::refreshList);
        this.scopeButton        = GenericButton.create(18, this::getScopeButtonDisplayName, this::toggleScope);

        this.hiveAvailableButton = OnOffButton.onOff(18, "litematica.button.material_list.hide_available",
                                                     this.materialList::getHideAvailable, this::toggleHideAvailable);
        this.hudButton = OnOffButton.onOff(18, "litematica.button.material_list.toggle_hud",
                                           this.materialList.getHudRenderer()::getShouldRenderCustom, this::toggleHud);

        this.multiplierEditor = new IntegerEditWidget(80, 18, 1, 1, 1000000, this::setMultiplier);

        this.clearCacheButton.translateAndAddHoverString("litematica.hover.button.material_list.clear_cache");
        this.exportButton.translateAndAddHoverString("litematica.hover.button.material_list.export_shift_for_csv");
        this.multiplierEditor.getTextField().translateAndAddHoverString("litematica.hover.material_list.multiplier");

        MaterialListUtils.updateAvailableCounts(this.materialList.getAllMaterials());

        // Remember the last opened material list, for the hotkey
        if (DataManager.getMaterialList() == null)
        {
            DataManager.setMaterialList(materialList);
        }

        this.setTitle(materialList.getTitle());
        this.updateProgressLabel();
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.progressLabel);
        this.addWidget(this.infoIcon);
        this.addWidget(this.multiplierEditor);
        this.addWidget(this.clearCacheButton);
        this.addWidget(this.clearIgnoredButton);
        this.addWidget(this.exportButton);
        this.addWidget(this.hiveAvailableButton);
        this.addWidget(this.hudButton);
        this.addWidget(this.mainMenuButton);
        this.addWidget(this.refreshButton);
        this.addWidget(this.scopeButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.infoIcon.setRight(this.getRight() - 20);
        this.infoIcon.setY(this.y + 4);

        int y = this.y + 20;
        this.scopeButton.setPosition(this.x + 10, y);
        this.hiveAvailableButton.setPosition(this.scopeButton.getRight() + 2, y);
        this.hudButton.setPosition(this.hiveAvailableButton.getRight() + 2, y);

        y += 19;
        this.refreshButton.setPosition(this.scopeButton.getX(), y);
        this.clearIgnoredButton.setPosition(this.refreshButton.getRight() + 2, y);
        this.clearCacheButton.setPosition(this.clearIgnoredButton.getRight() + 2, y);
        this.exportButton.setPosition(this.clearCacheButton.getRight() + 2, y);

        this.multiplierEditor.setRight(this.getListWidget().getRight() - 2);
        this.multiplierEditor.setBottom(this.getListY() - 2);

        this.progressLabel.setPosition(this.x + 12, this.getListWidget().getBottom() + 4);

        this.mainMenuButton.setRight(this.getRight() - 10);
        this.mainMenuButton.setY(this.getBottom() - 22);
    }

    @Override
    protected DataListWidget<MaterialListEntry> createListWidget()
    {
        Supplier<List<MaterialListEntry>> supplier = () -> this.materialList.getFilteredMaterials(true);
        DataListWidget<MaterialListEntry> listWidget = new DataListWidget<>(supplier, true);

        listWidget.setListEntryWidgetFixedHeight(20);
        listWidget.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0x80101010);
        listWidget.addDefaultSearchBar();
        listWidget.setHeaderWidgetFactory(this::createListHeaderWidget);
        listWidget.setEntryFilterStringFunction((e) -> Collections.singletonList(e.getStack().getDisplayName()));
        listWidget.setDataListEntryWidgetFactory((d, cd) -> new MaterialListEntryWidget(d, cd, this.materialList));
        listWidget.setWidgetInitializer(new MaterialListEntryWidget.WidgetInitializer());

        listWidget.setColumnSupplier(() -> MaterialListEntryWidget.COLUMNS);
        listWidget.setDefaultSortColumn(MaterialListEntryWidget.TOTAL_COUNT_COLUMN);
        listWidget.setHasDataColumns(true);
        listWidget.setShouldSortList(true);
        listWidget.updateActiveColumns();

        return listWidget;
    }

    protected DataListHeaderWidget<MaterialListEntry> createListHeaderWidget(DataListWidget<MaterialListEntry> listWidget)
    {
        ColumnizedDataListHeaderWidget<MaterialListEntry> widget =
                new ColumnizedDataListHeaderWidget<>(this.getListWidget().getWidth() - 10, 16,
                                                     this.getListWidget(), MaterialListEntryWidget.COLUMNS);
        widget.getMargin().setAll(2, 0, 0, 1);
        return widget;
    }

    @Override
    public void saveScrollBarPositionForCurrentTab()
    {
        scrollBarPosition = this.getCurrentScrollbarPosition();
    }

    @Override
    public void restoreScrollBarPositionForCurrentTab()
    {
        this.setCurrentScrollbarPosition(scrollBarPosition);
    }

    protected void onRefreshFinished()
    {
        // re-create the list widgets when a material list task finishes, but only if this screen is still open
        if (GuiUtils.getCurrentScreen() == this)
        {
            this.getListWidget().refreshEntries();
            this.updateProgressLabel();
        }
    }

    protected void setMultiplier(int multiplier)
    {
        this.materialList.setMultiplier(multiplier);
        this.getListWidget().refreshEntries();
    }

    protected void clearCache()
    {
        MaterialCache.getInstance().clearCache();
        MessageDispatcher.success(3000).translate("litematica.message.info.material_cache_cleared");
    }

    protected void clearIgnored()
    {
        this.materialList.clearIgnored();
        this.getListWidget().refreshEntries();
    }

    protected void exportToFile()
    {
        boolean csv = BaseScreen.isShiftDown();
        Format format = csv ? Format.CSV : Format.ASCII;
        Path dir = LitematicaDirectories.getMaterialListDirectory();
        Path file = DataDump.dumpDataToFile(dir, "material_list", csv ? ".csv" : ".txt",
                                            this.materialList.getMaterialListDump(format).getLines());

        if (file != null)
        {
            String key = "litematica.message.info.material_list.written_to_file";
            MessageDispatcher.generic(key, file.getFileName().toString());
            StringUtils.sendOpenFileChatMessage(key, file);
        }
    }

    protected void refreshList()
    {
        this.materialList.reCreateMaterialList();
    }

    protected void toggleHideAvailable()
    {
        this.materialList.setHideAvailable(! this.materialList.getHideAvailable());
        this.materialList.refreshPreFilteredList();
        this.materialList.reCreateFilteredList();
        this.getListWidget().refreshEntries();
    }

    protected void toggleHud()
    {
        MaterialListHudRenderer renderer = this.materialList.getHudRenderer();
        renderer.toggleShouldRender();

        if (this.materialList.getHudRenderer().getShouldRenderCustom())
        {
            InfoHud.getInstance().addInfoHudRenderer(renderer, true);
        }
        else
        {
            InfoHud.getInstance().removeInfoHudRenderersOfType(renderer.getClass(), true);
        }
    }

    protected void toggleScope()
    {
        BlockInfoListType type = this.materialList.getMaterialListType();
        type = type == BlockInfoListType.ALL ? BlockInfoListType.RENDER_LAYERS : BlockInfoListType.ALL;
        this.materialList.setMaterialListType(type);
        this.materialList.reCreateMaterialList();
        this.scopeButton.updateButtonState();
        this.getListWidget().refreshEntries();
        this.updateWidgetPositions();
    }

    protected String getScopeButtonDisplayName()
    {
        return StringUtils.translate("litematica.button.material_list.scope",
                                     this.materialList.getMaterialListType().getDisplayName());
    }

    protected void updateProgressLabel()
    {
        long total = this.materialList.getCountTotal();

        if (total != 0 && (this.materialList instanceof MaterialListAreaAnalyzer) == false)
        {
            long missing = this.materialList.getCountMissing() - this.materialList.getCountMismatched();
            long mismatch = this.materialList.getCountMismatched();

            if (missing == 0 && mismatch == 0)
            {
                this.progressLabel.translateSetLines("litematica.label.material_list.progress.finished", total);
            }
            else
            {
                DecimalFormat format = new DecimalFormat("#.#");
                String pctDone = format.format(((double) (total - (missing + mismatch)) / (double) total) * 100);
                String pctMissing = format.format(((double) missing / (double) total) * 100);
                String pctIncorrect = format.format(((double) mismatch / (double) total) * 100);

                String key = "litematica.label.material_list.progress.incomplete";
                this.progressLabel.translateSetLines(key, total, pctDone, pctMissing, pctIncorrect);
            }
        }
    }
}
