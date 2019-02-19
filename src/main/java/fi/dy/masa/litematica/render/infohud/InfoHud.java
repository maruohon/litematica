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
    protected final List<IInfoHudRenderer> renderers = new ArrayList<>();
    protected boolean enabled = true;

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
            this.lineList.clear();

            this.updateHudText();

            final int maxLines = Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue();
            int xOffset = 2;
            int yOffset = 2;
            int lineIndex = 0;
            boolean isGui = this.mc.currentScreen != null;

            if (this.renderers.isEmpty() == false)
            {
                for (IInfoHudRenderer renderer : this.renderers)
                {
                    if (renderer.getShouldRender() && (isGui == false || renderer.shouldRenderInGuis()))
                    {
                        List<String> lines = renderer.getText();

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
            }

            if (this.lineList.isEmpty() == false)
            {
                yOffset += fi.dy.masa.malilib.render.RenderUtils.renderText(this.mc, xOffset, yOffset, 1, 0xFFFFFFFF, 0x80000000, this.getHudAlignment(), true, true, this.lineList);
            }

            if (this.renderers.isEmpty() == false)
            {
                for (IInfoHudRenderer renderer : this.renderers)
                {
                    if (renderer.getShouldRender() && (isGui == false || renderer.shouldRenderInGuis()))
                    {
                        yOffset += renderer.render(xOffset, yOffset, this.getHudAlignment());
                    }
                }
            }
        }
    }

    public void addInfoHudRenderer(IInfoHudRenderer renderer, boolean enable)
    {
        if (this.renderers.contains(renderer) == false)
        {
            this.renderers.add(renderer);
        }

        this.enabled |= enable;
    }

    public void removeInfoHudRenderer(IInfoHudRenderer renderer, boolean disableIfEmpty)
    {
        this.renderers.remove(renderer);

        if (disableIfEmpty && this.renderers.isEmpty())
        {
            this.enabled = false;
        }
    }

    public void removeInfoHudRenderersOfType(Class<? extends IInfoHudRenderer> clazz, boolean disableIfEmpty)
    {
        for (int i = 0; i < this.renderers.size(); ++i)
        {
            if (this.renderers.get(i).getClass().isAssignableFrom(clazz))
            {
                this.renderers.remove(i);
                i--;
            }
        }

        if (disableIfEmpty && this.renderers.isEmpty())
        {
            this.enabled = false;
        }
    }

    public void reset()
    {
        this.renderers.clear();
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
