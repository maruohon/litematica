package fi.dy.masa.litematica.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListBase.SortCriteria;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntrySortable;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.RandomSeed;

import javax.annotation.Nullable;
import java.util.List;

public class WidgetMaterialListEntry extends WidgetListEntrySortable<MaterialListEntry>
{
    private static final String[] HEADERS = new String[] {
            "litematica.gui.label.material_list.title.item",
            "litematica.gui.label.material_list.title.total",
            "litematica.gui.label.material_list.title.missing",
            "litematica.gui.label.material_list.title.available" };
    private static int maxNameLength;
    private static int maxCountLength1;
    private static int maxCountLength2;
    private static int maxCountLength3;

    private final MaterialListBase materialList;
    private final WidgetListMaterialList listWidget;
    @Nullable private final MaterialListEntry entry;
    @Nullable private final String header1;
    @Nullable private final String header2;
    @Nullable private final String header3;
    @Nullable private final String header4;
    private final String shulkerBoxAbbr;
    private final boolean isOdd;

    public WidgetMaterialListEntry(int x, int y, int width, int height, boolean isOdd,
            MaterialListBase materialList, @Nullable MaterialListEntry entry, int listIndex, WidgetListMaterialList listWidget)
    {
        super(x, y, width, height, entry, listIndex);

        this.columnCount = 4;
        this.entry = entry;
        this.isOdd = isOdd;
        this.listWidget = listWidget;
        this.materialList = materialList;
        this.shulkerBoxAbbr = StringUtils.translate("litematica.gui.label.material_list.abbr.shulker_box");

        if (this.entry != null)
        {
            this.header1 = null;
            this.header2 = null;
            this.header3 = null;
            this.header4 = null;
        }
        else
        {
            this.header1 = GuiBase.TXT_BOLD + StringUtils.translate(HEADERS[0]) + GuiBase.TXT_RST;
            this.header2 = GuiBase.TXT_BOLD + StringUtils.translate(HEADERS[1]) + GuiBase.TXT_RST;
            this.header3 = GuiBase.TXT_BOLD + StringUtils.translate(HEADERS[2]) + GuiBase.TXT_RST;
            this.header4 = GuiBase.TXT_BOLD + StringUtils.translate(HEADERS[3]) + GuiBase.TXT_RST;
        }

        int posX = x + width;
        int posY = y + 1;

        // Note: These are placed from right to left

        this.createButtonGeneric(posX, posY);
    }

    private void createButtonGeneric(int xRight, int y)
    {
        String label = ButtonListener.ButtonType.IGNORE.getDisplayName();
        ButtonListener listener = new ButtonListener(ButtonListener.ButtonType.IGNORE, this.materialList, this.entry, this.listWidget);
        this.addButton(new ButtonGeneric(xRight, y, -1, true, label), listener).getX();
    }

