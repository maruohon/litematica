package fi.dy.masa.litematica.gui;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.util.LitematicaIcons;
import fi.dy.masa.litematica.gui.widget.list.entry.SchematicPlacementSubRegionEntryWidget;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifierManager;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.TextInputScreen;
import fi.dy.masa.malilib.gui.icon.Icon;
import fi.dy.masa.malilib.gui.icon.MultiIcon;
import fi.dy.masa.malilib.gui.widget.BaseTextFieldWidget;
import fi.dy.masa.malilib.gui.widget.BlockPosEditWidget;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.gui.widget.IconWidget;
import fi.dy.masa.malilib.gui.widget.LabelWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.OnOffButton;
import fi.dy.masa.malilib.gui.widget.list.DataListWidget;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.render.text.StyledText;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.PositionUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.position.Coordinate;

public class SchematicPlacementSettingsScreen extends BaseListScreen<DataListWidget<SubRegionPlacement>>
{
    protected final SchematicPlacement placement;
    protected final SchematicPlacementManager manager;

    protected final LabelWidget originLabel;
    protected final LabelWidget schematicNameLabel;
    protected final LabelWidget subRegionsLabel;
    protected final IconWidget schematicTypeIcon;
    protected final BaseTextFieldWidget nameTextField;
    protected final GenericButton copyPasteSettingsButton;
    protected final GenericButton mirrorButton;
    protected final GenericButton openMaterialListButton;
    protected final GenericButton openPlacementListButton;
    protected final GenericButton openVerifierButton;
    protected final GenericButton renameButton;
    protected final GenericButton resetSubRegionsButton;
    protected final GenericButton rotateButton;
    protected final GenericButton toggleAllRegionsOffButton;
    protected final GenericButton toggleAllRegionsOnButton;
    protected final GenericButton toggleEnclosingBoxButton;
    protected final OnOffButton gridSettingsButton;
    protected final OnOffButton toggleEntitiesButton;
    protected final OnOffButton toggleLockedButton;
    protected final OnOffButton togglePlacementEnabledButton;
    protected final CheckBoxWidget lockXCoordCheckbox;
    protected final CheckBoxWidget lockYCoordCheckbox;
    protected final CheckBoxWidget lockZCoordCheckbox;
    protected final BlockPosEditWidget originEditWidget;

