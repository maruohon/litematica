package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.BaseRenderLayerEditScreen;
import fi.dy.masa.malilib.gui.widget.button.BaseButton;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.ButtonActionListener;
import fi.dy.masa.malilib.gui.config.ConfigTab;
import fi.dy.masa.malilib.gui.icon.Icon;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.util.position.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiRenderLayer extends BaseRenderLayerEditScreen
{
    public GuiRenderLayer()
    {
        super();

        this.addPlayerFollowingOptions = true;
    }

    @Override
    public void initGui()
    {
        DataManager.setConfigGuiTab(ConfigScreen.RENDER_LAYERS);

        super.initGui();

        int x = 10;
        int y = 26;

        for (ConfigTab tab : ConfigScreen.TABS)
        {
            x += this.createTabButton(x, y, -1, tab);
        }

        x = 10;
        y = 60;

        this.createLayerEditControls(x, y, this.getLayerRange());
    }

    @Override
    protected LayerRange getLayerRange()
    {
        return DataManager.getRenderLayerRange();
    }

    @Override
    protected Icon getValueAdjustButtonIcon()
    {
        return LitematicaIcons.BUTTON_PLUS_MINUS_16;
    }

    private int createTabButton(int x, int y, int width, ConfigTab tab)
    {
        ButtonListenerTab listener = new ButtonListenerTab(tab);
        boolean enabled = DataManager.getConfigGuiTab() != tab;
        String label = tab.getDisplayName();

        if (width < 0)
        {
            width = this.getStringWidth(label) + 10;
        }

        GenericButton button = new GenericButton(x, y, width, 20, label);
        button.setEnabled(enabled);
        this.addButton(button, listener);

        return width + 2;
    }

    @Override
    protected int createHotkeyCheckBoxes(int x, int y, LayerRange layerRange)
    {
        String label = StringUtils.translate("litematica.gui.label.render_layers.hotkey");
        String hover = StringUtils.translate("litematica.gui.label.render_layers.hover.hotkey");

        CheckBoxWidget cb = new CheckBoxWidget(x, y + 4, LitematicaIcons.CHECKBOX_UNSELECTED, LitematicaIcons.CHECKBOX_SELECTED, label, hover);
        cb.setChecked(layerRange.getMoveLayerRangeMax(), false);
        cb.setListener(new RangeHotkeyListener(layerRange, true));
        this.addWidget(cb);

        y += 23;
        cb = new CheckBoxWidget(x, y + 4, LitematicaIcons.CHECKBOX_UNSELECTED, LitematicaIcons.CHECKBOX_SELECTED, label, hover);
        cb.setChecked(layerRange.getMoveLayerRangeMin(), false);
        cb.setListener(new RangeHotkeyListener(layerRange, false));
        this.addWidget(cb);

        return y;
    }

    private static class ButtonListenerTab implements ButtonActionListener
    {
        private final ConfigTab tab;

        public ButtonListenerTab(ConfigTab tab)
        {
            this.tab = tab;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            DataManager.setConfigGuiTab(this.tab);
            BaseScreen.openGui(new ConfigScreen());
        }
    }
}
