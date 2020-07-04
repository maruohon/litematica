package de.meinbuild.liteschem.materials;

import java.util.List;
import de.meinbuild.liteschem.util.BlockInfoListType;

public interface IMaterialList
{
    BlockInfoListType getMaterialListType();

    void setMaterialListEntries(List<MaterialListEntry> list);
}
