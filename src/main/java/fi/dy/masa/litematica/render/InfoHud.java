package fi.dy.masa.litematica.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.HudAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;

public class InfoHud
{
    private static final InfoHud INSTANCE = new InfoHud();

    public static final String GREEN = TextFormatting.GREEN.toString();
    public static final String WHITE = TextFormatting.WHITE.toString();
    public static final String WHITE_ITA = TextFormatting.WHITE.toString() + TextFormatting.ITALIC.toString();
    public static final String RESET = TextFormatting.RESET.toString();

    protected final Minecraft mc;
    protected final List<String> lineList = new ArrayList<>();
    protected final List<IStringListProvider> providers = new ArrayList<>();
    protected boolean enabled;

    public static InfoHud getInstance()
    {
        return INSTANCE;
    }

    protected InfoHud()
    {
        this.mc = Minecraft.getMinecraft();
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public boolean toggleEnabled()
    {
        this.enabled = ! this.enabled;
        return this.enabled;
    }

    public void renderHud()
    {
        if (this.mc.player != null && this.shouldRender())
        {
            this.updateHudText();

            int y = 4;

            if (this.providers.isEmpty() == false)
            {
                for (IStringListProvider provider : this.providers)
                {
                    y = renderTextLines(this.mc, 4, y, 0xFFFFFFFF, 0x80000000, true, true, this.getHudAlignment(), provider.getLines());
                }
            }

            renderTextLines(this.mc, 4, y, 0xFFFFFFFF, 0x80000000, true, true, this.getHudAlignment(), this.lineList);
        }
    }

    public void addLineProvider(IStringListProvider provider)
    {
        if (this.providers.contains(provider) == false)
        {
            this.providers.add(provider);
        }
    }

    public void removeLineProvider(IStringListProvider provider)
    {
        this.providers.remove(provider);
    }

    public void removeLineProvidersOfType(Class<? extends IStringListProvider> clazz)
    {
        for (int i = 0; i < this.providers.size(); ++i)
        {
            if (this.providers.get(i).getClass() == clazz)
            {
                this.providers.remove(i);
                i--;
            }
        }
    }

    public void setLines(List<String> lines)
    {
        this.lineList.clear();
        this.lineList.addAll(lines);
    }

    protected boolean shouldRender()
    {
        return this.enabled;
    }

    protected HudAlignment getHudAlignment()
    {
        return (HudAlignment) Configs.Visuals.INFO_HUD_ALIGNMENT.getOptionListValue();
    }

    protected void updateHudText()
    {
    }

    public static int renderTextLines(Minecraft mc, int xOff, int yOff, int fontColor, int bgColor,
            boolean useBg, boolean useShadow, HudAlignment align, List<String> lines)
    {
        FontRenderer fontRenderer = mc.fontRenderer;
        ScaledResolution res = new ScaledResolution(mc);
        final int lineHeight = fontRenderer.FONT_HEIGHT + 2;
        final int bgMargin = 2;
        double posX = xOff + bgMargin;
        double posY = yOff + bgMargin;

        if (align == HudAlignment.TOP_RIGHT)
        {
            Collection<PotionEffect> effects = mc.player.getActivePotionEffects();

            if (effects.isEmpty() == false)
            {
                int y1 = 0;
                int y2 = 0;

                for (PotionEffect effect : effects)
                {
                    Potion potion = effect.getPotion();

                    if (effect.doesShowParticles() && potion.hasStatusIcon())
                    {
                        if (potion.isBeneficial())
                        {
                            y1 = 26;
                        }
                        else
                        {
                            y2 = 52;
                            break;
                        }
                    }
                }

                posY += Math.max(y1, y2);
            }
        }

        switch (align)
        {
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                posY = res.getScaledHeight() - (lines.size() * lineHeight) - yOff + 2;
                break;
            case CENTER:
                posY = (res.getScaledHeight() / 2.0d) - (lines.size() * lineHeight / 2.0d) + yOff;
                break;
            default:
        }

        for (String line : lines)
        {
            final int width = fontRenderer.getStringWidth(line);

            switch (align)
            {
                case TOP_RIGHT:
                case BOTTOM_RIGHT:
                    posX = res.getScaledWidth() - width - xOff - bgMargin;
                    break;
                case CENTER:
                    posX = (res.getScaledWidth() / 2) - (width / 2) - xOff;
                    break;
                default:
            }

            final int x = (int) posX;
            final int y = (int) posY;
            posY += (double) lineHeight;

            if (useBg)
            {
                Gui.drawRect(x - bgMargin, y - bgMargin, x + width + bgMargin, y + fontRenderer.FONT_HEIGHT, bgColor);
            }

            if (useShadow)
            {
                fontRenderer.drawStringWithShadow(line, x, y, fontColor);
            }
            else
            {
                fontRenderer.drawString(line, x, y, fontColor);
            }
        }

        return (int) Math.ceil(posY);
    }
}
