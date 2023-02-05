package litematica.gui.util;

import java.util.Locale;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.texture.DynamicTexture;

import malilib.util.FileNameUtils;
import malilib.util.data.Identifier;
import litematica.Reference;
import litematica.schematic.ISchematic;
import litematica.schematic.SchematicMetadata;

public class SchematicInfoCacheBySchematic extends AbstractSchematicInfoCache<ISchematic>
{
    @Override
    @Nullable
    protected SchematicInfo createSchematicInfo(ISchematic schematic)
    {
        SchematicMetadata metadata = schematic.getMetadata();
        String name;

        if (schematic.getFile() != null)
        {
            name = FileNameUtils.generateSimpleSafeFileName(schematic.getFile().toAbsolutePath().toString().toLowerCase(Locale.ROOT));
        }
        else
        {
            name = FileNameUtils.generateSimpleSafeFileName(metadata.getName() + "_" + metadata.getAuthor() + "_" + metadata.getTimeCreated());
        }

        Identifier iconName = new Identifier(Reference.MOD_ID, name);
        DynamicTexture texture = this.createPreviewImage(iconName, metadata);
        return new SchematicInfo(metadata, iconName, texture);
    }
}
