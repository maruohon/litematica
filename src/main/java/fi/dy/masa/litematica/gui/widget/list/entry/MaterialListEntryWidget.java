package fi.dy.masa.litematica.gui.widget.list.entry;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.gui.widget.MaterialListEntryHoverInfoWidget;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.config.value.SortDirection;
import fi.dy.masa.malilib.gui.util.ElementOffset;
import fi.dy.masa.malilib.gui.util.ScreenContext;
import fi.dy.masa.malilib.gui.widget.ItemStackWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import fi.dy.masa.malilib.gui.widget.list.entry.DataListEntryWidgetData;
import fi.dy.masa.malilib.gui.widget.list.header.DataColumn;
import fi.dy.masa.malilib.render.text.StyledTextLine;
import fi.dy.masa.malilib.render.text.TextStyle;
import fi.dy.masa.malilib.util.StringUtils;

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
    protected int totalColumnX;
    protected int missingColumnX;
    protected int availableColumnX;

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

        this.totalText = StyledTextLine.of(String.valueOf(totalCount));

        int green = 0xFF60FF60;
        int yellow = 0xFFFFF000;
        int red = 0xFFFF6060;
        int color = missingCount == 0 ? green : (availableCount >= missingCount ? yellow : red);
        this.missingText = StyledTextLine.of(String.valueOf(data.getMissingCount()), TextStyle.normal(color));

        color = availableCount >= missingCount ? green : red;
        this.availableText = StyledTextLine.of(String.valueOf(data.getAvailableCount()), TextStyle.normal(color));

        this.setText(StyledTextLine.of(data.getStack().getDisplayName()));
        this.getTextOffset().setXOffset(22);

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, this.isOdd ? 0xC0101010 : 0xC0303030);
        this.getBackgroundRenderer().getHoverSettings().setEnabledAndColor(true, 0xC0404040);
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
        this.renderTextLineRightAligned(x + this.totalColumnX, textY, z, color, true, this.totalText, ctx);
        this.renderTextLineRightAligned(x + this.missingColumnX, textY, z, color, true, this.missingText, ctx);
        this.renderTextLineRightAligned(x + this.availableColumnX, textY, z, color, true, this.availableText, ctx);
    }

    protected void ignoreEntry()
    {
        this.materialList.ignoreEntry(this.data);
        this.listWidget.refreshEntries();
    }

    @Nullable
    @Override
    public Consumer<MaterialListEntryWidget> createWidgetInitializer(List<MaterialListEntry> dataList)
    {
        int nameColumnLength = 0;
        int totalCountLength = getRenderWidth(TOTAL_COUNT_COLUMN.getName(), 40);
        int missingCountLength = getRenderWidth(MISSING_COUNT_COLUMN.getName(), 40);
        int availableCountLength = getRenderWidth(AVAILABLE_COUNT_COLUMN.getName(), 20);

        for (MaterialListEntry entry : dataList)
        {
            nameColumnLength = Math.max(nameColumnLength, StringUtils.getStringWidth(entry.getStack().getDisplayName()));
        }

        int extra = 24; // leave space for the sort direction icon and padding
        nameColumnLength += 32;
        totalCountLength += extra;
        missingCountLength += extra;
        availableCountLength += extra;

        ITEM_COLUMN.setRelativeStartX(2);
        ITEM_COLUMN.setWidth(nameColumnLength);

        int relativeStartX = nameColumnLength + 4;
        TOTAL_COUNT_COLUMN.setRelativeStartX(relativeStartX);
        TOTAL_COUNT_COLUMN.setWidth(totalCountLength);

        relativeStartX += totalCountLength + 2;
        MISSING_COUNT_COLUMN.setRelativeStartX(relativeStartX);
        MISSING_COUNT_COLUMN.setWidth(missingCountLength);

        relativeStartX += missingCountLength + 2;
        AVAILABLE_COUNT_COLUMN.setRelativeStartX(relativeStartX);
        AVAILABLE_COUNT_COLUMN.setWidth(availableCountLength);

        return (w) -> {
            w.totalColumnX = TOTAL_COUNT_COLUMN.getRelativeStartX() + TOTAL_COUNT_COLUMN.getWidth() - 4;
            w.missingColumnX = MISSING_COUNT_COLUMN.getRelativeStartX() + MISSING_COUNT_COLUMN.getWidth() - 4;
            w.availableColumnX = AVAILABLE_COUNT_COLUMN.getRelativeStartX() + AVAILABLE_COUNT_COLUMN.getWidth() - 4;
        };
    }

    protected static int getRenderWidth(Optional<StyledTextLine> optional, int minWidth)
    {
        int width = optional.isPresent() ? optional.get().renderWidth : minWidth;
        width = Math.max(width, minWidth);
        return width;
    }

    public static String getFormattedCountString(long itemCount, int maxStackSize)
    {
        long stacks = itemCount / maxStackSize;
        long remainder = itemCount % maxStackSize;
        double boxCount = (double) itemCount / (27.0 * maxStackSize);

        if (itemCount > maxStackSize)
        {
            DecimalFormat format = new DecimalFormat("#.##");
            String boxCountStr = format.format(boxCount);

            if (maxStackSize > 1)
            {
                if (remainder > 0L)
                {
                    return StringUtils.translate("litematica.label.material_list.item_counts.total_stacks_lose_boxes",
                                                 itemCount, stacks, maxStackSize, remainder, boxCountStr);
                }
                else
                {
                    return StringUtils.translate("litematica.label.material_list.item_counts.total_stacks_boxes",
                                                 itemCount, stacks, maxStackSize, boxCountStr);
                }
            }
            else
            {
                return StringUtils.translate("litematica.label.material_list.item_counts.total_boxes",
                                             itemCount, boxCountStr);
            }
        }
        else
        {
            return StringUtils.translate("litematica.label.material_list.item_counts.total", itemCount);
        }
    }
}