    public static void setMaxNameLength(List<MaterialListEntry> materials, int multiplier)
    {
        maxNameLength   = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate(HEADERS[0]) + GuiBase.TXT_RST);
        maxCountLength1 = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate(HEADERS[1]) + GuiBase.TXT_RST);
        maxCountLength2 = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate(HEADERS[2]) + GuiBase.TXT_RST);
        maxCountLength3 = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate(HEADERS[3]) + GuiBase.TXT_RST);

        for (MaterialListEntry entry : materials)
        {
            int countTotal = entry.getCountTotal() * multiplier;
            int countMissing = multiplier == 1 ? entry.getCountMissing() : countTotal;

            maxNameLength   = Math.max(maxNameLength,   StringUtils.getStringWidth(entry.getStack().getName().getString()));
            maxCountLength1 = Math.max(maxCountLength1, StringUtils.getStringWidth(String.valueOf(countTotal)));
            maxCountLength2 = Math.max(maxCountLength2, StringUtils.getStringWidth(String.valueOf(countMissing)));
            maxCountLength3 = Math.max(maxCountLength3, StringUtils.getStringWidth(String.valueOf(entry.getCountAvailable())));
        }
    }

    @Override
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton)
    {
        return false;
    }

    @Override
    protected int getCurrentSortColumn()
    {
        return this.materialList.getSortCriteria().ordinal();
    }

    @Override
    protected boolean getSortInReverse()
    {
        return this.materialList.getSortInReverse();
    }

    @Override
    protected int getColumnPosX(int column)
    {
        int x1 = this.x + 4;
        int x2 = x1 + maxNameLength + 40; // item icon plus offset
        int x3 = x2 + maxCountLength1 + 20;
        int x4 = x3 + maxCountLength2 + 20;

        return switch (column) {
            case 1 -> x2;
            case 2 -> x3;
            case 3 -> x4;
            case 4 -> x4 + maxCountLength3 + 20;
            default -> x1;
        };
    }

    @Override
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton)
    {
        if (super.onMouseClickedImpl(mouseX, mouseY, mouseButton))
        {
            return true;
        }

        if (this.entry != null)
        {
            return false;
        }

        int column = this.getMouseOverColumn(mouseX, mouseY);

        switch (column) {
            case 0 -> this.materialList.setSortCriteria(SortCriteria.NAME);
            case 1 -> this.materialList.setSortCriteria(SortCriteria.COUNT_TOTAL);
            case 2 -> this.materialList.setSortCriteria(SortCriteria.COUNT_MISSING);
            case 3 -> this.materialList.setSortCriteria(SortCriteria.COUNT_AVAILABLE);
            default -> {
                return false;
            }
        }

        // Re-create the widgets
        this.listWidget.refreshEntries();

        return true;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, DrawContext context)
    {
        // Draw a lighter background for the hovered and the selected entry
        if (this.header1 == null && (selected || this.isMouseOver(mouseX, mouseY)))
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0707070);
        }
        else if (this.isOdd)
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0101010);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0303030);
        }

        int x1 = this.getColumnPosX(0);
        int x2 = this.getColumnPosX(1);
        int x3 = this.getColumnPosX(2);
        int x4 = this.getColumnPosX(3);
        int y = this.y + 7;
        int color = 0xFFFFFFFF;

        if (this.header1 != null)
        {
            if (!this.listWidget.getSearchBarWidget().isSearchOpen())
            {
                this.drawString(x1, y, color, this.header1, context);
                this.drawString(x2, y, color, this.header2, context);
                this.drawString(x3, y, color, this.header3, context);
                this.drawString(x4, y, color, this.header4, context);

                this.renderColumnHeader(mouseX, mouseY, Icons.ARROW_DOWN, Icons.ARROW_UP);
            }
        }
        else if (this.entry != null)
        {
            int multiplier = this.materialList.getMultiplier();
            int countTotal = this.entry.getCountTotal() * multiplier;
            int countMissing = multiplier == 1 ? this.entry.getCountMissing() : countTotal;
            int countAvailable = this.entry.getCountAvailable();
            String green = GuiBase.TXT_GREEN;
            String gold = GuiBase.TXT_GOLD;
            String red = GuiBase.TXT_RED;
            String pre;
            this.drawString(x1 + 20, y, color, this.entry.getStack().getName().getString(), context);

            this.drawString(x2, y, color, String.valueOf(countTotal), context);

            pre = countMissing == 0 ? green : (countAvailable >= countMissing ? gold : red);
            this.drawString(x3, y, color, pre + countMissing, context);

            pre = countAvailable >= countMissing ? green : red;
            this.drawString(x4, y, color, pre + countAvailable, context);

            context.getMatrices().push();
            //TODO: RenderSystem.disableLighting();
            RenderUtils.enableDiffuseLightingGui3D();

            //mc.getRenderItem().zLevel -= 110;
            y = this.y + 3;
            RenderUtils.drawRect(x1, y, 16, 16, 0x20FFFFFF); // light background for the item
            this.mc.getItemRenderer().renderItem(this.entry.getStack(), ModelTransformationMode.GUI, 1, 0, context.getMatrices(), context.getVertexConsumers(), mc.world, (int)RandomSeed.getSeed());
            //mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.entry.getStack(), x1, y, null);
            //mc.getRenderItem().zLevel += 110;

            RenderSystem.disableBlend();
            RenderUtils.disableDiffuseLighting();
            context.getMatrices().pop();

            super.render(mouseX, mouseY, selected, context);
        }
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, DrawContext context)
    {
        if (this.entry != null)
        {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 200);

            String header1 = GuiBase.TXT_BOLD + StringUtils.translate(HEADERS[0]);
            String header2 = GuiBase.TXT_BOLD + StringUtils.translate(HEADERS[1]);
            String header3 = GuiBase.TXT_BOLD + StringUtils.translate(HEADERS[2]);

            ItemStack stack = this.entry.getStack();
            String stackName = stack.getName().getString();
            int multiplier = this.materialList.getMultiplier();
            int total = this.entry.getCountTotal() * multiplier;
            int missing = multiplier == 1 ? this.entry.getCountMissing() : total;
            String strCountTotal = this.getFormattedCountString(total, stack.getMaxCount());
            String strCountMissing = this.getFormattedCountString(missing, stack.getMaxCount());

            int w1 = Math.max(this.getStringWidth(header1), Math.max(this.getStringWidth(header2), this.getStringWidth(header3)));
            int w2 = Math.max(this.getStringWidth(stackName) + 20, Math.max(this.getStringWidth(strCountTotal), this.getStringWidth(strCountMissing)));
            int totalWidth = w1 + w2 + 60;

            int x = mouseX + 10;
            int y = mouseY - 10;

            if (x + totalWidth - 20 >= this.width)
            {
                x -= totalWidth + 20;
            }

            int x1 = x + 10;
            int x2 = x1 + w1 + 20;

            RenderUtils.drawOutlinedBox(x, y, totalWidth, 60, 0xFF000000, GuiBase.COLOR_HORIZONTAL_BAR);
            y += 6;
            int y1 = y;
            y += 4;

            this.drawString(x1, y, 0xFFFFFFFF, header1, context);
            this.drawString(x2 + 20, y, 0xFFFFFFFF, stackName, context);
            y += 16;

            this.drawString(x1, y, 0xFFFFFFFF, header2, context);
            this.drawString(x2, y, 0xFFFFFFFF, strCountTotal, context);
            y += 16;

            this.drawString(x1, y, 0xFFFFFFFF, header3, context);
            this.drawString(x2, y, 0xFFFFFFFF, strCountMissing, context);

            RenderUtils.drawRect(x2, y1, 16, 16, 0x20FFFFFF); // light background for the item

            //TODO: RenderSystem.disableLighting();
            RenderUtils.enableDiffuseLightingGui3D();

            //mc.getRenderItem().zLevel += 100;
            this.mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, 1, 0, context.getMatrices(), context.getVertexConsumers(), mc.world, (int)RandomSeed.getSeed());
            //mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, stack, x1, y, null);
            //mc.getRenderItem().zLevel -= 100;
            //RenderSystem.disableBlend();

            RenderUtils.disableDiffuseLighting();
            context.getMatrices().pop();
        }
    }

    private String getFormattedCountString(int total, int maxStackSize)
    {
        int stacks = total / maxStackSize;
        int remainder = total % maxStackSize;
        double boxCount = (double) total / (27D * maxStackSize);
        String strCount;

        if (total > maxStackSize)
        {
            if (maxStackSize > 1)
            {
                if (remainder > 0)
                {
                    strCount = String.format("%d = %d x %d + %d = %.2f %s", total, stacks, maxStackSize, remainder, boxCount, this.shulkerBoxAbbr);
                }
                else
                {
                    strCount = String.format("%d = %d x %d = %.2f %s", total, stacks, maxStackSize, boxCount, this.shulkerBoxAbbr);
                }
            }
            else
            {
                strCount = String.format("%d = %.2f %s", total, boxCount, this.shulkerBoxAbbr);
            }
        }
        else
        {
            strCount = String.format("%d", total);
        }

        return strCount;
    }

    static class ButtonListener implements IButtonActionListener
    {
        private final ButtonType type;
        private final MaterialListBase materialList;
        private final WidgetListMaterialList listWidget;
        private final MaterialListEntry entry;

        public ButtonListener(ButtonType type, MaterialListBase materialList, MaterialListEntry entry, WidgetListMaterialList listWidget)
        {
            this.type = type;
            this.materialList = materialList;
            this.listWidget = listWidget;
            this.entry = entry;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == ButtonType.IGNORE)
            {
                this.materialList.ignoreEntry(this.entry);
                this.listWidget.refreshEntries();
            }
        }

        public enum ButtonType
        {
            IGNORE  ("litematica.gui.button.material_list.ignore");

            private final String translationKey;

            ButtonType(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getDisplayName()
            {
                return StringUtils.translate(this.translationKey);
            }
        }
    }
}
