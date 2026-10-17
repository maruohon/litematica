package litematica.gui.widget;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;

import malilib.gui.BaseScreen;
import malilib.gui.edit.DualPaneListEditScreen;
import malilib.gui.icon.DefaultIcons;
import malilib.gui.util.GuiUtils;
import malilib.gui.widget.BooleanEditWidget;
import malilib.gui.widget.CheckBoxWidget;
import malilib.gui.widget.ContainerWidget;
import malilib.gui.widget.DropDownListWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.OptionConfigButton;
import malilib.gui.widget.list.entry.BlockEntryWidget;
import malilib.gui.widget.list.entry.BlockStateEntryWidget;
import malilib.gui.widget.list.entry.EntityEntryWidget;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.world.BlockState;
import litematica.schematic.SchematicSaveSettings;
import litematica.schematic.SchematicType;
import litematica.util.value.SchematicSaveWorldSelection;

public class SchematicSaveSettingsWidget extends ContainerWidget
{
    protected final SchematicSaveSettings settings;

    protected final OptionConfigButton saveSideButton;
    protected final BooleanEditWidget saveBlocksWidget;
    protected final BooleanEditWidget saveBlockEntitiesWidget;
    protected final BooleanEditWidget saveBlockTicksWidget;
    protected final BooleanEditWidget saveEntitiesWidget;
    protected final BooleanEditWidget exposedBlocksOnlyWidget;
    protected final LabelWidget worldSelectionLabel;
    protected final DropDownListWidget<SchematicSaveWorldSelection> worldSelectionDropdown;

    protected final CheckBoxWidget obeyIgnoredBlocksCheckbox;
    protected final CheckBoxWidget obeyIgnoredBlockStatesCheckbox;
    protected final CheckBoxWidget obeyIgnoredEntitiesCheckbox;
    protected final GenericButton configureIgnoredBlocksButton;
    protected final GenericButton configureIgnoredBlockStatesButton;
    protected final GenericButton configureIgnoredEntitiesButton;

    protected boolean clientOnlyWarningsEnabled;

