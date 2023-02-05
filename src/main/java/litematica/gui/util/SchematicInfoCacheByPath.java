package litematica.gui.util;

import java.nio.file.Path;
import java.util.Locale;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.NBTTagCompound;

import malilib.util.FileNameUtils;
import malilib.util.data.Identifier;
import malilib.util.nbt.NbtUtils;
import litematica.Reference;
import litematica.schematic.ISchematic;
import litematica.schematic.SchematicMetadata;
import litematica.schematic.SchematicType;

public class SchematicInfoCacheByPath extends AbstractSchematicInfoCache<Path>
{
    @Override
    @Nullable
    protected SchematicInfo createSchematicInfo(Path file)
    {
        // TODO Use a partial NBT read method to only read the metadata tag
        // TODO (that's only beneficial if it's stored before the bulk schematic data in the stream)
        NBTTagCompound tag = NbtUtils.readNbtFromFile(file);

        if (tag != null)
        {
            ISchematic schematic = SchematicType.tryCreateSchematicFrom(file, tag);

            if (schematic != null)
            {
                SchematicMetadata metadata = schematic.getMetadata();
                String filePath = FileNameUtils.generateSimpleSafeFileName(file.toAbsolutePath().toString().toLowerCase(Locale.ROOT));
                Identifier iconName = new Identifier(Reference.MOD_ID, filePath);
                DynamicTexture texture = this.createPreviewImage(iconName, metadata);
                return new SchematicInfo(metadata, iconName, texture);
            }
        }

        return null;
    }
}