    public SchematicPlacementSettingsScreen(SchematicPlacement placement)
    {
        super(10, 77, 150, 99);

        this.placement = placement;
        this.manager = DataManager.getSchematicPlacementManager();

        ISchematic schematic = placement.getSchematic();
        File file = schematic.getFile();
        String fileName = file != null ? file.getName() : "-";

        this.nameTextField = new BaseTextFieldWidget(300, 16, placement.getName());
        this.originLabel = new LabelWidget("litematica.label.schematic_placement_settings.placement_origin");
        this.subRegionsLabel = new LabelWidget();

        StyledText name = StyledText.translate("litematica.label.schematic_placement_settings.schematic_name",
                                           schematic.getMetadata().getName(), fileName);
        this.schematicNameLabel = new LabelWidget();
        this.schematicNameLabel.setLabelStyledText(name);

        this.copyPasteSettingsButton   = GenericButton.create(18, "malilib.button.export_slash_import", this::clickCopyPasteSettings);
        this.mirrorButton              = GenericButton.create(18, this::getMirrorButtonLabel, this::mirror);
        this.openMaterialListButton    = GenericButton.create(18, "litematica.button.misc.material_list", this::openMaterialList);
        this.openPlacementListButton   = GenericButton.create(18, "litematica.button.change_menu.schematic_placements", this::openPlacementList);
        this.openVerifierButton        = GenericButton.create(18, "litematica.button.misc.schematic_verifier", this::openVerifier);
        this.renameButton              = GenericButton.create(16, "litematica.button.misc.rename", this::renamePlacement);
        this.resetSubRegionsButton     = GenericButton.create(18, "litematica.button.schematic_placement_settings.reset_sub_regions", this::resetSubRegions);
        this.rotateButton              = GenericButton.create(18, this::getRotateButtonLabel, this::rotate);
        this.toggleAllRegionsOffButton = GenericButton.create(18, "litematica.button.schematic_placement_settings.toggle_all_off", this::toggleAllRegionsOff);
        this.toggleAllRegionsOnButton  = GenericButton.create(18, "litematica.button.schematic_placement_settings.toggle_all_on", this::toggleAllRegionsOn);
        this.toggleEnclosingBoxButton  = GenericButton.create(this::getEnclosingBoxButtonIcon, this::toggleEnclosingBoxRendering);

        this.gridSettingsButton   = OnOffButton.onOff(18, "litematica.button.schematic_placement_settings.grid_settings",
                                                      this.placement.getGridSettings()::isEnabled, this::clickGridSettings);
        this.toggleEntitiesButton = OnOffButton.onOff(18, "litematica.button.schematic_placement_settings.ignore_entities",
                                                      this.placement::ignoreEntities, this::toggleIgnoreEntities);
        this.toggleLockedButton   = OnOffButton.onOff(18, "litematica.button.schematic_placement_settings.locked",
                                                      this.placement::isLocked, this.placement::toggleLocked);
        this.togglePlacementEnabledButton = OnOffButton.onOff(18, "litematica.button.placement_list.placement_enabled",
                                                              this.placement::isEnabled, this::toggleEnabled);
        this.originEditWidget = new BlockPosEditWidget(90, 72, 2, true, placement.getOrigin(), this::setOrigin);

        this.lockXCoordCheckbox = new CheckBoxWidget();
        this.lockYCoordCheckbox = new CheckBoxWidget();
        this.lockZCoordCheckbox = new CheckBoxWidget();

        Icon icon = placement.getSchematicFile() != null ? placement.getSchematic().getType().getIcon() : LitematicaIcons.SCHEMATIC_TYPE_MEMORY;
        this.schematicTypeIcon = new IconWidget(icon);

        this.lockXCoordCheckbox.translateAndAddHoverString("litematica.hover.checkmark.schematic_placement_settings.lock_coordinate");
        this.lockYCoordCheckbox.translateAndAddHoverString("litematica.hover.checkmark.schematic_placement_settings.lock_coordinate");
        this.lockZCoordCheckbox.translateAndAddHoverString("litematica.hover.checkmark.schematic_placement_settings.lock_coordinate");

        this.copyPasteSettingsButton.setRenderButtonBackgroundTexture(true);
        this.copyPasteSettingsButton.translateAndAddHoverString("litematica.hover.button.schematic_placement_settings.copy_paste_settings");
        this.gridSettingsButton.translateAndAddHoverString("litematica.hover.button.schematic_placement_settings.grid_settings");
        this.renameButton.translateAndAddHoverString("litematica.hover.button.schematic_placement_settings.rename_placement");
        this.toggleLockedButton.translateAndAddHoverString("litematica.hover.button.schematic_placement_settings.lock");

        this.toggleEnclosingBoxButton.getHoverInfoFactory().setStringListProvider("_default", this::getEnclosingBoxButtonHoverText);
        this.resetSubRegionsButton.setEnabledStatusSupplier(() -> this.placement.isRegionPlacementModified() && this.isNotLocked());

        BooleanSupplier enabledSupplier = this::isNotLocked;
        this.mirrorButton.setEnabledStatusSupplier(enabledSupplier);
        this.rotateButton.setEnabledStatusSupplier(enabledSupplier);
        this.originEditWidget.setEnabledStatusSupplier(enabledSupplier);

        this.lockXCoordCheckbox.setBooleanStorage(() -> this.isCoordinateLocked(Coordinate.X), (val) -> this.setCoordinateLocked(val, Coordinate.X));
        this.lockYCoordCheckbox.setBooleanStorage(() -> this.isCoordinateLocked(Coordinate.Y), (val) -> this.setCoordinateLocked(val, Coordinate.Y));
        this.lockZCoordCheckbox.setBooleanStorage(() -> this.isCoordinateLocked(Coordinate.Z), (val) -> this.setCoordinateLocked(val, Coordinate.Z));

        this.setTitle("litematica.title.screen.schematic_placement_settings", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.copyPasteSettingsButton);
        this.addWidget(this.gridSettingsButton);
        this.addWidget(this.lockXCoordCheckbox);
        this.addWidget(this.lockYCoordCheckbox);
        this.addWidget(this.lockZCoordCheckbox);
        this.addWidget(this.mirrorButton);
        this.addWidget(this.nameTextField);
        this.addWidget(this.openMaterialListButton);
        this.addWidget(this.openPlacementListButton);
        this.addWidget(this.openVerifierButton);
        this.addWidget(this.originEditWidget);
        this.addWidget(this.originLabel);
        this.addWidget(this.renameButton);
        this.addWidget(this.resetSubRegionsButton);
        this.addWidget(this.rotateButton);
        this.addWidget(this.schematicNameLabel);
        this.addWidget(this.schematicTypeIcon);
        this.addWidget(this.subRegionsLabel);
        this.addWidget(this.toggleAllRegionsOffButton);
        this.addWidget(this.toggleAllRegionsOnButton);
        this.addWidget(this.toggleEnclosingBoxButton);
        this.addWidget(this.toggleEntitiesButton);
        this.addWidget(this.toggleLockedButton);
        this.addWidget(this.togglePlacementEnabledButton);

        this.updateLabels();
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + 12;
        int y = this.y + 25;
        this.schematicTypeIcon.setPosition(x, y + 2);
        this.nameTextField.setPosition(x + 16, y);
        this.nameTextField.setWidth(Math.min(240, this.screenWidth - 300));
        this.renameButton.setPosition(this.nameTextField.getRight() + 4, y);
        this.schematicNameLabel.setPosition(x + 2, this.nameTextField.getBottom() + 3);

        this.subRegionsLabel.setPosition(x + 2, this.getListY() - 10);

        this.copyPasteSettingsButton.setRight(this.getRight() - 140);
        this.copyPasteSettingsButton.setY(this.y + 6);
        this.gridSettingsButton.setRight(this.copyPasteSettingsButton.getRight());
        this.gridSettingsButton.setY(this.copyPasteSettingsButton.getBottom() + 1);
        this.toggleAllRegionsOffButton.setRight(this.gridSettingsButton.getRight());
        this.toggleAllRegionsOffButton.setY(this.gridSettingsButton.getBottom() + 1);
        this.toggleAllRegionsOnButton.setRight(this.toggleAllRegionsOffButton.getX() - 4);
        this.toggleAllRegionsOnButton.setY(this.toggleAllRegionsOffButton.getY());

        x = this.getRight() - 134;
        this.togglePlacementEnabledButton.setPosition(x, this.y + 6);

        this.toggleLockedButton.setPosition(x, this.togglePlacementEnabledButton.getBottom() + 1);
        this.toggleEnclosingBoxButton.setPosition(this.toggleLockedButton.getRight() + 2,
                                                  this.toggleLockedButton.getY() + 1);

        this.toggleEntitiesButton.setPosition(x, this.toggleLockedButton.getBottom() + 1);

        this.originLabel.setPosition(x + 2, this.toggleEntitiesButton.getBottom() + 4);
        this.originEditWidget.setPosition(x + 2, this.originLabel.getBottom() + 1);

        this.rotateButton.setPosition(x, this.originEditWidget.getBottom() + 1);
        this.mirrorButton.setPosition(x, this.rotateButton.getBottom() + 1);
        this.resetSubRegionsButton.setPosition(x, this.mirrorButton.getBottom() + 1);

        x = this.originEditWidget.getRight() + 2;
        y = this.originEditWidget.getY();
        this.lockXCoordCheckbox.setPosition(x, y + 2);
        this.lockYCoordCheckbox.setPosition(x, y + 20);
        this.lockZCoordCheckbox.setPosition(x, y + 38);

        y = this.getBottom() - 20;
        this.openMaterialListButton.setPosition(this.x + 10, y);
        this.openVerifierButton.setPosition(this.openMaterialListButton.getRight() + 2, y);
        this.openPlacementListButton.setRight(this.getRight() - 10);
        this.openPlacementListButton.setY(y);
    }

