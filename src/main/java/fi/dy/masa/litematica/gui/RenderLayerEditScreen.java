package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiScreen;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.gui.edit.BaseRenderLayerEditScreen;
import fi.dy.masa.malilib.util.position.LayerRange;

public class RenderLayerEditScreen extends BaseRenderLayerEditScreen
{
    public RenderLayerEditScreen()
    {
        super("litematica", ConfigScreen.ALL_TABS, ConfigScreen.RENDER_LAYERS);

        this.addPlayerFollowingOptions = true;
        this.addLayerRangeHotkeyCheckboxes = true;
        this.controlsStartX = 10;
        this.controlsStartY = 60;
    }

    @Override
    protected LayerRange getLayerRange()
    {
        return DataManager.getRenderLayerRange();
    }

    public static boolean screenValidator(@Nullable GuiScreen currentScreen)
    {
        return currentScreen instanceof RenderLayerEditScreen;
    }

    public static RenderLayerEditScreen openRenderLayerEditScreen(@Nullable GuiScreen currentScreen)
    {
        RenderLayerEditScreen screen = new RenderLayerEditScreen();
        screen.setCurrentTab(ConfigScreen.RENDER_LAYERS);
        return screen;
    }
}
