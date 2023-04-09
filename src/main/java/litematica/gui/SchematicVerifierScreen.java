package litematica.gui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import malilib.gui.BaseListScreen;
import malilib.gui.util.GuiUtils;
import malilib.gui.widget.DropDownListWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.OnOffButton;
import malilib.gui.widget.list.DataListWidget;
import malilib.gui.widget.list.ListEntryWidgetFactory;
import malilib.gui.widget.list.entry.BaseListEntryWidget;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.gui.widget.list.header.ColumnizedDataListHeaderWidget;
import malilib.gui.widget.list.header.DataListHeaderWidget;
import malilib.render.text.StyledTextLine;
import malilib.util.StringUtils;
import malilib.util.data.RunStatus;
import litematica.gui.widget.list.entry.SchematicVerifierCategoryEntryWidget;
import litematica.gui.widget.list.entry.SchematicVerifierResultEntryWidget;
import litematica.render.infohud.InfoHud;
import litematica.render.infohud.RenderPhase;
import litematica.schematic.verifier.BlockStatePairCount;
import litematica.schematic.verifier.SchematicVerifier;
import litematica.schematic.verifier.VerifierResultType;
import litematica.schematic.verifier.VerifierStatus;
import litematica.util.value.BlockInfoListType;

public class SchematicVerifierScreen extends BaseListScreen<DataListWidget<BlockStatePairCount>>
{
    protected static int scrollBarPosition;

    protected final SchematicVerifier verifier;
    protected final LabelWidget statusLabel;
    protected final DropDownListWidget<VerifierResultType> visibleCategoriesDropdown;
    protected final GenericButton autoRefreshButton;
    protected final GenericButton clearSelectionButton;
    protected final GenericButton infoHudButton;
    protected final GenericButton mainMenuButton;
    protected final GenericButton rangeButton;
    protected final GenericButton resetIgnoredButton;
    protected final GenericButton resetVerifierButton;
    protected final GenericButton startButton;
    protected final GenericButton stopButton;

    public SchematicVerifierScreen(SchematicVerifier verifier)
    {
        super(10, 60, 20, 98);

        this.verifier = verifier;

        this.statusLabel = new LabelWidget();
        this.autoRefreshButton    = OnOffButton.onOff(18, "litematica.button.schematic_verifier.toggle_auto_refresh",
                                                      this.verifier::getAutoRefresh, this.verifier::toggleAutoRefreshEnabled);
        this.infoHudButton        = OnOffButton.onOff(18, "litematica.button.schematic_verifier.toggle_info_hud",
                                                      this::isHudOn, this::toggleInfoHud);
        this.clearSelectionButton = GenericButton.create(18, "litematica.button.schematic_verifier.clear_selection", this::clearSelection);
        this.mainMenuButton       = GenericButton.create(18, "litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.rangeButton          = GenericButton.create(18, this::getRangeButtonLabel, this::toggleRange);
        this.resetIgnoredButton   = GenericButton.create(18, "litematica.button.schematic_verifier.reset_ignored", this::resetIgnored);
        this.resetVerifierButton  = GenericButton.create(18, "litematica.button.schematic_verifier.reset_verifier", this::resetVerifier);
        this.startButton          = GenericButton.create(18, this::getStartButtonLabel, this::startVerifier);
        this.stopButton           = GenericButton.create(18, "litematica.button.schematic_verifier.stop", this::stopVerifier);

        this.resetIgnoredButton.setEnabledStatusSupplier(this.verifier::hasIgnoredPairs);
        this.resetVerifierButton.setEnabledStatusSupplier(this.verifier::hasData);
        this.stopButton.setEnabledStatusSupplier(() -> this.verifier.getStatus() == RunStatus.RUNNING || this.verifier.getStatus() == RunStatus.PAUSED);

        this.statusLabel.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xC0000000);
        this.statusLabel.getBorderRenderer().getNormalSettings().setEnabled(true);
        this.statusLabel.getPadding().setAll(3, 3, 0, 3);

        this.visibleCategoriesDropdown = new DropDownListWidget<>(18, 10, VerifierResultType.SELECTABLE_CATEGORIES, VerifierResultType::getDisplayName);
        this.visibleCategoriesDropdown.setSelectionHandler(new DropDownListWidget.SimpleMultiEntrySelectionHandler<>(this::isResultTypeVisible, this::toggleResultTypeVisible, this::getVisibleCategoriesCount));
        this.visibleCategoriesDropdown.setCloseOnSelect(false);
        this.visibleCategoriesDropdown.setMultiSelectionHoverTextSupplier(this::getMultiEntryHoverText);
        this.visibleCategoriesDropdown.setMultiSelectionTranslationKey("litematica.label.schematic_verifier.visible_categories_count");

        // Why was this after super.initScreen() ?
        this.verifier.setStatusChangeListener(this::onVerifierDataChanged);

        this.addPreScreenCloseListener(() -> this.verifier.setStatusChangeListener(null));
        this.setTitle("litematica.title.screen.schematic_verifier", verifier.getName());
        this.updateStatusLabel();
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.autoRefreshButton);
        this.addWidget(this.clearSelectionButton);
        this.addWidget(this.infoHudButton);
        this.addWidget(this.mainMenuButton);
        this.addWidget(this.rangeButton);
        this.addWidget(this.resetIgnoredButton);
        this.addWidget(this.resetVerifierButton);
        this.addWidget(this.startButton);
        this.addWidget(this.stopButton);
        this.addWidget(this.visibleCategoriesDropdown);

