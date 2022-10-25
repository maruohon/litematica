package litematica.materials;

import java.util.List;

import litematica.util.BlockInfoListType;

public interface IMaterialList
{
    BlockInfoListType getMaterialListType();

    void setMaterialListEntries(List<MaterialListEntry> list);
}
