package litematica.gui.widget;

import java.text.DecimalFormat;

import net.minecraft.item.ItemStack;

import malilib.gui.widget.ContainerWidget;
import malilib.gui.widget.ItemStackWidget;
import malilib.gui.widget.LabelWidget;
import malilib.render.text.StyledTextLine;
import malilib.util.StringUtils;

public class MaterialListEntryHoverInfoWidget extends ContainerWidget
{
    protected final ItemStackWidget itemWidget;
    protected final LabelWidget headerLabel;
    protected final LabelWidget itemNameLabel;
    protected final LabelWidget itemCountsLabel;

    public MaterialListEntryHoverInfoWidget(ItemStack stack, long totalCount, long missingCount)
    {
        super(-1, 54);

        // Note: All the positions here are relative positions within this widget,
        // and the widget gets rendered based on the mouse cursor position
        this.headerLabel = new LabelWidget("litematica.label.material_list.hover_info.row_names");
        this.headerLabel.setPosition(6, 8);
        this.headerLabel.setLineHeight(16);

        this.itemWidget = new ItemStackWidget(stack);
        this.itemWidget.setPosition(this.headerLabel.getWidth() + 16, 4);

        this.itemNameLabel = new LabelWidget();
        this.itemNameLabel.setLines(StyledTextLine.parseFirstLine(stack.getDisplayName()));
        this.itemNameLabel.setPosition(this.itemWidget.getRight() + 4, 8);

        int maxStackSize = stack.getMaxStackSize();
        String strTotal = getFormattedCountString(totalCount, maxStackSize);
        String strMissing = getFormattedCountString(missingCount, maxStackSize);
        this.itemCountsLabel = new LabelWidget();
        this.itemCountsLabel.setLines(StyledTextLine.parseFirstLine(strTotal),
                                      StyledTextLine.parseFirstLine(strMissing));
        this.itemCountsLabel.setPosition(this.itemWidget.getX(), this.headerLabel.getY() + 16);
        this.itemCountsLabel.setLineHeight(16);

        int width = this.headerLabel.getWidth();
        width += Math.max(this.itemNameLabel.getWidth() + 22, this.itemCountsLabel.getWidth());
        width += 22;
        this.setWidth(width);

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xFF000000);
        this.getBorderRenderer().getNormalSettings().setEnabled(true);
        this.reAddSubWidgets();
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidget(this.itemWidget);
        this.addWidget(this.headerLabel);
        this.addWidget(this.itemNameLabel);
        this.addWidget(this.itemCountsLabel);
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
