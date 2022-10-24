package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;

import malilib.gui.edit.BaseRenderLayerEditScreen;
import malilib.util.position.LayerRange;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;

public class RenderLayerEditScreen extends BaseRenderLayerEditScreen
{
    public RenderLayerEditScreen()
    {
        super("litematica", ConfigScreen.ALL_TABS, ConfigScreen.RENDER_LAYERS);

        this.addPlayerFollowingOptions = true;
        this.addLayerRangeHotkeyCheckboxes = true;
        this.shouldCreateTabButtons = true;
        this.controlsStartX = 10;
        this.controlsStartY = 60;

        this.setTitle("litematica.title.screen.render_layers", Reference.MOD_VERSION);
        this.createSwitchModConfigScreenDropDown(Reference.MOD_INFO);
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

    public static RenderLayerEditScreen openRenderLayerEditScreen()
    {
        RenderLayerEditScreen screen = new RenderLayerEditScreen();
        screen.setCurrentTab(ConfigScreen.RENDER_LAYERS);
        return screen;
    }
}
