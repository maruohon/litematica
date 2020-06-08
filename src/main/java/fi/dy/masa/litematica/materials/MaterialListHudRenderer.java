package fi.dy.masa.litematica.materials;

import java.util.Collections;
import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.litematica.render.infohud.RenderPhase;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class MaterialListHudRenderer implements IInfoHudRenderer
{
    protected final MaterialListBase materialList;
    protected final MaterialListSorter sorter;
    protected boolean shouldRender;
    protected long lastUpdateTime;

    public MaterialListHudRenderer(MaterialListBase materialList)
    {
        this.materialList = materialList;
        this.sorter = new MaterialListSorter(materialList);
    }

    @Override
    public boolean getShouldRenderText(RenderPhase phase)
    {
        return false;
    }

    @Override
    public boolean getShouldRenderCustom()
    {
        return this.shouldRender;
    }

    @Override
    public boolean shouldRenderInGuis()
    {
        return Configs.Generic.RENDER_MATERIALS_IN_GUI.getBooleanValue();
    }

    public void toggleShouldRender()
    {
        this.shouldRender = ! this.shouldRender;
    }

    @Override
    public List<String> getText(RenderPhase phase)
    {
        return Collections.emptyList();
    }

    @Override
    public int render(int xOffset, int yOffset, HudAlignment alignment, MatrixStack matrixStack)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        long currentTime = System.currentTimeMillis();
        List<MaterialListEntry> list;

        if (currentTime - this.lastUpdateTime > 2000)
        {
            MaterialListUtils.updateAvailableCounts(this.materialList.getMaterialsAll(), mc.player);
            list = this.materialList.getMaterialsMissingOnly(true);
            Collections.sort(list, this.sorter);
            this.lastUpdateTime = currentTime;
        }
        else
        {
            list = this.materialList.getMaterialsMissingOnly(false);
        }

        if (list.size() == 0)
        {
            return 0;
        }

        TextRenderer font = mc.textRenderer;
        final double scale = Configs.InfoOverlays.MATERIAL_LIST_HUD_SCALE.getDoubleValue();
        final int maxLines = Configs.InfoOverlays.MATERIAL_LIST_HUD_MAX_LINES.getIntegerValue();
        int bgMargin = 2;
        int lineHeight = 16;
        int contentHeight = (Math.min(list.size(), maxLines) * lineHeight) + bgMargin + 10;
        int maxTextLength = 0;
        int maxCountLength = 0;
        int posX = xOffset + bgMargin;
        int posY = yOffset + bgMargin;
        int bgColor = 0xA0000000;
        int textColor = 0xFFFFFFFF;
        boolean useBackground = true;
        boolean useShadow = false;
        final int size = Math.min(list.size(), maxLines);

        // Only Chuck Norris can divide by zero
        if (scale == 0d)
        {
            return 0;
        }

        for (int i = 0; i < size; ++i)
        {
            MaterialListEntry entry = list.get(i);
            maxTextLength = Math.max(maxTextLength, font.getWidth(entry.getStack().getName().getString()));
            int multiplier = this.materialList.getMultiplier();
            int count = multiplier == 1 ? entry.getCountMissing() - entry.getCountAvailable() : entry.getCountTotal();
            count *= multiplier;
            String strCount = GuiBase.TXT_RED + this.getFormattedCountString(count, entry.getStack().getMaxCount()) + GuiBase.TXT_RST;
            maxCountLength = Math.max(maxCountLength, font.getWidth(strCount));
        }

        final int maxLineLength = maxTextLength + maxCountLength + 30;

        switch (alignment)
        {
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                posX = (int) ((GuiUtils.getScaledWindowWidth() / scale) - maxLineLength - xOffset - bgMargin);
                break;
            case CENTER:
                posX = (int) ((GuiUtils.getScaledWindowWidth() / scale / 2) - (maxLineLength / 2) - xOffset);
                break;
            default:
        }

        if (scale != 1 && scale != 0)
        {
            yOffset = (int) (yOffset / scale);
        }

        posY = RenderUtils.getHudPosY(posY, yOffset, contentHeight, scale, alignment);
        posY += RenderUtils.getHudOffsetForPotions(alignment, scale, mc.player);

        if (scale != 1d)
        {
            RenderSystem.pushMatrix();
            RenderSystem.scaled(scale, scale, scale);
        }

        if (useBackground)
        {
            RenderUtils.drawRect(posX - bgMargin, posY - bgMargin,
                         maxLineLength + bgMargin * 2, contentHeight + bgMargin, bgColor);
        }

        String title = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.button.material_list") + GuiBase.TXT_RST;

        if (useShadow)
        {
            font.drawWithShadow(matrixStack, title, posX + 2, posY + 2, textColor);
        }
        else
        {
            font.draw(matrixStack, title, posX + 2, posY + 2, textColor);
        }

        posY += 12;
        final int itemCountTextColor = Configs.Colors.MATERIAL_LIST_HUD_ITEM_COUNTS.getIntegerValue();
        int x = posX + 18;
        int y = posY + 4;

        for (int i = 0; i < size; ++i)
        {
            MaterialListEntry entry = list.get(i);
            String text = entry.getStack().getName().getString();
            int multiplier = this.materialList.getMultiplier();
            int count = multiplier == 1 ? entry.getCountMissing() - entry.getCountAvailable() : entry.getCountTotal();
            count *= multiplier;
            String strCount = this.getFormattedCountString(count, entry.getStack().getMaxCount());
            int cntLen = font.getWidth(strCount);
            int cntPosX = posX + maxLineLength - cntLen - 2;

            if (useShadow)
            {
                font.drawWithShadow(matrixStack, text, x, y, textColor);
                font.drawWithShadow(matrixStack, strCount, cntPosX, y, itemCountTextColor);
            }
            else
            {
                font.draw(matrixStack, text, x, y, textColor);
                font.draw(matrixStack, strCount, cntPosX, y, itemCountTextColor);
            }

            y += lineHeight;
        }

        x = posX;
        y = posY;

        RenderSystem.enableRescaleNormal();
        RenderUtils.setupBlend();
        RenderUtils.enableDiffuseLightingGui3D();

        for (int i = 0; i < size; ++i)
        {
            mc.getItemRenderer().renderInGui(list.get(i).getStack(), x, y);
            y += lineHeight;
        }

        RenderUtils.disableDiffuseLighting();
        RenderSystem.disableRescaleNormal();
        RenderSystem.disableBlend();

        if (scale != 1d)
        {
            RenderSystem.popMatrix();
        }

        return contentHeight + 4;
    }

    protected String getFormattedCountString(int count, int maxStackSize)
    {
        int stacks = count / maxStackSize;
        int remainder = count % maxStackSize;
        double boxCount = (double) count / (27D * maxStackSize);

        if (count > maxStackSize)
        {
            if (boxCount >= 1.0)
            {
                return String.format("%d (%.2f %s)", count, boxCount, StringUtils.translate("litematica.gui.label.material_list.abbr.shulker_box"));
            }
            else if (remainder > 0)
            {
                return String.format("%d (%d x %d + %d)", count, stacks, maxStackSize, remainder);
            }
            else
            {
                return String.format("%d (%d x %d)", count, stacks, maxStackSize);
            }
        }
        else
        {
            return String.format("%d", count);
        }
    }
}
