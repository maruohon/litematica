package litematica.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import malilib.config.option.BooleanConfig;
import malilib.config.option.ConfigOption;
import malilib.gui.BaseScreen;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.BooleanConfigButton;
import malilib.util.StringUtils;
import litematica.config.Configs;

public class SchematicInfoConfigScreen extends BaseScreen
{
    protected final List<LabelWidget> labels = new ArrayList<>();
    protected final List<BooleanConfigButton> buttons = new ArrayList<>();
    protected int labelWidth;

    public SchematicInfoConfigScreen()
    {
        this.backgroundColor = 0xFF000000;
        this.renderBorder = true;
        this.useTitleHierarchy = false;

        this.setTitle("litematica.title.screen.schematic_info_options");

        List<ConfigOption<?>> configList = Configs.Internal.OPTIONS.stream().filter(c -> c.getName().startsWith("schematicInfo")).collect(Collectors.toList());
        this.labelWidth = StringUtils.getMaxStringRenderWidthOfObjects(configList, ConfigOption::getPrettyName);

        int totalWidth = this.labelWidth + 70;
        totalWidth = Math.max(totalWidth, this.titleText.renderWidth + 20); // title needs to have been set before!
        int totalHeight = configList.size() * 18 + 30;

        for (ConfigOption<?> cfg : configList)
        {
            if (cfg instanceof BooleanConfig)
            {
                this.createLabelAndConfigWidgets(this.labelWidth, (BooleanConfig) cfg);
            }
        }

        this.setScreenWidthAndHeight(totalWidth, totalHeight);
        this.centerOnScreen();
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.labels.forEach(this::addWidget);
        this.buttons.forEach(this::addWidget);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + 10;
        int y = this.y + 24;
        int max = this.buttons.size();

        for (int i = 0; i < max; ++i)
        {
            this.labels.get(i).setPosition(x, y);
            this.buttons.get(i).setPosition(x + this.labelWidth + 10, y);
            y += 18;
        }
    }

    protected void createLabelAndConfigWidgets(int labelWidth, BooleanConfig config)
    {
        int color = config.isModified() ? 0xFFFFFF55 : 0xFFAAAAAA;
        LabelWidget label = new LabelWidget(color, config.getPrettyName());
        label.setSize(labelWidth + 4, 16);
        label.getPadding().setTop(3);
        config.getComment().ifPresent(label::addHoverStrings);

        this.labels.add(label);
        this.buttons.add(new BooleanConfigButton(-1, 16, config));
    }
}