        this.addWidget(this.statusLabel);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int y = this.y + 20;
        this.startButton.setPosition(this.x + 10, y);
        this.stopButton.setPosition(this.startButton.getRight() + 2, y);
        this.resetVerifierButton.setPosition(this.stopButton.getRight() + 2, y);
        this.resetIgnoredButton.setPosition(this.resetVerifierButton.getRight() + 2, y);
        this.clearSelectionButton.setPosition(this.resetIgnoredButton.getRight() + 2, y);

        y += 20;
        this.infoHudButton.setPosition(this.x + 10, y);
        this.rangeButton.setPosition(this.infoHudButton.getRight() + 2, y);
        this.autoRefreshButton.setPosition(this.rangeButton.getRight() + 2, y);
        this.visibleCategoriesDropdown.setPosition(this.autoRefreshButton.getRight() + 4, y);

        this.statusLabel.setX(this.x + 10);
        this.statusLabel.setBottom(this.getBottom() - 2);
        this.mainMenuButton.setRight(this.getRight() - 10);
        this.mainMenuButton.setY(this.getBottom() - 22);
    }

    @Override
    protected DataListWidget<BlockStatePairCount> createListWidget()
    {
        Supplier<List<BlockStatePairCount>> supplier = this.verifier::getNonIgnoredBlockPairs;
        DataListWidget<BlockStatePairCount> listWidget = new DataListWidget<>(supplier, true);

        listWidget.setListEntryWidgetFixedHeight(20);
        listWidget.setHeaderWidgetFactory(this::createListHeaderWidget);
        listWidget.setListEntryWidgetFactory(new WidgetFactory(this.verifier, this));
        listWidget.setWidgetInitializer(new SchematicVerifierResultEntryWidget.WidgetInitializer());

        listWidget.addDefaultSearchBar();
        //listWidget.setEntryFilterStringFunction((e) -> Collections.singletonList(e.getStack().getDisplayName()));

        listWidget.setColumnSupplier(() -> SchematicVerifierResultEntryWidget.COLUMNS);
        listWidget.setDefaultSortColumn(SchematicVerifierResultEntryWidget.EXPECTED_COLUMN);
        listWidget.setHasDataColumns(true);
        listWidget.setShouldSortList(true);
        listWidget.updateActiveColumns();

        return listWidget;
    }

    protected DataListHeaderWidget<BlockStatePairCount> createListHeaderWidget(DataListWidget<BlockStatePairCount> listWidget)
    {
        ColumnizedDataListHeaderWidget<BlockStatePairCount> widget =
                new ColumnizedDataListHeaderWidget<>(this.getListWidget().getWidth() - 10, 16,
                                                     this.getListWidget(), SchematicVerifierResultEntryWidget.COLUMNS);
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

    protected void updateVerifierScreenState()
    {
        this.getListWidget().refreshEntries();
        this.updateWidgetStates();
        this.updateWidgetPositions();
        this.updateStatusLabel();
    }

    protected void onVerifierFinished()
    {
        if (GuiUtils.getCurrentScreen() == this)
        {
            this.updateVerifierScreenState();
        }
    }

    protected void onVerifierDataChanged()
    {
        if (GuiUtils.getCurrentScreen() == this)
        {
            this.updateStatusLabel();
        }
    }

    protected void clearSelection()
    {
        this.verifier.clearSelection();
        this.getListWidget().reCreateListEntryWidgets();
    }

    protected void resetIgnored()
    {
        this.verifier.resetIgnored();
        this.getListWidget().refreshEntries();
    }

    protected void resetVerifier()
    {
        this.verifier.reset();
        this.updateVerifierScreenState();
    }

    protected void startVerifier()
    {
        this.verifier.start(this::onVerifierFinished);
        this.updateVerifierScreenState();
    }

    protected void stopVerifier()
    {
        this.verifier.stop();
        this.updateVerifierScreenState();
    }

    protected void toggleInfoHud()
    {
        this.verifier.toggleInfoHudEnabled();

        if (this.verifier.getShouldRenderText(RenderPhase.POST))
        {
            InfoHud.getInstance().addInfoHudRenderer(this.verifier, true);
        }
        else
        {
            InfoHud.getInstance().removeInfoHudRenderer(this.verifier, false);
        }
    }

    protected void toggleRange()
    {
        BlockInfoListType type = this.verifier.getVerifierType();
        type = type == BlockInfoListType.ALL ? BlockInfoListType.RENDER_LAYERS : BlockInfoListType.ALL;
        this.verifier.reset();
        this.verifier.setVerifierType(type);
        this.updateVerifierScreenState();
    }

    protected boolean isResultTypeVisible(VerifierResultType type)
    {
        return this.verifier.isCategoryVisible(type);
    }

    protected void toggleResultTypeVisible(VerifierResultType type)
    {
        this.verifier.toggleCategoryVisible(type);
        this.getListWidget().refreshEntries();
    }

    protected int getVisibleCategoriesCount()
    {
        return this.verifier.getVisibleCategoriesCount();
    }

    protected void updateStatusLabel()
    {
        DecimalFormat fmt = new DecimalFormat("#.#");
        VerifierStatus status = this.verifier.getCurrentStatus();
        double pct = status.totalBlocks > 0 ? (double) status.correctBlocks * 100 / (double) status.totalBlocks : 0.0;
        List<StyledTextLine> lines = new ArrayList<>();

        StyledTextLine.translate(lines, "litematica.label.schematic_verifier.status.status",
                                 status.status.getColoredDisplayName(),
                                 status.processedChunks, status.totalChunks);
        StyledTextLine.translate(lines, "litematica.label.schematic_verifier.status.counts_1",
                                 status.totalBlocks, fmt.format(pct), status.correctBlocks);
        StyledTextLine.translate(lines, "litematica.label.schematic_verifier.status.counts_2",
                                 this.verifier.getTotalPositionCountFor(VerifierResultType.WRONG_BLOCK),
                                 this.verifier.getTotalPositionCountFor(VerifierResultType.WRONG_STATE),
                                 this.verifier.getTotalPositionCountFor(VerifierResultType.MISSING),
                                 this.verifier.getTotalPositionCountFor(VerifierResultType.EXTRA));
        this.statusLabel.setLines(lines);
    }

    protected boolean isHudOn()
    {
        return InfoHud.getInstance().isEnabled() && this.verifier.getShouldRenderText(RenderPhase.POST);
    }

    protected String getStartButtonLabel()
    {
        RunStatus status = this.verifier.getStatus();

        if (status == RunStatus.PAUSED)
        {
            return StringUtils.translate("litematica.button.schematic_verifier.resume");
        }
        else if (status == RunStatus.RUNNING)
        {
            return StringUtils.translate("litematica.button.schematic_verifier.pause");
        }
        else
        {
            return StringUtils.translate("litematica.button.schematic_verifier.start");
        }
    }

    protected String getRangeButtonLabel()
    {
        String name = this.verifier.getVerifierType().getDisplayName();
        return StringUtils.translate("litematica.button.schematic_verifier.range", name);
    }

    protected List<StyledTextLine> getMultiEntryHoverText()
    {
        List<StyledTextLine> list = new ArrayList<>();

        if (this.getVisibleCategoriesCount() > 1)
        {
            StyledTextLine.translate(list, "litematica.hover.schematic_verifier.visible_categories.title");
            String key = "litematica.hover.schematic_verifier.visible_categories.entry";

            for (VerifierResultType type : VerifierResultType.SELECTABLE_CATEGORIES)
            {
                if (this.isResultTypeVisible(type))
                {
                    StyledTextLine.translate(list, key, type.getDisplayName());
                }
            }
        }

        return list;
    }

    public static class WidgetFactory implements ListEntryWidgetFactory
    {
        // This also defines the order in the widget list
        public static final ImmutableList<VerifierResultType> CATEGORIES = ImmutableList.of(
                VerifierResultType.WRONG_BLOCK, VerifierResultType.WRONG_STATE, VerifierResultType.MISSING,
                VerifierResultType.EXTRA, VerifierResultType.CORRECT_STATE
        );

        protected final SchematicVerifier verifier;
        protected final SchematicVerifierScreen screen;
        protected final Int2ObjectArrayMap<VerifierResultType> categoryIndices = new Int2ObjectArrayMap<>();
        protected final Object2IntOpenHashMap<VerifierResultType> widgetsPerCategory = new Object2IntOpenHashMap<>();
        protected final Object2IntOpenHashMap<VerifierResultType> nonIgnoredPairsPerCategory = new Object2IntOpenHashMap<>();
        protected int totalEntryCount;

        public WidgetFactory(SchematicVerifier verifier, SchematicVerifierScreen screen)
        {
            this.verifier = verifier;
            this.screen = screen;
        }

        @Override
        public int getTotalListWidgetCount()
        {
            return this.totalEntryCount;
        }

        @Override
        public void createEntryWidgets(int startX, int startY, int usableHeight,
                                       int startIndex, Consumer<BaseListEntryWidget> widgetConsumer)
        {
            final int totalEntryCount = this.getTotalListWidgetCount();

            if (startIndex >= totalEntryCount)
            {
                return;
            }

            DataListWidget<BlockStatePairCount> listWidget = this.screen.getListWidget();
            List<BlockStatePairCount> dataList = listWidget.getFilteredDataList();
            int entryWidgetWidth = listWidget.getEntryWidgetWidth();
            int precedingCategoryTitles = 0;
            int usedHeight = 0;
            int cumulativeWidgets = 0;
            int y = startY;

            for (VerifierResultType type : CATEGORIES)
            {
                if (this.widgetsPerCategory.containsKey(type))
                {
                    if (startIndex <= cumulativeWidgets)
                    {
                        break;
                    }

                    cumulativeWidgets += this.widgetsPerCategory.getInt(type);
                    ++precedingCategoryTitles;
                }
            }

            for (int listIndex = startIndex ; listIndex < totalEntryCount; ++listIndex)
            {
                int dataIndex = listIndex - precedingCategoryTitles;
                int height = listWidget.getHeightForListEntryWidgetCreation(dataIndex);

                //System.out.printf("i: %d, usable: %d, used: %d, lh: %d, sy: %d\n", listIndex, usableHeight, usedHeight, this.listHeight, this.entryWidgetsStartY);
                if (usedHeight + height > usableHeight)
                {
                    break;
                }

                VerifierResultType type = this.categoryIndices.get(listIndex);
                BaseListEntryWidget widget;

                if (type != null)
                {
                    DataListEntryWidgetData constructData = new DataListEntryWidgetData(startX, y,
                                        entryWidgetWidth, height, listIndex, -1, listWidget);

                    int pairCount = this.nonIgnoredPairsPerCategory.getInt(type);
                    int positionCount = this.verifier.getTotalPositionCountFor(type);
                    widget = new SchematicVerifierCategoryEntryWidget(type, constructData, this.verifier,
                                                                      pairCount, positionCount);
                    ++precedingCategoryTitles;
                }
                else
                {
                    BlockStatePairCount data = dataList.get(dataIndex);
                    int originalDataIndex = listWidget.getOriginalListIndexFor(dataIndex);
                    DataListEntryWidgetData constructData = new DataListEntryWidgetData(startX, y,
                                        entryWidgetWidth, height, listIndex, originalDataIndex, listWidget);

                    widget = new SchematicVerifierResultEntryWidget(data, constructData, this.verifier);
                }

                widgetConsumer.accept(widget);

                int widgetHeight = widget.getHeight();
                usedHeight += widgetHeight;
                y += widgetHeight;
            }
        }

        @Override
        public void onListRefreshed()
        {
            this.categoryIndices.clear();
            this.widgetsPerCategory.clear();
            this.nonIgnoredPairsPerCategory.clear();
            this.totalEntryCount = 0;

            DataListWidget<BlockStatePairCount> listWidget = this.screen.getListWidget();
            // Get the possibly filtered (search bar) entries, add them to per-type lists,
            // sort those per-type lists, and then add them back to the main list in that order
            ArrayList<BlockStatePairCount> dataList = listWidget.getFilteredDataList();
            ArrayListMultimap<VerifierResultType, BlockStatePairCount> resultPairs = ArrayListMultimap.create();

            for (BlockStatePairCount pair : dataList)
            {
                resultPairs.put(pair.getPair().type, pair);
            }

            dataList.clear();
            Comparator<BlockStatePairCount> comparator = listWidget.getComparator();

            for (VerifierResultType type : CATEGORIES)
            {
                List<BlockStatePairCount> list = resultPairs.get(type);
                int dataCount = list.size();

                if (dataCount == 0 || this.verifier.isCategoryVisible(type) == false)
                {
                    continue;
                }

                list.sort(comparator);
                dataList.addAll(list);

                // The index of the category title, so in other words how many
                // other data entries and category titles in total there have been before
                this.categoryIndices.put(this.totalEntryCount, type);

                // +1 for the category title entry
                this.widgetsPerCategory.put(type, dataCount + 1);
                this.totalEntryCount += dataCount + 1;
            }

            for (BlockStatePairCount pair : this.verifier.getNonIgnoredBlockPairs())
            {
                this.nonIgnoredPairsPerCategory.addTo(pair.getPair().type, 1);
            }
        }
    }
}
