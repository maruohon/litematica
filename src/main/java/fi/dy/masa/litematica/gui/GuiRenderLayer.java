package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiRenderLayerEditBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IConfigGuiTab;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.gui.widgets.WidgetCheckBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiRenderLayer extends GuiRenderLayerEditBase
{
    @Override
    public void initGui()
    {
        DataManager.setConfigGuiTab(GuiConfigs.RENDER_LAYERS);

        super.initGui();

        int x = 10;
        int y = 26;

        for (IConfigGuiTab tab : GuiConfigs.TABS)
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
    protected IGuiIcon getValueAdjustButtonIcon()
    {
        return Icons.BUTTON_PLUS_MINUS_16;
    }

    private int createTabButton(int x, int y, int width, IConfigGuiTab tab)
    {
        ButtonListenerTab listener = new ButtonListenerTab(tab);
        boolean enabled = DataManager.getConfigGuiTab() != tab;
        String label = tab.getDisplayName();

        if (width < 0)
        {
            width = this.getStringWidth(label) + 10;
        }

        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
        button.setEnabled(enabled);
        this.addButton(button, listener);

        return width + 2;
    }

    @Override
    protected int createHotkeyCheckBoxes(int x, int y, LayerRange layerRange)
    {
        String label = StringUtils.translate("litematica.gui.label.render_layers.hotkey");
        String hover = StringUtils.translate("litematica.gui.label.render_layers.hover.hotkey");

        WidgetCheckBox cb = new WidgetCheckBox(x, y + 4, Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED, label, hover);
        cb.setChecked(layerRange.getMoveLayerRangeMax(), false);
        cb.setListener(new RangeHotkeyListener(layerRange, true));
        this.addWidget(cb);

        y += 23;
        cb = new WidgetCheckBox(x, y + 4, Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED, label, hover);
        cb.setChecked(layerRange.getMoveLayerRangeMin(), false);
        cb.setListener(new RangeHotkeyListener(layerRange, false));
        this.addWidget(cb);

        return y;
    }

    private static class ButtonListenerTab implements IButtonActionListener
    {
        private final IConfigGuiTab tab;

        public ButtonListenerTab(IConfigGuiTab tab)
        {
            this.tab = tab;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            DataManager.setConfigGuiTab(this.tab);
            GuiBase.openGui(new GuiConfigs());
        }
    }
}
