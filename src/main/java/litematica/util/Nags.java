package litematica.util;

import malilib.overlay.message.NagHelper;
import litematica.config.Configs;

public class Nags
{
    public static final NagHelper HELPER = new NagHelper(Configs.Nags.SHOW_HELPFUL_REMINDERS,
                                                         Configs.Nags.SHOW_REMINDERS_DISABLE,
                                                         Configs.Nags.SHOW_NEW_USER_EXTRA_NAGS,
                                                         "litematica.message.nag.info.disable_helpful_nags",
                                                         "litematica.message.nag.new_user_extra_nag");
}
