package litematica.selection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import malilib.overlay.message.MessageDispatcher;
import malilib.overlay.message.MessageOutput;
import malilib.util.data.json.JsonUtils;
import malilib.util.game.wrap.NbtWrap;
import malilib.util.position.IntBoundingBox;
import litematica.config.Configs;
import litematica.render.infohud.StatusInfoRenderer;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import litematica.util.PositionUtils;

public class AreaSelection
{
    protected final Map<String, SelectionBox> selectionBoxes = new HashMap<>();
    protected BlockPos automaticOrigin = BlockPos.ORIGIN;
    protected String name = "Unnamed";
    protected boolean automaticOriginNeedsUpdate = true;
    protected boolean originSelected;
    @Nullable protected String selectedBoxName;
    @Nullable protected BlockPos manualOriginPos;

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    protected void onAreaSelectionModified()
    {
        this.automaticOriginNeedsUpdate = true;

        if (Configs.Visuals.AREA_SELECTION_RENDERING.getBooleanValue() == false)
        {
            StatusInfoRenderer.getInstance().startOverrideDelay();
        }
    }

    @Nullable
    public String getSelectedSelectionBoxName()
    {
        return this.selectedBoxName;
    }

    public boolean setSelectedSelectionBox(@Nullable String name)
    {
        if (name == null || this.selectionBoxes.containsKey(name))
        {
            this.selectedBoxName = name;
            return true;
        }

        return false;
    }

    public boolean isOriginSelected()
    {
        return this.originSelected;
    }

    public void setOriginSelected(boolean selected)
    {
        this.originSelected = selected;
    }

    public boolean hasManualOrigin()
    {
        return this.manualOriginPos != null;
    }

    /**
     * @return Returns the effective origin point. This is the manual origin point if one has been set,
     *         otherwise it's an automatically calculated origin point, located at the minimum corner of
     *         the enclosing box around all the selection boxes.
     */
    public BlockPos getEffectiveOrigin()
    {
        if (this.manualOriginPos != null)
        {
            return this.manualOriginPos;
        }
        else
        {
            if (this.automaticOriginNeedsUpdate)
            {
                this.updateAutomaticOrigin();
            }

            return this.automaticOrigin;
        }
    }

    /**
     * @return the manual origin point, if any
     */
    @Nullable
    public BlockPos getManualOrigin()
    {
        return this.manualOriginPos;
    }

    public void setManualOrigin(@Nullable BlockPos origin)
    {
        this.manualOriginPos = origin;

        if (origin == null)
        {
            this.originSelected = false;
        }
    }

    protected void updateAutomaticOrigin()
    {
        this.automaticOrigin = PositionUtils.getMinCornerOfEnclosingBox(this.selectionBoxes.values());
        this.automaticOriginNeedsUpdate = false;
    }

    @Nullable
    public SelectionBox getSelectionBox(String name)
    {
        return this.selectionBoxes.get(name);
    }

    @Nullable
    public SelectionBox getSelectedSelectionBox()
    {
        return this.selectedBoxName != null ? this.selectionBoxes.get(this.selectedBoxName) : null;
    }

    public List<String> getAllSelectionBoxNames()
    {
        List<String> list = new ArrayList<>(this.selectionBoxes.keySet());
        list.sort(Comparator.naturalOrder());
        return list;
    }

    public List<SelectionBox> getAllSelectionBoxes()
    {
        return ImmutableList.copyOf(this.selectionBoxes.values());
    }

    public ImmutableMap<String, SelectionBox> getAllSelectionBoxesMap()
    {
        ImmutableMap.Builder<String, SelectionBox> builder = ImmutableMap.builder();
        builder.putAll(this.selectionBoxes);
        return builder.build();
    }

    public NBTTagCompound getAllSelectionBoxesAsNbtCompound()
    {
        NBTTagCompound tag = new NBTTagCompound();

        for (Map.Entry<String, SelectionBox> entry : this.selectionBoxes.entrySet())
        {
            IntBoundingBox bb = entry.getValue().asIntBoundingBox();
            String regionName = entry.getKey();
            NbtWrap.putTag(tag, regionName, bb.toNbtIntArray());
        }

        return tag;
    }

    @Nullable
    public String createNewSelectionBox(BlockPos pos1, final String nameIn)
    {
        this.clearCornerSelectionOfSelectedBox();
        this.setOriginSelected(false);

        String name = nameIn;
        int i = 1;

        while (this.selectionBoxes.containsKey(name))
        {
            name = nameIn + " " + i;
            i++;
        }

        SelectionBox box = new SelectionBox(pos1, pos1, name);
        box.setSelectedCorner(BoxCorner.CORNER_1);
        this.selectedBoxName = name;
        this.selectionBoxes.put(name, box);
        this.onAreaSelectionModified();

        return name;
    }

    public void clearCornerSelectionOfSelectedBox()
    {
        this.setSelectedCornerOfSelectedBox(BoxCorner.NONE);
    }

    public void setSelectedCornerOfSelectedBox(BoxCorner corner)
    {
        SelectionBox box = this.getSelectedSelectionBox();

        if (box != null)
        {
            box.setSelectedCorner(corner);
        }
    }

    /**
     * Adds the given SelectionBox, if either replace is true, or there isn't yet a box by the same name.
     * @param box the new box to add
     * @param replace true if the box should replace an existing box by the same name, if one exists already
     * @return true if the box was successfully added, false if replace was false and there was already a box with the same name
     */
    public boolean addSelectionBox(SelectionBox box, boolean replace)
    {
        if (replace || this.selectionBoxes.containsKey(box.getName()) == false)
        {
            this.selectionBoxes.put(box.getName(), box);
            this.onAreaSelectionModified();
            return true;
        }

        return false;
    }

