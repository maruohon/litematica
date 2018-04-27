package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.gui.interfaces.IDirectoryNavigator;
import fi.dy.masa.litematica.gui.widgets.base.WidgetBase;
import fi.dy.masa.litematica.util.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class WidgetDirectoryNavigation extends WidgetBase
{
    protected final File currentDir;
    protected final File rootDir;
    protected final Minecraft mc;
    protected final IDirectoryNavigator navigator;

    public WidgetDirectoryNavigation(int x, int y, int width, int height, float zLevel,
            File currentDir, File rootDir, Minecraft mc, IDirectoryNavigator navigator)
    {
        super(x, y, width, height, zLevel);

        this.currentDir = currentDir;
        this.rootDir = rootDir;
        this.mc = mc;
        this.navigator = navigator;
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (this.isHoveringIcon(mouseX, mouseY, 0))
        {
            this.navigator.switchToRootDirectory();
            return true;
        }
        else if (this.isHoveringIcon(mouseX, mouseY, 1))
        {
            this.navigator.switchToParentDirectory();
            return true;
        }

        return false;
    }

    protected boolean isHoveringIcon(int mouseX, int mouseY, int iconIndex)
    {
        final int iw = Icons.FILE_ICON_DIR_ROOT.getWidth();
        return mouseY >= this.y + 1 && mouseY < this.y + this.height &&
            mouseX >= this.x + iconIndex * (iw + 2) && mouseX < this.x + iconIndex * (iw + 2) + iw;
    }

    @Override
    public void render(int mouseX, int mouseY)
    {
        final int iw = Icons.FILE_ICON_DIR_ROOT.getWidth();

        // Hovering the "to root directory" widget/icon
        if (this.isHoveringIcon(mouseX, mouseY, 0))
        {
            GuiLitematicaBase.drawOutlinedBox(this.x     , this.y + 1, iw, iw, 0x20C0C0C0, 0xE0FFFFFF);
        }
        else if (this.isHoveringIcon(mouseX, mouseY, 1))
        {
            GuiLitematicaBase.drawOutlinedBox(this.x + iw + 2, this.y + 1, iw, iw, 0x20C0C0C0, 0xE0FFFFFF);
        }

        GlStateManager.color(1f, 1f, 1f);

        this.mc.getTextureManager().bindTexture(Icons.TEXTURE);
        Icons.FILE_ICON_DIR_ROOT  .renderAt(this.x         , this.y + 1, this.zLevel);
        Icons.FILE_ICON_DIR_UP    .renderAt(this.x + iw + 2, this.y + 1, this.zLevel);

        // Draw the directory path text background
        GuiLitematicaBase.drawRect(this.x + (iw + 2) * 2 + 2, this.y, this.x + this.width, this.y + this.height, 0x20FFFFFF);

        int textColor = 0xC0C0C0C0;
        int maxLen = (this.width - 40) / this.mc.fontRenderer.getStringWidth("a") - 4;
        String path = FileUtils.getJoinedTrailingPathElements(this.currentDir, DataManager.ROOT_SCHEMATIC_DIRECTORY, maxLen, " / ");
        this.mc.fontRenderer.drawString(path, this.x + iw * 2 + 9, this.y + 3, textColor);
    }
}
