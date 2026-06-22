package litematica;

import malilib.util.StringUtils;
import malilib.util.data.ModInfo;

public class Reference
{
    public static final String MOD_ID = "litematica";
    public static final String MOD_NAME = "Litematica";
    public static final String MOD_VERSION = StringUtils.getModVersionString(MOD_ID);

    public static final ModInfo MOD_INFO = new ModInfo(MOD_ID, MOD_NAME);
}
