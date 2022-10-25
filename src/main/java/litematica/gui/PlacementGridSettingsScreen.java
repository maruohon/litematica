package litematica.gui;

import net.minecraft.util.math.Vec3i;

import malilib.gui.BaseScreen;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.Vec3iEditWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.OnOffButton;
import litematica.data.DataManager;
import litematica.schematic.placement.GridSettings;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;

public class PlacementGridSettingsScreen extends BaseScreen
{
    protected final SchematicPlacementManager manager;
    protected final SchematicPlacement placement;
    protected final GridSettings cachedSettings = new GridSettings();
    protected final LabelWidget gridSizeLabel;
    protected final LabelWidget repeatCountLabel;
    protected final GenericButton resetGridSizeButton;
    protected final OnOffButton toggleGridEnabledButton;
    protected final Vec3iEditWidget sizeEditWidget;
    protected final Vec3iEditWidget negRepeatEditWidget;
    protected final Vec3iEditWidget posRepeatEditWidget;

    public PlacementGridSettingsScreen(SchematicPlacement placement)
    {
        this.placement = placement;
        this.manager = DataManager.getSchematicPlacementManager();

        GridSettings settings = placement.getGridSettings();
        this.cachedSettings.copyFrom(settings);

        this.gridSizeLabel = new LabelWidget("litematica.label.placement_grid_settings.grid_size");
        this.repeatCountLabel = new LabelWidget("litematica.label.placement_grid_settings.repeat_count");

        this.resetGridSizeButton    = GenericButton.create("litematica.button.placement_grid_settings.reset_size", this::resetGridSize);
        this.toggleGridEnabledButton = OnOffButton.onOff(20, "litematica.button.schematic_placement_settings.grid_settings",
                                                         this.placement.getGridSettings()::isEnabled,
                                                         this::toggleGridEnabled);

        this.sizeEditWidget = new Vec3iEditWidget(90, 60, 2, false, settings.getSize(), this::setGridSize);
        this.negRepeatEditWidget = new Vec3iEditWidget(90, 60, 2, false, settings.getRepeatNegative(), this::setRepeatNegative);
        this.posRepeatEditWidget = new Vec3iEditWidget(90, 60, 2, false, settings.getRepeatPositive(), this::setRepeatPositive);

        Vec3i s = settings.getDefaultSize();
        this.sizeEditWidget.setValidRange(s.getX(), s.getY(), s.getZ(), 1000000, 1000000, 1000000);
        this.negRepeatEditWidget.setValidRange(0, 0, 0, 10000, 10000, 10000);
        this.posRepeatEditWidget.setValidRange(0, 0, 0, 10000, 10000, 10000);
        this.negRepeatEditWidget.setLabels("-x", "-y", "-z");
        this.posRepeatEditWidget.setLabels("+x", "+y", "+z");

        this.backgroundColor = 0xC0000000;
        this.renderBorder = true;
        this.titleY = 14;
        this.setTitle("litematica.title.screen.placement_grid_settings", placement.getName());
        this.setScreenWidthAndHeight(360, 220);
        this.centerOnScreen();
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.gridSizeLabel);
        this.addWidget(this.repeatCountLabel);
        this.addWidget(this.resetGridSizeButton);
        this.addWidget(this.toggleGridEnabledButton);
        this.addWidget(this.sizeEditWidget);
        this.addWidget(this.negRepeatEditWidget);
        this.addWidget(this.posRepeatEditWidget);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + 10;

        this.toggleGridEnabledButton.setPosition(x, this.y + 30);
        this.resetGridSizeButton.setPosition(this.toggleGridEnabledButton.getRight() + 8,
                                             this.toggleGridEnabledButton.getY());

        this.gridSizeLabel.setPosition(x, this.toggleGridEnabledButton.getBottom() + 10);
        this.sizeEditWidget.setPosition(x, this.gridSizeLabel.getBottom() + 1);

        this.repeatCountLabel.setPosition(x, this.sizeEditWidget.getBottom() + 10);
        this.negRepeatEditWidget.setPosition(x, this.repeatCountLabel.getBottom() + 1);
        this.posRepeatEditWidget.setPosition(this.negRepeatEditWidget.getRight() + 20,
                                             this.negRepeatEditWidget.getY());
    }

    protected void resetGridSize()
    {
        this.placement.getGridSettings().resetSize();
        this.sizeEditWidget.setPosAndUpdate(this.placement.getGridSettings().getSize());
        this.updatePlacementManager();
    }

    protected void toggleGridEnabled()
    {
        this.placement.getGridSettings().toggleEnabled();
        this.updatePlacementManager();
    }

    protected void setGridSize(Vec3i size)
    {
        this.placement.getGridSettings().setSize(size);
        this.updatePlacementManager();
    }

    protected void setRepeatNegative(Vec3i repeat)
    {
        this.placement.getGridSettings().setRepeatCountNegative(repeat);
        this.updatePlacementManager();
    }

    protected void setRepeatPositive(Vec3i repeat)
    {
        this.placement.getGridSettings().setRepeatCountPositive(repeat);
        this.updatePlacementManager();
    }

    protected void updatePlacementManager()
    {
        GridSettings currentSettings = this.placement.getGridSettings();

        if (this.cachedSettings.equals(currentSettings) == false)
        {
            this.manager.updateGridPlacementsFor(this.placement);
            this.cachedSettings.copyFrom(currentSettings);
        }
    }
}
