package fi.dy.masa.litematica.render;

import java.util.List;
import fi.dy.masa.litematica.util.BlockUtils;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;

public class BlockInfo
{
    private final String title;
    private final IBlockState state;
    private final ItemStack stack;
    private final String blockRegistryname;
    private final String stackName;
    private final List<String> props;
    private final int totalWidth;
    private final int totalHeight;
    private final int columnWidth;

    public BlockInfo(IBlockState state, String titleKey)
    {
        String pre = GuiBase.TXT_WHITE + GuiBase.TXT_BOLD;
        this.title = pre + I18n.format(titleKey) + GuiBase.TXT_RST;
        this.state = state;
        this.stack = ItemUtils.getItemForState(this.state);

        ResourceLocation rl = IRegistry.BLOCK.getKey(this.state.getBlock());
        this.blockRegistryname = rl != null ? rl.toString() : "<null>";

        Minecraft mc = Minecraft.getInstance();
        this.stackName = this.stack.getDisplayName().getString();

        int w = mc.fontRenderer.getStringWidth(this.stackName) + 20;
        w = Math.max(w, mc.fontRenderer.getStringWidth(this.blockRegistryname));
        w = Math.max(w, mc.fontRenderer.getStringWidth(this.title));
        this.columnWidth = w;

        this.props = BlockUtils.getFormattedBlockStateProperties(this.state);
        this.totalWidth = this.columnWidth + 40;
        this.totalHeight = this.props.size() * (mc.fontRenderer.FONT_HEIGHT + 2) + 60;
    }

    public int getTotalWidth()
    {
        return this.totalWidth;
    }

    public int getTotalHeight()
    {
        return this.totalHeight;
    }

    public void render(int x, int y, Minecraft mc)
    {
        if (this.state != null)
        {
            GlStateManager.pushMatrix();

            RenderUtils.drawOutlinedBox(x, y, this.totalWidth, this.totalHeight, 0xFF000000, GuiBase.COLOR_HORIZONTAL_BAR);

            int x1 = x + 10;
            y += 4;

            mc.fontRenderer.drawString(this.title, x1, y, 0xFFFFFFFF);

            y += 12;

            GlStateManager.disableLighting();
            RenderHelper.enableGUIStandardItemLighting();

            //mc.getRenderItem().zLevel += 100;
            Gui.drawRect(x1, y, x1 + 16, y + 16, 0x20FFFFFF); // light background for the item
            mc.getItemRenderer().renderItemAndEffectIntoGUI(mc.player, this.stack, x1, y);
            mc.getItemRenderer().renderItemOverlayIntoGUI(mc.fontRenderer, this.stack, x1, y, null);
            //mc.getRenderItem().zLevel -= 100;

            //GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();

            mc.fontRenderer.drawString(this.stackName, x1 + 20, y + 4, 0xFFFFFFFF);

            y += 20;
            mc.fontRenderer.drawString(this.blockRegistryname, x1, y, 0xFF4060FF);
            y += mc.fontRenderer.FONT_HEIGHT + 4;

            RenderUtils.renderText(x1, y, 0xFFB0B0B0, this.props, mc.fontRenderer);

            GlStateManager.popMatrix();
        }
    }
}
