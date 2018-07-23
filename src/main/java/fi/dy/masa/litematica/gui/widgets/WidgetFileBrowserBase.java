package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import org.lwjgl.input.Keyboard;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.base.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.interfaces.IDirectoryNavigator;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.litematica.gui.widgets.base.WidgetListBase;
import fi.dy.masa.litematica.util.FileUtils;
import fi.dy.masa.malilib.gui.RenderUtils;

public abstract class WidgetFileBrowserBase extends WidgetListBase<DirectoryEntry, WidgetDirectoryEntry> implements IDirectoryNavigator
{
    protected static final FileFilter DIRECTORY_FILTER = new FileFilterDirectories();
    protected static final FileFilter SCHEMATIC_FILTER = new FileFilterSchematics();
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    protected File currentDirectory;
    private final String browserContext;

    @Nullable
    protected WidgetDirectoryNavigation directoryNavigationWidget;

    public WidgetFileBrowserBase(int x, int y, int width, int height,
            String browserContext, File defaultDirectory, GuiSchematicBrowserBase parent, @Nullable ISelectionListener<DirectoryEntry> selectionListener)
    {
        super(x, y, width, height, selectionListener);

        this.browserContext = browserContext;
        this.currentDirectory = DataManager.getCurrentDirectoryForContext(this.browserContext, false);

        if (this.currentDirectory == null)
        {
            this.currentDirectory = defaultDirectory;
        }

        this.setSize(width, height);
    }

    @Override
    public boolean onKeyTyped(char typedChar, int keyCode)
    {
        if ((keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_LEFT) && this.currentDirectoryIsRoot() == false)
        {
            this.switchToParentDirectory();
            return true;
        }
        else if ((keyCode == Keyboard.KEY_RIGHT || keyCode == Keyboard.KEY_RETURN) &&
                  this.getSelectedEntry() != null && this.getSelectedEntry().getType() == DirectoryEntryType.DIRECTORY)
        {
            this.switchToDirectory(new File(this.getSelectedEntry().getDirectory(), this.getSelectedEntry().getName()));
            return true;
        }
        else
        {
            return super.onKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (this.directoryNavigationWidget != null && this.directoryNavigationWidget.onMouseClickedImpl(mouseX, mouseY, mouseButton))
        {
            return true;
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawContents(int mouseX, int mouseY, float partialTicks)
    {
        // Draw an outline around the entire file browser
        RenderUtils.drawOutlinedBox(this.posX, this.posY, this.browserWidth, this.browserHeight, 0xB0000000, COLOR_HORIZONTAL_BAR);

        // Draw the root/up widget, is the current directory has that (ie. is not the root directory)
        if (this.directoryNavigationWidget != null)
        {
            this.directoryNavigationWidget.render(mouseX, mouseY, false);
        }

        this.drawAdditionalContents(mouseX, mouseY);

        super.drawContents(mouseX, mouseY, partialTicks);
    }

    protected abstract void drawAdditionalContents(int mouseX, int mouseY);

    @Override
    public void setSize(int width, int height)
    {
        super.setSize(width, height);

        this.browserWidth = this.getBrowserWidthForTotalWidth(width);
        this.browserEntryWidth = this.browserWidth - 14;
    }

    protected int getBrowserWidthForTotalWidth(int width)
    {
        return width - 6;
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

    @Override
    public void refreshEntries()
    {
        this.listContents.clear();

        File dir = this.currentDirectory;

        if (dir.isDirectory() && dir.canRead())
        {
            List<DirectoryEntry> list = new ArrayList<>();

            this.addDirectoryEntriesToList(dir, list);
            this.listContents.addAll(list);
            list.clear();

            this.addFileEntriesToList(dir, list);
            this.listContents.addAll(list);
        }

        this.scrollBar.setMaxValue(this.listContents.size() - this.maxVisibleBrowserEntries);

        this.updateBrowserOffsets();
        this.updateBrowserMaxVisibleEntries();
        this.recreateListWidgets();
    }

    protected abstract File getRootDirectory();

    protected void addDirectoryEntriesToList(File dir, List<DirectoryEntry> list)
    {
        // Show directories at the top
        for (File file : dir.listFiles(DIRECTORY_FILTER))
        {
            list.add(new DirectoryEntry(DirectoryEntryType.fromFile(file), dir, file.getName()));
        }

        Collections.sort(list);
    }

    protected abstract void addFileEntriesToList(File dir, List<DirectoryEntry> list);

    @Override
    protected void recreateListWidgets()
    {
        super.recreateListWidgets();

        int x = this.posX + 2;
        int y = this.posY + 4;
        int width = this.browserEntryWidth;
        int height = this.browserEntryHeight;

        if (this.currentDirectoryIsRoot() == false)
        {
            this.directoryNavigationWidget = new WidgetDirectoryNavigation(x, y, width, height, height,
                    this.currentDirectory, this.getRootDirectory(), this.mc, this);
            y += this.browserEntriesOffsetY;
        }
        else
        {
            this.directoryNavigationWidget = null;
        }
    }

    @Override
    protected WidgetDirectoryEntry createListWidget(int x, int y, boolean isOdd, DirectoryEntry entry)
    {
        return new WidgetDirectoryEntry(x, y, this.browserEntryWidth, this.browserEntryHeight, this.zLevel, isOdd, entry, this.mc, this);
    }

    protected boolean currentDirectoryIsRoot()
    {
        return this.currentDirectory.equals(this.getRootDirectory());
    }

    @Override
    public File getCurrentDirectory()
    {
        return this.currentDirectory;
    }

    @Override
    public void switchToDirectory(File dir)
    {
        this.clearSelection();

        this.currentDirectory = FileUtils.getCanonicalFileIfPossible(dir);
        DataManager.setCurrentDirectoryForContext(this.browserContext, dir);

        this.refreshEntries();
    }

    @Override
    public void switchToRootDirectory()
    {
        this.switchToDirectory(this.getRootDirectory());
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
