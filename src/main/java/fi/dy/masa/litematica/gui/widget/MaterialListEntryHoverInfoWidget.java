package fi.dy.masa.litematica.gui.widget;

import net.minecraft.item.ItemStack;
import fi.dy.masa.litematica.gui.widget.list.entry.MaterialListEntryWidget;
import fi.dy.masa.malilib.gui.widget.ContainerWidget;
import fi.dy.masa.malilib.gui.widget.ItemStackWidget;
import fi.dy.masa.malilib.gui.widget.LabelWidget;
import fi.dy.masa.malilib.render.text.StyledTextLine;

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
        this.itemNameLabel.setLabelStyledTextLines(StyledTextLine.of(stack.getDisplayName()));
        this.itemNameLabel.setPosition(this.itemWidget.getRight() + 4, 8);

        int maxStackSize = stack.getMaxStackSize();
        String strTotal = MaterialListEntryWidget.getFormattedCountString(totalCount, maxStackSize);
        String strMissing = MaterialListEntryWidget.getFormattedCountString(missingCount, maxStackSize);
        this.itemCountsLabel = new LabelWidget();
        this.itemCountsLabel.setLabelStyledTextLines(StyledTextLine.of(strTotal),
                                                     StyledTextLine.of(strMissing));
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
}
