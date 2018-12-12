package fi.dy.masa.litematica.materials;

import java.util.Collections;
import java.util.List;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;

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
    public boolean getShouldRender()
    {
        return this.shouldRender;
    }

    public void toggleShouldRender()
    {
        this.shouldRender = ! this.shouldRender;
    }

    @Override
    public List<String> getText()
    {
        return Collections.emptyList();
    }

    @Override
    public int render(int xOffset, int yOffset, HudAlignment alignment)
    {
        Minecraft mc = Minecraft.getInstance();
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

        MainWindow window = mc.mainWindow;
        FontRenderer font = mc.fontRenderer;
        double scale = 1;
        int bgMargin = 2;
        int lineHeight = 16;
        int maxLines = Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue();
        int contentHeight = (Math.min(list.size(), maxLines) * lineHeight) + bgMargin + 10;
        int maxTextLength = 0;
        int maxCountLength = 0;
        int posX = xOffset + bgMargin;
        int posY = yOffset + bgMargin;
        int bgColor = 0xA0000000;
        int textColor = 0xFFFFFFFF;
        boolean useBackground = true;
        boolean useShadow = false;
        int lineCount = 0;

        // Only Chuck Norris can divide by zero
        if (scale == 0d)
        {
            return 0;
        }

        for (MaterialListEntry entry : list)
        {
            maxTextLength = Math.max(maxTextLength, font.getStringWidth(entry.getStack().getDisplayName().getString()));
            int multiplier = this.materialList.getMultiplier();
            int count = multiplier == 1 ? entry.getCountMissing() - entry.getCountAvailable() : entry.getCountTotal();
            count *= multiplier;
            String strCount = GuiBase.TXT_RED + this.getFormattedCountString(count, entry.getStack().getMaxStackSize()) + GuiBase.TXT_RST;
            maxCountLength = Math.max(maxCountLength, font.getStringWidth(strCount));

            if (++lineCount >= maxLines)
            {
                break;
            }
        }

        int maxLineLength = maxTextLength + maxCountLength + 30;

        switch (alignment)
        {
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                posX = (int) ((window.getScaledWidth() / scale) - maxLineLength - xOffset - bgMargin);
                break;
            case CENTER:
                posX = (int) ((window.getScaledWidth() / scale / 2) - (maxLineLength / 2) - xOffset);
                break;
            default:
        }

        if (scale != 1d)
        {
            GlStateManager.pushMatrix();
            GlStateManager.scaled(scale, scale, 0);
        }

        posY = RenderUtils.getHudPosY((int) posY, yOffset, contentHeight, scale, alignment);
        posY += RenderUtils.getHudOffsetForPotions(alignment, scale, mc.player);

        if (useBackground)
        {
            Gui.drawRect(posX - bgMargin, posY - bgMargin,
                         posX + maxLineLength + bgMargin, posY + contentHeight, bgColor);
        }

        String title = GuiBase.TXT_BOLD + I18n.format("litematica.gui.button.material_list") + GuiBase.TXT_RST;

        int textX = posX + 2;
        int textY = posY + 2;

        if (useShadow)
        {
            font.drawStringWithShadow(title, textX, textY, textColor);
        }
        else
        {
            font.drawString(title, textX, textY, textColor);
        }

        textX = posX + 18;
        posY += 10;
        lineCount = 0;

        for (MaterialListEntry entry : list)
        {
            textY = posY + 4;
            String text = entry.getStack().getDisplayName().getString();
            int multiplier = this.materialList.getMultiplier();
            int count = multiplier == 1 ? entry.getCountMissing() - entry.getCountAvailable() : entry.getCountTotal();
            count *= multiplier;
            String strCount = GuiBase.TXT_RED + this.getFormattedCountString(count, entry.getStack().getMaxStackSize()) + GuiBase.TXT_RST;
            int cntLen = font.getStringWidth(strCount);
            int cntPosX = posX + maxLineLength - cntLen - 2;

            if (useShadow)
            {
                font.drawStringWithShadow(text, textX, textY, textColor);
                font.drawStringWithShadow(strCount, cntPosX, textY, textColor);
            }
            else
            {
                font.drawString(text, textX, textY, textColor);
                font.drawString(strCount, cntPosX, textY, textColor);
            }

            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            RenderHelper.enableGUIStandardItemLighting();

            mc.getItemRenderer().renderItemAndEffectIntoGUI(mc.player, entry.getStack(), posX, posY);

            GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();

            posY += lineHeight;

            if (++lineCount >= maxLines)
            {
                break;
            }
        }

        if (scale != 1d)
        {
            GlStateManager.popMatrix();
        }

        return contentHeight + 4;
    }

    protected String getFormattedCountString(int count, int maxStackSize)
    {
        double boxCount = (double) count / (27D * maxStackSize);
        return String.format("%d (%.2f SB)", count, boxCount);
    }
}
