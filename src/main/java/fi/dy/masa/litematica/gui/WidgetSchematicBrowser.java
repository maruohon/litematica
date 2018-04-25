package fi.dy.masa.litematica.gui;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.lwjgl.input.Keyboard;
import com.mumfrey.liteloader.client.gui.GuiSimpleScrollBar;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.util.FileUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class WidgetSchematicBrowser extends GuiLitematicaBase
{
    protected static final FileFilter DIRECTORY_FILTER = new FileFilterDirectories();
    protected static final FileFilter SCHEMATIC_FILTER = new FileFilterSchematics();
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    protected final List<DirectoryEntry> directoryContents = new ArrayList<>();
    protected Map<File, SchematicMetadata> cachedMetadata = new HashMap<>();
    protected Set<File> checkedMetadataFiles = new HashSet<>();
    protected File currentDirectory;
    protected final GuiSimpleScrollBar scrollBar = new GuiSimpleScrollBar();
    protected final int posX;
    protected final int posY;
    protected int totalWidth;
    protected int totalHeight;
    protected int infoWidth;
    protected int infoHeight;
    protected int browserWidth;
    protected int browserHeight;
    protected int entryHeight;
    protected int browserEntriesStartX;
    protected int browserEntriesStartY;
    protected int browserEntiresOffsetY;
    protected int browserEntryWidth;
    protected int browserEntryHeight;
    protected int maxVisibleBrowserEntries;
    protected int selectedEntryIndex = -1;
    @Nullable
    protected DirectoryEntry selectedEntry;
    @Nullable
    protected final IStringConsumer selectionListener;

    public WidgetSchematicBrowser(int x, int y, int width, int height, @Nullable IStringConsumer selectionListener)
    {
        this.posX = x;
        this.posY = y;
        this.selectionListener = selectionListener;

        this.setSize(width, height);

        this.setDirectory(DataManager.getCurrentSchematicDirectory());
    }

    @Override
    protected String getTitle()
    {
        return I18n.format("litematica.gui.title.schematic_browser");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.updateBrowserMaxVisibleEntries(4);
    }

    @Override
    public void drawContents(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDirectoryEntries(mouseX, mouseY);

        int scrollbarHeight = this.browserHeight - 8;
        int totalHeight = Math.max(this.directoryContents.size() * this.browserEntryHeight, scrollbarHeight);
        this.scrollBar.drawScrollBar(mouseX, mouseY, partialTicks,
                this.posX + this.browserWidth - 9, this.browserEntriesStartY, 8, scrollbarHeight, totalHeight);

        this.drawSelectedSchematicInfo(this.selectedEntry);
    }

    public void setSize(int width, int height)
    {
        int browserPaddingX = 3;
        int browserPaddingY = 4;
        this.totalWidth = width;
        this.totalHeight = height;
        this.infoWidth = 160;
        this.infoHeight = 280;
        this.browserWidth = width - this.infoWidth - 10;
        this.browserHeight = height;
        this.browserEntriesStartX = this.posX + browserPaddingX;
        this.browserEntriesStartY = this.posY + browserPaddingY;
        this.browserEntryWidth = this.browserWidth - 16;
        this.browserEntryHeight = 14;

        this.updateBrowserMaxVisibleEntries(browserPaddingY);
    }

    protected void updateBrowserMaxVisibleEntries(int browserPaddingY)
    {
        this.maxVisibleBrowserEntries = (this.browserHeight - browserPaddingY - this.browserEntiresOffsetY) / this.browserEntryHeight;
        this.scrollBar.setMaxValue(this.directoryContents.size() - this.maxVisibleBrowserEntries);
    }

    public void refreshEntries()
    {
        this.readDirectory(this.currentDirectory);
    }

    protected void setDirectory(File dir)
    {
        this.setSelectedEntry(null, -1);

        this.currentDirectory = FileUtils.getCanonicalFileIfPossible(dir);
        DataManager.setCurrentSchematicDirectory(dir);
        this.readDirectory(dir);

        if (this.currentDirectoryIsRoot())
        {
            this.browserEntiresOffsetY = 0;
        }
        else
        {
            this.browserEntiresOffsetY = this.browserEntryHeight + 2;
        }

        this.updateBrowserMaxVisibleEntries(4);
    }

    protected void readDirectory(File dir)
    {
        this.directoryContents.clear();

        if (dir.isDirectory() && dir.canRead())
        {
            List<DirectoryEntry> list = new ArrayList<>();

            // Show directories at the top
            for (File file : dir.listFiles(DIRECTORY_FILTER))
            {
                list.add(new DirectoryEntry(DirectoryEntryType.fromFile(file), dir, file.getName()));
            }

            Collections.sort(list);
            this.directoryContents.addAll(list);

            list.clear();

            for (File file : dir.listFiles(SCHEMATIC_FILTER))
            {
                list.add(new DirectoryEntry(DirectoryEntryType.fromFile(file), dir, file.getName()));
            }

            Collections.sort(list);
            this.directoryContents.addAll(list);
        }

        this.scrollBar.setMaxValue(this.directoryContents.size() - this.maxVisibleBrowserEntries);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        super.keyTyped(typedChar, keyCode);

        if (keyCode == Keyboard.KEY_UP) this.offsetSelectionOrScrollbar(-1);
        else if (keyCode == Keyboard.KEY_DOWN) this.offsetSelectionOrScrollbar(1);
        else if (keyCode == Keyboard.KEY_PRIOR) this.offsetSelectionOrScrollbar(-this.maxVisibleBrowserEntries / 2);
        else if (keyCode == Keyboard.KEY_NEXT) this.offsetSelectionOrScrollbar(this.maxVisibleBrowserEntries / 2);
        else if (keyCode == Keyboard.KEY_HOME) this.offsetSelectionOrScrollbar(-this.scrollBar.getMaxValue());
        else if (keyCode == Keyboard.KEY_END) this.offsetSelectionOrScrollbar(this.scrollBar.getMaxValue());
        else if ((keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_LEFT) && this.currentDirectoryIsRoot() == false)
        {
            this.switchToParentDirectory();
        }
        else if (keyCode == Keyboard.KEY_RIGHT && this.selectedEntry != null && this.selectedEntry.getType() == DirectoryEntryType.DIRECTORY)
        {
            this.setDirectory(new File(this.selectedEntry.getDirectory(), this.selectedEntry.getName()));
        }
    }

    protected void offsetSelectionOrScrollbar(int amount)
    {
        if (this.selectedEntryIndex >= 0)
        {
            int max = Math.max(this.directoryContents.size() - 1, 0);
            int index = Math.min(Math.max(this.selectedEntryIndex + amount, 0), max);

            if (index != this.selectedEntryIndex)
            {
                if (index < this.scrollBar.getValue() || index >= this.scrollBar.getValue() + this.maxVisibleBrowserEntries)
                {
                    this.scrollBar.offsetValue(index - this.selectedEntryIndex);
                }

                DirectoryEntry entry = this.directoryContents.get(index);
                this.setSelectedEntry(entry, index);
            }
        }
        else
        {
            //this.scrollBar.offsetValue(amount);

            int index = this.scrollBar.getValue();

            if (index >= 0 && index < this.directoryContents.size())
            {
                this.setSelectedEntry(this.directoryContents.get(index), index);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (mouseButton == 0)
        {
            if (this.scrollBar.wasMouseOver())
            {
                this.scrollBar.setDragging(true);
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        int relativeY = mouseY - this.browserEntriesStartY;

        // FIXME needs clean-up
        // The up/to root directory bar exists and was clicked
        if (this.browserEntiresOffsetY > 0 && relativeY >= 0 && relativeY < this.browserEntryHeight)
        {
            this.setSelectedEntry(null, -1);

            if (mouseX >= this.browserEntriesStartX &&
                mouseX < this.browserEntriesStartX + Widgets.FILE_ICON_DIR_ROOT.getWidth())
            {
                this.setDirectory(DataManager.ROOT_SCHEMATIC_DIRECTORY);
            }
            else if (mouseX >= this.browserEntriesStartX + Widgets.FILE_ICON_DIR_ROOT.getWidth() + 2 &&
                     mouseX < this.browserEntriesStartX + Widgets.FILE_ICON_DIR_UP.getWidth() * 2 + 2)
            {
                this.switchToParentDirectory();
            }

            return;
        }
        else
        {
            relativeY -= this.browserEntiresOffsetY;
        }

        int index = relativeY / this.browserEntryHeight + this.scrollBar.getValue();

        if (relativeY >= 0 && relativeY < this.maxVisibleBrowserEntries * this.browserEntryHeight &&
            mouseX >= this.browserEntriesStartX &&
            mouseX < this.browserEntriesStartX + this.browserEntryWidth)
        {
            if (index < Math.min(this.directoryContents.size(), this.maxVisibleBrowserEntries + this.scrollBar.getValue()))
            {
                DirectoryEntry entry = this.directoryContents.get(index);
                this.setSelectedEntry(entry, index);

                if (entry != null && entry.getType() == DirectoryEntryType.DIRECTORY)
                {
                    this.setDirectory(new File(entry.getDirectory(), entry.getName()));
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button)
    {
        if (button == 0)
        {
            this.scrollBar.setDragging(false);
        }

        super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void mouseWheelScrolled(int mouseX, int mouseY, int mouseWheelDelta)
    {
        if (mouseX >= this.posX && mouseX <= this.posX + this.browserWidth &&
            mouseY >= this.posY && mouseY <= this.posY + this.browserHeight)
        {
            this.scrollBar.offsetValue(-mouseWheelDelta / 120 * 3);
        }
    }

    protected boolean currentDirectoryIsRoot()
    {
        return this.currentDirectory.equals(DataManager.ROOT_SCHEMATIC_DIRECTORY);
    }

    protected void switchToParentDirectory()
    {
        File parent = this.currentDirectory.getParentFile();

        if (parent != null)
        {
            this.setDirectory(parent);
        }
        else
        {
            this.setDirectory(DataManager.ROOT_SCHEMATIC_DIRECTORY);
        }
    }

    @Nullable
    public DirectoryEntry getSelectedEntry()
    {
        return this.selectedEntry;
    }

    public File getCurrentDirectory()
    {
        return this.currentDirectory;
    }

    public void setSelectedEntry(@Nullable DirectoryEntry entry, int index)
    {
        this.selectedEntry = entry;
        this.selectedEntryIndex = index;

        if (entry != null && this.selectionListener != null)
        {
            this.selectionListener.setString(entry.getName());
        }
    }

    protected void drawDirectoryEntries(int mouseX, int mouseY)
    {
        int x = this.posX + 2;
        int y = this.posY + 4;

        GuiLitematicaBase.drawOutlinedBox(this.posX, this.posY, this.browserWidth, this.browserHeight, 0xB0000000, COLOR_HORIZONTAL_BAR);

        if (this.browserEntiresOffsetY > 0)
        {
            this.drawParentOrRootDirectoryBar(x, y, this.browserWidth - 16, this.browserEntryHeight, mouseX, mouseY);
            y += this.browserEntiresOffsetY;
        }

        final int entries = this.directoryContents.size();
        final int max = Math.min(this.maxVisibleBrowserEntries, entries);

        for (int i = 0, index = this.scrollBar.getValue(); i < max && index < entries; i++, index++)
        {
            this.drawDirectoryEntry(index, x, y, this.browserWidth - 16, this.browserEntryHeight, mouseX, mouseY);
            y += this.browserEntryHeight;
        }
    }

    protected void drawParentOrRootDirectoryBar(int x, int y, int width, int height, int mouseX, int mouseY)
    {
        int relativeY = mouseY - this.browserEntriesStartY;

        // FIXME needs clean-up
        // The up/to root directory bar exists and one of the buttons is being hovered
        if (this.browserEntiresOffsetY > 0 && relativeY >= 0 && relativeY < this.browserEntryHeight)
        {
            if (mouseX >= this.browserEntriesStartX &&
                mouseX < this.browserEntriesStartX + Widgets.FILE_ICON_DIR_ROOT.getWidth())
            {
                drawOutlinedBox(x, y + 1, 12, 12, 0x20C0C0C0, 0xE0FFFFFF);
            }
            else if (mouseX >= this.browserEntriesStartX + Widgets.FILE_ICON_DIR_ROOT.getWidth() + 2 &&
                     mouseX < this.browserEntriesStartX + Widgets.FILE_ICON_DIR_UP.getWidth() * 2 + 2)
            {
                drawOutlinedBox(x + 14, y + 1, 12, 12, 0x20C0C0C0, 0xE0FFFFFF);
            }
        }

        GlStateManager.color(1f, 1f, 1f);

        this.mc.getTextureManager().bindTexture(Widgets.TEXTURE);
        Widgets.FILE_ICON_DIR_ROOT.renderAt(x, y + 1, this.zLevel);
        Widgets.FILE_ICON_DIR_UP.renderAt(x + 14, y + 1, this.zLevel);

        // Draw the directory path text background
        drawRect(x + 28, y, x + width, y + height, 0x20FFFFFF);

        int textColor = 0xC0C0C0C0;
        int maxLen = (this.browserWidth - 40) / this.fontRenderer.getStringWidth("a") - 4;
        String path = FileUtils.getJoinedTrailingPathElements(this.currentDirectory, DataManager.ROOT_SCHEMATIC_DIRECTORY, maxLen);
        this.fontRenderer.drawString(path, x + 31, y + 3, textColor);
    }

    protected void drawDirectoryEntry(int index, int x, int y, int width, int height, int mouseX, int mouseY)
    {
        GlStateManager.color(1f, 1f, 1f);

        DirectoryEntry entry = this.directoryContents.get(index);
        this.mc.getTextureManager().bindTexture(Widgets.TEXTURE);
        Widgets widget = Widgets.FILE_ICON_DIR;

        switch (entry.getType())
        {
            case DIRECTORY:             widget = Widgets.FILE_ICON_DIR; break;
            case LITEMATICA_SCHEMATIC:  widget = Widgets.FILE_ICON_LITEMATIC; break;
            case SCHEMATICA_SCHEMATIC:  widget = Widgets.FILE_ICON_SCHEMATIC; break;
            case VANILLA_STRUCTURE:     widget = Widgets.FILE_ICON_VANILLA; break;
            default:
        }

        widget.renderAt(x, y + 1, this.zLevel);
        int iw = widget.getWidth();

        // Draw a lighter background for the hovered and the selected entry
        if (index == this.selectedEntryIndex || (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height))
        {
            drawRect(x + iw + 2, y, x + width, y + height, 0x70FFFFFF);
        }
        else if ((index % 2) != 0)
        {
            drawRect(x + iw + 2, y, x + width, y + height, 0x20FFFFFF);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            drawRect(x + iw + 2, y, x + width, y + height, 0x38FFFFFF);
        }

        if (index == this.selectedEntryIndex)
        {
            GuiLitematicaBase.drawOutline(x + iw + 2, y, width - iw - 2, height, 0xEEEEEEEE);
        }

        String name = FileUtils.getNameWithoutExtension(entry.getName());
        this.fontRenderer.drawString(name, x + iw + 4, y + 3, 0xFFFFFFFF);
    }

    protected void drawSelectedSchematicInfo(@Nullable DirectoryEntry entry)
    {
        int x = this.posX + this.totalWidth - this.infoWidth;
        int y = this.posY;

        GuiLitematicaBase.drawOutlinedBox(x, y, this.infoWidth, this.infoHeight, 0xA0000000, COLOR_HORIZONTAL_BAR);

        if (entry == null)
        {
            return;
        }

        SchematicMetadata meta = this.getSchematicMetadata(entry);

        if (meta != null)
        {
            x += 3;
            y += 3;
            int textColor = 0xC0C0C0C0;

            String str = I18n.format("litematica.gui.label.schematic_info.author", meta.getAuthor());
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            String strDate = DATE_FORMAT.format(new Date(meta.getTimeCreated()));
            str = I18n.format("litematica.gui.label.schematic_info.time_created", strDate);
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            if (meta.hasBeenModified())
            {
                strDate = DATE_FORMAT.format(new Date(meta.getTimeModified()));
                str = I18n.format("litematica.gui.label.schematic_info.time_modified", strDate);
                this.fontRenderer.drawString(str, x, y, textColor);
                y += 12;
            }

            str = I18n.format("litematica.gui.label.schematic_info.region_count", meta.getRegionCount());
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            str = I18n.format("litematica.gui.label.schematic_info.total_volume", meta.getTotalVolume());
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            str = I18n.format("litematica.gui.label.schematic_info.total_blocks", meta.getTotalBlocks());
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            str = I18n.format("litematica.gui.label.schematic_info.enclosing_size");
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;
            BlockPos p = meta.getEnclosingSize();
            String tmp = String.format("%d x %d x %d", p.getX(), p.getY(), p.getZ());
            this.fontRenderer.drawString(tmp, x, y, textColor);
            y += 12;

            str = I18n.format("litematica.gui.label.description");
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;
        }
    }

    public void clearSchematicMetadataCache()
    {
        this.cachedMetadata.clear();
        this.checkedMetadataFiles.clear();
    }

    @Nullable
    protected SchematicMetadata getSchematicMetadata(DirectoryEntry entry)
    {
        File file = new File(entry.getDirectory(), entry.getName() + "_meta");
        SchematicMetadata meta = this.cachedMetadata.get(file);

        if (meta == null && this.checkedMetadataFiles.contains(file) == false)
        {
            this.checkedMetadataFiles.add(file);
            meta = SchematicMetadata.fromFile(file);

            if (meta != null)
            {
                this.cachedMetadata.put(file, meta);
            }
        }

        return meta;
    }

    public static class DirectoryEntry implements Comparable<DirectoryEntry>
    {
        private final DirectoryEntryType type;
        private final File dir;
        private final String name;

        public DirectoryEntry(DirectoryEntryType type, File dir, String name)
        {
            this.type = type;
            this.dir = dir;
            this.name = name;
        }

        public DirectoryEntryType getType()
        {
            return this.type;
        }

        public File getDirectory()
        {
            return this.dir;
        }

        public String getName()
        {
            return this.name;
        }

        @Override
        public int compareTo(DirectoryEntry other)
        {
            return this.name.toLowerCase(Locale.US).compareTo(other.getName().toLowerCase(Locale.US));
        }
    }

    public enum DirectoryEntryType
    {
        INVALID,
        DIRECTORY,
        LITEMATICA_SCHEMATIC,
        SCHEMATICA_SCHEMATIC,
        VANILLA_STRUCTURE;

        public static DirectoryEntryType fromFile(File file)
        {
            if (file.isDirectory())
            {
                return DirectoryEntryType.DIRECTORY;
            }
            else
            {
                String name = file.getName();

                if (name.endsWith(".litematic"))
                {
                    return DirectoryEntryType.LITEMATICA_SCHEMATIC;
                }
                else if (name.endsWith(".schematic"))
                {
                    return DirectoryEntryType.SCHEMATICA_SCHEMATIC;
                }
                else if (name.endsWith(".nbt"))
                {
                    return DirectoryEntryType.VANILLA_STRUCTURE;
                }

                return DirectoryEntryType.INVALID;
            }
        }
    }

    public static class FileFilterDirectories implements FileFilter
    {
        @Override
        public boolean accept(File pathName)
        {
            return pathName.isDirectory();
        }
    }

    public static class FileFilterSchematics implements FileFilter
    {
        @Override
        public boolean accept(File pathName)
        {
            String name = pathName.getName();
            return  name.endsWith(".litematic") ||
                    name.endsWith(".schematic") ||
                    name.endsWith(".nbt");
        }
    }
}
