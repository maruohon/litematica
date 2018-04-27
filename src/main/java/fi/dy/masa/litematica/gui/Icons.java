package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import net.minecraft.util.ResourceLocation;

public enum Icons
{
    FILE_ICON_LITEMATIC     (102,   0, 12, 12),
    FILE_ICON_SCHEMATIC     (102,  12, 12, 12),
    FILE_ICON_VANILLA       (102,  24, 12, 12),
    FILE_ICON_DIR           (102,  36, 12, 12),
    FILE_ICON_DIR_UP        (102,  48, 12, 12),
    FILE_ICON_DIR_ROOT      (102,  60, 12, 12);

    public static final ResourceLocation TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/gui/gui_widgets.png");

    private final int u;
    private final int v;
    private final int w;
    private final int h;

    private Icons(int u, int v, int w, int h)
    {
        this.u = u;
        this.v = v;
        this.w = w;
        this.h = h;
    }

    public int getWidth()
    {
        return this.w;
    }

    public int getHeight()
    {
        return this.h;
    }

    public void renderAt(int x, int y, float zLevel)
    {
        GuiLitematicaBase.drawTexturedRect(x, y, this.u, this.v, this.w, this.h, zLevel);
    }
}
