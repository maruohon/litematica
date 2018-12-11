package fi.dy.masa.litematica.gui.widgets;

import java.util.List;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListBase.SortCriteria;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

public class WidgetMaterialListEntry extends WidgetBase
{
    private static final String[] HEADERS = new String[] {
            "litematica.gui.label.material_list.item",
            "litematica.gui.label.material_list.total",
            "litematica.gui.label.material_list.missing",
            "litematica.gui.label.material_list.available" };
    private static int maxNameLength;
    private static int maxCountLength;

    private final MaterialListBase materialList;
    private final WidgetListMaterialList listWidget;
    @Nullable private final MaterialListEntry entry;
    @Nullable private final String header1;
    @Nullable private final String header2;
    @Nullable private final String header3;
    @Nullable private final String header4;
    private final Minecraft mc;
    private final boolean isOdd;

    public WidgetMaterialListEntry(int x, int y, int width, int height, float zLevel, boolean isOdd,
            MaterialListBase materialList, @Nullable MaterialListEntry entry, WidgetListMaterialList listWidget)
    {
        super(x, y, width, height, zLevel);

        this.mc = Minecraft.getMinecraft();
        this.entry = entry;
        this.isOdd = isOdd;
        this.listWidget = listWidget;
        this.materialList = materialList;

        if (this.entry != null)
        {
            this.header1 = null;
            this.header2 = null;
            this.header3 = null;
            this.header4 = null;

            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            maxNameLength = Math.max(maxNameLength, font.getStringWidth(entry.getStack().getDisplayName()));
        }
        else
        {
            this.header1 = GuiBase.TXT_BOLD + I18n.format(HEADERS[0]) + GuiBase.TXT_RST;
            this.header2 = GuiBase.TXT_BOLD + I18n.format(HEADERS[1]) + GuiBase.TXT_RST;
            this.header3 = GuiBase.TXT_BOLD + I18n.format(HEADERS[2]) + GuiBase.TXT_RST;
            this.header4 = GuiBase.TXT_BOLD + I18n.format(HEADERS[3]) + GuiBase.TXT_RST;
        }
    }

    public static void setMaxNameLength(List<MaterialListEntry> materials, Minecraft mc)
    {
        FontRenderer font = mc.fontRenderer;
        maxNameLength = 60;
        maxCountLength = 7 * font.getStringWidth("8");

        for (MaterialListEntry entry : materials)
        {
            maxNameLength = Math.max(maxNameLength, font.getStringWidth(entry.getStack().getDisplayName()));
        }

        for (int i = 0; i < HEADERS.length; ++i)
        {
            maxCountLength = Math.max(maxCountLength, font.getStringWidth(GuiBase.TXT_BOLD + I18n.format(HEADERS[i]) + GuiBase.TXT_RST));
        }
    }

