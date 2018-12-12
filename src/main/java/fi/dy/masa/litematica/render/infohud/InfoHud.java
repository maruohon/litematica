package fi.dy.masa.litematica.render.infohud;

import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.HudAlignment;
import net.minecraft.client.Minecraft;

public class InfoHud
{
    private static final InfoHud INSTANCE = new InfoHud();

    protected final Minecraft mc;
    protected final List<String> lineList = new ArrayList<>();
    protected final List<IInfoHudRenderer> providers = new ArrayList<>();
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

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
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

            this.lineList.clear();
            final int maxLines = Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue();
            int xOffset = 4;
            int yOffset = 4;
            int lineIndex = 0;

            if (this.providers.isEmpty() == false)
            {
                for (IInfoHudRenderer provider : this.providers)
                {
                    if (provider.getShouldRender())
                    {
                        List<String> lines = provider.getText();

                        for (int i = 0; i < lines.size() && lineIndex < maxLines; ++i, ++lineIndex)
                        {
                            this.lineList.add(lines.get(i));
                        }

                        if (this.lineList.size() >= maxLines)
                        {
                            break;
                        }
                    }
                }

                yOffset += fi.dy.masa.malilib.render.RenderUtils.renderText(this.mc, 4, yOffset, 1, 0xFFFFFFFF, 0x80000000, this.getHudAlignment(), true, true, this.lineList);

                for (IInfoHudRenderer provider : this.providers)
                {
                    if (provider.getShouldRender())
                    {
                        yOffset += provider.render(xOffset, yOffset, this.getHudAlignment());
                    }
                }
            }
        }
    }

    public void addInfoHudRenderer(IInfoHudRenderer provider, boolean enable)
    {
        if (this.providers.contains(provider) == false)
        {
            this.providers.add(provider);
        }

        this.enabled |= enable;
    }

    public void removeInfoHudRenderer(IInfoHudRenderer provider, boolean disableIfEmpty)
    {
        this.providers.remove(provider);

        if (disableIfEmpty && this.providers.isEmpty())
        {
            this.enabled = false;
        }
    }

    public void removeInfoHudRenderersOfType(Class<? extends IInfoHudRenderer> clazz, boolean disableIfEmpty)
    {
        for (int i = 0; i < this.providers.size(); ++i)
        {
            if (this.providers.get(i).getClass().isAssignableFrom(clazz))
            {
                this.providers.remove(i);
                i--;
            }
        }

        if (disableIfEmpty && this.providers.isEmpty())
        {
            this.enabled = false;
        }
    }

    public void reset()
    {
        this.providers.clear();
        this.lineList.clear();
    }

    protected boolean shouldRender()
    {
        return this.enabled;
    }

    protected HudAlignment getHudAlignment()
    {
        return (HudAlignment) Configs.InfoOverlays.INFO_HUD_ALIGNMENT.getOptionListValue();
    }

    protected void updateHudText()
    {
    }
}
