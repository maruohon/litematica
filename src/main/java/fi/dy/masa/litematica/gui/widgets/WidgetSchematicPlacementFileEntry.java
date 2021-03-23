package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementUnloaded;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.widget.button.BaseButton;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.ButtonActionListener;
import fi.dy.masa.malilib.overlay.message.MessageType;
import fi.dy.masa.malilib.gui.widget.list.entry.DirectoryEntryWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntryType;
import fi.dy.masa.malilib.overlay.message.MessageUtils;
import fi.dy.masa.malilib.overlay.message.MessageHelpers;
import fi.dy.masa.malilib.render.TextRenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSchematicPlacementFileEntry extends DirectoryEntryWidget
{
    private final WidgetSchematicPlacementBrowser browser;
    private final FileType fileType;
    @Nullable private SchematicPlacementUnloaded placement;
    private int buttonsStartX;

    public WidgetSchematicPlacementFileEntry(int x, int y, int width, int height, boolean isOdd,
            DirectoryEntry entry, int listIndex, WidgetSchematicPlacementBrowser parent)
    {
        super(x, y, width, height, isOdd, entry, listIndex, parent, parent.getIconProvider());

        this.fileType = FileType.fromFileName(this.entry.getFullPath());
        this.browser = parent;

        int posX = x + width - 2;
        int posY = y + 1;

        // Note: These are placed from right to left

        if (entry.getType() == DirectoryEntryType.FILE && this.fileType == FileType.JSON)
        {
            posX = this.createButton(posX, posY, ButtonListener.ButtonType.REMOVE);
            posX = this.createButton(posX, posY, ButtonListener.ButtonType.LOAD);
        }

        this.buttonsStartX = posX;
    }

    private int createButton(int xRight, int y, ButtonListener.ButtonType type)
    {
        GenericButton button = new GenericButton(xRight, y, -1, true, type.getDisplayName());
        button.translateAndAddHoverString(type.getHoverKey());

        this.addButton(button, new ButtonListener(type, this));

        return button.getX() - 2;
    }

    @Nullable
    protected SchematicPlacementUnloaded getPlacement()
    {
        if (this.placement == null)
        {
            this.placement = this.browser.getOrLoadPlacement(this.getDirectoryEntry().getFullPath());
        }

        return this.placement;
    }

    @Override
    public boolean canHoverAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX < this.buttonsStartX && super.canHoverAt(mouseX, mouseY, mouseButton);
    }

    @Override
    protected String getDisplayName()
    {
        if (this.entry.getType() == DirectoryEntryType.FILE && this.fileType == FileType.JSON)
        {
            SchematicPlacementUnloaded placement = this.getPlacement();
            String prefix = this.entry.getDisplayNamePrefix();
            return placement != null ? (prefix != null ? prefix + placement.getName() : placement.getName()) : "<error>";
        }

        return super.getDisplayName();
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId)
    {
        List<String> text = new ArrayList<>();
        SchematicPlacementUnloaded placement = this.getPlacement();

        if (placement != null)
        {
            BlockPos o = placement.getOrigin();
            String rot = PositionUtils.getRotationNameShort(placement.getRotation());
            String mir = PositionUtils.getMirrorName(placement.getMirror());
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.name", placement.getName()));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_file", placement.getSchematicFile().getName()));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.placement_file", this.entry.getName()));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.origin", o.getX(), o.getY(), o.getZ()));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.rotation_mirror", rot, mir));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.enabled", MessageHelpers.getOnOffColored(placement.isEnabled(), true)));
        }

        int offset = 12;

        if (BaseScreen.isMouseOver(mouseX, mouseY, this.getX(), this.getY(), this.buttonsStartX - offset, this.getHeight()))
        {
            TextRenderUtils.renderHoverText(mouseX, mouseY, this.getZLevel() + 1, text);
        }
    }

    private static class ButtonListener implements ButtonActionListener
    {
        private final WidgetSchematicPlacementFileEntry widget;
        private final ButtonType type;

        public ButtonListener(ButtonType type, WidgetSchematicPlacementFileEntry widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            File file = this.widget.getDirectoryEntry().getFullPath();

            if (this.type == ButtonType.LOAD)
            {
                DataManager.getSchematicPlacementManager().loadPlacementFromFile(file);
            }
            else if (this.type == ButtonType.REMOVE)
            {
                try
                {
                    if (file.delete())
                    {
                        this.widget.browser.refreshEntries();
                    }
                }
                catch (Exception e)
                {
                    MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "malilib.error.file_delete_failed", file.getAbsolutePath());
                }
            }
        }

        public enum ButtonType
        {
            REMOVE  ("litematica.gui.button.schematic_placements.remove", "litematica.gui.hover.schematic_placement.button.remove"),
            LOAD    ("litematica.gui.button.schematic_placements.load",   "litematica.gui.hover.schematic_placement.button.load");

            private final String translationKey;
            private final String hoverKey;

            private ButtonType(String translationKey, String hoverKey)
            {
                this.translationKey = translationKey;
                this.hoverKey = hoverKey;
            }

            public String getHoverKey()
            {
                return this.hoverKey;
            }

            public String getDisplayName()
            {
                return StringUtils.translate(this.translationKey);
            }
        }
    }
}
