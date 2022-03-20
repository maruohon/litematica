package fi.dy.masa.litematica.gui;

import java.io.File;
import javax.annotation.Nullable;
import org.lwjgl.input.Keyboard;
import fi.dy.masa.malilib.gui.icon.DefaultIcons;
import fi.dy.masa.malilib.gui.icon.MultiIcon;
import fi.dy.masa.malilib.gui.icon.MultiIconProvider;
import fi.dy.masa.malilib.gui.widget.BaseTextFieldWidget;
import fi.dy.masa.malilib.gui.widget.DropDownListWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntryType;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.util.LitematicaIcons;
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

        this.schematicTypeDropdown = new DropDownListWidget<>(-1, 18, 110, 6, SchematicType.KNOWN_TYPES, SchematicType::getDisplayName);
        this.schematicTypeDropdown.setIconProvider(new SchematicIconProvider());
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
        if (this.fileNameTextField.isFocused() && keyCode == Keyboard.KEY_RETURN)
        {
            this.saveSchematic();
            return true;
        }
        else if (this.fileNameTextField.onKeyTyped(keyCode, scanCode, modifiers))
        {
            this.getListWidget().clearSelection();
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

    public static String getDefaultFileNameForSchematic(ISchematic schematic)
    {
        File file = schematic.getFile();
        String name;

        if (file != null)
        {
            name = FileNameUtils.getFileNameWithoutExtension(file.getName());
        }
        else
        {
            name = schematic.getMetadata().getName();

            if (Configs.Generic.GENERATE_LOWERCASE_NAMES.getBooleanValue())
            {
                name = FileNameUtils.generateSimpleSafeFileName(name);
            }
        }

        return name;
    }

    public static class SchematicIconProvider implements MultiIconProvider<SchematicType<?>>
    {
        @Override
        public int getExpectedWidth()
        {
            return LitematicaIcons.FILE_ICON_LITEMATIC.getWidth();
        }

        @Override
        public MultiIcon getIconFor(SchematicType<?> entry)
        {
            return entry.getIcon();
        }
    }
}
