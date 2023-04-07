package litematica.render;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

import malilib.render.RenderUtils;
import malilib.render.ShapeRenderUtils;
import malilib.render.text.StyledText;
import malilib.render.text.TextRenderer;
import malilib.util.StringUtils;
import malilib.util.game.BlockUtils;
import malilib.util.game.wrap.GameUtils;
import malilib.util.game.wrap.RegistryUtils;
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

    public void render(int x, int y, int zLevel)
    {
        if (this.state != null)
        {
            GlStateManager.pushMatrix();

            ShapeRenderUtils.renderOutlinedRectangle(x, y, zLevel, this.totalWidth, this.totalHeight, 0xFF000000, 0xFF999999);

            Minecraft mc = GameUtils.getClient();
            FontRenderer textRenderer = mc.fontRenderer;
            int x1 = x + 10;
            y += 4;

            // TODO FIXME use a StringListRenderer?
            TextRenderer.INSTANCE.renderText(x1, y, 0, 0xFFFFFFFF, true, StyledText.parse(this.title));

            y += 12;

            GlStateManager.disableLighting();
            RenderUtils.enableGuiItemLighting();

            ShapeRenderUtils.renderRectangle(x1, y, zLevel, 16, 16, 0x20FFFFFF); // light background for the item

            float origZ = mc.getRenderItem().zLevel;
            mc.getRenderItem().zLevel = zLevel + 1;
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.stack, x1, y);
            mc.getRenderItem().renderItemOverlayIntoGUI(textRenderer, this.stack, x1, y, null);
            mc.getRenderItem().zLevel = origZ;

            //GlStateManager.disableBlend();
            RenderUtils.disableItemLighting();

            TextRenderer.INSTANCE.renderText(x1 + 20, y + 4, 0, 0xFFFFFFFF, true, StyledText.parse(this.stackName));

            y += 20;
            TextRenderer.INSTANCE.renderText(x1, y, 0, 0xFF4060FF, true, StyledText.parse(this.blockRegistryname));
            y += TextRenderer.INSTANCE.getFontHeight() + 4;

            TextRenderer.INSTANCE.renderText(x1, y, 0, 0xFFB0B0B0, true, StyledText.parseList(this.props));

            GlStateManager.popMatrix();
        }
    }
}
