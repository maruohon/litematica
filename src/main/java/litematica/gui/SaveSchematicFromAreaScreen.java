package litematica.gui;

import java.nio.file.Path;
import com.google.common.collect.ImmutableList;

import malilib.config.option.BooleanConfig;
import malilib.config.option.OptionListConfig;
import malilib.config.value.BaseOptionListConfigValue;
import malilib.gui.widget.BooleanEditWidget;
import malilib.gui.widget.button.BooleanConfigButton;
import malilib.gui.widget.button.OnOffButton;
import malilib.gui.widget.button.OptionListConfigButton;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.game.wrap.GameUtils;
import litematica.config.Configs;
import litematica.data.SchematicHolder;
import litematica.network.SchematicSavePacketHandler;
import litematica.scheduler.TaskScheduler;
import litematica.schematic.ISchematic;
import litematica.schematic.LitematicaSchematic;
import litematica.schematic.util.SchematicCreationUtils;
import litematica.schematic.util.SchematicSaveSettings;
import litematica.selection.AreaSelection;
import litematica.task.CreateSchematicTask;

public class SaveSchematicFromAreaScreen extends BaseSaveSchematicScreen
{
    protected final AreaSelection selection;
    protected final SchematicSaveSettings settings = new SchematicSaveSettings();
    protected final OptionListConfig<SaveSide> saveSide;
    protected final BooleanConfig customSettingsEnabled = new BooleanConfig("-", false);
    protected final BooleanConfigButton customSettingsButton;
    protected final OptionListConfigButton saveSideButton;
    protected final BooleanEditWidget saveBlocksWidget;
    protected final BooleanEditWidget saveBlockEntitiesWidget;
    protected final BooleanEditWidget saveBlockTicksWidget;
    protected final BooleanEditWidget saveEntitiesWidget;
    protected final BooleanEditWidget exposedBlocksOnlyWidget;
    protected final BooleanEditWidget fromNormalWorldWidget;
    protected final BooleanEditWidget fromSchematicWorldWidget;

    public SaveSchematicFromAreaScreen(AreaSelection selection)
    {
        super(10, 74, 20 + 170 + 2, 80, "save_schematic_from_area");

        String areaName = selection.getName();
        this.selection = selection;
        this.originalName = getFileNameFromDisplayName(areaName);
        this.fileNameTextField.setText(this.originalName);

        SaveSide side = Configs.Internal.SAVE_SIDE.getValue();
        this.saveSide = new OptionListConfig<>("-", side, SaveSide.VALUES);
        this.customSettingsEnabled.setBooleanValue(Configs.Internal.SAVE_WITH_CUSTOM_SETTINGS.getBooleanValue());

        this.customSettingsButton = new BooleanConfigButton(-1, 18, this.customSettingsEnabled, OnOffButton.OnOffStyle.TEXT_ON_OFF, "litematica.button.schematic_save.custom_settings");
        this.saveSideButton = new OptionListConfigButton(-1, 16, this.saveSide, "litematica.button.schematic_save.save_side");

        this.saveBlocksWidget         = new BooleanEditWidget(14, this.settings.saveBlocks,             "litematica.button.schematic_save.save_blocks");
        this.saveBlockEntitiesWidget  = new BooleanEditWidget(14, this.settings.saveBlockEntities,      "litematica.button.schematic_save.save_block_entities");
        this.saveBlockTicksWidget     = new BooleanEditWidget(14, this.settings.saveBlockTicks,         "litematica.button.schematic_save.save_block_ticks");
        this.saveEntitiesWidget       = new BooleanEditWidget(14, this.settings.saveEntities,           "litematica.button.schematic_save.save_entities");
        this.exposedBlocksOnlyWidget  = new BooleanEditWidget(14, this.settings.exposedBlocksOnly,      "litematica.button.schematic_save.exposed_blocks_only");
        this.fromNormalWorldWidget    = new BooleanEditWidget(14, this.settings.saveFromNormalWorld,    "litematica.button.schematic_save.from_normal_world");
        this.fromSchematicWorldWidget = new BooleanEditWidget(14, this.settings.saveFromSchematicWorld, "litematica.button.schematic_save.from_schematic_world");

        this.customSettingsEnabled.setValueChangeCallback((n, o) -> this.onCustomSettingsToggled());

        String hoverKey;

        if (GameUtils.isSinglePlayer())
        {
            hoverKey = "litematica.hover.button.schematic_save.save_side.single_player";
            this.saveSideButton.getHoverInfoFactory().removeAll();
            this.saveSideButton.setEnabled(false);
        }
        else
        {
            hoverKey = "litematica.hover.button.schematic_save.save_side.info";
        }

        this.saveSideButton.translateAndAddHoverString(hoverKey);

        this.addPreScreenCloseListener(this::saveSettings);
        this.setTitle("litematica.title.screen.save_schematic_from_area", areaName);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        //this.addWidget(this.schematicTypeDropdown);
        this.addWidget(this.saveSideButton);
        this.addWidget(this.customSettingsButton);

        if (this.customSettingsEnabled.getBooleanValue())
        {
            this.addWidget(this.saveBlocksWidget);
            this.addWidget(this.saveBlockEntitiesWidget);
            this.addWidget(this.saveBlockTicksWidget);
            this.addWidget(this.saveEntitiesWidget);
            this.addWidget(this.exposedBlocksOnlyWidget);
            this.addWidget(this.fromNormalWorldWidget);
            this.addWidget(this.fromSchematicWorldWidget);
        }
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        //this.schematicTypeDropdown.setPosition(this.fileNameTextField.getX(), this.fileNameTextField.getBottom() + 2);
        //this.saveButton.setPosition(this.schematicTypeDropdown.getRight() + 2, this.fileNameTextField.getBottom() + 2);
        this.saveButton.setPosition(this.fileNameTextField.getX(), this.fileNameTextField.getBottom() + 2);

        int x = this.schematicInfoWidget.getX();
        this.saveSideButton.setPosition(x, this.y + 10);
        this.customSettingsButton.setPosition(x, this.saveSideButton.getBottom() + 1);

        if (this.customSettingsEnabled.getBooleanValue())
        {
            int gap = 1;
            this.saveBlocksWidget.setPosition(x, this.customSettingsButton.getBottom() + gap);
            this.saveBlockEntitiesWidget.setPosition(x, this.saveBlocksWidget.getBottom() + gap);
            this.saveBlockTicksWidget.setPosition(x, this.saveBlockEntitiesWidget.getBottom() + gap);
            this.saveEntitiesWidget.setPosition(x, this.saveBlockTicksWidget.getBottom() + gap);
            this.exposedBlocksOnlyWidget.setPosition(x, this.saveEntitiesWidget.getBottom() + gap);
            this.fromNormalWorldWidget.setPosition(x, this.exposedBlocksOnlyWidget.getBottom() + gap);
            this.fromSchematicWorldWidget.setPosition(x, this.fromNormalWorldWidget.getBottom() + gap);
            this.schematicInfoWidget.setY(this.fromSchematicWorldWidget.getBottom() + 4);
            this.schematicInfoWidget.setHeight(this.getListHeight() - (this.schematicInfoWidget.getY() - this.getListY()));
        }
        else
        {
            this.schematicInfoWidget.setY(this.getListY());
            this.schematicInfoWidget.setHeight(this.getListHeight());
        }
    }

