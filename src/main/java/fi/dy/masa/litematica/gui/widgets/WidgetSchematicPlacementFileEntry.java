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
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.Messages;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSchematicPlacementFileEntry extends WidgetDirectoryEntry
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
        ButtonGeneric button = new ButtonGeneric(xRight, y, -1, true, type.getDisplayName());
        String hover = type.getHoverKey();

        if (org.apache.commons.lang3.StringUtils.isBlank(hover) == false)
        {
            button.setHoverStrings(hover);
        }

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
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX < this.buttonsStartX && super.canSelectAt(mouseX, mouseY, mouseButton);
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
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
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
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.origin", o.getX(), o.getY(), o.getZ()));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.rotation_mirror", rot, mir));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.enabled", Messages.getOnOffColored(placement.isEnabled(), true)));
        }

        int offset = 12;

        if (GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.buttonsStartX - offset, this.height))
        {
            RenderUtils.drawHoverText(mouseX, mouseY, text);
        }
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final WidgetSchematicPlacementFileEntry widget;
        private final ButtonType type;

        public ButtonListener(ButtonType type, WidgetSchematicPlacementFileEntry widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
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
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "malilib.error.file_delete_failed", file.getAbsolutePath());
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
