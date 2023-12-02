package litematica.render.infohud;

import java.util.ArrayList;
import java.util.List;

import malilib.config.value.HorizontalAlignment;
import malilib.config.value.HudAlignment;
import malilib.gui.util.GuiUtils;
import malilib.render.RenderContext;
import malilib.render.text.StringListRenderer;
import litematica.config.Configs;

public class InfoHud
{
    private static final InfoHud INSTANCE = new InfoHud();

    protected final List<String> lineList = new ArrayList<>();
    protected final List<IInfoHudRenderer> renderers = new ArrayList<>();
    protected final StringListRenderer stringListRenderer = new StringListRenderer();
    protected boolean enabled = true;

    public static InfoHud getInstance()
    {
        return INSTANCE;
    }

    protected InfoHud()
    {
    }

    public boolean isEnabled()
    {
        return this.enabled;
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
        return Configs.InfoOverlays.INFO_HUD_OFFSET.getValue().x;
    }

    protected int getOffsetY()
    {
        return Configs.InfoOverlays.INFO_HUD_OFFSET.getValue().y;
    }

    public void renderHud(RenderContext ctx)
    {
        if (this.shouldRender())
        {
            this.lineList.clear();

            final int maxLines = Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue();
            int x = this.getOffsetX();
            int y = this.getOffsetY();
            boolean isGui = GuiUtils.isScreenOpen();

            // TODO FIXME all of this junk needs to be rewritten and cleaned up...

            double scale = Math.max(0.05, this.getScaleFactor());

            this.getLinesForPhase(RenderPhase.PRE, maxLines, isGui);
            this.updateHudText();
            this.getLinesForPhase(RenderPhase.POST, maxLines, isGui);

            if (this.lineList.isEmpty() == false)
            {
                this.stringListRenderer.clearText();
                this.stringListRenderer.getNormalTextSettings().setTextColor(0xFFFFFFFF);
                this.stringListRenderer.getNormalTextSettings().setBackgroundEnabled(true);
                this.stringListRenderer.getNormalTextSettings().setBackgroundColor(0x80000000);
                this.stringListRenderer.getPadding().setAll(2, 2, 2, 2);
                this.stringListRenderer.setLineHeight(12);
                //this.stringListRenderer.setLineHeight(11);
                this.stringListRenderer.setText(this.lineList);

                //int ySize = TextRenderUtils.renderText(xOffset, yOffset, 0, scale,
                //                                       0xFFFFFFFF, 0x80000000, this.getHudAlignment(), true, true, this.lineList);
                //yOffset += (int) Math.ceil(ySize * scale);
                HudAlignment align = this.getHudAlignment();

                if (align == HudAlignment.BOTTOM_RIGHT || align == HudAlignment.TOP_RIGHT)
                {
                    this.stringListRenderer.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                    x = GuiUtils.getScaledWindowWidth() - this.stringListRenderer.getTotalRenderWidth() - this.getOffsetX();
                }
                else if (align == HudAlignment.CENTER)
                {
                    this.stringListRenderer.setHorizontalAlignment(HorizontalAlignment.CENTER);
                }
                else
                {
                    this.stringListRenderer.setHorizontalAlignment(HorizontalAlignment.LEFT);
                }

                if (align == HudAlignment.BOTTOM_RIGHT || align == HudAlignment.BOTTOM_LEFT)
                {
                    y = GuiUtils.getScaledWindowHeight() - this.stringListRenderer.getTotalRenderHeight() - this.getOffsetY() + 2;
                }

                this.stringListRenderer.renderAt(x, y, 0, false, ctx);
                y += this.stringListRenderer.getTotalRenderHeight();
            }

            if (this.renderers.isEmpty() == false)
            {
                for (IInfoHudRenderer renderer : this.renderers)
                {
                    if (renderer.getShouldRenderCustom() && (isGui == false || renderer.shouldRenderInGuis()))
                    {
                        // FIXME: This is technically wrong, the yOffset should be separate per hud alignment
                        y += renderer.render(x, y, this.getHudAlignment(), ctx);
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
        return Configs.InfoOverlays.INFO_HUD_ALIGNMENT.getValue();
    }

    protected void updateHudText()
    {
    }
}