    @Override
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton)
    {
        return false;
    }

    private int getMouseOverColumn(int mouseX, int mouseY)
    {
        int x1 = this.getColumnPosX(0);
        int xEnd = this.getColumnPosX(4);

        if (mouseY >= this.y && mouseY <= this.y + this.height && mouseX >= x1 && mouseX < xEnd)
        {
            for (int column = 1; column <= 4; ++column)
            {
                if (mouseX < this.getColumnPosX(column))
                {
                    return column - 1;
                }
            }
        }

        return -1;
    }

    private int getColumnPosX(int column)
    {
        int x1 = this.x + 4;
        int x2 = x1 + maxNameLength + 40; // item icon plus offset
        int x3 = x2 + maxCountLength + 20;
        int x4 = x3 + maxCountLength + 20;

        switch (column)
        {
            case 0: return x1;
            case 1: return x2;
            case 2: return x3;
            case 3: return x4;
            case 4: return x4 + maxCountLength + 20;
            default: return x1;
        }
    }

    @Override
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton)
    {
        int column = this.getMouseOverColumn(mouseX, mouseY);

        switch (column)
        {
            case 0:
                this.materialList.setSortCriteria(SortCriteria.NAME);
                break;
            case 1:
                this.materialList.setSortCriteria(SortCriteria.COUNT_TOTAL);
                break;
            case 2:
                this.materialList.setSortCriteria(SortCriteria.COUNT_MISSING);
                break;
            case 3:
                this.materialList.setSortCriteria(SortCriteria.COUNT_AVAILABLE);
                break;
            default:
                return false;
        }

        // Re-create the widgets
        this.listWidget.refreshEntries();

        return true;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        // Draw a lighter background for the hovered and the selected entry
        if (this.header1 == null && (selected || this.isMouseOver(mouseX, mouseY)))
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0707070);
        }
        else if (this.isOdd)
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0101010);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0303030);
        }

        Minecraft mc = this.mc;
        int x1 = this.getColumnPosX(0);
        int x2 = this.getColumnPosX(1);
        int x3 = this.getColumnPosX(2);
        int x4 = this.getColumnPosX(3);
        int y = this.y + 7;
        int color = 0xFFFFFFFF;

        if (this.header1 != null)
        {
            mc.fontRenderer.drawString(this.header1, x1, y, color);
            mc.fontRenderer.drawString(this.header2, x2, y, color);
            mc.fontRenderer.drawString(this.header3, x3, y, color);
            mc.fontRenderer.drawString(this.header4, x4, y, color);

            int mouseOverColumn = this.getMouseOverColumn(mouseX, mouseY);
            int sortColumn = this.getCurrentSortColumn();
            boolean reverse = this.materialList.getSortInReverse();
            int iconX = this.getColumnPosX(sortColumn + 1) - 21; // align to the right edge

            IGuiIcon icon = reverse ? Icons.ARROW_UP : Icons.ARROW_DOWN;
            this.mc.getTextureManager().bindTexture(icon.getTexture());
            icon.renderAt(iconX, this.y + 3, this.zLevel, true, sortColumn == mouseOverColumn);

            for (int i = 0; i < 4; ++i)
            {
                int outlineColor = mouseOverColumn == i ? 0xFFFFFFFF : 0xC0707070;
                int xStart = this.getColumnPosX(i);
                int xEnd = this.getColumnPosX(i + 1);

                RenderUtils.drawOutline(xStart - 3, this.y + 1, xEnd - xStart - 2, this.height - 2, outlineColor);
            }
        }
        else if (this.entry != null)
        {
            int countTotal = this.entry.getCountTotal();
            int countMissing = this.entry.getCountMissing();
            int countAvailable = this.entry.getCountAvailable();
            String pre = countAvailable >= countMissing ? TextFormatting.GREEN.toString() : TextFormatting.RED.toString();
            mc.fontRenderer.drawString(this.entry.getStack().getDisplayName(), x1 + 20, y, color);
            mc.fontRenderer.drawString(String.valueOf(countTotal)          , x2, y, color);
            mc.fontRenderer.drawString(pre + String.valueOf(countMissing)  , x3, y, color);
            mc.fontRenderer.drawString(pre + String.valueOf(countAvailable), x4, y, color);

            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            RenderHelper.enableGUIStandardItemLighting();

            //mc.getRenderItem().zLevel -= 110;
            y = this.y + 3;
            Gui.drawRect(x1, y, x1 + 16, y + 16, 0x20FFFFFF); // light background for the item
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.entry.getStack(), x1, y);
            //mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.entry.getStack(), x1, y, null);
            //mc.getRenderItem().zLevel += 110;

            GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }

    private int getCurrentSortColumn()
    {
        return this.materialList.getSortCriteria().ordinal();
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
    {
        if (this.entry != null)
        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 200);

            Minecraft mc = this.mc;
            String header1 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.item");
            String header2 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.total");
            String header3 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.missing");

            ItemStack stack = this.entry.getStack();
            String stackName = stack.getDisplayName();
            int total = this.entry.getCountTotal();
            int missing = this.entry.getCountMissing();
            String strCountTotal = this.getFormattedCountString(total, stack.getMaxStackSize());
            String strCountMissing = this.getFormattedCountString(missing, stack.getMaxStackSize());

            FontRenderer fr = mc.fontRenderer;
            int w1 = Math.max(fr.getStringWidth(header1), Math.max(fr.getStringWidth(header2), fr.getStringWidth(header3)));
            int w2 = Math.max(fr.getStringWidth(stackName) + 20, Math.max(fr.getStringWidth(strCountTotal), fr.getStringWidth(strCountMissing)));
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

            mc.fontRenderer.drawString(header1,         x1     , y, 0xFFFFFFFF);
            mc.fontRenderer.drawString(stackName,       x2 + 20, y, 0xFFFFFFFF);
            y += 16;

            mc.fontRenderer.drawString(header2,         x1, y, 0xFFFFFFFF);
            mc.fontRenderer.drawString(strCountTotal,   x2, y, 0xFFFFFFFF);
            y += 16;

            mc.fontRenderer.drawString(header3,         x1, y, 0xFFFFFFFF);
            mc.fontRenderer.drawString(strCountMissing, x2, y, 0xFFFFFFFF);

            Gui.drawRect(x2, y1, x2 + 16, y1 + 16, 0x20FFFFFF); // light background for the item

            GlStateManager.disableLighting();
            RenderHelper.enableGUIStandardItemLighting();

            //mc.getRenderItem().zLevel += 100;
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, stack, x2, y1);
            //mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, stack, x1, y, null);
            //mc.getRenderItem().zLevel -= 100;
            //GlStateManager.disableBlend();

            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
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
                    strCount = String.format("%d = %d x %d + %d = %.2f SB", total, stacks, maxStackSize, remainder, boxCount);
                }
                else
                {
                    strCount = String.format("%d = %d x %d = %.2f SB", total, stacks, maxStackSize, boxCount);
                }
            }
            else
            {
                strCount = String.format("%d = %.2f SB", total, boxCount);
            }
        }
        else
        {
            strCount = String.format("%d", total);
        }

        return strCount;
    }
}
