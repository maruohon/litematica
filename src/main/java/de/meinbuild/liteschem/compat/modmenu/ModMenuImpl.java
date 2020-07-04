package de.meinbuild.liteschem.compat.modmenu;

import java.util.function.Function;
import net.minecraft.client.gui.screen.Screen;
import de.meinbuild.liteschem.Reference;
import de.meinbuild.liteschem.gui.GuiConfigs;
import io.github.prospector.modmenu.api.ModMenuApi;

public class ModMenuImpl implements ModMenuApi
{
    @Override
    public String getModId()
    {
        return Reference.MOD_ID;
    }

    @Override
    public Function<Screen, ? extends Screen> getConfigScreenFactory()
    {
        return (screen) -> {
            GuiConfigs gui = new GuiConfigs();
            gui.setParent(screen);
            return gui;
        };
    }
}