    public SchematicSaveSettingsWidget(int width, int height, SchematicSaveSettings settings)
    {
        super(width, height);

        this.settings = settings;

        this.saveSideButton = new OptionConfigButton(-1, 16, this.settings.saveSide, "litematica.button.schematic_save.save_side");
        this.saveBlocksWidget         = new BooleanEditWidget(14, this.settings.saveBlocks,              "litematica.button.schematic_save.save_blocks");
        this.saveBlockEntitiesWidget  = new BooleanEditWidget(14, this.settings.saveBlockEntities,       "litematica.button.schematic_save.save_block_entities");
        this.saveBlockTicksWidget     = new BooleanEditWidget(14, this.settings.saveScheduledBlockTicks, "litematica.button.schematic_save.save_block_ticks");
        this.saveEntitiesWidget       = new BooleanEditWidget(14, this.settings.saveEntities,            "litematica.button.schematic_save.save_entities");
        this.exposedBlocksOnlyWidget  = new BooleanEditWidget(14, this.settings.exposedBlocksOnly,       "litematica.button.schematic_save.exposed_blocks_only");

        this.obeyIgnoredBlocksCheckbox      = new CheckBoxWidget("litematica.checkbox.schematic_save.obey_ignored_blocks", settings.obeyIgnoredBlocks);
        this.obeyIgnoredBlockStatesCheckbox = new CheckBoxWidget("litematica.checkbox.schematic_save.obey_ignored_block_states", settings.obeyIgnoredBlockStates);
        this.obeyIgnoredEntitiesCheckbox    = new CheckBoxWidget("litematica.checkbox.schematic_save.obey_ignored_entities", settings.obeyIgnoredEntities);
        this.configureIgnoredBlocksButton = GenericButton.create(10, DefaultIcons.SMALL_ARROW_RIGHT, this::openConfigureIgnoredBlocksScreen);
        this.configureIgnoredBlockStatesButton = GenericButton.create(10, DefaultIcons.SMALL_ARROW_RIGHT, this::openConfigureIgnoredBlockStatesScreen);
        this.configureIgnoredEntitiesButton = GenericButton.create(10, DefaultIcons.SMALL_ARROW_RIGHT, this::openConfigureIgnoredEntitiesScreen);

        this.configureIgnoredBlocksButton.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFA0A0A0);
        this.configureIgnoredBlocksButton.getBorderRenderer().getHoverSettings().setBorderWidthAndColor(1, 0xFFFFFFFF);
        this.configureIgnoredBlockStatesButton.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFA0A0A0);
        this.configureIgnoredBlockStatesButton.getBorderRenderer().getHoverSettings().setBorderWidthAndColor(1, 0xFFFFFFFF);
        this.configureIgnoredEntitiesButton.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFA0A0A0);
        this.configureIgnoredEntitiesButton.getBorderRenderer().getHoverSettings().setBorderWidthAndColor(1, 0xFFFFFFFF);

        this.worldSelectionLabel = new LabelWidget("litematica.gui.label.schematic_save.from_world");
        this.worldSelectionDropdown = new DropDownListWidget<>(14, 10, SchematicSaveWorldSelection.VALUES, SchematicSaveWorldSelection::getDisplayName);
        this.worldSelectionDropdown.setSelectedEntry(settings.worldSelection.getValue());
        this.worldSelectionDropdown.setSelectionListener(this.settings.worldSelection::setValue);
        this.worldSelectionDropdown.setMaxWidth(180);
        this.worldSelectionDropdown.setHoverInfoRequiresShift(true);
        this.worldSelectionDropdown.translateAndAddHoverString("litematica.hover.schematic_save_settings.world_selection");

        String hoverKey;

        if (GameWrap.isSinglePlayer())
        {
            hoverKey = "litematica.hover.button.schematic_save.save_side.single_player";
            this.saveSideButton.getHoverInfoFactory().removeAll();
            this.saveSideButton.setEnabled(false);
        }
        else
        {
            hoverKey = "litematica.hover.button.schematic_save.save_side.info";
        }

        this.saveSideButton.setHoverInfoRequiresShift(true);
        this.saveSideButton.translateAndAddHoverString(hoverKey);
        this.saveBlockTicksWidget.setShowAsOffIfDisabled(true);

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xC0000000);
        this.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFC0C0C0);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidget(this.saveSideButton);
        this.addWidget(this.worldSelectionLabel);
        this.addWidget(this.worldSelectionDropdown);

        this.addWidget(this.saveBlocksWidget);
        this.addWidget(this.saveBlockEntitiesWidget);
        this.addWidget(this.saveBlockTicksWidget);
        this.addWidget(this.saveEntitiesWidget);
        this.addWidget(this.exposedBlocksOnlyWidget);

        this.addWidget(this.obeyIgnoredBlocksCheckbox);
        this.addWidget(this.obeyIgnoredBlockStatesCheckbox);
        this.addWidget(this.obeyIgnoredEntitiesCheckbox);

        this.addWidget(this.configureIgnoredBlocksButton);
        this.addWidget(this.configureIgnoredBlockStatesButton);
        this.addWidget(this.configureIgnoredEntitiesButton);

        // Apply to sub-widgets after their sub-widgets have been added first
        this.setClientOnlyWarnings(this.clientOnlyWarningsEnabled);
    }

    @Override
    public void updateSubWidgetPositions()
    {
        super.updateSubWidgetPositions();

        int x = this.getX() + 5;
        int y = this.getY() + 5;
        int gap = 1;

        this.saveSideButton.setPosition(x, y);
        this.worldSelectionLabel.setPosition(x, this.saveSideButton.getBottom() + 3);
        this.worldSelectionDropdown.setPosition(x, this.worldSelectionLabel.getBottom());

        this.saveBlocksWidget.setPosition(x, this.worldSelectionDropdown.getBottom() + 3);
        this.saveBlockEntitiesWidget.setPosition(x, this.saveBlocksWidget.getBottom() + gap);
        this.saveBlockTicksWidget.setPosition(x, this.saveBlockEntitiesWidget.getBottom() + gap);
        this.saveEntitiesWidget.setPosition(x, this.saveBlockTicksWidget.getBottom() + gap);
        this.exposedBlocksOnlyWidget.setPosition(x, this.saveEntitiesWidget.getBottom() + gap);

        this.obeyIgnoredBlocksCheckbox.setPosition(x, this.exposedBlocksOnlyWidget.getBottom() + 6);
        this.obeyIgnoredBlockStatesCheckbox.setPosition(x, this.obeyIgnoredBlocksCheckbox.getBottom() + gap);
        this.obeyIgnoredEntitiesCheckbox.setPosition(x, this.obeyIgnoredBlockStatesCheckbox.getBottom() + gap);

        x = this.getRight() - 14;
        this.configureIgnoredBlocksButton.setPosition(x, this.obeyIgnoredBlocksCheckbox.getY() + 2);
        this.configureIgnoredBlockStatesButton.setPosition(x, this.obeyIgnoredBlockStatesCheckbox.getY() + 2);
        this.configureIgnoredEntitiesButton.setPosition(x, this.obeyIgnoredEntitiesCheckbox.getY() + 2);
    }

    protected void onCustomSettingsToggled()
    {
        this.reAddSubWidgets();
        this.updateSubWidgetPositions();
    }

    protected void openConfigureIgnoredBlocksScreen()
    {
        List<Block> availableValues = RegistryUtils.getSortedBlockList();
        BaseScreen screen = new DualPaneListEditScreen<>(18, this.settings.ignoredBlocks, availableValues,
                                                         this.settings::setIgnoredBlocks,
                                                         BlockEntryWidget::new,
                                                         this::getBlockSearchStrings,
                                                         Comparator.comparing(RegistryUtils::getBlockIdStr));

        this.configureAndOpenEditScreen(screen, "litematica.title.screen.select_blocks");
    }

    protected void openConfigureIgnoredBlockStatesScreen()
    {
        List<BlockState> availableValues = RegistryUtils.getSortedBlockStatesList();
        BaseScreen screen = new DualPaneListEditScreen<>(18, this.settings.ignoredBlockStates, availableValues,
                                                         this.settings::setIgnoredBlockStates,
                                                         BlockStateEntryWidget::new,
                                                         this::getBlockStateSearchStrings,
                                                         Comparator.comparing(BlockState::getRegistryName));

        this.configureAndOpenEditScreen(screen, "litematica.title.screen.select_block_states");
    }

    protected void openConfigureIgnoredEntitiesScreen()
    {
        List<Class<? extends Entity>> availableValues = RegistryUtils.getSortedEntityList();
        BaseScreen screen = new DualPaneListEditScreen<>(18, this.settings.ignoredEntities, availableValues,
                                                         this.settings::setIgnoredEntities,
                                                         EntityEntryWidget::new,
                                                         this::getEntitySearchStrings,
                                                         Comparator.comparing(e -> EntityList.getKey(e).toString()));

        this.configureAndOpenEditScreen(screen, "litematica.title.screen.select_entities");
    }

    protected void configureAndOpenEditScreen(BaseScreen screen, String title)
    {
        int width = GuiUtils.getScaledWindowWidth() - 10;
        int height = GuiUtils.getScaledWindowHeight() - 12;

        screen.setScreenWidthAndHeight(width, height);
        screen.centerOnScreen();
        screen.setTitle(title);

        BaseScreen.openPopupScreenWithCurrentScreenAsParent(screen);
    }

    protected List<String> getBlockSearchStrings(Block block)
    {
        return Collections.singletonList(RegistryUtils.getBlockIdStr(block));
    }

    protected List<String> getBlockStateSearchStrings(BlockState state)
    {
        return Collections.singletonList(state.getRegistryName());
    }

    protected List<String> getEntitySearchStrings(Class<? extends Entity> clazz)
    {
        return Collections.singletonList(EntityList.getKey(clazz).toString());
    }

    public SchematicSaveSettings getSaveSettings(SchematicType schematicType)
    {
        SchematicSaveSettings effectiveSettings = this.settings.copy();

        effectiveSettings.worldSelection.setValue(this.worldSelectionDropdown.getSelectedEntry());
        effectiveSettings.schematicType = schematicType;

        return effectiveSettings;
    }

    public void setClientOnlyWarnings(boolean clientOnlyWarningsEnabled)
    {
        this.clientOnlyWarningsEnabled = clientOnlyWarningsEnabled;

        this.saveBlockTicksWidget.setEnabled(!clientOnlyWarningsEnabled);

        if (clientOnlyWarningsEnabled)
        {
            int color = 0xFFFFAA00;
            this.saveBlockEntitiesWidget.setNormalStateOnAndOffLabelColor(color);
            this.saveEntitiesWidget.setNormalStateOnAndOffLabelColor(color);
        }
        else
        {
            int color = 0xFFF0F0F0;
            this.saveBlockEntitiesWidget.setNormalStateOnLabelColor(color);
            this.saveEntitiesWidget.setNormalStateOnLabelColor(color);

            color = 0xFFD0D0D0;
            this.saveBlockEntitiesWidget.setNormalStateOffLabelColor(color);
            this.saveEntitiesWidget.setNormalStateOffLabelColor(color);
        }

        // First remove possible already added hover strings
        this.saveBlockTicksWidget.getHoverInfoFactory().removeAll();
        this.saveBlockEntitiesWidget.getHoverInfoFactory().removeAll();
        this.saveEntitiesWidget.getHoverInfoFactory().removeAll();

        if (clientOnlyWarningsEnabled)
        {
            this.saveBlockTicksWidget.translateAndAddHoverString("litematica.hover.schematic_save_settings.multiplayer_warn.block_ticks");
            this.saveBlockEntitiesWidget.translateAndAddHoverString("litematica.hover.schematic_save_settings.multiplayer_warn.block_entities");
            this.saveEntitiesWidget.translateAndAddHoverString("litematica.hover.schematic_save_settings.multiplayer_warn.entities");
        }
    }
}
