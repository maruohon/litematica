package fi.dy.masa.litematica.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import fi.dy.masa.malilib.gui.icon.DefaultIcons;
import fi.dy.masa.malilib.gui.widget.BaseTextFieldWidget;
import fi.dy.masa.malilib.gui.widget.DropDownListWidget;
import fi.dy.masa.malilib.gui.widget.IconWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntryType;
import fi.dy.masa.malilib.input.Keys;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.SchematicType;

public abstract class BaseSaveSchematicScreen extends BaseSchematicBrowserScreen
{
    protected final BaseTextFieldWidget fileNameTextField;
    protected final GenericButton revertNameButton;
    protected final GenericButton saveButton;
    protected final DropDownListWidget<SchematicType<?>> schematicTypeDropdown;
    protected String originalName = "";

    protected BaseSaveSchematicScreen(int listX, int listY,
                                      int totalListMarginX, int totalListMarginY,
                                      String browserContext)
    {
        super(listX, listY, totalListMarginX, totalListMarginY, browserContext);

        this.fileNameTextField = new BaseTextFieldWidget(320, 16);
        this.fileNameTextField.setFocused(true);

        this.revertNameButton = GenericButton.create(DefaultIcons.RESET_12, this::revertName);
        this.saveButton = GenericButton.create(18, "litematica.button.save_schematic.save_schematic", this::saveSchematic);

        this.schematicTypeDropdown = new DropDownListWidget<>(18, 6, SchematicType.KNOWN_TYPES, SchematicType::getDisplayName, (e) -> new IconWidget(e.getIcon()));
        this.schematicTypeDropdown.setSelectedEntry(SchematicType.LITEMATICA);

        this.revertNameButton.translateAndAddHoverString("litematica.hover.button.save_schematic.revert_name");
        this.saveButton.translateAndAddHoverString("litematica.hover.button.save_schematic.hold_shift_to_overwrite");
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.fileNameTextField);
        this.addWidget(this.revertNameButton);
        this.addWidget(this.saveButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.fileNameTextField.setPosition(this.x + 10, this.y + 24);
        this.revertNameButton.setX(this.fileNameTextField.getRight() + 2);
        this.revertNameButton.centerVerticallyInside(this.fileNameTextField);
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        super.onSelectionChange(entry);

        if (entry != null && entry.getType() == DirectoryEntryType.FILE)
        {
            String name = FileNameUtils.getFileNameWithoutExtension(entry.getName());
            this.fileNameTextField.setText(name);
        }
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers)
    {
        if (this.fileNameTextField.isFocused() && keyCode == Keys.KEY_ENTER)
        {
            this.saveSchematic();
            return true;
        }
        else if (keyCode != Keys.KEY_ESCAPE && this.fileNameTextField.onKeyTyped(keyCode, scanCode, modifiers))
        {
            this.getListWidget().clearSelection();
            return true;
        }
        else if (keyCode == Keys.KEY_ESCAPE)
        {
            this.closeScreenOrShowParent();
            return true;
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    protected void revertName()
    {
        this.fileNameTextField.setText(this.originalName);
        this.fileNameTextField.setFocused(true);
    }

    protected abstract void saveSchematic();

    @Nullable
    protected Path getSchematicFileIfCanSave(boolean overwrite)
    {
        Path dir = this.getListWidget().getCurrentDirectory();
        String name = this.fileNameTextField.getText();
        SchematicType<?> outputType = this.schematicTypeDropdown.getSelectedEntry();

        if (outputType == null)
        {
            MessageDispatcher.error("litematica.message.error.save_schematic.no_type_selected");
            return null;
        }

        return getSchematicFileIfCanSave(dir, name, outputType, overwrite);
    }

    @Nullable
    public static Path getSchematicFileIfCanSave(Path dir, String name, SchematicType<?> outputType, boolean overwrite)
    {
        if (StringUtils.isBlank(name))
        {
            MessageDispatcher.error("litematica.message.error.save_schematic.no_file_name");
            return null;
        }

        if (FileNameUtils.doesFileNameContainIllegalCharacters(name))
        {
            MessageDispatcher.error("malilib.message.error.illegal_characters_in_file_name", name);
            return null;
        }

        if (Files.isDirectory(dir) == false && FileUtils.createDirectoriesIfMissing(dir) == false)
        {
            String key = "litematica.message.error.save_schematic.invalid_directory";
            MessageDispatcher.error(key, dir.toAbsolutePath());
            return null;
        }

        String extension = outputType.getFileNameExtension();

        if (name.endsWith(extension) == false)
        {
            name += extension;
        }

        Path file = dir.resolve(name);

        if (overwrite == false && Files.exists(file))
        {
            String key = "litematica.message.error.file_exists.hold_shift_to_override";
            MessageDispatcher.error(key, name);
            return null;
        }

        /*
        try
        {
            if (Files.exists(file) == false && FileUtils.createFile(file) == false)
            {
                String key = "litematica.message.error.schematic_save.failed_to_create_empty_file";
                MessageDispatcher.error(key, name);
                return null;
            }
        }
        catch (Exception ignore) {}
        */

        return file;
    }

    public static String getDefaultFileNameForSchematic(ISchematic schematic)
    {
        Path file = schematic.getFile();

        if (file != null)
        {
            return FileNameUtils.getFileNameWithoutExtension(file.getFileName().toString());
        }
        else
        {
            return getFileNameFromDisplayName(schematic.getMetadata().getName());
        }
    }

    public static String getFileNameFromDisplayName(String name)
    {
        if (Configs.Generic.GENERATE_LOWERCASE_NAMES.getBooleanValue())
        {
            name = FileNameUtils.generateSimpleSafeFileName(name);
        }

        return name;
    }
}
