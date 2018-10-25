package fi.dy.masa.litematica.render;

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
                    if (provider.shouldRenderStrings())
                    {
                        y = fi.dy.masa.malilib.render.RenderUtils.renderText(this.mc, 4, y, 1, 0xFFFFFFFF, 0x80000000, this.getHudAlignment(), true, true, provider.getLines());
                    }
                }
            }

            fi.dy.masa.malilib.render.RenderUtils.renderText(this.mc, 4, y, 1, 0xFFFFFFFF, 0x80000000, this.getHudAlignment(), true, true, this.lineList);
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

    public void reset()
    {
        this.providers.clear();
        this.lineList.clear();
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
}
