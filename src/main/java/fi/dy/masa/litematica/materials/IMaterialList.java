package fi.dy.masa.litematica.materials;

import java.util.List;
import fi.dy.masa.litematica.util.BlockInfoListType;

public interface IMaterialList
{
    BlockInfoListType getMaterialListType();

    void setMaterialListEntries(List<MaterialListEntry> list);
}