    protected void updateWidgetStates()
    {
        this.mirrorButton.updateWidgetState();
        this.nameTextField.updateWidgetState();
        this.originEditWidget.updateWidgetState();
        this.resetSubRegionsButton.updateWidgetState();
        this.rotateButton.updateWidgetState();
        this.toggleEnclosingBoxButton.updateWidgetState();
        this.toggleEntitiesButton.updateWidgetState();
        this.toggleLockedButton.updateWidgetState();
        this.togglePlacementEnabledButton.updateWidgetState();
    }

    @Override
    protected DataListWidget<SubRegionPlacement> createListWidget()
    {
        Supplier<List<SubRegionPlacement>> supplier = this.placement::getAllSubRegionsPlacements;
        DataListWidget<SubRegionPlacement> listWidget = new DataListWidget<>(supplier, true);

        listWidget.setListEntryWidgetFixedHeight(20);
        listWidget.getEntrySelectionHandler()
                .setAllowSelection(true)
                .setSelectionListener(this::onSelectionChange);
        listWidget.addDefaultSearchBar();
        listWidget.setEntryFilterStringFunction((p) -> Collections.singletonList(p.getName()));
        listWidget.setDataListEntryWidgetFactory((d, cd) -> new SchematicPlacementSubRegionEntryWidget(d, cd, this.placement));

        return listWidget;
    }

