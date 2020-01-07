package fi.dy.masa.litematica.gui;

import net.minecraft.util.ResourceLocation;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.malilib.gui.util.GuiIconBase;

public class LitematicaGuiIcons extends GuiIconBase
{
    public static final ResourceLocation LITEMATICA_GUI_TEXTURES = new ResourceLocation(Reference.MOD_ID, "textures/gui/gui_widgets.png");

    // Non-hover-variant icons
    public static final LitematicaGuiIcons DUMMY                          = new LitematicaGuiIcons(  0,   0,  0,  0, 0, 0);
    public static final LitematicaGuiIcons BUTTON_PLUS_MINUS_8            = new LitematicaGuiIcons(  0,   0,  8,  8, 0, 0);
    public static final LitematicaGuiIcons BUTTON_PLUS_MINUS_12           = new LitematicaGuiIcons( 24,   0, 12, 12, 0, 0);
    public static final LitematicaGuiIcons BUTTON_PLUS_MINUS_16           = new LitematicaGuiIcons(  0, 128, 16, 16, 0, 0);
    public static final LitematicaGuiIcons ENCLOSING_BOX_ENABLED          = new LitematicaGuiIcons(  0, 144, 16, 16, 0, 0);
    public static final LitematicaGuiIcons ENCLOSING_BOX_DISABLED         = new LitematicaGuiIcons(  0, 160, 16, 16, 0, 0);

    public static final LitematicaGuiIcons FILE_ICON_LITEMATIC            = new LitematicaGuiIcons(144,   0, 12, 12, 0, 0);
    public static final LitematicaGuiIcons FILE_ICON_SCHEMATIC            = new LitematicaGuiIcons(144,  12, 12, 12, 0, 0);
    public static final LitematicaGuiIcons FILE_ICON_SPONGE               = new LitematicaGuiIcons(144,  24, 12, 12, 0, 0);
    public static final LitematicaGuiIcons FILE_ICON_VANILLA              = new LitematicaGuiIcons(144,  36, 12, 12, 0, 0);
    public static final LitematicaGuiIcons FILE_ICON_JSON                 = new LitematicaGuiIcons(144,  44, 12, 12, 0, 0);

    public static final LitematicaGuiIcons INFO_11                        = new LitematicaGuiIcons(168,  18, 11, 11, 0, 0);
    public static final LitematicaGuiIcons NOTICE_EXCLAMATION_11          = new LitematicaGuiIcons(168,  29, 11, 11, 0, 0);
    public static final LitematicaGuiIcons LOCK_LOCKED                    = new LitematicaGuiIcons(168,  51, 11, 11, 0, 0);
    public static final LitematicaGuiIcons SCHEMATIC_TYPE_MEMORY          = new LitematicaGuiIcons(186,   0, 12, 12, 0, 0);
    public static final LitematicaGuiIcons CHECKBOX_UNSELECTED            = new LitematicaGuiIcons(198,   0, 11, 11, 0, 0);
    public static final LitematicaGuiIcons CHECKBOX_SELECTED              = new LitematicaGuiIcons(198,  11, 11, 11, 0, 0);
    public static final LitematicaGuiIcons ARROW_UP                       = new LitematicaGuiIcons(209,   0, 15, 15, 0, 0);
    public static final LitematicaGuiIcons ARROW_DOWN                     = new LitematicaGuiIcons(209,  15, 15, 15, 0, 0);

    // Hover-variant icons
    public static final LitematicaGuiIcons AREA_EDITOR                    = new LitematicaGuiIcons(102,  70, 14, 14);
    public static final LitematicaGuiIcons AREA_SELECTION                 = new LitematicaGuiIcons(102,   0, 14, 14);
    public static final LitematicaGuiIcons CONFIGURATION                  = new LitematicaGuiIcons(102,  84, 14, 14);
    public static final LitematicaGuiIcons LOADED_SCHEMATICS              = new LitematicaGuiIcons(102,  14, 14, 14);
    public static final LitematicaGuiIcons SCHEMATIC_BROWSER              = new LitematicaGuiIcons(102,  28, 14, 14);
    public static final LitematicaGuiIcons SCHEMATIC_MANAGER              = new LitematicaGuiIcons(102,  56, 14, 14);
    public static final LitematicaGuiIcons SCHEMATIC_PLACEMENTS           = new LitematicaGuiIcons(102,  42, 14, 14);
    public static final LitematicaGuiIcons SCHEMATIC_PROJECTS             = new LitematicaGuiIcons(102,  98, 14, 14);
    public static final LitematicaGuiIcons TASK_MANAGER                   = new LitematicaGuiIcons(102, 112, 14, 14);

    private LitematicaGuiIcons(int u, int v, int w, int h)
    {
        this(u, v, w, h, w, 0);
    }

    private LitematicaGuiIcons(int u, int v, int w, int h, int hoverOffU, int hoverOffV)
    {
        super(u, v, w, h, hoverOffU, hoverOffV);
    }

    @Override
    public ResourceLocation getTexture()
    {
        return LITEMATICA_GUI_TEXTURES;
    }
}
