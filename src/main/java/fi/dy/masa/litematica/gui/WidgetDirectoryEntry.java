package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.gui.WidgetSchematicBrowser.DirectoryEntry;
import fi.dy.masa.litematica.util.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class WidgetDirectoryEntry extends WidgetBase
{
    private final DirectoryEntry entry;
    private final Minecraft mc;
    private final boolean isOdd;

    public WidgetDirectoryEntry(int x, int y, int width, int height, float zLevel, boolean isOdd, DirectoryEntry entry, Minecraft mc)
    {
        super(x, y, width, height, zLevel);

        this.isOdd = isOdd;
        this.entry = entry;
        this.mc = mc;
    }

    public DirectoryEntry getDirectoryEntry()
    {
        return this.entry;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean isSelected)
    {
        Widgets widget;

        switch (this.entry.getType())
        {
            case DIRECTORY:             widget = Widgets.FILE_ICON_DIR; break;
            case LITEMATICA_SCHEMATIC:  widget = Widgets.FILE_ICON_LITEMATIC; break;
            case SCHEMATICA_SCHEMATIC:  widget = Widgets.FILE_ICON_SCHEMATIC; break;
            case VANILLA_STRUCTURE:     widget = Widgets.FILE_ICON_VANILLA; break;
            default: return;
        }

        GlStateManager.color(1f, 1f, 1f);
        this.mc.getTextureManager().bindTexture(Widgets.TEXTURE);

        widget.renderAt(this.x, this.y + 1, this.zLevel);
        int iw = widget.getWidth();

        // Draw a lighter background for the hovered and the selected entry
        if (isSelected || this.isMouseOver(mouseX, mouseY))
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
        if (isSelected)
        {
            GuiLitematicaBase.drawOutline(this.x + iw + 2, this.y, this.width - iw - 2, this.height, 0xEEEEEEEE);
        }

        String name = FileUtils.getNameWithoutExtension(this.entry.getName());
        this.mc.fontRenderer.drawString(name, this.x + iw + 4, this.y + 3, 0xFFFFFFFF);
    }
}
