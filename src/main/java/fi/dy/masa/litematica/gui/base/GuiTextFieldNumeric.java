package fi.dy.masa.litematica.gui.base;

import java.util.regex.Pattern;
import com.google.common.base.Predicate;
import net.minecraft.client.gui.FontRenderer;

public class GuiTextFieldNumeric extends GuiTextFieldGeneric
{
    private static final Pattern PATTER_NUMBER = Pattern.compile("-?[0-9]*");

    public GuiTextFieldNumeric(int id, int x, int y, int width, int height, FontRenderer fontrenderer)
    {
        super(id, fontrenderer, x, y, width, height);

        this.setValidator(new Predicate<String>()
        {
            @Override
            public boolean apply(String input)
            {
                if (input.length() > 0 && PATTER_NUMBER.matcher(input).matches() == false)
                {
                    return false;
                }

                return true;
            }
        });
    }

}
