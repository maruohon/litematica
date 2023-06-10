package fi.dy.masa.litematica.render.infohud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.util.GuiUtils;

public class InfoHud
{
    private static final InfoHud INSTANCE = new InfoHud();

    protected final MinecraftClient mc;
    protected final List<String> lineList = new ArrayList<>();
    protected final List<IInfoHudRenderer> renderers = new ArrayList<>();
    protected boolean enabled = true;

    public static InfoHud getInstance()
    {
        return INSTANCE;
    }

    protected InfoHud()
    {
        this.mc = MinecraftClient.getInstance();
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    protected double getScaleFactor()
    {
        return Configs.InfoOverlays.INFO_HUD_SCALE.getDoubleValue();
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

    protected int getOffsetX()
    {
        return Configs.InfoOverlays.INFO_HUD_OFFSET_X.getIntegerValue();
    }

    protected int getOffsetY()
    {
        return Configs.InfoOverlays.INFO_HUD_OFFSET_Y.getIntegerValue();
    }

    public void renderHud(DrawContext drawContext)
    {
        if (this.mc.player != null && this.shouldRender())
        {
            this.lineList.clear();

            final int maxLines = Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue();
            int xOffset = this.getOffsetX();
            int yOffset = this.getOffsetY();
            boolean isGui = GuiUtils.getCurrentScreen() != null;
            double scale = Math.max(0.05, this.getScaleFactor());

            this.getLinesForPhase(RenderPhase.PRE, maxLines, isGui);
            this.updateHudText();
            this.getLinesForPhase(RenderPhase.POST, maxLines, isGui);

            if (this.lineList.isEmpty() == false)
            {
                int ySize = fi.dy.masa.malilib.render.RenderUtils.renderText(xOffset, yOffset, scale, 0xFFFFFFFF, 0x80000000, this.getHudAlignment(), true, true, this.lineList, drawContext);
                yOffset += (int) Math.ceil(ySize * scale);
            }

            if (this.renderers.isEmpty() == false)
            {
                for (IInfoHudRenderer renderer : this.renderers)
                {
                    if (renderer.getShouldRenderCustom() && (isGui == false || renderer.shouldRenderInGuis()))
                    {
                        // FIXME: This is technically wrong, the yOffset should be separate per hud alignment
                        yOffset += renderer.render(xOffset, yOffset, this.getHudAlignment(), drawContext);
                    }
                }
            }
        }
    }

    protected void getLinesForPhase(RenderPhase phase, int maxLines, boolean isGui)
    {
        if (this.renderers.isEmpty() == false)
        {
            for (int rendererIndex = 0; rendererIndex < this.renderers.size(); ++rendererIndex)
            {
                IInfoHudRenderer renderer = this.renderers.get(rendererIndex);

                if (renderer.getShouldRenderText(phase) && (isGui == false || renderer.shouldRenderInGuis()))
                {
                    List<String> lines = renderer.getText(phase);

                    for (int i = 0; i < lines.size() && this.lineList.size() < maxLines; ++i)
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
