package fi.dy.masa.litematica.render;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class BlockInfo
{
    private final String title;
    private final BlockState state;
    private final ItemStack stack;
    private final String blockRegistryname;
    private final String stackName;
    private final List<String> props;
    private final int totalWidth;
    private final int totalHeight;
    private final int columnWidth;

    public BlockInfo(BlockState state, String titleKey)
    {
        String pre = GuiBase.TXT_WHITE + GuiBase.TXT_BOLD;
        this.title = pre + StringUtils.translate(titleKey) + GuiBase.TXT_RST;
        this.state = state;
        this.stack = ItemUtils.getItemForState(this.state);

        Identifier rl = Registry.BLOCK.getId(this.state.getBlock());
        this.blockRegistryname = rl != null ? rl.toString() : "<null>";

        this.stackName = this.stack.getName().getString();

        int w = StringUtils.getStringWidth(this.stackName) + 20;
        w = Math.max(w, StringUtils.getStringWidth(this.blockRegistryname));
        w = Math.max(w, StringUtils.getStringWidth(this.title));
        this.columnWidth = w;

        this.props = BlockUtils.getFormattedBlockStateProperties(this.state, " = ");
        this.totalWidth = this.columnWidth + 40;
        this.totalHeight = this.props.size() * (StringUtils.getFontHeight() + 2) + 60;
    }

    public int getTotalWidth()
    {
        return this.totalWidth;
    }

    public int getTotalHeight()
    {
        return this.totalHeight;
    }

    public void render(int x, int y, MinecraftClient mc, MatrixStack matrixStack)
    {
        if (this.state != null)
        {
            RenderSystem.pushMatrix();

            RenderUtils.drawOutlinedBox(x, y, this.totalWidth, this.totalHeight, 0xFF000000, GuiBase.COLOR_HORIZONTAL_BAR);

            TextRenderer textRenderer = mc.textRenderer;
            int x1 = x + 10;
            y += 4;

            textRenderer.draw(matrixStack, this.title, x1, y, 0xFFFFFFFF);

            y += 12;

            RenderSystem.disableLighting();
            RenderUtils.enableDiffuseLightingGui3D();

            //mc.getRenderItem().zLevel += 100;
            RenderUtils.drawRect(x1, y, 16, 16, 0x20FFFFFF); // light background for the item
            mc.getItemRenderer().renderInGui(this.stack, x1, y);
            mc.getItemRenderer().renderGuiItemOverlay(textRenderer, this.stack, x1, y, null);
            //mc.getRenderItem().zLevel -= 100;

            //RenderSystem.disableBlend();
            RenderUtils.disableDiffuseLighting();

            textRenderer.draw(matrixStack, this.stackName, x1 + 20, y + 4, 0xFFFFFFFF);

            y += 20;
            textRenderer.draw(matrixStack, this.blockRegistryname, x1, y, 0xFF4060FF);
            y += textRenderer.fontHeight + 4;

            RenderUtils.renderText(x1, y, 0xFFB0B0B0, this.props, matrixStack);

            RenderSystem.popMatrix();
        }
    }
}
