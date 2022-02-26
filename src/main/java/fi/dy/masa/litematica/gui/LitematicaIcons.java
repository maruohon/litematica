package fi.dy.masa.litematica.gui;

import net.minecraft.util.ResourceLocation;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.malilib.gui.icon.BaseMultiIcon;

public class LitematicaIcons extends BaseMultiIcon
{
    public static final ResourceLocation LITEMATICA_GUI_TEXTURES = new ResourceLocation(Reference.MOD_ID, "textures/gui/gui_widgets.png");

    // Non-hover-variant icons
    public static final LitematicaIcons DUMMY                       = new LitematicaIcons(0, 0, 0, 0);
    public static final LitematicaIcons BUTTON_PLUS_MINUS_8         = new LitematicaIcons(0, 0, 8, 8);
    public static final LitematicaIcons BUTTON_PLUS_MINUS_12        = new LitematicaIcons(24, 0, 12, 12);
    public static final LitematicaIcons BUTTON_PLUS_MINUS_16        = new LitematicaIcons(0, 128, 16, 16);
    public static final LitematicaIcons ENCLOSING_BOX_ENABLED       = new LitematicaIcons(0, 144, 16, 16);
    public static final LitematicaIcons ENCLOSING_BOX_DISABLED      = new LitematicaIcons(0, 160, 16, 16);

    public static final LitematicaIcons FILE_ICON_LITEMATIC         = new LitematicaIcons(144, 0, 12, 12, 0, 0);
    public static final LitematicaIcons FILE_ICON_SCHEMATIC         = new LitematicaIcons(144, 12, 12, 12, 0, 0);
    public static final LitematicaIcons FILE_ICON_SPONGE            = new LitematicaIcons(144, 24, 12, 12, 0, 0);
    public static final LitematicaIcons FILE_ICON_VANILLA           = new LitematicaIcons(144, 36, 12, 12, 0, 0);
    public static final LitematicaIcons FILE_ICON_JSON              = new LitematicaIcons(144, 44, 12, 12, 0, 0);

    public static final LitematicaIcons INFO_11                     = new LitematicaIcons(168, 18, 11, 11, 0, 0);
    public static final LitematicaIcons NOTICE_EXCLAMATION_11       = new LitematicaIcons(168, 29, 11, 11, 0, 0);
    public static final LitematicaIcons LOCK_LOCKED                 = new LitematicaIcons(168, 51, 11, 11, 0, 0);
    public static final LitematicaIcons SCHEMATIC_TYPE_MEMORY       = new LitematicaIcons(186, 0, 12, 12, 0, 0);
    public static final LitematicaIcons CHECKBOX_UNSELECTED         = new LitematicaIcons(198, 0, 11, 11, 0, 0);
    public static final LitematicaIcons CHECKBOX_SELECTED           = new LitematicaIcons(198, 11, 11, 11, 0, 0);
    public static final LitematicaIcons ARROW_UP                    = new LitematicaIcons(209, 0, 15, 15);
    public static final LitematicaIcons ARROW_DOWN                  = new LitematicaIcons(209, 15, 15, 15);

    // Hover-variant icons
    public static final LitematicaIcons AREA_EDITOR                 = new LitematicaIcons(102, 70, 14, 14);
    public static final LitematicaIcons AREA_SELECTION              = new LitematicaIcons(102, 0, 14, 14);
    public static final LitematicaIcons CONFIGURATION               = new LitematicaIcons(102, 84, 14, 14);
    public static final LitematicaIcons DUPLICATE                   = new LitematicaIcons(102, 168, 14, 14);
    public static final LitematicaIcons LOADED_SCHEMATICS           = new LitematicaIcons(102, 14, 14, 14);
    public static final LitematicaIcons PLACEMENT                   = new LitematicaIcons(102, 196, 14, 14);
    public static final LitematicaIcons RELOAD                      = new LitematicaIcons(102, 182, 14, 14);
    public static final LitematicaIcons SAVE_TO_DISK                = new LitematicaIcons(102, 140, 14, 14);
    public static final LitematicaIcons SCHEMATIC_BROWSER           = new LitematicaIcons(102, 28, 14, 14);
    public static final LitematicaIcons SCHEMATIC_MANAGER           = new LitematicaIcons(102, 56, 14, 14);
    public static final LitematicaIcons SCHEMATIC_PLACEMENTS        = new LitematicaIcons(102, 42, 14, 14);
    public static final LitematicaIcons SCHEMATIC_VCS               = new LitematicaIcons(102, 98, 14, 14);
    public static final LitematicaIcons TASK_MANAGER                = new LitematicaIcons(102, 112, 14, 14);
    public static final LitematicaIcons TRASH_CAN                   = new LitematicaIcons(102, 154, 14, 14);

    private LitematicaIcons(int u, int v, int w, int h)
    {
        super(u, v, w, h, LITEMATICA_GUI_TEXTURES);
    }

    private LitematicaIcons(int u, int v, int w, int h, int hoverOffU, int hoverOffV)
    {
        super(u, v, w, h, hoverOffU, hoverOffV, LITEMATICA_GUI_TEXTURES);
    }
}
