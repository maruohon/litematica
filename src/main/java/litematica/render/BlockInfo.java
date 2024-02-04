package litematica.render;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import malilib.render.RenderContext;
import malilib.render.ShapeRenderUtils;
import malilib.render.buffer.VanillaWrappingVertexBuilder;
import malilib.render.buffer.VertexBuilder;
import malilib.render.text.StyledText;
import malilib.render.text.TextRenderer;
import malilib.util.StringUtils;
import malilib.util.game.BlockUtils;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.game.wrap.RenderWrap;
import litematica.util.ItemUtils;

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

    public BlockInfo(IBlockState state, String titleKey)
    {
        this.title = StringUtils.translate(titleKey);
        this.state = state;
        this.stack = ItemUtils.getItemForState(this.state);
        this.stackName = this.stack.getDisplayName();

        String id = RegistryUtils.getBlockIdStr(this.state.getBlock());
        this.blockRegistryname = id != null ? id : StringUtils.translate("litematica.label.misc.null.brackets");

        int w = StringUtils.getStringWidth(this.stackName) + 20;
        w = Math.max(w, StringUtils.getStringWidth(this.blockRegistryname));
        w = Math.max(w, StringUtils.getStringWidth(this.title));

        this.props = BlockUtils.getFormattedBlockStateProperties(this.state, " = ");
        this.totalWidth = w + 40;
        this.totalHeight = this.props.size() * (StringUtils.getFontHeight() + 2) + 50;
    }

    public int getTotalWidth()
    {
        return this.totalWidth;
    }

    public int getTotalHeight()
    {
        return this.totalHeight;
    }

    public void render(int x, int y, int zLevel, RenderContext ctx)
    {
        if (this.state != null)
        {
            Minecraft mc = GameWrap.getClient();
            FontRenderer textRenderer = mc.fontRenderer;
            int x1 = x + 10;

            RenderWrap.pushMatrix(ctx);

            VertexBuilder builder = VanillaWrappingVertexBuilder.coloredQuads();

            // Dark background box
            ShapeRenderUtils.renderOutlinedRectangle(x, y, zLevel, this.totalWidth, this.totalHeight, 0xFF000000, 0xFF999999, builder);

            // Light background for the item
            ShapeRenderUtils.renderRectangle(x1, y + 16, zLevel, 16, 16, 0x20FFFFFF, builder);
            builder.draw();

            y += 4;

            // TODO FIXME use a StringListRenderer?
            TextRenderer.INSTANCE.renderText(x1, y, 0, 0xFFFFFFFF, true, StyledText.parse(this.title), ctx);

            y += 12;

            RenderWrap.disableLighting();
            RenderWrap.enableGuiItemLighting(ctx);

            float origZ = mc.getRenderItem().zLevel;
            mc.getRenderItem().zLevel = zLevel + 1;
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.stack, x1, y);
            mc.getRenderItem().renderItemOverlayIntoGUI(textRenderer, this.stack, x1, y, null);
            mc.getRenderItem().zLevel = origZ;

            //RenderWrap.disableBlend();
            RenderWrap.disableItemLighting();

            TextRenderer.INSTANCE.renderText(x1 + 20, y + 4, 0, 0xFFFFFFFF, true, StyledText.parse(this.stackName), ctx);

            y += 20;
            TextRenderer.INSTANCE.renderText(x1, y, 0, 0xFF4060FF, true, StyledText.parse(this.blockRegistryname), ctx);
            y += TextRenderer.INSTANCE.getFontHeight() + 4;

            TextRenderer.INSTANCE.renderText(x1, y, 0, 0xFFB0B0B0, true, StyledText.parseList(this.props), ctx);

            RenderWrap.popMatrix(ctx);
        }
    }
}
