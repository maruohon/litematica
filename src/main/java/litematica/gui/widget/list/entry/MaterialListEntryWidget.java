package litematica.gui.widget.list.entry;

import java.util.Comparator;
import java.util.Optional;
import com.google.common.collect.ImmutableList;

import malilib.config.value.SortDirection;
import malilib.gui.util.ElementOffset;
import malilib.gui.util.ScreenContext;
import malilib.gui.widget.InteractableWidget;
import malilib.gui.widget.ItemStackWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.DataListWidget;
import malilib.gui.widget.list.ListEntryWidgetInitializer;
import malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.gui.widget.list.header.DataColumn;
import malilib.render.text.StyledTextLine;
import malilib.render.text.TextStyle;
import malilib.util.StringUtils;
import litematica.gui.widget.MaterialListEntryHoverInfoWidget;
import litematica.materials.MaterialListBase;
import litematica.materials.MaterialListEntry;

public class MaterialListEntryWidget extends BaseDataListEntryWidget<MaterialListEntry>
{
    public static final DataColumn<MaterialListEntry> ITEM_COLUMN =
            new DataColumn<>("litematica.label.material_list.column.item",
                             Comparator.comparing((e) -> e.getStack().getDisplayName()));

    public static final DataColumn<MaterialListEntry> TOTAL_COUNT_COLUMN =
            new DataColumn<>("litematica.label.material_list.column.total",
                             Comparator.comparingLong(MaterialListEntry::getTotalCount),
                             SortDirection.DESCENDING);

    public static final DataColumn<MaterialListEntry> MISSING_COUNT_COLUMN =
            new DataColumn<>("litematica.label.material_list.column.missing",
                             Comparator.comparingLong(MaterialListEntry::getMissingCount),
                             SortDirection.DESCENDING);

    public static final DataColumn<MaterialListEntry> AVAILABLE_COUNT_COLUMN =
            new DataColumn<>("litematica.label.material_list.column.available",
                             Comparator.comparingLong(MaterialListEntry::getAvailableCount),
                             SortDirection.DESCENDING);

    public static final ImmutableList<DataColumn<MaterialListEntry>> COLUMNS
            = ImmutableList.of(ITEM_COLUMN, TOTAL_COUNT_COLUMN, MISSING_COUNT_COLUMN, AVAILABLE_COUNT_COLUMN);

    protected final MaterialListBase materialList;
    protected final GenericButton ignoreButton;
    protected final ItemStackWidget itemWidget;
    protected final StyledTextLine availableText;
    protected final StyledTextLine missingText;
    protected final StyledTextLine totalText;
    protected int totalColumnRightX;
    protected int missingColumnRightX;
    protected int availableColumnRightX;

    public MaterialListEntryWidget(MaterialListEntry data,
                                   DataListEntryWidgetData constructData,
                                   MaterialListBase materialList)
    {
        super(data, constructData);

        this.materialList = materialList;
        this.ignoreButton = GenericButton.create(18, "litematica.button.material_list.ignore", this::ignoreEntry);
        this.itemWidget = new ItemStackWidget(data.getStack());
        this.hoverInfoWidget = new MaterialListEntryHoverInfoWidget(data.getStack(), data.getTotalCount(), data.getMissingCount());

        long multiplier = materialList.getMultiplier();
        long totalCount = data.getTotalCount() * multiplier;
        long missingCount = materialList.getMultipliedMissingCount(data);
        long availableCount = data.getAvailableCount();

        int green = 0xFF60FF60;
        int yellow = 0xFFFFF000;
        int red = 0xFFFF6060;
        int missingColor = missingCount == 0 ? green : (availableCount >= missingCount ? yellow : red);
        int availableColor = availableCount >= missingCount ? green : red;

        this.totalText = StyledTextLine.parseFirstLine(String.valueOf(totalCount));
        this.missingText = StyledTextLine.parseFirstLine(String.valueOf(data.getMissingCount()), TextStyle.normal(missingColor));
        this.availableText = StyledTextLine.parseFirstLine(String.valueOf(data.getAvailableCount()), TextStyle.normal(availableColor));

        this.setText(StyledTextLine.parseFirstLine(data.getStack().getDisplayName()));
        this.getTextOffset().setXOffset(22);

        this.getBackgroundRenderer().getNormalSettings().setColor(this.isOdd ? 0x80101010 : 0x80202020);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidget(this.itemWidget);
        this.addWidget(this.ignoreButton);
    }

