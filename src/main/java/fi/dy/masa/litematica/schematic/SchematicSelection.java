package fi.dy.masa.litematica.schematic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class SchematicSelection
{
    private final Map<String, SelectionBox> selectionBoxes = new HashMap<>();
    private String name = "Unnamed";

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Nullable
    public SelectionBox getSelectionBox(String name)
    {
        return this.selectionBoxes.get(name);
    }

    public Collection<SelectionBox> getAllSelectionsBoxes()
    {
        return this.selectionBoxes.values();
    }

    public void removeAllSelectionBoxes()
    {
        this.selectionBoxes.clear();
    }

    /**
     * Adds the given SelectionBox, if either replace is true, or there isn't yet a box by the same name.
     * @param box
     * @param replace
     * @return true if the box was successfully added, false if replace was false and there was already a box with the same name
     */
    public boolean addSelectionBox(SelectionBox box, boolean replace)
    {
        if (replace || this.selectionBoxes.containsKey(box.getName()) == false)
        {
            this.selectionBoxes.put(box.getName(), box);
            return true;
        }

        return false;
    }

    public boolean removeSelectionBox(String name)
    {
        return this.selectionBoxes.remove(name) != null;
    }
}