    protected void onSelectionChange(@Nullable SubRegionPlacement placement)
    {
        boolean clearSelection = placement == null || placement.getName().equals(this.placement.getSelectedSubRegionName());
        this.placement.setSelectedSubRegionName(clearSelection ? null : placement.getName());
    }

    protected boolean isNotLocked()
    {
        return this.placement.isLocked() == false;
    }

    protected boolean isCoordinateLocked(Coordinate coordinate)
    {
        return this.placement.isCoordinateLocked(coordinate);
    }

    protected void setCoordinateLocked(boolean locked, Coordinate coordinate)
    {
        this.placement.setCoordinateLocked(coordinate, locked);
    }

    protected void setOrigin(BlockPos origin)
    {
        this.manager.setOrigin(this.placement, origin);
    }

    protected boolean clickCopyPasteSettings(int mouseButton, GenericButton button)
    {
        if (isShiftDown())
        {
            if (mouseButton == 1)
            {
                // Ctrl + Shift + Right click: load settings from clip board
                if (isCtrlDown())
                {
                    String str = getClipboardString();

                    if (this.manager.loadPlacementSettings(this.placement, str))
                    {
                        MessageDispatcher.success("litematica.message.info.settings_loaded_from_clipboard");
                        this.initScreen();
                    }
                }
            }
            // Shift + left click: Copy settings to clip board
            else
            {
                String str = JsonUtils.jsonToString(this.placement.baseSettingsToJson(true), true);
                GuiScreen.setClipboardString(str);
                MessageDispatcher.success("litematica.message.info.settings_copied_to_clipboard");
            }
        }
        else
        {
            JsonObject origJson = this.placement.baseSettingsToJson(true);
            String titleKey = "litematica.title.screen.schematic_placement_settings.copy_or_load_settings";

            openPopupScreen(new TextInputScreen(titleKey, origJson.toString(),
                                                (str) -> this.loadSettingsFromString(str, origJson), this));
        }

        return true;
    }

    protected boolean loadSettingsFromString(String str, JsonObject origJson)
    {
        JsonElement el = JsonUtils.parseJsonFromString(str);

        if (el == null || el.isJsonObject() == false)
        {
            return false;
        }

        JsonObject obj = el.getAsJsonObject();

        if (obj.equals(origJson) == false && this.manager.loadPlacementSettings(this.placement, obj))
        {
            MessageDispatcher.success("litematica.message.info.settings_loaded_from_clipboard");
        }

        return true;
    }

