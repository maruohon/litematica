package fi.dy.masa.litematica.gui.widgets;

import java.util.List;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.gui.widgets.base.WidgetBase;
import fi.dy.masa.litematica.util.BlockUtils;
import fi.dy.masa.litematica.util.ItemUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class WidgetSchematicVerificationResult extends WidgetBase
{
    private static int maxNameLengthExpected;
    private static int maxNameLengthFound;
    @Nullable private final String header1;
    @Nullable private final String header2;
    @Nullable private final IBlockState stateExpected;
    @Nullable private final IBlockState stateFound;
    private final ItemStack stackExpected;
    private final ItemStack stackFound;
    private final String blockRegistrynameExpected;
    private final String blockRegistrynameFound;
    private final int count;
    private final boolean isOdd;

    public WidgetSchematicVerificationResult(int x, int y, int width, int height, float zLevel, boolean isOdd, BlockMismatchEntry entry)
    {
        super(x, y, width, height, zLevel);

        this.isOdd = isOdd;

        if (entry.title1 != null && entry.title2 != null)
        {
            this.header1 = entry.title1;
            this.header2 = entry.title2;
            this.stateExpected = null;
            this.stateFound = null;
            this.stackExpected = ItemStack.EMPTY;
            this.stackFound = ItemStack.EMPTY;
            this.blockRegistrynameExpected = "<null>";
            this.blockRegistrynameFound = "<null>";
            this.count = 0;
        }
        else if (entry.title1 != null)
        {
            this.header1 = entry.title1;
            this.header2 = null;
            this.stateExpected = null;
            this.stateFound = null;
            this.stackExpected = ItemStack.EMPTY;
            this.stackFound = ItemStack.EMPTY;
            this.blockRegistrynameExpected = "<null>";
            this.blockRegistrynameFound = "<null>";
            this.count = 0;
        }
        else
        {
            this.header1 = null;
            this.header2 = null;
            this.stateExpected = entry.blockMismatch.statePair.getLeft();
            this.stateFound = entry.blockMismatch.statePair.getRight();
            this.stackExpected = ItemUtils.getItemForState(this.stateExpected);
            this.stackFound = ItemUtils.getItemForState(this.stateFound);
            this.count = entry.blockMismatch.count;

            ResourceLocation rl1 = Block.REGISTRY.getNameForObject(this.stateExpected.getBlock());
            ResourceLocation rl2 = Block.REGISTRY.getNameForObject(this.stateFound.getBlock());
            this.blockRegistrynameExpected = rl1 != null ? rl1.toString() : "<null>";
            this.blockRegistrynameFound = rl2 != null ? rl2.toString() : "<null>";

            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            maxNameLengthExpected = Math.max(maxNameLengthExpected, font.getStringWidth(this.stackExpected.getDisplayName()));
            maxNameLengthFound = Math.max(maxNameLengthFound, font.getStringWidth(this.stackFound.getDisplayName()));
        }
    }

    public static void resetNameLengths()
    {
        maxNameLengthExpected = 60;
        maxNameLengthFound = 60;
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        return true;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        // Draw a lighter background for the hovered and the selected entry
        if (selected || this.isMouseOver(mouseX, mouseY))
        {
            GuiLitematicaBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0707070);
        }
        else if (this.isOdd)
        {
            GuiLitematicaBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0101010);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            GuiLitematicaBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0303030);
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (this.header1 != null && this.header2 != null)
        {
            mc.fontRenderer.drawString(this.header1, this.x + 4, this.y + 7, 0xFFFFFFFF);
            mc.fontRenderer.drawString(this.header2, this.x + maxNameLengthExpected + 50, this.y + 7, 0xFFFFFFFF);
        }
        else if (this.header1 != null)
        {
            mc.fontRenderer.drawString(this.header1, this.x + 4, this.y + 7, 0xFFFFFFFF);
        }
        else if (this.stackExpected != null && this.stackFound != null)
        {
            //GlStateManager.pushMatrix();
            GlStateManager.disableLighting();

            RenderHelper.enableGUIStandardItemLighting();

            int x = this.x + 4;
            int y = this.y + 3;
            //mc.getRenderItem().zLevel -= 110;
            Gui.drawRect(x, y, x + 16, y + 16, 0x20FFFFFF); // light background for the item
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.stackExpected, x, y);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.stackExpected, x, y, null);
            mc.fontRenderer.drawString(this.stackExpected.getDisplayName(), x + 20, y + 4, 0xFFFFFFFF);

            x = this.x + maxNameLengthExpected + 50;
            Gui.drawRect(x, y, x + 16, y + 16, 0x20FFFFFF); // light background for the item
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.stackFound, x, y);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.stackFound, x, y, null);
            mc.fontRenderer.drawString(this.stackFound.getDisplayName(), x + 20, y + 4, 0xFFFFFFFF);
            //mc.getRenderItem().zLevel += 110;

            //GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();
            //GlStateManager.popMatrix();

            x += maxNameLengthFound + 50;
            mc.fontRenderer.drawString(String.valueOf(this.count), x, this.y + 7, 0xFFFFFFFF);
        }
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
    {
        if (this.stateExpected != null && this.stateFound != null)
        {
            Minecraft mc = Minecraft.getMinecraft();

            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, 0f, 200f);

            int x = mouseX + 10;
            int y = Math.min(mouseY, mc.currentScreen.height - 130);

            String name1 = this.stackExpected.getDisplayName();
            String name2 = this.stackFound.getDisplayName();
            int w1 = Math.max(mc.fontRenderer.getStringWidth(name1), mc.fontRenderer.getStringWidth(this.blockRegistrynameExpected));
            int w2 = Math.max(mc.fontRenderer.getStringWidth(name2), mc.fontRenderer.getStringWidth(this.blockRegistrynameFound));

            GuiLitematicaBase.drawOutlinedBox(x, y, w1 + w2 + 60, 120, 0xFF000000, GuiLitematicaBase.COLOR_HORIZONTAL_BAR);

            GlStateManager.disableLighting();

            RenderHelper.enableGUIStandardItemLighting();

            int x1 = x + 10;
            int x2 = x + w1 + 30;
            y += 4;
            //mc.getRenderItem().zLevel += 100;
            Gui.drawRect(x1, y, x1 + 16, y + 16, 0x20FFFFFF); // light background for the item
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.stackExpected, x1, y);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.stackExpected, x1, y, null);
            mc.fontRenderer.drawString(name1, x1 + 20, y + 4, 0xFFFFFFFF);

            Gui.drawRect(x2, y, x2 + 16, y + 16, 0x20FFFFFF); // light background for the item
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.stackFound, x2, y);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.stackFound, x2, y, null);
            mc.fontRenderer.drawString(name2, x2 + 20, y + 4, 0xFFFFFFFF);
            //mc.getRenderItem().zLevel -= 100;

            //GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();

            y += 20;
            mc.fontRenderer.drawString(this.blockRegistrynameExpected, x1, y, 0xB0B0B0B0);
            mc.fontRenderer.drawString(this.blockRegistrynameFound, x2, y, 0xB0B0B0B0);
            y += mc.fontRenderer.FONT_HEIGHT + 8;

            this.renderLines(x1, y, BlockUtils.getFormattedBlockStateProperties(this.stateExpected), mc.fontRenderer);
            this.renderLines(x2, y, BlockUtils.getFormattedBlockStateProperties(this.stateFound), mc.fontRenderer);

            GlStateManager.popMatrix();
        }
    }

    private void renderLines(int x, int y, List<String> lines, FontRenderer font)
    {
        if (lines.isEmpty() == false)
        {
            for (String line : lines)
            {
                font.drawString(line, x, y, 0xB0B0B0B0);
                y += font.FONT_HEIGHT + 2;
            }
        }
    }
}
