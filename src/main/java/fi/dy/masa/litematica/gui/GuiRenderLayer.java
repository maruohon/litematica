package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiConfigs.ConfigGuiTab;
import fi.dy.masa.litematica.util.LayerMode;
import fi.dy.masa.litematica.util.LayerRange;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldInteger;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonIcon;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;

public class GuiRenderLayer extends GuiBase
{
    private GuiTextField textField1;
    private GuiTextField textField2;
    private int id;

    @Override
    public void initGui()
    {
        super.initGui();

        this.id = 0;
        int x = 10;
        int y = 26;

        x += this.createTabButton(x, y, -1, ConfigGuiTab.GENERIC) + 4;
        x += this.createTabButton(x, y, -1, ConfigGuiTab.VISUALS) + 4;
        x += this.createTabButton(x, y, -1, ConfigGuiTab.COLORS) + 4;
        x += this.createTabButton(x, y, -1, ConfigGuiTab.HOTKEYS) + 4;
        x += this.createTabButton(x, y, -1, ConfigGuiTab.RENDER_LAYERS) + 4;

        x = 10;
        y = 60;

        x += this.createLayerConfigButton(x, y, ButtonListenerMode.Type.MODE) + 10;
        this.createLayerConfigButton(x, y, ButtonListenerMode.Type.AXIS);
        y += 26;
        x = 10;

        y += this.createTextFields(x, y, 60);
    }

    private int createTabButton(int x, int y, int width, ConfigGuiTab tab)
    {
        ButtonListenerTab listener = new ButtonListenerTab(tab);
        boolean enabled = DataManager.getConfigGuiTab() != tab;
        String label = tab.getDisplayName();

        if (width < 0)
        {
            width = this.mc.fontRenderer.getStringWidth(label) + 10;
        }

        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, width, 20, label);
        button.enabled = enabled;
        this.addButton(button, listener);

