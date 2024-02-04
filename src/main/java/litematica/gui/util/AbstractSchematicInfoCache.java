package litematica.gui.util;

import java.util.HashMap;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.texture.DynamicTexture;

import malilib.util.data.Identifier;
import malilib.util.game.wrap.GameWrap;
import litematica.schematic.SchematicMetadata;

public abstract class AbstractSchematicInfoCache<T>
{
    protected final HashMap<T, SchematicInfo> cachedData = new HashMap<>();

    @Nullable
    protected abstract SchematicInfo createSchematicInfo(T key);

    @Nullable
    public SchematicInfo getSchematicInfo(T key)
    {
        return this.cachedData.get(key);
    }

    @Nullable
    public SchematicInfo getOrCacheSchematicInfo(T key)
    {
        SchematicInfo info = this.cachedData.get(key);

        if (info != null)
        {
            return info;
        }

        SchematicInfo data = this.createSchematicInfo(key);
        this.cachedData.put(key, data);

        return data;
    }

    public void clearCache()
    {
        for (SchematicInfo info : this.cachedData.values())
        {
            if (info != null && info.texture != null)
            {
                GameWrap.getClient().getTextureManager().deleteTexture(info.iconName);
            }
        }

        this.cachedData.clear();
    }

    @Nullable
    protected DynamicTexture createPreviewImage(Identifier iconName, SchematicMetadata meta)
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
                    GameWrap.getClient().getTextureManager().loadTexture(iconName, tex);

                    System.arraycopy(previewImageData, 0, tex.getTextureData(), 0, previewImageData.length);
                    tex.updateDynamicTexture();

                    return tex;
                }
            }
            catch (Exception ignore) {}
        }

        return null;
    }

    public static class SchematicInfo
    {
        public final SchematicMetadata schematicMetadata;
        public final Identifier iconName;
        @Nullable public final DynamicTexture texture;

        protected SchematicInfo(SchematicMetadata schematicMetadata,
                                Identifier iconName,
                                @Nullable DynamicTexture texture)
        {
            this.schematicMetadata = schematicMetadata;
            this.iconName = iconName;
            this.texture = texture;
        }
    }
}
