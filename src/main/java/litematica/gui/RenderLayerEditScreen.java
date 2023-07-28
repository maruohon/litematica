package litematica.gui;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;

import malilib.gui.edit.BaseLayerRangeEditScreen;
import litematica.Reference;
import litematica.data.DataManager;

public class RenderLayerEditScreen extends BaseLayerRangeEditScreen
{
    public RenderLayerEditScreen()
    {
        super("litematica", ConfigScreen.ALL_TABS, ConfigScreen.RENDER_LAYERS, DataManager.getRenderLayerRange());

        this.shouldCreateTabButtons = true;
        this.editWidget.setAddLayerRangeHotkeyCheckboxes(true);
        this.editWidget.setAddPlayerFollowingOptions(true);

        this.setTitle("litematica.title.screen.render_layers", Reference.MOD_VERSION);
        this.createSwitchModConfigScreenDropDown(Reference.MOD_INFO);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.editWidget.setPosition(this.x + 10, this.y + 60);
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
