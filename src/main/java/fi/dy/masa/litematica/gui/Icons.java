package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.malilib.gui.IGuiIcon;
import fi.dy.masa.malilib.gui.RenderUtils;
import net.minecraft.util.ResourceLocation;

public enum Icons implements IGuiIcon
{
    BUTTON_PLUS_MINUS_8     (  0,   0,  8,  8),
    BUTTON_PLUS_MINUS_12    ( 24,   0, 12, 12),
    FILE_ICON_LITEMATIC     (144,   0, 12, 12),
    FILE_ICON_SCHEMATIC     (144,  12, 12, 12),
    FILE_ICON_VANILLA       (144,  24, 12, 12),
    FILE_ICON_DIR           (156,   0, 12, 12),
    FILE_ICON_DIR_UP        (156,  12, 12, 12),
    FILE_ICON_DIR_ROOT      (156,  24, 12, 12),
    SCHEMATIC_TYPE_FILE     (144,   0, 12, 12),
    SCHEMATIC_TYPE_MEMORY   (186,   0, 12, 12);

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

    @Override
    public int getWidth()
    {
        return this.w;
    }

    @Override
    public int getHeight()
    {
        return this.h;
    }

    @Override
    public int getU()
    {
        return this.u;
    }

    @Override
    public int getV()
    {
        return this.v;
    }

    @Override
    public void renderAt(int x, int y, float zLevel, boolean enabled, boolean selected)
    {
        RenderUtils.drawTexturedRect(x, y, this.u, this.v, this.w, this.h, zLevel);
    }

    @Override
    public ResourceLocation getTexture()
    {
        return TEXTURE;
    }
}
