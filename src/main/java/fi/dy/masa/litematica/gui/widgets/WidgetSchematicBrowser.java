package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.SchematicType;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.NBTUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSchematicBrowser extends WidgetFileBrowserBase implements ISelectionListener<DirectoryEntry>
{
    protected static final FileFilter SCHEMATIC_FILTER = new FileFilterSchematics();

    protected final Map<File, CachedSchematicData> cachedData = new HashMap<>();
    protected final GuiSchematicBrowserBase parent;
    @Nullable protected final ISelectionListener<DirectoryEntry> parentSelectionListener;
    protected final int infoWidth;
    protected final int infoHeight;

    public WidgetSchematicBrowser(int x, int y, int width, int height, GuiSchematicBrowserBase parent, @Nullable ISelectionListener<DirectoryEntry> selectionListener)
    {
        super(x, y, width, height, DataManager.getDirectoryCache(), parent.getBrowserContext(),
                parent.getDefaultDirectory(), null, Icons.FILE_ICON_LITEMATIC);

        this.parentSelectionListener = selectionListener;
        this.title = StringUtils.translate("litematica.gui.title.schematic_browser");
        this.infoWidth = 170;
        this.infoHeight = 290;
        this.parent = parent;
        this.setSelectionListener(this);
    }

    @Override
    protected int getBrowserWidthForTotalWidth(int width)
    {
        return super.getBrowserWidthForTotalWidth(width) - this.infoWidth;
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();

        this.clearSchematicMetadataCache();
    }

    private void clearPreviewImages()
    {
        for (CachedSchematicData data : this.cachedData.values())
        {
            if (data != null && data.texture != null)
            {
                this.mc.getTextureManager().deleteTexture(data.iconName);
            }
        }
    }

    @Override
    protected File getRootDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    protected FileFilter getFileFilter()
    {
        return SCHEMATIC_FILTER;
    }

    @Override
    public void onSelectionChange(DirectoryEntry entry)
    {
        if (entry != null)
        {
            this.cacheSchematicData(entry);
        }

        if (this.parentSelectionListener != null)
        {
            this.parentSelectionListener.onSelectionChange(entry);
        }
    }

    @Override
    protected void drawAdditionalContents(int mouseX, int mouseY)
    {
        this.drawSelectedSchematicInfo(this.getLastSelectedEntry());
    }

    protected void drawSelectedSchematicInfo(@Nullable DirectoryEntry entry)
    {
        int x = this.posX + this.totalWidth - this.infoWidth;
        int y = this.posY;
        int height = Math.min(this.infoHeight, this.parent.getMaxInfoHeight());

        RenderUtils.drawOutlinedBox(x, y, this.infoWidth, height, 0xA0000000, COLOR_HORIZONTAL_BAR);

        if (entry == null)
        {
            return;
        }

        CachedSchematicData data = this.cachedData.get(entry.getFullPath());

        if (data != null)
        {
            SchematicMetadata meta = data.schematic.getMetadata();

            x += 3;
            y += 3;
            int textColor = 0xC0C0C0C0;
            int valueColor = 0xC0FFFFFF;

            String str = StringUtils.translate("litematica.gui.label.schematic_info.name");
            this.drawString(str, x, y, textColor);
            y += 12;

            this.drawString(meta.getName(), x + 4, y, valueColor);
            y += 12;

            str = StringUtils.translate("litematica.gui.label.schematic_info.schematic_author", meta.getAuthor());
            this.drawString(str, x, y, textColor);
            y += 12;

            String strDate = DATE_FORMAT.format(new Date(meta.getTimeCreated()));
            str = StringUtils.translate("litematica.gui.label.schematic_info.time_created", strDate);
            this.drawString(str, x, y, textColor);
            y += 12;

            if (meta.hasBeenModified())
            {
                strDate = DATE_FORMAT.format(new Date(meta.getTimeModified()));
                str = StringUtils.translate("litematica.gui.label.schematic_info.time_modified", strDate);
                this.drawString(str, x, y, textColor);
                y += 12;
            }

            str = StringUtils.translate("litematica.gui.label.schematic_info.region_count", meta.getRegionCount());
            this.drawString(str, x, y, textColor);
            y += 12;

            if (this.parent.height >= 340)
            {
                str = StringUtils.translate("litematica.gui.label.schematic_info.total_volume", meta.getTotalVolume());
                this.drawString(str, x, y, textColor);
                y += 12;

                str = StringUtils.translate("litematica.gui.label.schematic_info.total_blocks", meta.getTotalBlocks());
                this.drawString(str, x, y, textColor);
                y += 12;

                str = StringUtils.translate("litematica.gui.label.schematic_info.enclosing_size");
                this.drawString(str, x, y, textColor);
                y += 12;

                Vec3i areaSize = meta.getEnclosingSize();
                String tmp = String.format("%d x %d x %d", areaSize.getX(), areaSize.getY(), areaSize.getZ());
                this.drawString(tmp, x + 4, y, valueColor);
                y += 12;
            }
            else
            {
                str = StringUtils.translate("litematica.gui.label.schematic_info.total_blocks_and_volume", meta.getTotalBlocks(), meta.getTotalVolume());
                this.drawString(str, x, y, textColor);
                y += 12;

                Vec3i areaSize = meta.getEnclosingSize();
                String tmp = String.format("%d x %d x %d", areaSize.getX(), areaSize.getY(), areaSize.getZ());
                str = StringUtils.translate("litematica.gui.label.schematic_info.enclosing_size_value", tmp);
                this.drawString(str, x, y, textColor);
                y += 12;
            }

            /*
            str = StringUtils.translate("litematica.gui.label.schematic_info.description");
            this.drawString(x, y, textColor, str);
            */
            //y += 12;

            if (data.texture != null)
            {
                y += 14;

                int iconSize = (int) Math.sqrt(data.texture.getTextureData().length);
                boolean needsScaling = height < this.infoHeight;

                RenderUtils.color(1f, 1f, 1f, 1f);

                if (needsScaling)
                {
                    iconSize = height - y + this.posY - 6;
                }

                RenderUtils.drawOutlinedBox(x + 4, y, iconSize, iconSize, 0xA0000000, COLOR_HORIZONTAL_BAR);

                this.bindTexture(data.iconName);
                Gui.drawModalRectWithCustomSizedTexture(x + 4, y, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            }
        }
    }

    public void clearSchematicMetadataCache()
    {
        this.clearPreviewImages();
        this.cachedData.clear();
    }

    @Nullable
    public CachedSchematicData getCachedData(File file)
    {
        return this.cachedData.get(file);
    }

    protected void cacheSchematicData(DirectoryEntry entry)
    {
        File file = new File(entry.getDirectory(), entry.getName());

        if (this.cachedData.containsKey(file) == false)
        {
            NBTTagCompound tag = NBTUtils.readNbtFromFile(file);
            CachedSchematicData data = null;

            if (tag != null)
            {
                ISchematic schematic = SchematicType.tryCreateSchematicFrom(file, tag);

                if (schematic != null)
                {
                    SchematicMetadata metadata = schematic.getMetadata();
                    ResourceLocation iconName = new ResourceLocation(Reference.MOD_ID, file.getAbsolutePath());
                    DynamicTexture texture = this.createPreviewImage(iconName, metadata);
                    data = new CachedSchematicData(tag, schematic, iconName, texture);
                }
            }

            this.cachedData.put(file, data);
        }
    }

    @Nullable
    private DynamicTexture createPreviewImage(ResourceLocation iconName, SchematicMetadata meta)
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
                    this.mc.getTextureManager().loadTexture(iconName, tex);

                    System.arraycopy(previewImageData, 0, tex.getTextureData(), 0, previewImageData.length);
                    tex.updateDynamicTexture();

                    return tex;
                }
            }
            catch (Exception e)
            {
            }
        }

        return null;
    }

    public static class CachedSchematicData
    {
        public final NBTTagCompound tag;
        public final ISchematic schematic;
        public final ResourceLocation iconName;
        @Nullable public final DynamicTexture texture;

        protected CachedSchematicData(NBTTagCompound tag, ISchematic schematic, ResourceLocation iconName, @Nullable DynamicTexture texture)
        {
            this.tag = tag;
            this.schematic = schematic;
            this.iconName = iconName;
            this.texture = texture;
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
                    name.endsWith(".schem") ||
                    name.endsWith(".nbt");
        }
    }
}
