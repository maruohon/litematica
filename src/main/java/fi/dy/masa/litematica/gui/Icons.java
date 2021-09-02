package fi.dy.masa.litematica.gui;

import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.util.Identifier;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.interfaces.IFileBrowserIconProvider;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.render.RenderUtils;

public enum Icons implements IGuiIcon, IFileBrowserIconProvider
{
    DUMMY                   (  0,   0,  0,  0),
    BUTTON_PLUS_MINUS_8     (  0,   0,  8,  8),
    BUTTON_PLUS_MINUS_12    ( 24,   0, 12, 12),
    BUTTON_PLUS_MINUS_16    (  0, 128, 16, 16),
    ENCLOSING_BOX_ENABLED   (  0, 144, 16, 16),
    ENCLOSING_BOX_DISABLED  (  0, 160, 16, 16),
    FILE_ICON_LITEMATIC     (144,   0, 12, 12),
    FILE_ICON_SCHEMATIC     (144,  12, 12, 12),
    FILE_ICON_SPONGE_SCH    (144,  24, 12, 12),
    FILE_ICON_VANILLA       (144,  36, 12, 12),
    FILE_ICON_JSON          (144,  44, 12, 12),
    FILE_ICON_DIR           (156,   0, 12, 12),
    FILE_ICON_DIR_UP        (156,  12, 12, 12),
    FILE_ICON_DIR_ROOT      (156,  24, 12, 12),
    FILE_ICON_SEARCH        (156,  36, 12, 12),
    FILE_ICON_CREATE_DIR    (156,  48, 12, 12),
    SCHEMATIC_TYPE_FILE     (144,   0, 12, 12),
    SCHEMATIC_TYPE_MEMORY   (186,   0, 12, 12),
    INFO_11                 (168,  18, 11, 11),
    NOTICE_EXCLAMATION_11   (168,  29, 11, 11),
    LOCK_LOCKED             (168,  51, 11, 11),
    CHECKBOX_UNSELECTED     (198,   0, 11, 11),
    CHECKBOX_SELECTED       (198,  11, 11, 11),
    ARROW_UP                (209,   0, 15, 15),
    ARROW_DOWN              (209,  15, 15, 15);

    public static final Identifier TEXTURE = new Identifier(Reference.MOD_ID, "textures/gui/gui_widgets.png");

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
    public Identifier getTexture()
    {
        return TEXTURE;
    }

    @Override
    public IGuiIcon getIconRoot()
    {
        return FILE_ICON_DIR_ROOT;
    }

    @Override
    public IGuiIcon getIconUp()
    {
        return FILE_ICON_DIR_UP;
    }

    @Override
    public IGuiIcon getIconCreateDirectory()
    {
        return FILE_ICON_CREATE_DIR;
    }

    @Override
    public IGuiIcon getIconSearch()
    {
        return FILE_ICON_SEARCH;
    }

    @Override
    public IGuiIcon getIconDirectory()
    {
        return FILE_ICON_DIR;
    }

    @Override
    @Nullable
    public IGuiIcon getIconForFile(File file)
    {
        if (this == DUMMY)
        {
            return null;
        }

        FileType fileType = FileType.fromFile(file);

        switch (fileType)
        {
            case LITEMATICA_SCHEMATIC:  return FILE_ICON_LITEMATIC;
            case SCHEMATICA_SCHEMATIC:  return FILE_ICON_SCHEMATIC;
            case VANILLA_STRUCTURE:     return FILE_ICON_VANILLA;
            case SPONGE_SCHEMATIC:      return FILE_ICON_SPONGE_SCH;
            case JSON:                  return FILE_ICON_JSON;

            default:
                return DUMMY;
        }
    }
}
