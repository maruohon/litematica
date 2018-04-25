package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class InfoWidget
{
    private final GuiLitematicaBase parent;
    private final int posX;
    private final int posY;
    private final int width;
    private final int height;
    private final int u;
    private final int v;
    private final ResourceLocation texture = Widgets.TEXTURE;
    private final List<String> lines = new ArrayList<>();

    public InfoWidget(int x, int y, int u, int v, int width, int height, GuiLitematicaBase parent, String key, Object... args)
    {
        this.posX = x;
        this.posY = y;
        this.width = width;
        this.height = height;
        this.u = u;
        this.v = v;
        this.parent = parent;
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

    public boolean isMouseOver(int mouseX, int mouseY)
    {
        return mouseX >= this.posX && mouseX < this.posX + this.width &&
               mouseY >= this.posY && mouseY < this.posY + this.height;
    }

    public void render(int mouseX, int mouseY)
    {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.parent.bindTexture(this.texture);
        this.parent.drawTexturedModalRect(this.posX, this.posY, this.u, this.v, this.width, this.height);

        if (this.isMouseOver(mouseX, mouseY))
        {
            this.parent.drawHoveringText(this.lines, mouseX, mouseY);
        }
    }
}