    protected void clickGridSettings()
    {
        if (BaseScreen.isShiftDown())
        {
            this.placement.getGridSettings().toggleEnabled();
            this.manager.updateGridPlacementsFor(this.placement);
        }
        else
        {
            PlacementGridSettingsScreen screen = new PlacementGridSettingsScreen(this.placement);
            screen.setParent(this);
            BaseScreen.openPopupScreen(screen);
        }
    }

    protected boolean mirror(int mouseButton, GenericButton button)
    {
        boolean reverse = mouseButton == 1;
        Mirror mirror = PositionUtils.cycleMirror(this.placement.getMirror(), reverse);
        this.manager.setMirror(this.placement, mirror);
        return true;
    }

    protected boolean rotate(int mouseButton, GenericButton button)
    {
        boolean reverse = mouseButton == 1;
        Rotation rotation = PositionUtils.cycleRotation(this.placement.getRotation(), reverse);
        this.manager.setRotation(this.placement, rotation);
        return true;
    }

    protected void openMaterialList()
    {
        MaterialListPlacement materialList = new MaterialListPlacement(this.placement, true);
        DataManager.setMaterialList(materialList); // Remember the last opened material list for the hotkey to (re-) open it
        BaseScreen.openScreen(new MaterialListScreen(materialList));
    }

    protected void openPlacementList()
    {
        openScreenWithParent(new SchematicPlacementsListScreen());
    }

    protected void openVerifier()
    {
        SchematicVerifier verifier = SchematicVerifierManager.INSTANCE.getOrCreateVerifierForPlacement(this.placement);
        BaseScreen.openScreen(new SchematicVerifierScreen(verifier));
    }

    protected void renamePlacement()
    {
        this.placement.setName(this.nameTextField.getText());
    }

    protected void resetSubRegions()
    {
        this.manager.resetAllSubRegionsToSchematicValues(this.placement);
        this.initScreen();
        this.updateWidgetStates();
    }

    protected void toggleAllRegionsOff()
    {
        this.manager.setSubRegionsEnabled(this.placement, false, this.getListWidget().getFilteredDataList());
        this.initScreen();
    }

    protected void toggleAllRegionsOn()
    {
        this.manager.setSubRegionsEnabled(this.placement, true, this.getListWidget().getFilteredDataList());
        this.initScreen();
    }

    protected void toggleEnclosingBoxRendering()
    {
        this.placement.toggleRenderEnclosingBox();
        this.toggleEnclosingBoxButton.updateHoverStrings();
    }

    protected void toggleEnabled()
    {
        this.manager.toggleEnabled(this.placement);
    }

    protected void toggleIgnoreEntities()
    {
        this.manager.toggleIgnoreEntities(this.placement);
    }

    protected void updateLabels()
    {
        int regionCount = this.placement.getSubRegionCount();
        String key = "litematica.label.schematic_placement_settings.sub_regions";
        this.subRegionsLabel.setLabelStyledText(StyledText.translate(key, regionCount));
    }

    protected MultiIcon getEnclosingBoxButtonIcon()
    {
        return this.placement.shouldRenderEnclosingBox() ? LitematicaIcons.ENCLOSING_BOX_ENABLED :
                       LitematicaIcons.ENCLOSING_BOX_DISABLED;
    }

    protected List<String> getEnclosingBoxButtonHoverText()
    {
        String key;

        if (this.placement.shouldRenderEnclosingBox())
        {
            key = "litematica.hover.button.schematic_placement_settings.enclosing_box.on";
        }
        else
        {
            key = "litematica.hover.button.schematic_placement_settings.enclosing_box.off";
        }

        return ImmutableList.of(StringUtils.translate(key));
    }

    protected String getMirrorButtonLabel()
    {
        String val = fi.dy.masa.litematica.util.PositionUtils.getMirrorName(this.placement.getMirror());
        String key = "litematica.button.schematic_placement_settings.mirror_value";
        return StringUtils.translate(key, val);
    }

    protected String getRotateButtonLabel()
    {
        String val = fi.dy.masa.litematica.util.PositionUtils.getRotationNameShort(this.placement.getRotation());
        String key = "litematica.button.schematic_placement_settings.rotation_value";
        return StringUtils.translate(key, val);
    }
}
