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
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class WidgetSchematicBrowser extends GuiLitematicaBase
{
    protected static final FileFilter DIRECTORY_FILTER = new FileFilterDirectories();
    protected static final FileFilter SCHEMATIC_FILTER = new FileFilterSchematics();
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final GuiSimpleScrollBar scrollBar = new GuiSimpleScrollBar();
    private final List<DirectoryEntry> directoryContents = new ArrayList<>();
    protected Map<File, SchematicMetadata> cachedMetadata = new HashMap<>();
    protected Set<File> checkedMetadataFiles = new HashSet<>();
    protected File currentDirectory;
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

    public WidgetSchematicBrowser(int x, int y, int width, int height)
    {
        this.posX = x;
        this.posY = y;

        this.setSize(width, height);

        this.setDirectory(DataManager.getCurrentSchematicDirectory());
        this.initBrowser();
    }

    @Override
    protected String getTitle()
    {
        return I18n.format("litematica.gui.title.schematic_browser");
    }

    @Override
    public void initGui()
    {
        
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        //super.drawScreen(mouseX, mouseY, partialTicks);

        this.drawWidget(mouseX, mouseY);
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
        this.maxVisibleBrowserEntries = (this.browserHeight - browserPaddingY * 2 - this.browserEntiresOffsetY) / this.browserEntryHeight;
    }

    protected void initBrowser()
    {
        
    }

    protected void setDirectory(File dir)
    {
        this.currentDirectory = dir;
        DataManager.setCurrentSchematicDirectory(dir);
        this.readDirectory(dir);

        if (this.currentDirectory.equals(DataManager.ROOT_SCHEMATIC_DIRECTORY))
        {
            this.browserEntiresOffsetY = 0;
        }
        else
        {
            this.browserEntiresOffsetY = this.browserEntryHeight + 2;
        }

        // Update the maxVisibleBrowserEntries field
        this.setSize(this.totalWidth, this.totalHeight);
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
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        super.keyTyped(typedChar, keyCode);

        if (keyCode == Keyboard.KEY_UP) this.scrollBar.offsetValue(-10);
        if (keyCode == Keyboard.KEY_DOWN) this.scrollBar.offsetValue(10);
        if (keyCode == Keyboard.KEY_PRIOR) this.scrollBar.offsetValue(-this.height + 10);
        if (keyCode == Keyboard.KEY_NEXT) this.scrollBar.offsetValue(this.height - 10);
        if (keyCode == Keyboard.KEY_HOME) this.scrollBar.setValue(0);
        if (keyCode == Keyboard.KEY_END) this.scrollBar.setValue(100); // TODO
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int relativeY = mouseY - this.browserEntriesStartY;

        // The up/to root directory bar exists and was clicked
        if (this.browserEntiresOffsetY > 0 && relativeY >= 0 && relativeY < this.browserEntryHeight)
        {
            this.setSelectedEntry(null, -1);

            if (mouseX >= this.browserEntriesStartX &&
                mouseX < this.browserEntriesStartX + Widgets.FILE_ICON_DIR_ROOT.getWidth())
            {
                this.setDirectory(DataManager.ROOT_SCHEMATIC_DIRECTORY.getCanonicalFile());
            }
            else if (mouseX >= this.browserEntriesStartX + Widgets.FILE_ICON_DIR_ROOT.getWidth() + 2 &&
                     mouseX < this.browserEntriesStartX + Widgets.FILE_ICON_DIR_UP.getWidth() * 2 + 2)
            {
                if (this.currentDirectory != null)
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
            }

            return;
        }
        else
        {
            relativeY -= this.browserEntiresOffsetY;
        }

        int index = relativeY / this.browserEntryHeight;

        if (relativeY >= 0 &&
            mouseX >= this.browserEntriesStartX &&
            mouseX < this.browserEntriesStartX + this.browserEntryWidth &&
            index < this.directoryContents.size())
        {
            DirectoryEntry entry = this.directoryContents.get(index);
            this.setSelectedEntry(entry, index);
        }
        else
        {
            this.setSelectedEntry(null, -1);
        }
    }

    protected void setSelectedEntry(DirectoryEntry entry, int index)
    {
        if (entry != null && entry.getType() == DirectoryEntryType.DIRECTORY)
        {
            this.selectedEntry = null;
            this.selectedEntryIndex = -1;
            this.setDirectory(new File(entry.getDirectory(), entry.getName()));
        }
        else
        {
            this.selectedEntry = entry;
            this.selectedEntryIndex = index;
        }
    }

    public void drawWidget(int mouseX, int mouseY)
    {
        this.drawDirectoryEntries(mouseX, mouseY);

        this.drawSelectedSchematicInfo(this.selectedEntry);
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

        for (int index = 0; index < this.directoryContents.size(); index++)
        {
            this.drawDirectoryEntry(index, x, y, this.browserWidth - 16, this.browserEntryHeight, mouseX, mouseY);
            y += this.browserEntryHeight;
        }
    }

    protected void drawParentOrRootDirectoryBar(int x, int y, int width, int height, int mouseX, int mouseY)
    {
        this.mc.getTextureManager().bindTexture(Widgets.TEXTURE);
        Widgets.FILE_ICON_DIR_ROOT.renderAt(x, y + 1, this.zLevel);
        Widgets.FILE_ICON_DIR_UP.renderAt(x + 14, y + 1, this.zLevel);

        drawRect(x + 28, y, x + width, y + height, 0x20FFFFFF);

        int textColor = 0xC0C0C0C0;
        String path = "";
        int maxLen = (this.browserWidth - 40) / this.fontRenderer.getStringWidth("a") - 4;

        try
        {
            File root = DataManager.ROOT_SCHEMATIC_DIRECTORY.getCanonicalFile();
            File file = this.currentDirectory.getCanonicalFile();

            while (file != null)
            {
                String name = file.getName();

                if (path.isEmpty() == false)
                {
                    path = name + " => " + path;
                }
                else
                {
                    path = name;
                }

                int len = path.length();

                if (len > maxLen)
                {
                    path = "... " + path.substring(len - maxLen, len);
                    break;
                }

                if (file.equals(root))
                {
                    break;
                }

                file = file.getParentFile();
            }
        }
        catch (IOException e)
        {
            path = "<error>";
        }

        this.fontRenderer.drawString(path, x + 31, y + 3, textColor);
    }

    protected void drawDirectoryEntry(int index, int x, int y, int width, int height, int mouseX, int mouseY)
    {
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

        this.fontRenderer.drawString(entry.getName(), x + iw + 4, y + 3, 0xFFFFFFFF);
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