    @Override
    protected void saveSchematic()
    {
        boolean overwrite = isShiftDown();
        Path file = this.getSchematicFileIfCanSave(overwrite);

        if (file == null)
        {
            return;
        }

        //SchematicType<?> outputType = this.schematicTypeDropdown.getSelectedEntry();
        //ISchematic schematic = outputType.createSchematic(null); // TODO
        SchematicSaveSettings settings = this.customSettingsEnabled.getBooleanValue() ? this.settings : new SchematicSaveSettings();
        SaveSide side = this.saveSide.getValue();
        boolean supportsServerSideSaving = false; // TODO

        if (GameUtils.isSinglePlayer() == false &&
            (side == SaveSide.SERVER || (side == SaveSide.AUTO && supportsServerSideSaving)))
        {
            this.saveSchematicOnServer(settings, file, overwrite);
        }
        else
        {
            this.saveSchematicOnClient(settings, file, overwrite);
        }
    }

    protected void saveSchematicOnClient(SchematicSaveSettings settings, Path file, boolean overwrite)
    {
        LitematicaSchematic schematic = SchematicCreationUtils.createEmptySchematic(this.selection);
        CreateSchematicTask task = new CreateSchematicTask(schematic, this.selection,
                                                           settings.saveEntities.getBooleanValue() == false,
                                                           () -> this.writeSchematicToFile(schematic, file, overwrite));

        TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 10);
    }

    protected void saveSchematicOnServer(SchematicSaveSettings settings, Path file, boolean overwrite)
    {
        SchematicSavePacketHandler.INSTANCE.requestSchematicSaveAllAtOnce(this.selection, settings,
                                                                          sch -> this.writeSchematicToFile(sch, file, overwrite));
    }

    protected void writeSchematicToFile(ISchematic schematic, Path file, boolean overwrite)
    {
        String fileName = file.getFileName().toString();

        SchematicCreationUtils.setSchematicMetadataOnCreation(schematic, this.selection.getName());

        if (schematic.writeToFile(file, overwrite))
        {
            this.onSchematicSaved(fileName);
        }
        else
        {
            SchematicHolder.getInstance().addSchematic(schematic, false);
            MessageDispatcher.error("litematica.message.error.save_schematic.failed_to_save_from_area", fileName);
        }
    }

    protected void onSchematicSaved(String fileName)
    {
        this.onSchematicChange();
        MessageDispatcher.success("litematica.message.success.save_schematic_new", fileName);
    }

    protected void onCustomSettingsToggled()
    {
        this.reAddActiveWidgets();
        this.updateWidgetPositions();
    }

    protected void saveSettings()
    {
        Configs.Internal.SAVE_SIDE.setValue(this.saveSide.getValue());
        Configs.Internal.SAVE_WITH_CUSTOM_SETTINGS.setBooleanValue(this.customSettingsEnabled.getBooleanValue());
    }

    public static class SaveSide extends BaseOptionListConfigValue
    {
        public static final SaveSide AUTO   = new SaveSide("auto",   "litematica.name.save_side.auto");
        public static final SaveSide CLIENT = new SaveSide("client", "litematica.name.save_side.client");
        public static final SaveSide SERVER = new SaveSide("server", "litematica.name.save_side.server");

        public static final ImmutableList<SaveSide> VALUES = ImmutableList.of(AUTO, CLIENT, SERVER);

        public SaveSide(String name, String translationKey)
        {
            super(name, translationKey);
        }
    }
}
