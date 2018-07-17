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
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class WidgetSchematicVerificationResult extends WidgetBase
{
    private static int maxNameLengthExpected;
    private static int maxNameLengthFound;
    private static int maxCountLength;
    @Nullable private final String header1;
    @Nullable private final String header2;
    @Nullable private final String header3;
    @Nullable private final BlockMismatchInfo mismatchInfo;
    private final int count;
    private final boolean isOdd;

    public WidgetSchematicVerificationResult(int x, int y, int width, int height, float zLevel, boolean isOdd, BlockMismatchEntry entry)
    {
        super(x, y, width, height, zLevel);

        this.isOdd = isOdd;

        if (entry.header1 != null && entry.header2 != null)
        {
            this.header1 = entry.header1;
            this.header2 = entry.header2;
            this.header3 = GuiLitematicaBase.TXT_BOLD + I18n.format("litematica.gui.label.schematic_verifier.count");
            this.mismatchInfo = null;
            this.count = 0;
        }
        else if (entry.header1 != null)
        {
            this.header1 = entry.header1;
            this.header2 = null;
            this.header3 = null;
            this.mismatchInfo = null;
            this.count = 0;
        }
        else
        {
            this.header1 = null;
            this.header2 = null;
            this.header3 = null;
            this.mismatchInfo = new BlockMismatchInfo(entry.blockMismatch.stateExpected, entry.blockMismatch.stateFound);
            this.count = entry.blockMismatch.count;

            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            maxNameLengthExpected = Math.max(maxNameLengthExpected, font.getStringWidth(this.mismatchInfo.stackExpected.getDisplayName()));
            maxNameLengthFound = Math.max(maxNameLengthFound, font.getStringWidth(this.mismatchInfo.stackFound.getDisplayName()));
            maxCountLength = Math.max(maxCountLength, font.getStringWidth(String.valueOf(this.count)));
        }
    }

    public static void resetNameLengths()
    {
        maxNameLengthExpected = 60;
        maxNameLengthFound = 60;
        maxCountLength = 40;
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
            mc.fontRenderer.drawString(this.header3, this.x + maxNameLengthExpected + maxNameLengthFound + 100, this.y + 7, 0xFFFFFFFF);
        }
        else if (this.header1 != null)
        {
            mc.fontRenderer.drawString(this.header1, this.x + 4, this.y + 7, 0xFFFFFFFF);
        }
        else if (this.mismatchInfo != null)
        {
            int x = this.x + 4;
            int y = this.y + 3;
            int x2 = this.x + maxNameLengthExpected + 50;

            mc.fontRenderer.drawString(this.mismatchInfo.stackExpected.getDisplayName(), x + 20, y + 4, 0xFFFFFFFF);
            mc.fontRenderer.drawString(this.mismatchInfo.stackFound.getDisplayName(), x2 + 20, y + 4, 0xFFFFFFFF);
            mc.fontRenderer.drawString(String.valueOf(this.count), x2 + maxNameLengthFound + 50, this.y + 7, 0xFFFFFFFF);

            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            RenderHelper.enableGUIStandardItemLighting();

            //mc.getRenderItem().zLevel -= 110;
            Gui.drawRect(x, y, x + 16, y + 16, 0x20FFFFFF); // light background for the item
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.mismatchInfo.stackExpected, x, y);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.mismatchInfo.stackExpected, x, y, null);

            x = this.x + maxNameLengthExpected + 50;
            Gui.drawRect(x, y, x + 16, y + 16, 0x20FFFFFF); // light background for the item
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.mismatchInfo.stackFound, x, y);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.mismatchInfo.stackFound, x, y, null);
            //mc.getRenderItem().zLevel += 110;

            GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
    {
        if (this.mismatchInfo != null)
        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, 0f, 200f);

            Minecraft mc = Minecraft.getMinecraft();

            this.mismatchInfo.render(mouseX + 10, Math.min(mouseY, mc.currentScreen.height - 130), mc);

            GlStateManager.popMatrix();
        }
    }

    private static void renderLines(int x, int y, List<String> lines, FontRenderer font)
    {
        if (lines.isEmpty() == false)
        {
            for (String line : lines)
            {
                font.drawString(line, x, y, 0xFFB0B0B0);
                y += font.FONT_HEIGHT + 2;
            }
        }
    }

    public static class BlockMismatchInfo
    {
        public final IBlockState stateExpected;
        public final IBlockState stateFound;
        public final ItemStack stackExpected;
        public final ItemStack stackFound;
        public final String blockRegistrynameExpected;
        public final String blockRegistrynameFound;

        public BlockMismatchInfo(IBlockState stateExpected, IBlockState stateFound)
        {
            this.stateExpected = stateExpected;
            this.stateFound = stateFound;

            this.stackExpected = ItemUtils.getItemForState(this.stateExpected);
            this.stackFound = ItemUtils.getItemForState(this.stateFound);

            ResourceLocation rl1 = Block.REGISTRY.getNameForObject(this.stateExpected.getBlock());
            ResourceLocation rl2 = Block.REGISTRY.getNameForObject(this.stateFound.getBlock());
            this.blockRegistrynameExpected = rl1 != null ? rl1.toString() : "<null>";
            this.blockRegistrynameFound = rl2 != null ? rl2.toString() : "<null>";
        }

        public void render(int x, int y, Minecraft mc)
        {
            if (this.stateExpected != null && this.stateFound != null)
            {
                GlStateManager.pushMatrix();

                List<String> propsExpected = BlockUtils.getFormattedBlockStateProperties(this.stateExpected);
                List<String> propsFound = BlockUtils.getFormattedBlockStateProperties(this.stateFound);

                int height = Math.max(propsExpected.size(), propsFound.size()) * (mc.fontRenderer.FONT_HEIGHT + 2) + 60;

                String name1 = this.stackExpected.getDisplayName();
                String name2 = this.stackFound.getDisplayName();
                int w1 = Math.max(mc.fontRenderer.getStringWidth(name1) + 20, mc.fontRenderer.getStringWidth(this.blockRegistrynameExpected));
                int w2 = Math.max(mc.fontRenderer.getStringWidth(name2) + 20, mc.fontRenderer.getStringWidth(this.blockRegistrynameFound));

                GuiLitematicaBase.drawOutlinedBox(x, y, w1 + w2 + 40, height, 0xFF000000, GuiLitematicaBase.COLOR_HORIZONTAL_BAR);

                int x1 = x + 10;
                int x2 = x + w1 + 30;
                y += 4;

                String pre = GuiLitematicaBase.TXT_WHITE + GuiLitematicaBase.TXT_BOLD;
                String strExpected = pre + I18n.format("litematica.gui.label.schematic_verifier.expected") + GuiLitematicaBase.TXT_RST;
                String strFound =    pre + I18n.format("litematica.gui.label.schematic_verifier.found") + GuiLitematicaBase.TXT_RST;
                mc.fontRenderer.drawString(strExpected, x1, y, 0xFFFFFFFF);
                mc.fontRenderer.drawString(strFound,    x2, y, 0xFFFFFFFF);

                y += 12;

                GlStateManager.disableLighting();
                RenderHelper.enableGUIStandardItemLighting();

                //mc.getRenderItem().zLevel += 100;
                Gui.drawRect(x1, y, x1 + 16, y + 16, 0x20FFFFFF); // light background for the item
                mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.stackExpected, x1, y);
                mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.stackExpected, x1, y, null);

                Gui.drawRect(x2, y, x2 + 16, y + 16, 0x20FFFFFF); // light background for the item
                mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.stackFound, x2, y);
                mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.stackFound, x2, y, null);
                //mc.getRenderItem().zLevel -= 100;

                //GlStateManager.disableBlend();
                RenderHelper.disableStandardItemLighting();

                mc.fontRenderer.drawString(name1, x1 + 20, y + 4, 0xFFFFFFFF);
                mc.fontRenderer.drawString(name2, x2 + 20, y + 4, 0xFFFFFFFF);

                y += 20;
                mc.fontRenderer.drawString(this.blockRegistrynameExpected, x1, y, 0xFF4060FF);
                mc.fontRenderer.drawString(this.blockRegistrynameFound, x2, y, 0xFF4060FF);
                y += mc.fontRenderer.FONT_HEIGHT + 4;

                renderLines(x1, y, propsExpected, mc.fontRenderer);
                renderLines(x2, y, propsFound, mc.fontRenderer);

                GlStateManager.popMatrix();
            }
        }

        @Nullable
        public static BlockMismatchInfo forPosition(BlockPos pos)
        {
            return null;
        }
    }
}
