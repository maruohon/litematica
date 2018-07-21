package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.base.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.interfaces.IDirectoryNavigator;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.DirectoryEntry;
import fi.dy.masa.litematica.gui.widgets.base.WidgetListBase;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.util.FileUtils;
import fi.dy.masa.malilib.gui.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3i;

public class WidgetSchematicBrowser extends WidgetListBase<DirectoryEntry, WidgetDirectoryEntry> implements IDirectoryNavigator
{
    protected static final FileFilter DIRECTORY_FILTER = new FileFilterDirectories();
    protected static final FileFilter SCHEMATIC_FILTER = new FileFilterSchematics();
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    protected Map<File, SchematicMetadata> cachedMetadata = new HashMap<>();
    protected Map<File, Pair<ResourceLocation, DynamicTexture>> cachedPreviewImages = new HashMap<>();
    protected File currentDirectory;
    protected final int infoWidth;
    protected final int infoHeight;
    protected final GuiSchematicBrowserBase parent;
    @Nullable
    protected WidgetDirectoryNavigation directoryNavigationWidget;

    public WidgetSchematicBrowser(int x, int y, int width, int height, GuiSchematicBrowserBase parent, @Nullable ISelectionListener<DirectoryEntry> selectionListener)
    {
        super(x, y, width, height, selectionListener);

        this.title = I18n.format("litematica.gui.title.schematic_browser");
        this.infoWidth = 170;
        this.infoHeight = 280;
        this.parent = parent;
        this.currentDirectory = parent.getInitialDirectory();

        this.setSize(width, height);
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();

        this.clearPreviewImages();
    }

    private void clearPreviewImages()
    {
        for (Pair<ResourceLocation, DynamicTexture> pair : this.cachedPreviewImages.values())
        {
            this.mc.getTextureManager().deleteTexture(pair.getLeft());
        }
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

        this.drawSelectedSchematicInfo(this.getSelectedEntry());

        super.drawContents(mouseX, mouseY, partialTicks);
    }

    @Override
    public void setSize(int width, int height)
    {
        super.setSize(width, height);

        this.browserWidth = width - this.infoWidth - 6;
        this.browserEntryWidth = this.browserWidth - 14;
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

            // Show directories at the top
            for (File file : dir.listFiles(DIRECTORY_FILTER))
            {
                list.add(new DirectoryEntry(DirectoryEntryType.fromFile(file), dir, file.getName()));
            }

            Collections.sort(list);
            this.listContents.addAll(list);

            list.clear();

            for (File file : dir.listFiles(SCHEMATIC_FILTER))
            {
                list.add(new DirectoryEntry(DirectoryEntryType.fromFile(file), dir, file.getName()));
            }

            Collections.sort(list);
            this.listContents.addAll(list);
        }

        this.scrollBar.setMaxValue(this.listContents.size() - this.maxVisibleBrowserEntries);

        this.updateBrowserOffsets();
        this.updateBrowserMaxVisibleEntries();
        this.recreateListWidgets();
    }

    @Override
    protected void recreateListWidgets()
    {
        super.recreateListWidgets();

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
    }

    @Override
    protected WidgetDirectoryEntry createListWidget(int x, int y, boolean isOdd, DirectoryEntry entry)
    {
        return new WidgetDirectoryEntry(x, y, this.browserEntryWidth, this.browserEntryHeight, this.zLevel, isOdd, entry, this.mc, this);
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
        this.clearSelection();

        this.currentDirectory = FileUtils.getCanonicalFileIfPossible(dir);
        this.parent.storeCurrentDirectory(dir);

        this.refreshEntries();
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

    protected void drawSelectedSchematicInfo(@Nullable DirectoryEntry entry)
    {
        int x = this.posX + this.totalWidth - this.infoWidth;
        int y = this.posY;

        RenderUtils.drawOutlinedBox(x, y, this.infoWidth, this.infoHeight, 0xA0000000, COLOR_HORIZONTAL_BAR);

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
            int valueColor = 0xC0FFFFFF;

            String str = I18n.format("litematica.gui.label.schematic_info.name");
            this.fontRenderer.drawString(str, x, y, textColor);
            y += 12;

            this.fontRenderer.drawString(meta.getName(), x + 4, y, valueColor);
            y += 12;

            str = I18n.format("litematica.gui.label.schematic_info.author", meta.getAuthor());
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

            Vec3i areaSize = meta.getEnclosingSize();
            String tmp = String.format("%d x %d x %d", areaSize.getX(), areaSize.getY(), areaSize.getZ());
            this.fontRenderer.drawString(tmp, x + 4, y, valueColor);
            y += 12;

            /*
            str = I18n.format("litematica.gui.label.schematic_info.description");
            this.fontRenderer.drawString(str, x, y, textColor);
            */
            y += 12;

            Pair<ResourceLocation, DynamicTexture> pair = this.cachedPreviewImages.get(entry.getFullPath());

            if (pair != null)
            {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.bindTexture(pair.getLeft());

                int iconSize = (int) Math.sqrt(pair.getRight().getTextureData().length);
                Gui.drawModalRectWithCustomSizedTexture(x + 10, y, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            }
        }
    }

    public void clearSchematicMetadataCache()
    {
        this.clearPreviewImages();
        this.cachedMetadata.clear();
        this.cachedPreviewImages.clear();
    }

    @Nullable
    protected SchematicMetadata getSchematicMetadata(DirectoryEntry entry)
    {
        File file = new File(entry.getDirectory(), entry.getName() + "_meta");
        SchematicMetadata meta = this.cachedMetadata.get(file);

        if (meta == null)
        {
            meta = SchematicMetadata.fromFile(file);

            if (meta != null)
            {
                this.cachedMetadata.put(file, meta);
                this.createPreviewImage(new File(entry.getDirectory(), entry.getName()), meta);
            }
        }

        return meta;
    }

    private void createPreviewImage(File file, SchematicMetadata meta)
    {
        int[] previewImageData = meta.getPreviewImagePixelData();

        if (previewImageData != null && previewImageData.length > 0)
        {
            try
            {
                int size = (int) Math.sqrt(previewImageData.length);

                if (size * size == previewImageData.length)
                {
                    //BufferedImage buf = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                    //buf.setRGB(0, 0, size, size, previewImageData, 0, size);

                    DynamicTexture tex = new DynamicTexture(size, size);
                    ResourceLocation rl = new ResourceLocation("litematica", file.getAbsolutePath());
                    this.mc.getTextureManager().loadTexture(rl, tex);

                    System.arraycopy(previewImageData, 0, tex.getTextureData(), 0, previewImageData.length);
                    tex.updateDynamicTexture();

                    this.cachedPreviewImages.put(file, Pair.of(rl, tex));
                }
            }
            catch (Exception e)
            {
            }
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