        return width;
    }

    private int createLayerConfigButton(int x, int y, ButtonListenerMode.Type type)
    {
        if (type == ButtonListenerMode.Type.MODE || DataManager.getRenderLayerRange().getLayerMode() != LayerMode.ALL)
        {
            ButtonListenerMode listener = new ButtonListenerMode(type, this);
            String label = type.getDisplayName();
            int width = this.mc.fontRenderer.getStringWidth(label) + 10;

            ButtonGeneric button = new ButtonGeneric(this.id++, x, y, width, 20, label);
            this.addButton(button, listener);

            return width;
        }

        return 0;
    }

    private int createTextFields(int x, int y, int width)
    {
        LayerRange layerRange = DataManager.getRenderLayerRange();
        LayerMode layerMode = layerRange.getLayerMode();

        if (layerMode == LayerMode.ALL)
        {
            return 0;
        }

        int yOffset = 22;

        if (layerMode == LayerMode.LAYER_RANGE)
        {
            String labelMin = I18n.format("litematica.gui.label.render_layers.layer_min") + ":";
            String labelMax = I18n.format("litematica.gui.label.render_layers.layer_max") + ":";
            int w1 = GuiBase.getTextWidth(labelMin);
            int w2 = GuiBase.getTextWidth(labelMax);

            this.addLabel(this.id++, x, y     , w1, 20, 0xFFFFFF, labelMax);
            this.addLabel(this.id++, x, y + 23, w2, 20, 0xFFFFFF, labelMin);

            x += Math.max(w1, w2) + 10;
            yOffset = 45;
        }
        else
        {
            String label = I18n.format("litematica.gui.label.render_layers.layer") + ":";
            int w = GuiBase.getTextWidth(label);
            this.addLabel(this.id++, x, y, w, 20, 0xFFFFFF, label);

            x += w + 10;
        }

        if (layerMode == LayerMode.LAYER_RANGE)
        {
            this.textField2 = new GuiTextFieldInteger(this.id++, x, y, width, 20, this.mc.fontRenderer);
            this.addTextField(this.textField2, this.createTextFieldListener(layerMode, true));
            this.createValueAdjustButton(x + width + 3, y, true);
            y += 23;
        }
        else
        {
            this.textField2 = null;
        }

        this.textField1 = new GuiTextFieldInteger(this.id++, x, y, width, 20, this.mc.fontRenderer);
        this.addTextField(this.textField1, this.createTextFieldListener(layerMode, false));
        this.createValueAdjustButton(x + width + 3, y, false);
        y += 23;

        this.updateTextFieldValues(layerRange);

        this.createLayerConfigButton(x - 1, y, ButtonListenerMode.Type.SET_HERE);

        return yOffset;
    }

    private void updateTextFieldValues(LayerRange layerRange)
    {
        if (this.textField1 != null)
        {
            this.textField1.setText(String.valueOf(layerRange.getCurrentLayerValue(false)));
        }

        if (this.textField2 != null)
        {
            this.textField2.setText(String.valueOf(layerRange.getCurrentLayerValue(true)));
        }
    }

    private void createValueAdjustButton(int x, int y, boolean isSecondValue)
    {
        LayerRange layerRange = DataManager.getRenderLayerRange();
        LayerMode layerMode = layerRange.getLayerMode();
        ButtonListenerChangeValue listener = new ButtonListenerChangeValue(layerMode, isSecondValue, this);
        ButtonIcon button = new ButtonIcon(this.id++, x, y + 2, 16, 16, Icons.BUTTON_PLUS_MINUS_16);
        this.addButton(button, listener);
    }

    private TextFieldListener createTextFieldListener(LayerMode mode, boolean isSecondLimit)
    {
        return new TextFieldListener(mode, isSecondLimit);
    }

    private static class ButtonListenerTab implements IButtonActionListener<ButtonGeneric>
    {
        private final ConfigGuiTab tab;

        public ButtonListenerTab(ConfigGuiTab tab)
        {
            this.tab = tab;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            DataManager.setConfigGuiTab(this.tab);
            Minecraft.getMinecraft().displayGuiScreen(new GuiConfigs());
        }
    }

    private static class ButtonListenerMode implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiRenderLayer parent;
        private final Type type;

        public ButtonListenerMode(Type type, GuiRenderLayer parent)
        {
            this.type = type;
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            LayerRange layerRange = DataManager.getRenderLayerRange();

            if (this.type == Type.MODE)
            {
                layerRange.setLayerMode((LayerMode) layerRange.getLayerMode().cycle(mouseButton == 0));
            }
            else if (this.type == Type.AXIS)
            {
                EnumFacing.Axis axis = layerRange.getAxis();
                int next = mouseButton == 0 ? ((axis.ordinal() + 1) % 3) : (axis.ordinal() == 0 ? 2 : axis.ordinal() - 1);
                axis = EnumFacing.Axis.values()[next % 3];
                layerRange.setAxis(axis);
            }
            else if (this.type == Type.SET_HERE)
            {
                layerRange.setToPosition(this.parent.mc.player);
            }

            this.parent.initGui();
        }

        public enum Type
        {
            MODE        ("litematica.gui.button.render_layers_gui.layers"),
            AXIS        ("litematica.gui.button.render_layers_gui.axis"),
            SET_HERE    ("litematica.gui.button.render_layers_gui.set_here");

            private final String translationKey;

            private Type(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getDisplayName()
            {
                if (this == SET_HERE)
                {
                    return I18n.format(this.translationKey);
                }
                else
                {
                    LayerRange layerRange = DataManager.getRenderLayerRange();
                    String valueStr = this == MODE ? layerRange.getLayerMode().getDisplayName() : layerRange.getAxis().name();

                    return I18n.format(this.translationKey, valueStr);
                }
            }
        }
    }

    private static class ButtonListenerChangeValue implements IButtonActionListener<ButtonIcon>
    {
        private final GuiRenderLayer parent;
        private final LayerMode mode;
        private final boolean isSecondLimit;

        private ButtonListenerChangeValue(LayerMode mode, boolean isSecondLimit, GuiRenderLayer parent)
        {
            this.mode = mode;
            this.isSecondLimit = isSecondLimit;
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ButtonIcon control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonIcon control, int mouseButton)
        {
            LayerRange layerRange = DataManager.getRenderLayerRange();
            int change = mouseButton == 1 ? -1 : 1;

            if (GuiScreen.isShiftKeyDown())
            {
                change *= 16;
            }

            if (GuiScreen.isCtrlKeyDown())
            {
                change *= 64;
            }

            switch (this.mode)
            {
                case ALL_ABOVE:
                    layerRange.setLayerAbove(layerRange.getLayerAbove() + change);
                    break;

                case ALL_BELOW:
                    layerRange.setLayerBelow(layerRange.getLayerBelow() + change);
                    break;

                case SINGLE_LAYER:
                    layerRange.setLayerSingle(layerRange.getLayerSingle() + change);
                    break;

                case LAYER_RANGE:
                    if (this.isSecondLimit)
                    {
                        layerRange.setLayerRangeMax(layerRange.getLayerRangeMax() + change);
                    }
                    else
                    {
                        layerRange.setLayerRangeMin(layerRange.getLayerRangeMin() + change);
                    }
                    break;

                default:
            }

            this.parent.updateTextFieldValues(layerRange);
        }
    }

    private static class TextFieldListener implements ITextFieldListener<GuiTextField>
    {
        private final LayerMode mode;
        private final boolean isSecondLimit;

        private TextFieldListener(LayerMode mode, boolean isSecondLimit)
        {
            this.mode = mode;
            this.isSecondLimit = isSecondLimit;
        }

        @Override
        public boolean onGuiClosed(GuiTextField textField)
        {
            return this.onTextChange(textField);
        }

        @Override
        public boolean onTextChange(GuiTextField textField)
        {
            int value = 0;

            try
            {
                value = Integer.parseInt(textField.getText());
            }
            catch (NumberFormatException e)
            {
                return false;
            }

            LayerRange layerRange = DataManager.getRenderLayerRange();

            switch (this.mode)
            {
                case ALL_ABOVE:
                    layerRange.setLayerAbove(value);
                    break;

                case ALL_BELOW:
                    layerRange.setLayerBelow(value);
                    break;

                case SINGLE_LAYER:
                    layerRange.setLayerSingle(value);
                    break;

                case LAYER_RANGE:
                    if (this.isSecondLimit)
                    {
                        layerRange.setLayerRangeMax(value);
                    }
                    else
                    {
                        layerRange.setLayerRangeMin(value);
                    }
                    break;

                default:
            }

            return true;
        }
    }
}
