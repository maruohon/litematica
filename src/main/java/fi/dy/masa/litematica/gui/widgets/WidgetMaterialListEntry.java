package fi.dy.masa.litematica.gui.widgets;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.util.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.RenderUtils;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

public class WidgetMaterialListEntry extends WidgetBase
{
    private static int maxNameLength;

    private final GuiMaterialList gui;
    @Nullable private final MaterialListEntry entry;
    @Nullable private final String header1;
    @Nullable private final String header2;
    @Nullable private final String header3;
    private final Minecraft mc;
    private final boolean isOdd;

    public WidgetMaterialListEntry(int x, int y, int width, int height, float zLevel, boolean isOdd,
            @Nullable MaterialListEntry entry, GuiMaterialList gui)
    {
        super(x, y, width, height, zLevel);

        this.mc = Minecraft.getMinecraft();
        this.entry = entry;
        this.gui = gui;
        this.isOdd = isOdd;

        if (this.entry != null)
        {
            this.header1 = null;
            this.header2 = null;
            this.header3 = null;

            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            maxNameLength = Math.max(maxNameLength, font.getStringWidth(entry.getStack().getDisplayName()));
        }
        else
        {
            this.header1 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.name");
            this.header2 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.required");
            this.header3 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.available");
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
        int y = this.y + 7;
        int color = 0xFFFFFFFF;

        if (this.header1 != null)
        {
            mc.fontRenderer.drawString(this.header1, x1, y, color);
            mc.fontRenderer.drawString(this.header2, x2, y, color);
            mc.fontRenderer.drawString(this.header3, x3, y, color);
        }
        else if (this.entry != null) 
        {
            mc.fontRenderer.drawString(this.entry.getStack().getDisplayName(), x1 + 20, y, color);
            mc.fontRenderer.drawString(String.valueOf(this.entry.getCountRequired()), x2, y, color);
            mc.fontRenderer.drawString(String.valueOf(this.entry.getCountAvailable()), x3, y, color);

            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            RenderHelper.enableGUIStandardItemLighting();

            //mc.getRenderItem().zLevel -= 110;
            y = this.y + 3;
            Gui.drawRect(x1, y, x1 + 16, y + 16, 0x20FFFFFF); // light background for the item
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.entry.getStack(), x1, y);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.entry.getStack(), x1, y, null);
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
            GlStateManager.translate(0, 0, 200);

            Minecraft mc = this.mc;
            String header1 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.item");
            String header2 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.material_list.required");
            ItemStack stack = this.entry.getStack();
            String stackName = stack.getDisplayName();

            int total = this.entry.getCountRequired();
            int maxStack = stack.getMaxStackSize();
            int stacks = total / maxStack;
            int rem = total % maxStack;
            double boxCount = (double) total / (27D * maxStack);
            String strCount;

            if (total > maxStack)
            {
                if (maxStack > 1)
                {
                    if (rem > 0)
                    {
                        strCount = String.format("%d = %d x %d + %d = %.2f SB", total, stacks, maxStack, rem, boxCount);
                    }
                    else
                    {
                        strCount = String.format("%d = %d x %d = %.2f SB", total, stacks, maxStack, boxCount);
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

            int w1 = Math.max(mc.fontRenderer.getStringWidth(stackName), mc.fontRenderer.getStringWidth(header1));
            int w2 = Math.max(mc.fontRenderer.getStringWidth(strCount), mc.fontRenderer.getStringWidth(header2));
            int width = w1 + w2 + 60;

            int x = mouseX + 10;
            int y = mouseY - 10;

            if (x + width - 20 >= this.width)
            {
                x -= width + 20;
            }

            int x1 = x + 10;
            int x2 = x1 + w1 + 40;

            RenderUtils.drawOutlinedBox(x, y, width, 50, 0xFF000000, GuiBase.COLOR_HORIZONTAL_BAR);
            y += 6;

            mc.fontRenderer.drawString(header1, x1, y, 0xFFFFFFFF);
            mc.fontRenderer.drawString(header2, x2, y, 0xFFFFFFFF);

            mc.fontRenderer.drawString(stackName, x1 + 20, y + 20, 0xFFFFFFFF);
            mc.fontRenderer.drawString(strCount, x2, y + 20, 0xFFFFFFFF);

            GlStateManager.disableLighting();
            RenderHelper.enableGUIStandardItemLighting();

            //mc.getRenderItem().zLevel += 100;
            y += 16;
            Gui.drawRect(x1, y, x1 + 16, y + 16, 0x20FFFFFF); // light background for the item
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, stack, x1, y);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, stack, x1, y, null);
            //mc.getRenderItem().zLevel -= 100;

            //GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }
}