    public void removeAllSelectionBoxes()
    {
        this.selectionBoxes.clear();
        this.onAreaSelectionModified();
    }

    public boolean removeSelectionBox(String name)
    {
        boolean success = this.selectionBoxes.remove(name) != null;
        this.onAreaSelectionModified();

        if (success && name.equals(this.selectedBoxName))
        {
            this.selectedBoxName = null;
        }

        return success;
    }

    public boolean removeSelectedBox()
    {
        boolean success = this.selectedBoxName != null && this.selectionBoxes.remove(this.selectedBoxName) != null;
        this.selectedBoxName = null;
        this.onAreaSelectionModified();
        return success;
    }

    public boolean renameSelectionBox(String oldName, String newName)
    {
        return this.renameSelectionBox(oldName, newName, MessageOutput.NONE);
    }

    public boolean renameSelectionBox(String oldName, String newName, MessageOutput output)
    {
        SelectionBox box = this.selectionBoxes.get(oldName);

        if (box != null)
        {
            if (this.selectionBoxes.containsKey(newName))
            {
                MessageDispatcher.error().type(output)
                        .translate("litematica.error.area_editor.rename_sub_region.exists", newName);

                return false;
            }

            box.setName(newName);
            this.selectionBoxes.remove(oldName);
            this.selectionBoxes.put(newName, box);

            if (this.selectedBoxName != null && this.selectedBoxName.equals(oldName))
            {
                this.selectedBoxName = newName;
            }

            return true;
        }

        return false;
    }

    public void moveEntireAreaSelectionTo(BlockPos newOrigin, boolean printMessage)
    {
        BlockPos old = this.getEffectiveOrigin();
        BlockPos diff = newOrigin.subtract(old);

        for (SelectionBox box : this.selectionBoxes.values())
        {
            box.setCorner1(box.getCorner1().add(diff));
            box.setCorner2(box.getCorner2().add(diff));
        }

        if (this.getManualOrigin() != null)
        {
            this.setManualOrigin(newOrigin);
        }

        this.onAreaSelectionModified();

        if (printMessage)
        {
            String oldStr = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
            String newStr = String.format("x: %d, y: %d, z: %d", newOrigin.getX(), newOrigin.getY(), newOrigin.getZ());
            // TODO malilib refactor - this used to be showGuiOrActionBarMessage
            MessageDispatcher.success().customHotbar().translate("litematica.message.moved_selection", oldStr, newStr);
        }
    }

    public void moveSelectedElement(EnumFacing direction, int amount)
    {
        if (this.isOriginSelected())
        {
            if (this.getManualOrigin() != null)
            {
                this.setManualOrigin(this.getManualOrigin().offset(direction, amount));
            }

            return;
        }

        SelectionBox box = this.getSelectedSelectionBox();

        if (box != null)
        {
            box.offsetSelectedCorner(direction, amount);
            this.onAreaSelectionModified();
        }
        else
        {
            BlockPos newOrigin = this.getEffectiveOrigin().offset(direction, amount);
            this.moveEntireAreaSelectionTo(newOrigin, false);
        }
    }

    public void setSelectedSelectionBoxCornerPos(BlockPos pos, BoxCorner corner)
    {
        SelectionBox box = this.getSelectedSelectionBox();

        if (box != null)
        {
            this.setBoxCornerPos(box, corner, pos);
        }
    }

    public void setBoxCornerPos(CornerDefinedBox box, BoxCorner corner, BlockPos pos)
    {
        box.setCornerPosition(corner, pos);
        this.onAreaSelectionModified();
    }

    public AreaSelection copy()
    {
        return fromJson(this.toJson());
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        JsonUtils.addStringIfNotNull(obj, "name", this.name);
        JsonUtils.writeBlockPosIfNotNull(obj, "origin", this.getManualOrigin());

        JsonArray arr = JsonUtils.toArray(this.selectionBoxes.values(), SelectionBox::toJson);

        if (arr.size() > 0)
        {
            JsonUtils.addStringIfNotNull(obj, "current", this.selectedBoxName);
            obj.add("boxes", arr);
        }

        return obj;
    }

    public static AreaSelection fromJson(JsonObject obj)
    {
        AreaSelection area = new AreaSelection();

        JsonUtils.readArrayElementsIfObjects(obj, "boxes", o -> SelectionBox.fromJson(o, b -> area.selectionBoxes.put(b.getName(), b)));

        area.name = JsonUtils.getStringOrDefault(obj, "name", area.name);
        area.selectedBoxName = JsonUtils.getStringOrDefault(obj, "current", area.selectedBoxName);

        BlockPos pos = JsonUtils.blockPosFromJson(obj, "origin");

        if (pos != null)
        {
            area.setManualOrigin(pos);
        }
        else
        {
            area.updateAutomaticOrigin();
        }

        return area;
    }

    public static AreaSelection fromPlacement(SchematicPlacement placement)
    {
        ImmutableMap<String, SelectionBox> boxes = placement.getSubRegionBoxes(RequiredEnabled.ANY);
        AreaSelection selection = new AreaSelection();

        selection.name = placement.getName();
        selection.setManualOrigin(placement.getOrigin());

        for (Map.Entry<String, SelectionBox> entry : boxes.entrySet())
        {
            selection.selectionBoxes.put(entry.getKey(), entry.getValue().copy());
        }

        return selection;
    }
}
