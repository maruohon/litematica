package fi.dy.masa.litematica.gui.util;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;

import malilib.gui.icon.FileBrowserIconProvider;
import malilib.gui.icon.MultiIcon;
import fi.dy.masa.litematica.schematic.SchematicType;

public class SchematicBrowserIconProvider implements FileBrowserIconProvider
{
    protected final HashMap<Path, MultiIcon> cachedIcons = new HashMap<>();

    @Override
    @Nullable
    public MultiIcon getIconForFile(Path file)
    {
        MultiIcon icon = this.cachedIcons.get(file);

        if (icon == null && this.cachedIcons.containsKey(file) == false)
        {
            List<SchematicType<?>> possibleTypes = SchematicType.getPossibleTypesFromFileName(file);

            if (possibleTypes.isEmpty() == false)
            {
                icon = possibleTypes.get(0).getIcon();
            }

            this.cachedIcons.put(file, icon);
        }

        return icon;
    }

    public void setIconForFile(Path file, @Nullable MultiIcon icon)
    {
        this.cachedIcons.put(file, icon);
    }
}
