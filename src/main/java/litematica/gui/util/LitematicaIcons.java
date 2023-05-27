package litematica.gui.util;

import malilib.gui.icon.BaseIcon;
import malilib.registry.Registry;
import malilib.util.data.Identifier;
import litematica.Reference;

public class LitematicaIcons
{
    public static final Identifier LITEMATICA_GUI_TEXTURES = new Identifier(Reference.MOD_ID, "textures/gui/gui_widgets.png");

    public static final BaseIcon DUMMY                          = register(  0,   0,  0,  0,  0,  0);
    public static final BaseIcon ENCLOSING_BOX_ENABLED          = register(  0, 144, 16, 16);
    public static final BaseIcon ENCLOSING_BOX_DISABLED         = register(  0, 160, 16, 16);

    // Non-hover-variant icons
    public static final BaseIcon SCHEMATIC_LITEMATIC            = register(144,   0, 12, 12,  0,  0);
    public static final BaseIcon SCHEMATIC_SCHEMATICA           = register(144,  12, 12, 12,  0,  0);
    public static final BaseIcon SCHEMATIC_SPONGE               = register(144,  24, 12, 12,  0,  0);
    public static final BaseIcon SCHEMATIC_VANILLA              = register(144,  36, 12, 12,  0,  0);
    public static final BaseIcon SCHEMATIC_MCEDIT               = register(144,  48, 12, 12,  0,  0);
    public static final BaseIcon FILE_ICON_JSON                 = register(144,  60, 12, 12,  0,  0);

    public static final BaseIcon SCHEMATIC_TYPE_MEMORY          = register(144,  72, 12, 12,  0,  0);
    public static final BaseIcon IN_MEMORY_OVERLAY              = register(144,  84, 12, 12,  0,  0);

    public static final BaseIcon SCHEMATIC_IN_MEMORY_LITEMATIC  = register(156,   0, 12, 12,  0,  0);
    public static final BaseIcon SCHEMATIC_IN_MEMORY_SCHEMATICA = register(156,  12, 12, 12,  0,  0);
    public static final BaseIcon SCHEMATIC_IN_MEMORY_SPONGE     = register(156,  24, 12, 12,  0,  0);
    public static final BaseIcon SCHEMATIC_IN_MEMORY_VANILLA    = register(156,  36, 12, 12,  0,  0);
    public static final BaseIcon SCHEMATIC_IN_MEMORY_MCEDIT     = register(156,  48, 12, 12,  0,  0);

    // Hover-variant icons
    public static final BaseIcon AREA_EDITOR                = register(102,  70, 14, 14);
    public static final BaseIcon AREA_SELECTION             = register(102,   0, 14, 14);
    public static final BaseIcon CONFIGURATION              = register(102,  84, 14, 14);
    public static final BaseIcon DUPLICATE                  = register(102, 168, 14, 14);
    public static final BaseIcon LOADED_SCHEMATICS          = register(102,  14, 14, 14);
    public static final BaseIcon PLACEMENT                  = register(102, 196, 14, 14);
    public static final BaseIcon RELOAD                     = register(102, 182, 14, 14);
    public static final BaseIcon SAVE_TO_DISK               = register(102, 140, 14, 14);
    public static final BaseIcon SCHEMATIC_BROWSER          = register(102,  28, 14, 14);
    public static final BaseIcon SCHEMATIC_MANAGER          = register(102,  56, 14, 14);
    public static final BaseIcon SCHEMATIC_PLACEMENTS       = register(102,  42, 14, 14);
    public static final BaseIcon SCHEMATIC_VCS              = register(102,  98, 14, 14);
    public static final BaseIcon TASK_MANAGER               = register(102, 112, 14, 14);
    public static final BaseIcon TRASH_CAN                  = register(102, 154, 14, 14);

    private static BaseIcon register(int u, int v, int w, int h)
    {
        return register(u, v, w, h, w, 0);
    }

    private static BaseIcon register(int u, int v, int w, int h, int variantOffU, int variantOffV)
    {
        BaseIcon icon = new BaseIcon(u, v, w, h, variantOffU, variantOffV, LITEMATICA_GUI_TEXTURES);
        return Registry.ICON.registerModIcon(icon);
    }
}
