package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.gui.interfaces.IDirectoryNavigator;
import fi.dy.masa.litematica.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.litematica.gui.widgets.base.WidgetBase;
import fi.dy.masa.litematica.util.FileUtils;
import fi.dy.masa.malilib.gui.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class WidgetDirectoryEntry extends WidgetBase
{
    protected final IDirectoryNavigator navigator;
    protected final DirectoryEntry entry;
    protected final Minecraft mc;
    protected final boolean isOdd;

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
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton)
    {
        if (this.entry.getType() == DirectoryEntryType.DIRECTORY)
        {
            this.navigator.switchToDirectory(new File(this.entry.getDirectory(), this.entry.getName()));
        }
        else
        {
            return super.onMouseClickedImpl(mouseX, mouseY, mouseButton);
        }

        return true;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        Icons icon = null;

        switch (this.entry.getType())
        {
            case DIRECTORY:             icon = Icons.FILE_ICON_DIR; break;
            //case JSON:                  icon = Icons.FILE_ICON_JSON; break;
            case LITEMATICA_SCHEMATIC:  icon = Icons.FILE_ICON_LITEMATIC; break;
            case SCHEMATICA_SCHEMATIC:  icon = Icons.FILE_ICON_SCHEMATIC; break;
            case VANILLA_STRUCTURE:     icon = Icons.FILE_ICON_VANILLA; break;
            default:
        }

        int iconWidth = icon != null ? icon.getWidth() : 0;
        int xOffset = iconWidth + 2;

        if (icon != null)
        {
            GlStateManager.color(1, 1, 1, 1);
            this.mc.getTextureManager().bindTexture(Icons.TEXTURE);
            icon.renderAt(this.x, this.y + (this.height - icon.getHeight()) / 2, this.zLevel, false, false);
        }

        // Draw a lighter background for the hovered and the selected entry
        if (selected || this.isMouseOver(mouseX, mouseY))
        {
            GuiLitematicaBase.drawRect(this.x + xOffset, this.y, this.x + this.width, this.y + this.height, 0x70FFFFFF);
        }
        else if (this.isOdd)
        {
            GuiLitematicaBase.drawRect(this.x + xOffset, this.y, this.x + this.width, this.y + this.height, 0x20FFFFFF);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            GuiLitematicaBase.drawRect(this.x + xOffset, this.y, this.x + this.width, this.y + this.height, 0x38FFFFFF);
        }

        // Draw an outline if this is the currently selected entry
        if (selected)
        {
            RenderUtils.drawOutline(this.x + xOffset, this.y, this.width - iconWidth - 2, this.height, 0xEEEEEEEE);
        }

        int yOffset = (this.height - this.mc.fontRenderer.FONT_HEIGHT) / 2 + 1;
        this.mc.fontRenderer.drawString(this.getDisplayName(), this.x + xOffset + 2, this.y + yOffset, 0xFFFFFFFF);
    }

    protected String getDisplayName()
    {
        return FileUtils.getNameWithoutExtension(this.entry.getName());
    }
}
