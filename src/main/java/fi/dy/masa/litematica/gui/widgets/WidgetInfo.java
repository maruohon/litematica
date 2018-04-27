package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.gui.widgets.base.WidgetBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class WidgetInfo extends WidgetBase
{
    private final GuiLitematicaBase parent;
    private final ResourceLocation texture = Icons.TEXTURE;
    private final List<String> lines = new ArrayList<>();
    private final int u;
    private final int v;

    public WidgetInfo(int x, int y, int u, int v, int width, int height, float zLevel, GuiLitematicaBase parent, String key, Object... args)
    {
        super(x, y, width, height, zLevel);

        this.parent = parent;
        this.u = u;
        this.v = v;

        this.setInfoLines(key, args);
    }

    protected void setInfoLines(String key, Object... args)
    {
        String[] split = I18n.format(key, args).split("\\n");

        for (String str : split)
        {
            this.lines.add(str);
        }
    }

    @Override
    public void render(int mouseX, int mouseY)
    {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.parent.bindTexture(this.texture);
        this.parent.drawTexturedModalRect(this.x, this.y, this.u, this.v, this.width, this.height);

        if (this.isMouseOver(mouseX, mouseY))
        {
            this.parent.drawHoveringText(this.lines, mouseX, mouseY);
        }
    }
}
