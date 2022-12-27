package litematica.gui;

import java.nio.file.Path;
import java.util.function.BooleanSupplier;

import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;

import malilib.gui.BaseScreen;
import malilib.gui.widget.BlockPosEditWidget;
import malilib.gui.widget.CheckBoxWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.OnOffButton;
import malilib.util.StringUtils;
import malilib.util.position.Coordinate;
import malilib.util.position.PositionUtils;
import litematica.Reference;
import litematica.data.DataManager;
import litematica.schematic.ISchematic;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;
import litematica.schematic.placement.SubRegionPlacement;

public class SchematicPlacementSubRegionSettingsScreen extends BaseScreen
{
    protected final SchematicPlacementManager manager;
    protected final SchematicPlacement placement;
    protected final SubRegionPlacement subRegion;

    protected final LabelWidget originLabel;
    protected final LabelWidget placementNameLabel;
    protected final LabelWidget regionNameLabel;
    protected final LabelWidget schematicNameLabel;
    protected final GenericButton mirrorButton;
    protected final GenericButton openPlacementSettingsButton;
    protected final GenericButton resetSubRegionButton;
    protected final GenericButton rotateButton;
    protected final GenericButton toggleEntitiesButton;
    protected final GenericButton toggleRegionEnabledButton;
    protected final CheckBoxWidget lockXCoordCheckbox;
    protected final CheckBoxWidget lockYCoordCheckbox;
    protected final CheckBoxWidget lockZCoordCheckbox;
    protected final BlockPosEditWidget originEditWidget;

    public SchematicPlacementSubRegionSettingsScreen(SchematicPlacement placement, SubRegionPlacement subRegion)
    {
        this.placement = placement;
        this.subRegion = subRegion;
        this.manager = DataManager.getSchematicPlacementManager();

        ISchematic schematic = placement.getSchematic();
        Path file = schematic.getFile();
        String fileName = file != null ? file.getFileName().toString() : "-";

        this.originLabel = new LabelWidget("litematica.label.schematic_placement_sub_region_settings.region_position");
        this.placementNameLabel = new LabelWidget(StringUtils.translate("litematica.label.schematic_placement_sub_region_settings.placement_name", placement.getName()));
        this.regionNameLabel = new LabelWidget(StringUtils.translate("litematica.label.schematic_placement_sub_region_settings.region_name", subRegion.getName()));
        this.schematicNameLabel = new LabelWidget(StringUtils.translate("litematica.label.schematic_placement_settings.schematic_name", schematic.getMetadata().getName(), fileName));

        this.mirrorButton                = GenericButton.create(18, this::getMirrorButtonLabel, this::mirror);
        this.openPlacementSettingsButton = GenericButton.create(18, "litematica.button.schematic_placement_sub_region_settings.placement_settings", this::openPlacementSettings);
        this.resetSubRegionButton        = GenericButton.create(18, "litematica.button.schematic_placement_sub_region_settings.reset_region", this::resetSubRegion);
        this.rotateButton                = GenericButton.create(18, this::getRotateButtonLabel, this::rotate);

        this.toggleEntitiesButton      = OnOffButton.onOff(18, "litematica.button.schematic_placement_settings.ignore_entities",
                                                           this.subRegion::ignoreEntities, this::toggleIgnoreEntities);
        this.toggleRegionEnabledButton = OnOffButton.onOff(18, "litematica.button.schematic_placement_sub_region_settings.region_enabled",
                                                           this.subRegion::isEnabled, this::toggleEnabled);

        BlockPos pos = this.subRegion.getPos();
        pos = litematica.util.PositionUtils.getTransformedBlockPos(pos, placement.getMirror(), placement.getRotation()).add(placement.getOrigin());
        this.originEditWidget = new BlockPosEditWidget(90, 72, 2, true, pos, this::setOrigin);

        this.lockXCoordCheckbox = new CheckBoxWidget(null, () -> this.isCoordinateLocked(Coordinate.X), (val) -> this.setCoordinateLocked(val, Coordinate.X));
        this.lockYCoordCheckbox = new CheckBoxWidget(null, () -> this.isCoordinateLocked(Coordinate.Y), (val) -> this.setCoordinateLocked(val, Coordinate.Y));
        this.lockZCoordCheckbox = new CheckBoxWidget(null, () -> this.isCoordinateLocked(Coordinate.Z), (val) -> this.setCoordinateLocked(val, Coordinate.Z));
        this.lockXCoordCheckbox.translateAndAddHoverString("litematica.hover.checkmark.schematic_placement_settings.lock_coordinate");
        this.lockYCoordCheckbox.translateAndAddHoverString("litematica.hover.checkmark.schematic_placement_settings.lock_coordinate");
        this.lockZCoordCheckbox.translateAndAddHoverString("litematica.hover.checkmark.schematic_placement_settings.lock_coordinate");

        BooleanSupplier enabledSupplier = this::isNotLocked;
        this.mirrorButton.setEnabledStatusSupplier(enabledSupplier);
        this.rotateButton.setEnabledStatusSupplier(enabledSupplier);
        this.originEditWidget.setEnabledStatusSupplier(enabledSupplier);
        this.resetSubRegionButton.setEnabledStatusSupplier(() -> this.subRegion.isRegionPlacementModifiedFromDefault() && this.isNotLocked());

        this.setTitle("litematica.title.screen.schematic_placement_sub_region_settings", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.originLabel);
        this.addWidget(this.placementNameLabel);
        this.addWidget(this.regionNameLabel);
        this.addWidget(this.schematicNameLabel);
        this.addWidget(this.mirrorButton);
        this.addWidget(this.openPlacementSettingsButton);
        this.addWidget(this.resetSubRegionButton);
        this.addWidget(this.rotateButton);
        this.addWidget(this.toggleEntitiesButton);
        this.addWidget(this.toggleRegionEnabledButton);
        this.addWidget(this.lockXCoordCheckbox);
        this.addWidget(this.lockYCoordCheckbox);
        this.addWidget(this.lockZCoordCheckbox);
        this.addWidget(this.originEditWidget);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + 12;
        int y = this.y + 25;

        this.regionNameLabel.setPosition(x + 2, y + 2);
        this.placementNameLabel.setPosition(x + 2, this.regionNameLabel.getBottom() + 2);
        this.schematicNameLabel.setPosition(x + 2, this.placementNameLabel.getBottom() + 2);

        x = this.getRight() - 134;
        this.toggleRegionEnabledButton.setPosition(x, this.y + 6);
        this.toggleEntitiesButton.setPosition(x, this.toggleRegionEnabledButton.getBottom() + 1);

        this.originLabel.setPosition(x + 2, this.toggleEntitiesButton.getBottom() + 4);
        this.originEditWidget.setPosition(x + 2, this.originLabel.getBottom() + 1);

        this.rotateButton.setPosition(x, this.originEditWidget.getBottom() + 1);
        this.mirrorButton.setPosition(x, this.rotateButton.getBottom() + 1);
        this.resetSubRegionButton.setPosition(x, this.mirrorButton.getBottom() + 1);

        x = this.originEditWidget.getRight() + 2;
        y = this.originEditWidget.getY();
        this.lockXCoordCheckbox.setPosition(x, y + 2);
        this.lockYCoordCheckbox.setPosition(x, y + 20);
        this.lockZCoordCheckbox.setPosition(x, y + 38);

        y = this.getBottom() - 20;
        this.openPlacementSettingsButton.setPosition(this.x + 10, y);
    }

