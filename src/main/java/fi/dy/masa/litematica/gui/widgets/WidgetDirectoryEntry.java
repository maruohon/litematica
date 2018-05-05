package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.gui.interfaces.IDirectoryNavigator;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.DirectoryEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.DirectoryEntryType;
import fi.dy.masa.litematica.gui.widgets.base.WidgetBase;
import fi.dy.masa.litematica.util.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class WidgetDirectoryEntry extends WidgetBase
{
    protected final IDirectoryNavigator navigator;
    private final DirectoryEntry entry;
    private final Minecraft mc;
    private final boolean isOdd;

    public WidgetDirectoryEntry(int x, int y, int width, int height, float zLevel, boolean isOdd,
            DirectoryEntry entry, Minecraft mc, IDirectoryNavigator navigator)
    {
        super(x, y, width, height, zLevel);

        this.isOdd = isOdd;
        this.entry = entry;
        this.mc = mc;
        this.navigator = navigator;
    }

    public DirectoryEntry getDirectoryEntry()
    {
        return this.entry;
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (this.entry.getType() == DirectoryEntryType.DIRECTORY)
        {
            this.navigator.switchToDirectory(new File(this.entry.getDirectory(), this.entry.getName()));
        }
        else
        {
            return super.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        return true;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        Icons widget;

        switch (this.entry.getType())
        {
            case DIRECTORY:             widget = Icons.FILE_ICON_DIR; break;
            case LITEMATICA_SCHEMATIC:  widget = Icons.FILE_ICON_LITEMATIC; break;
            case SCHEMATICA_SCHEMATIC:  widget = Icons.FILE_ICON_SCHEMATIC; break;
            case VANILLA_STRUCTURE:     widget = Icons.FILE_ICON_VANILLA; break;
            default: return;
        }

        GlStateManager.color(1f, 1f, 1f);
        this.mc.getTextureManager().bindTexture(Icons.TEXTURE);

        widget.renderAt(this.x, this.y + 1, this.zLevel);
        int iw = widget.getWidth();

        // Draw a lighter background for the hovered and the selected entry
        if (selected || this.isMouseOver(mouseX, mouseY))
        {
            GuiLitematicaBase.drawRect(this.x + iw + 2, this.y, this.x + this.width, this.y + this.height, 0x70FFFFFF);
        }
        else if (this.isOdd)
        {
            GuiLitematicaBase.drawRect(this.x + iw + 2, this.y, this.x + this.width, this.y + this.height, 0x20FFFFFF);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            GuiLitematicaBase.drawRect(this.x + iw + 2, this.y, this.x + this.width, this.y + this.height, 0x38FFFFFF);
        }

        // Draw an outline if this is the currently selected entry
        if (selected)
        {
            GuiLitematicaBase.drawOutline(this.x + iw + 2, this.y, this.width - iw - 2, this.height, 0xEEEEEEEE);
        }

        String name = FileUtils.getNameWithoutExtension(this.entry.getName());
        this.mc.fontRenderer.drawString(name, this.x + iw + 4, this.y + 3, 0xFFFFFFFF);
    }
}
