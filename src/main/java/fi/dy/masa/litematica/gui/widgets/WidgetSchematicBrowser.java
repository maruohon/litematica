package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSchematicBrowser extends WidgetFileBrowserBase
{
    protected static final FileFilter SCHEMATIC_FILTER = new FileFilterSchematics();

    protected final Map<File, SchematicMetadata> cachedMetadata = new HashMap<>();
    protected final Map<File, Pair<Identifier, NativeImageBackedTexture>> cachedPreviewImages = new HashMap<>();
    protected final GuiSchematicBrowserBase parent;
    protected final int infoWidth;
    protected final int infoHeight;

    public WidgetSchematicBrowser(int x, int y, int width, int height, GuiSchematicBrowserBase parent, @Nullable ISelectionListener<DirectoryEntry> selectionListener)
    {
        super(x, y, width, height, DataManager.getDirectoryCache(), parent.getBrowserContext(),
                parent.getDefaultDirectory(), selectionListener, Icons.FILE_ICON_LITEMATIC);

        this.title = StringUtils.translate("litematica.gui.title.schematic_browser");
        this.infoWidth = 170;
        this.infoHeight = 290;
        this.parent = parent;
    }

    @Override
    protected int getBrowserWidthForTotalWidth(int width)
    {
        return super.getBrowserWidthForTotalWidth(width) - this.infoWidth;
    }

    @Override
    public void onClose()
    {
        super.onClose();

        this.clearPreviewImages();
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

        SchematicMetadata meta = this.getSchematicMetadata(entry);

        if (meta != null)
        {
            RenderUtils.color(1f, 1f, 1f, 1f);

            x += 3;
            y += 3;
            int textColor = 0xC0C0C0C0;
            int valueColor = 0xFFFFFFFF;

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

            Pair<Identifier, NativeImageBackedTexture> pair = this.cachedPreviewImages.get(entry.getFullPath());

            if (pair != null)
            {
                y += 14;

                int iconSize = pair.getRight().getImage().getWidth();
                boolean needsScaling = height < this.infoHeight;

                RenderUtils.color(1f, 1f, 1f, 1f);

                if (needsScaling)
                {
                    iconSize = height - y + this.posY - 6;
                }

                RenderUtils.drawOutlinedBox(x + 4, y, iconSize, iconSize, 0xA0000000, COLOR_HORIZONTAL_BAR);

                this.bindTexture(pair.getLeft());
                DrawableHelper.blit(x + 4, y, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
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
        File file = new File(entry.getDirectory(), entry.getName());
        SchematicMetadata meta = this.cachedMetadata.get(file);

        if (meta == null && this.cachedMetadata.containsKey(file) == false)
        {
            if (entry.getName().endsWith(LitematicaSchematic.FILE_EXTENSION))
            {
                LitematicaSchematic schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.getName());

                if (schematic != null)
                {
                    meta = schematic.getMetadata();
                    this.createPreviewImage(file, meta);
                }
            }

            this.cachedMetadata.put(file, meta);
        }

        return meta;
    }

    private void clearPreviewImages()
    {
        for (Pair<Identifier, NativeImageBackedTexture> pair : this.cachedPreviewImages.values())
        {
            this.mc.getTextureManager().destroyTexture(pair.getLeft());
        }
    }

    private void createPreviewImage(File file, SchematicMetadata meta)
    {
        int[] previewImageData = meta.getPreviewImagePixelData();

        if (previewImageData != null && previewImageData.length > 0)
        {
            int size = (int) Math.sqrt(previewImageData.length);

            if ((size * size) == previewImageData.length)
            {
                try
                {
                    NativeImage image = new NativeImage(size, size, false);
                    NativeImageBackedTexture tex = new NativeImageBackedTexture(image);
                    Identifier rl = new Identifier("litematica", DigestUtils.sha1Hex(file.getAbsolutePath()));
                    this.mc.getTextureManager().registerTexture(rl, tex);

                    for (int y = 0, i = 0; y < size; ++y)
                    {
                        for (int x = 0; x < size; ++x)
                        {
                            int val = previewImageData[i++];
                            // Swap the color channels from ARGB to ABGR
                            val = (val & 0xFF00FF00) | (val & 0xFF0000) >> 16 | (val & 0xFF) << 16;
                            image.setPixelRgba(x, y, val);
                        }
                    }

                    tex.upload();

                    this.cachedPreviewImages.put(file, Pair.of(rl, tex));
                }
                catch (Exception e)
                {
                    Litematica.logger.warn("Failed to create a preview image", e);
                }
            }
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