    protected boolean isNotLocked()
    {
        return this.placement.isLocked() == false;
    }

    protected boolean isCoordinateLocked(Coordinate coordinate)
    {
        return this.subRegion.isCoordinateLocked(coordinate);
    }

    protected void setCoordinateLocked(boolean locked, Coordinate coordinate)
    {
        this.subRegion.setCoordinateLocked(coordinate, locked);
    }

    protected void openPlacementSettings()
    {
        BaseScreen.openScreenWithParent(new SchematicPlacementSettingsScreen(this.placement));
    }

    protected void setOrigin(BlockPos origin)
    {
        this.manager.moveSubRegionTo(this.placement, this.subRegion.getName(), origin);
    }

    protected boolean mirror(int mouseButton, GenericButton button)
    {
        boolean reverse = mouseButton == 1;
        Mirror mirror = PositionUtils.cycleMirror(this.subRegion.getMirror(), reverse);
        this.manager.setSubRegionMirror(this.placement,this.subRegion.getName(),  mirror);
        return true;
    }

    protected boolean rotate(int mouseButton, GenericButton button)
    {
        boolean reverse = mouseButton == 1;
        Rotation rotation = PositionUtils.cycleRotation(this.subRegion.getRotation(), reverse);
        this.manager.setSubRegionRotation(this.placement, this.subRegion.getName(), rotation);
        return true;
    }

    protected void resetSubRegion()
    {
        this.manager.resetSubRegionToSchematicValues(this.placement, this.subRegion.getName());
        this.updateWidgetStates();
    }

    protected void toggleEnabled()
    {
        this.manager.toggleSubRegionEnabled(this.placement, this.subRegion.getName());
    }

    protected void toggleIgnoreEntities()
    {
        this.manager.toggleSubRegionIgnoreEntities(this.placement, this.subRegion.getName());
    }

    protected String getMirrorButtonLabel()
    {
        String val = litematica.util.PositionUtils.getMirrorName(this.subRegion.getMirror());
        String key = "litematica.button.schematic_placement_settings.mirror_value";
        return StringUtils.translate(key, val);
    }

    protected String getRotateButtonLabel()
    {
        String val = litematica.util.PositionUtils.getRotationNameShort(this.subRegion.getRotation());
        String key = "litematica.button.schematic_placement_settings.rotation_value";
        return StringUtils.translate(key, val);
    }
}
