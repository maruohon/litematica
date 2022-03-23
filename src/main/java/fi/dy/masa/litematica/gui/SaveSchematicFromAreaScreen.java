package fi.dy.masa.litematica.gui;

import java.io.File;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.data.SimpleBooleanStorage;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.util.SchematicCreationUtils;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.task.CreateSchematicTask;

public class SaveSchematicFromAreaScreen extends BaseSaveSchematicScreen
{
    protected final AreaSelection selection;
    protected final CheckBoxWidget ignoreEntitiesCheckbox;
    protected final SimpleBooleanStorage ignoreEntitiesValue = new SimpleBooleanStorage(false);

    public SaveSchematicFromAreaScreen(AreaSelection selection)
    {
        super(10, 74, 20 + 170 + 2, 80, "save_schematic_from_area");

        String areaName = selection.getName();
        this.selection = selection;
        this.originalName = getFileNameFromDisplayName(areaName);
        this.fileNameTextField.setText(this.originalName);

        this.ignoreEntitiesCheckbox = new CheckBoxWidget("litematica.checkmark.save_schematic.ignore_entities",
                                                         this.ignoreEntitiesValue);

        this.setTitle("litematica.title.screen.save_schematic_from_area", areaName);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        //this.addWidget(this.schematicTypeDropdown);
        this.addWidget(this.ignoreEntitiesCheckbox);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        //this.schematicTypeDropdown.setPosition(this.fileNameTextField.getX(), this.fileNameTextField.getBottom() + 2);
        //this.saveButton.setPosition(this.schematicTypeDropdown.getRight() + 2, this.fileNameTextField.getBottom() + 2);
        this.saveButton.setPosition(this.fileNameTextField.getX(), this.fileNameTextField.getBottom() + 2);
        this.ignoreEntitiesCheckbox.setX(this.saveButton.getRight() + 4);
        this.ignoreEntitiesCheckbox.centerVerticallyInside(this.saveButton);
    }

    @Override
    protected void saveSchematic()
    {
        boolean isHoldingShift = BaseScreen.isShiftDown();
        File file = this.getSchematicFileIfCanSave(isHoldingShift);

        if (file == null)
        {
            return;
        }

        //SchematicType<?> outputType = this.schematicTypeDropdown.getSelectedEntry();
        //ISchematic schematic = outputType.createSchematic(null); // TODO
        LitematicaSchematic schematic = SchematicCreationUtils.createEmptySchematic(this.selection);
        CreateSchematicTask task = new CreateSchematicTask(schematic, this.selection,
                                                           this.ignoreEntitiesValue.getBooleanValue(),
                                                           () -> this.writeSchematicToFile(schematic, file, isHoldingShift));

        TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 10);
    }

    protected void writeSchematicToFile(ISchematic schematic, File file, boolean isHoldingShift)
    {
        String fileName = file.getName();

        SchematicCreationUtils.setSchematicMetadataOnCreation(schematic, this.selection.getName());

        if (schematic.writeToFile(file, isHoldingShift))
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
        this.schematicInfoWidget.clearCache();
        this.getListWidget().clearSelection();
        this.getListWidget().refreshEntries();
        MessageDispatcher.success("litematica.message.success.save_schematic_new", fileName);
    }
}
