package fi.dy.masa.litematica.gui.widgets;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.util.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
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
    private static int maxNameLength;

    @Nullable private final MaterialListEntry entry;
    @Nullable private final String header1;
    @Nullable private final String header2;
    @Nullable private final String header3;
    @Nullable private final String header4;
    private final Minecraft mc;
    private final boolean isOdd;

    public WidgetMaterialListEntry(int x, int y, int width, int height, float zLevel, boolean isOdd,
            @Nullable MaterialListEntry entry, GuiMaterialList gui)
    {
        super(x, y, width, height, zLevel);

        this.mc = Minecraft.getInstance();
        this.entry = entry;
        this.isOdd = isOdd;

        if (this.entry != null)
        {
            this.header1 = null;
            this.header2 = null;
            this.header3 = null;
            this.header4 = null;

            FontRenderer font = this.mc.fontRenderer;
            maxNameLength = Math.max(maxNameLength, font.getStringWidth(entry.getStack().getDisplayName().getString()));
        }
        else
        {
            this.header1 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.item");
            this.header2 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.total");
            this.header3 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.missing");
            this.header4 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.available");
        }
    }

    public static void resetNameLengths()
    {
        maxNameLength = 60;
    }

    @Override
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton)
    {
        return false;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        // Draw a lighter background for the hovered and the selected entry
        if (selected || this.isMouseOver(mouseX, mouseY))
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
        int x1 = this.x + 4;
        int x2 = this.x + maxNameLength + 50;
        int x3 = x2 + 80;
        int x4 = x3 + 80;
        int y = this.y + 7;
        int color = 0xFFFFFFFF;

        if (this.header1 != null)
        {
            mc.fontRenderer.drawString(this.header1, x1, y, color);
            mc.fontRenderer.drawString(this.header2, x2, y, color);
            mc.fontRenderer.drawString(this.header3, x3, y, color);
            mc.fontRenderer.drawString(this.header4, x4, y, color);
        }
        else if (this.entry != null)
        {
            int countTotal = this.entry.getCountTotal();
            int countMissing = this.entry.getCountMissing();
            int countAvailable = this.entry.getCountAvailable();
            String pre = countAvailable >= countMissing ? TextFormatting.GREEN.toString() : TextFormatting.RED.toString();
            mc.fontRenderer.drawString(this.entry.getStack().getDisplayName().getString(), x1 + 20, y, color);
            mc.fontRenderer.drawString(String.valueOf(countTotal), x2, y, color);
            mc.fontRenderer.drawString(String.valueOf(countMissing), x3, y, color);
            mc.fontRenderer.drawString(pre + String.valueOf(countAvailable), x4, y, color);

            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            RenderHelper.enableGUIStandardItemLighting();

            //mc.getRenderItem().zLevel -= 110;
            y = this.y + 3;
            Gui.drawRect(x1, y, x1 + 16, y + 16, 0x20FFFFFF); // light background for the item
            mc.getItemRenderer().renderItemAndEffectIntoGUI(mc.player, this.entry.getStack(), x1, y);
            //mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.entry.getStack(), x1, y, null);
            //mc.getRenderItem().zLevel += 110;

            GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
    {
        if (this.entry != null)
        {
            GlStateManager.pushMatrix();
            GlStateManager.translatef(0, 0, 200);

            Minecraft mc = this.mc;
            String header1 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.item");
            String header2 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.total");
            String header3 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.missing");

            ItemStack stack = this.entry.getStack();
            String stackName = stack.getDisplayName().getString();
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
            mc.getItemRenderer().renderItemAndEffectIntoGUI(mc.player, stack, x2, y1);
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
