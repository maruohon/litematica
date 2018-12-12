package fi.dy.masa.litematica.materials;

import java.util.Collections;
import java.util.List;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
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
        Minecraft mc = Minecraft.getMinecraft();
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

        ScaledResolution res = new ScaledResolution(mc);
        FontRenderer font = mc.fontRenderer;
        double scale = 1;
        int lineHeight = 16;
        int maxLines = Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue();
        int contentHeight = (Math.min(list.size(), maxLines) + 1) * lineHeight;
        int maxTextLength = 0;
        int maxCountLength = 0;
        int bgMargin = 2;
        int posX = 4 + bgMargin;
        int posY = yOffset + bgMargin;
        int bgColor = 0xA0000000;
        int textColor = 0xFFFFFFFF;
        boolean useBackground = true;
        boolean useShadow = false;
        int count = 0;

        // Only Chuck Norris can divide by zero
        if (scale == 0d)
        {
            return 0;
        }

        for (MaterialListEntry entry : list)
        {
            maxTextLength = Math.max(maxTextLength, font.getStringWidth(entry.getStack().getDisplayName()));
            maxCountLength = Math.max(maxCountLength, font.getStringWidth(String.valueOf(entry.getCountMissing())));

            if (++count >= maxLines)
            {
                break;
            }
        }

        int maxLineLength = maxTextLength + maxCountLength + 30;

        switch (alignment)
        {
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                posX = (int) ((res.getScaledWidth() / scale) - maxLineLength - xOffset - bgMargin);
                break;
            case CENTER:
                posX = (int) ((res.getScaledWidth() / scale / 2) - (maxLineLength / 2) - xOffset);
                break;
            default:
        }

        if (scale != 1d)
        {
            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, 0);
        }

        posY += RenderUtils.getHudOffsetForPotions(alignment, scale, mc.player);
        posY = RenderUtils.getHudPosY((int) posY, yOffset, contentHeight, scale, alignment);

        int textX = posX + 18;
        int textY = posY + 3;
        count = 0;

        if (useBackground)
        {
            Gui.drawRect(posX - bgMargin, posY - bgMargin,
                         posX + maxLineLength + bgMargin, posY + contentHeight + bgMargin, bgColor);
        }

        String title = GuiBase.TXT_BOLD + I18n.format("litematica.gui.button.material_list") + GuiBase.TXT_RST;

        if (useShadow)
        {
            font.drawStringWithShadow(title, posX + 2, textY, textColor);
        }
        else
        {
            font.drawString(title, posX + 2, textY, textColor);
        }

        posY += lineHeight;

        for (MaterialListEntry entry : list)
        {
            textY = posY + 4;
            String text = entry.getStack().getDisplayName();
            String strCount = GuiBase.TXT_RED + String.valueOf(entry.getCountMissing() - entry.getCountAvailable()) + GuiBase.TXT_RST;
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

            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, entry.getStack(), posX, posY);

            GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();

            posY += lineHeight;

            if (++count >= maxLines)
            {
                break;
            }
        }

        if (scale != 1d)
        {
            GlStateManager.popMatrix();
        }

        return contentHeight;
    }
}
