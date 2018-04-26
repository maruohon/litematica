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
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class WidgetSchematicBrowser extends GuiLitematicaBase implements IDirectoryNavigator
{
    protected static final FileFilter DIRECTORY_FILTER = new FileFilterDirectories();
    protected static final FileFilter SCHEMATIC_FILTER = new FileFilterSchematics();
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    protected final List<DirectoryEntry> directoryContents = new ArrayList<>();
    protected final List<WidgetDirectoryEntry> directoryEntryWidgets = new ArrayList<>();
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
    protected int browserEntriesOffsetY;
    protected int browserEntryWidth;
    protected int browserEntryHeight;
    protected int browserPaddingX;
    protected int browserPaddingY;
    protected int maxVisibleBrowserEntries;
    protected int selectedEntryIndex = -1;
    protected int lastScrollbarPosition = -1;
    @Nullable
    protected DirectoryEntry selectedEntry;
    @Nullable
    protected final IStringConsumer selectionListener;
    @Nullable
    protected WidgetDirectoryNavigation directoryNavigationWidget;

    public WidgetSchematicBrowser(int x, int y, int width, int height, @Nullable IStringConsumer selectionListener)
    {
        this.mc = Minecraft.getMinecraft();
        this.posX = x;
        this.posY = y;
        this.selectionListener = selectionListener;
        this.currentDirectory = DataManager.getCurrentSchematicDirectory();

        this.setSize(width, height);
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

        this.updateBrowserMaxVisibleEntries();
        this.refreshEntries();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        super.keyTyped(typedChar, keyCode);

        if (keyCode == Keyboard.KEY_UP)         this.offsetSelectionOrScrollbar(-1, true);
        else if (keyCode == Keyboard.KEY_DOWN)  this.offsetSelectionOrScrollbar( 1, true);
        else if (keyCode == Keyboard.KEY_PRIOR) this.offsetSelectionOrScrollbar(-this.maxVisibleBrowserEntries / 2, false);
        else if (keyCode == Keyboard.KEY_NEXT)  this.offsetSelectionOrScrollbar( this.maxVisibleBrowserEntries / 2, false);
        else if (keyCode == Keyboard.KEY_HOME)  this.offsetSelectionOrScrollbar(-this.scrollBar.getMaxValue(), false);
        else if (keyCode == Keyboard.KEY_END)   this.offsetSelectionOrScrollbar( this.scrollBar.getMaxValue(), false);
        else if ((keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_LEFT) && this.currentDirectoryIsRoot() == false)
        {
            this.switchToParentDirectory();
        }
        else if ((keyCode == Keyboard.KEY_RIGHT || keyCode == Keyboard.KEY_RETURN) &&
                  this.selectedEntry != null && this.selectedEntry.getType() == DirectoryEntryType.DIRECTORY)
        {
            this.switchToDirectory(new File(this.selectedEntry.getDirectory(), this.selectedEntry.getName()));
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

        if (this.directoryNavigationWidget != null && this.directoryNavigationWidget.mouseClicked(mouseX, mouseY, mouseButton))
        {
            return;
        }

        int relativeY = mouseY - this.browserEntriesStartY - this.browserEntriesOffsetY;
        int index = relativeY / this.browserEntryHeight + this.scrollBar.getValue();

        if (relativeY >= 0 &&
            relativeY < this.maxVisibleBrowserEntries * this.browserEntryHeight &&
            mouseX >= this.browserEntriesStartX &&
            mouseX < this.browserEntriesStartX + this.browserEntryWidth)
        {
            if (index < Math.min(this.directoryContents.size(), this.maxVisibleBrowserEntries + this.scrollBar.getValue()))
            {
                DirectoryEntry entry = this.directoryContents.get(index);
                this.setSelectedEntry(entry, index);

                if (entry != null && entry.getType() == DirectoryEntryType.DIRECTORY)
                {
                    this.switchToDirectory(new File(entry.getDirectory(), entry.getName()));
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
            this.offsetSelectionOrScrollbar(mouseWheelDelta < 0 ? 3 : -3, false);
        }
    }

    @Override
    public void drawContents(int mouseX, int mouseY, float partialTicks)
    {
        // Draw an outline around the entire file browser
        GuiLitematicaBase.drawOutlinedBox(this.posX, this.posY, this.browserWidth, this.browserHeight, 0xB0000000, COLOR_HORIZONTAL_BAR);

        // Draw the root/up widget, is the current directory has that (ie. is not the root directory)
        if (this.directoryNavigationWidget != null)
        {
            this.directoryNavigationWidget.render(mouseX, mouseY, false);
        }

        // Draw the currently visible directory entries
        for (int i = 0; i < this.directoryEntryWidgets.size(); i++)
        {
            WidgetDirectoryEntry widget = this.directoryEntryWidgets.get(i);
            widget.render(mouseX, mouseY, widget.getDirectoryEntry() == this.selectedEntry);
        }

        int scrollbarHeight = this.browserHeight - 8;
        int totalHeight = Math.max(this.directoryContents.size() * this.browserEntryHeight, scrollbarHeight);
        this.scrollBar.drawScrollBar(mouseX, mouseY, partialTicks,
                this.posX + this.browserWidth - 9, this.browserEntriesStartY, 8, scrollbarHeight, totalHeight);

        // The value gets updated in the drawScrollBar() method above, if dragging
        if (this.scrollBar.getValue() != this.lastScrollbarPosition)
        {
            this.lastScrollbarPosition = this.scrollBar.getValue();
            this.recreateDirectoryEntryWidgets();
        }

        this.drawSelectedSchematicInfo(this.selectedEntry);
    }

    public void setSize(int width, int height)
    {
        this.totalWidth = width;
        this.totalHeight = height;
        this.infoWidth = 160;
        this.infoHeight = 280;
        this.browserWidth = width - this.infoWidth - 10;
        this.browserHeight = height;
        this.browserPaddingX = 3;
        this.browserPaddingY = 4;
        this.browserEntriesStartX = this.posX + this.browserPaddingX;
        this.browserEntriesStartY = this.posY + this.browserPaddingY;
        this.browserEntryWidth = this.browserWidth - 14;
        this.browserEntryHeight = 14;

        this.updateBrowserMaxVisibleEntries();
        this.refreshEntries();
    }

    protected void updateBrowserMaxVisibleEntries()
    {
        this.maxVisibleBrowserEntries = (this.browserHeight - this.browserPaddingY - this.browserEntriesOffsetY) / this.browserEntryHeight;
        this.scrollBar.setMaxValue(this.directoryContents.size() - this.maxVisibleBrowserEntries);
    }

    protected void updateBrowserOffsets()
    {
        if (this.currentDirectoryIsRoot())
        {
            this.browserEntriesOffsetY = 0;
        }
        else
        {
            this.browserEntriesOffsetY = this.browserEntryHeight + 2;
        }
    }

    public void refreshEntries()
    {
        this.readDirectory(this.currentDirectory);
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

        this.updateBrowserOffsets();
        this.updateBrowserMaxVisibleEntries();
        this.recreateDirectoryEntryWidgets();
    }

    protected void recreateDirectoryEntryWidgets()
    {
        this.directoryEntryWidgets.clear();

        final int numEntries = this.directoryContents.size();
        final int maxShown = Math.min(this.maxVisibleBrowserEntries, numEntries);
        int x = this.posX + 2;
        int y = this.posY + 4;
        int width = this.browserEntryWidth;
        int height = this.browserEntryHeight;

        if (this.browserEntriesOffsetY > 0)
        {
            this.directoryNavigationWidget = new WidgetDirectoryNavigation(x, y, width, height, height,
                    this.currentDirectory, DataManager.ROOT_SCHEMATIC_DIRECTORY, this.mc, this);
            y += this.browserEntriesOffsetY;
        }
        else
        {
            this.directoryNavigationWidget = null;
        }

        for (int i = 0, index = this.scrollBar.getValue(); i < maxShown && index < numEntries; i++, index++)
        {
            DirectoryEntry entry = this.directoryContents.get(index);
            boolean isOdd = (index & 0x1) != 0;
            this.directoryEntryWidgets.add(new WidgetDirectoryEntry(x, y, width, height, this.zLevel, isOdd, entry, this.mc));
            y += height;
        }
    }

    protected void offsetSelectionOrScrollbar(int amount, boolean changeSelection)
    {
        if (changeSelection == false)
        {
            this.scrollBar.offsetValue(amount);
        }
        else if (this.selectedEntryIndex >= 0)
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

        this.recreateDirectoryEntryWidgets();
    }

    protected boolean currentDirectoryIsRoot()
    {
        return this.currentDirectory.equals(DataManager.ROOT_SCHEMATIC_DIRECTORY);
    }

    @Override
    public File getCurrentDirectory()
    {
        return this.currentDirectory;
    }

    @Override
    public void switchToDirectory(File dir)
    {
        this.setSelectedEntry(null, -1);

        this.currentDirectory = FileUtils.getCanonicalFileIfPossible(dir);
        DataManager.setCurrentSchematicDirectory(dir);

        this.readDirectory(dir);
    }

    @Override
    public void switchToRootDirectory()
    {
        this.switchToDirectory(DataManager.ROOT_SCHEMATIC_DIRECTORY);
    }

    @Override
    public void switchToParentDirectory()
    {
        File parent = this.currentDirectory.getParentFile();

        if (parent != null)
        {
            this.switchToDirectory(parent);
        }
        else
        {
            this.switchToRootDirectory();
        }
    }

    @Nullable
    public DirectoryEntry getSelectedEntry()
    {
        return this.selectedEntry;
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

        public File getFullPath()
        {
            return new File(this.dir, this.name);
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
