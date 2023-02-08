package litematica.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;

import malilib.gui.widget.BaseTextFieldWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.BaseFileBrowserWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.FileNameUtils;
import litematica.Reference;
import litematica.schematic.placement.SchematicPlacement;

public class SaveSchematicPlacementScreen extends BaseSavedSchematicPlacementsBrowserScreen
{
    protected final BaseTextFieldWidget nameTextField;
    protected final GenericButton saveButton;
    protected final SchematicPlacement placement;

    public SaveSchematicPlacementScreen(SchematicPlacement placement)
    {
        super(10, 50, 20 + 2 + 170, 102);

        this.placement = placement;

        this.nameTextField = new BaseTextFieldWidget(240, 16, placement.getSuggestedSaveFileName());
        this.saveButton = GenericButton.create(16, "litematica.button.save_placement.save", this::savePlacement);
        this.saveButton.translateAndAddHoverString("litematica.hover.button.save_file.hold_shift_to_overwrite");

        this.setTitle("litematica.title.screen.save_placement", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.nameTextField);
        this.addWidget(this.saveButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.nameTextField.setPosition(this.x + 10, this.y + 30);

        this.saveButton.setX(this.nameTextField.getRight() + 4);
        this.saveButton.centerVerticallyInside(this.nameTextField);
    }

    @Override
    protected BaseFileBrowserWidget createListWidget()
    {
        BaseFileBrowserWidget listWidget = super.createListWidget();
        //listWidget.setDataListEntryWidgetFactory((d, cd) -> new DirectoryEntryWidget(d, cd, listWidget, null));
        return listWidget;
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        super.onSelectionChange(entry);

        if (entry != null)
        {
            this.nameTextField.setText(FileNameUtils.getFileNameWithoutExtension(entry.getName()));
        }
    }

    protected boolean savePlacement()
    {
        boolean overwrite = isShiftDown();
        Path file = null;

        try
        {
            String name = this.nameTextField.getText().replaceAll("/", "-");
            name = FileNameUtils.addExtensionIfNotExists(name, ".json");
            file = this.getListWidget().getCurrentDirectory().resolve(name);

            if (overwrite == false && Files.exists(file))
            {
                MessageDispatcher.error("litematica.message.error.save_placement.write_to_file_failed.exists",
                                        file.toAbsolutePath().toString());
                return false;
            }

            boolean success = this.placement.saveToFile(file);

            if (success)
            {
                this.placementInfoWidget.clearInfoCache();
                this.getListWidget().refreshEntries();
            }

            return success;
        }
        catch (Exception e)
        {
            String name = file != null ? file.toAbsolutePath().toString() : "<error>";
            MessageDispatcher.error("litematica.message.error.save_placement.write_to_file_failed.exception",
                                    name, e.getMessage());
        }

        return false;
    }
}
