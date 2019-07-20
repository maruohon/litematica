package fi.dy.masa.litematica.config.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.DefaultGuiFactory;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.gui.GuiConfigs;

public class LitematicaGuiFactory extends DefaultGuiFactory
{
    public LitematicaGuiFactory()
    {
        super(Reference.MOD_ID, Reference.MOD_NAME + " configs");
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parent)
    {
        GuiConfigs gui = new GuiConfigs();
        gui.setParent(parent);
        return gui;
    }
}