    @Override
    public void updateSubWidgetPositions()
    {
        super.updateSubWidgetPositions();

        this.itemWidget.setX(this.getX() + 2);
        this.itemWidget.centerVerticallyInside(this);

        this.ignoreButton.setRight(this.getRight() - 2);
        this.ignoreButton.centerVerticallyInside(this);
    }

    @Override
    public void renderAt(int x, int y, float z, ScreenContext ctx)
    {
        super.renderAt(x, y, z, ctx);

        int textY = y + ElementOffset.getCenteredElementOffset(this.getHeight(), 8);
        int color = 0xFFFFFFFF;
        z += 0.0125f;
        this.renderTextLineRightAligned(x + this.totalColumnRightX, textY, z, color, true, this.totalText, ctx);
        this.renderTextLineRightAligned(x + this.missingColumnRightX, textY, z, color, true, this.missingText, ctx);
        this.renderTextLineRightAligned(x + this.availableColumnRightX, textY, z, color, true, this.availableText, ctx);
    }

    protected void ignoreEntry()
    {
        this.materialList.ignoreEntry(this.data);
        this.listWidget.refreshEntries();
    }

    public static class WidgetInitializer implements ListEntryWidgetInitializer<MaterialListEntry>
    {
        @Override
        public void onListContentsRefreshed(DataListWidget<MaterialListEntry> dataListWidget, int entryWidgetWidth)
        {
            int nameColumnLength = 0;
            int totalCountLength = getRenderWidth(TOTAL_COUNT_COLUMN.getName(), 40);
            int missingCountLength = getRenderWidth(MISSING_COUNT_COLUMN.getName(), 40);
            int availableCountLength = getRenderWidth(AVAILABLE_COUNT_COLUMN.getName(), 20);

            for (MaterialListEntry entry : dataListWidget.getNonFilteredDataList())
            {
                nameColumnLength = Math.max(nameColumnLength, StringUtils.getStringWidth(entry.getStack().getDisplayName()));
            }

            int extra = 24; // leave space for the sort direction icon and padding
            nameColumnLength += 32;
            totalCountLength += extra;
            missingCountLength += extra;
            availableCountLength += extra;
            int relativeStartX = 2;

            ITEM_COLUMN.setRelativeStartX(relativeStartX);
            ITEM_COLUMN.setWidth(nameColumnLength);
            relativeStartX += nameColumnLength + 2;

            TOTAL_COUNT_COLUMN.setRelativeStartX(relativeStartX);
            TOTAL_COUNT_COLUMN.setWidth(totalCountLength);
            relativeStartX += totalCountLength + 2;

            MISSING_COUNT_COLUMN.setRelativeStartX(relativeStartX);
            MISSING_COUNT_COLUMN.setWidth(missingCountLength);
            relativeStartX += missingCountLength + 2;

            AVAILABLE_COUNT_COLUMN.setRelativeStartX(relativeStartX);
            AVAILABLE_COUNT_COLUMN.setWidth(availableCountLength);
        }

        @Override
        public void applyToEntryWidgets(DataListWidget<MaterialListEntry> dataListWidget)
        {
            int totalColumnRight = TOTAL_COUNT_COLUMN.getRelativeRight() - 3;
            int missingColumnRight = MISSING_COUNT_COLUMN.getRelativeRight() - 3;
            int availableColumnRight = AVAILABLE_COUNT_COLUMN.getRelativeRight() - 3;

            for (InteractableWidget w : dataListWidget.getEntryWidgetList())
            {
                if (w instanceof MaterialListEntryWidget)
                {
                    MaterialListEntryWidget widget = (MaterialListEntryWidget) w;
                    widget.totalColumnRightX = totalColumnRight;
                    widget.missingColumnRightX = missingColumnRight;
                    widget.availableColumnRightX = availableColumnRight;
                }
            }
        }

        protected static int getRenderWidth(Optional<StyledTextLine> optional, int minWidth)
        {
            int width = optional.isPresent() ? optional.get().renderWidth : minWidth;
            return Math.max(width, minWidth);
        }
    }
}
