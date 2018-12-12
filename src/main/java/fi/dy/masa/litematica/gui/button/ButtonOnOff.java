package fi.dy.masa.litematica.gui.button;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class ButtonOnOff extends ButtonGeneric
{
    protected final String translationKey;

    public ButtonOnOff(int x, int y, int width, int height, String translationKey, boolean isCurrentlyOn, String... hoverStrings)
    {
        super(0, x, y, width, height, "", hoverStrings);

        this.translationKey = translationKey;
        this.updateDisplayString(isCurrentlyOn);
    }

    public void updateDisplayString(boolean isCurrentlyOn)
    {
        this.displayString = getDisplayStringForStatus(this.translationKey, isCurrentlyOn);
    }

    public static String getDisplayStringForStatus(String translationKey, boolean isCurrentlyOn)
    {
        String strStatus = "litematica.gui.label_colored." + (isCurrentlyOn ? "on" : "off");
        return I18n.format(translationKey, I18n.format(strStatus));
    }

    /**
     * Creates a button. Pass -1 as the <b>btnWidth</b> to automatically set the width
     * to a value where the ON and OFF buttons are the same width, using the given translation key.
     * @param x
     * @param y
     * @param btnWidth
     * @param labelKey
     * @return
     */
    public static ButtonOnOff create(int x, int y, int btnWidth, boolean rightAlign, String translationKey, boolean isCurrentlyOn, String... hoverStrings)
    {
        if (btnWidth < 0)
        {
            Minecraft mc = Minecraft.getInstance();
            int w1 = mc.fontRenderer.getStringWidth(ButtonOnOff.getDisplayStringForStatus(translationKey, true));
            int w2 = mc.fontRenderer.getStringWidth(ButtonOnOff.getDisplayStringForStatus(translationKey, false));
            btnWidth = Math.max(w1, w2) + 10;
        }

        if (rightAlign)
        {
            x -= (btnWidth + 2);
        }

        return new ButtonOnOff(x, y, btnWidth, 20, translationKey, isCurrentlyOn, hoverStrings);
    }
}
