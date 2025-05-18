package fi.dy.masa.litematica.selection;

import java.util.*;
import java.util.function.Function;
import javax.annotation.Nullable;

import fi.dy.masa.malilib.MaLiLib;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.infohud.StatusInfoRenderer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.interfaces.IMessageConsumer;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.PositionUtils.CoordinateType;

public class AreaSelection
{
    protected final Map<String, Box> subRegionBoxes = new HashMap<>();
    protected String name = "Unnamed";
    protected boolean originSelected;
    protected BlockPos calculatedOrigin = BlockPos.ORIGIN;
    protected boolean calculatedOriginDirty = true;
    @Nullable protected BlockPos explicitOrigin = null;
    @Nullable protected String currentBox;

    public static AreaSelection fromPlacement(SchematicPlacement placement)
    {
        ImmutableMap<String, Box> boxes = placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
        BlockPos origin = placement.getOrigin();

        AreaSelection selection = new AreaSelection();
        selection.setExplicitOrigin(origin);
        selection.name = placement.getName();
        selection.subRegionBoxes.putAll(boxes);

        return selection;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    protected void markDirty()
    {
        this.calculatedOriginDirty = true;

        if (Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING.getBooleanValue() == false)
        {
            StatusInfoRenderer.getInstance().startOverrideDelay();
        }
    }

    @Nullable
    public String getCurrentSubRegionBoxName()
    {
        return this.currentBox;
    }

    public boolean setSelectedSubRegionBox(@Nullable String name)
    {
        if (name == null || this.subRegionBoxes.containsKey(name))
        {
            this.currentBox = name;
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

    /**
     * Returns the effective origin point. This is the explicit origin point, if one has been set,
     * otherwise it's an automatically calculated origin point, located at the minimum corner
     * of all the boxes.
     * @return
     */
    public BlockPos getEffectiveOrigin()
    {
        if (this.explicitOrigin != null)
        {
            return this.explicitOrigin;
        }
        else
        {
            if (this.calculatedOriginDirty)
            {
                this.updateCalculatedOrigin();
            }

            return this.calculatedOrigin;
        }
    }

    /**
     * Get the explicitly defined origin point, if any.
     * @return
     */
    @Nullable
    public BlockPos getExplicitOrigin()
    {
        return this.explicitOrigin;
    }

    public void setExplicitOrigin(@Nullable BlockPos origin)
    {
        this.explicitOrigin = origin;

        if (origin == null)
        {
            this.originSelected = false;
        }
    }

    protected void updateCalculatedOrigin()
    {
        Pair<BlockPos, BlockPos> pair = PositionUtils.getEnclosingAreaCorners(this.subRegionBoxes.values());

        if (pair != null)
        {
            this.calculatedOrigin = pair.getLeft();
        }
        else
        {
            this.calculatedOrigin = BlockPos.ORIGIN;
        }

        this.calculatedOriginDirty = false;
    }

    @Nullable
    public Box getSubRegionBox(String name)
    {
        return this.subRegionBoxes.get(name);
    }

    @Nullable
    public Box getSelectedSubRegionBox()
    {
        return this.currentBox != null ? this.subRegionBoxes.get(this.currentBox) : null;
    }

    public Collection<String> getAllSubRegionNames()
    {
        return this.subRegionBoxes.keySet();
    }

    public List<Box> getAllSubRegionBoxes()
    {
        return ImmutableList.copyOf(this.subRegionBoxes.values());
    }

    public ImmutableMap<String, Box> getAllSubRegions()
    {
        ImmutableMap.Builder<String, Box> builder = ImmutableMap.builder();
        builder.putAll(this.subRegionBoxes);
        return builder.build();
    }

    @Nullable
    public String createNewSubRegionBox(BlockPos pos1, final String nameIn)
    {
        this.clearCurrentSelectedCorner();
        this.setOriginSelected(false);

        String name = nameIn;
        int i = 1;

        while (this.subRegionBoxes.containsKey(name))
        {
            name = nameIn + " " + i;
            i++;
        }

        Box box = new Box();
        box.setName(name);
        box.setSelectedCorner(Corner.CORNER_1);
        this.currentBox = name;
        this.subRegionBoxes.put(name, box);
        this.setSubRegionCornerPos(box, Corner.CORNER_1, pos1);
        this.setSubRegionCornerPos(box, Corner.CORNER_2, pos1);

        return name;
    }


    private double calculateDistance2(double x1, double z1, double x2, double z2) {
        return (x1 - x2) * (x1 - x2) + (z1 - z2) * (z1 - z2);
    }

    @Nullable
    public String createNewSubRegionBoxCircle(BlockPos cor1, BlockPos cor2, boolean onCircle, boolean expand) {
        try {
            double c1x = cor1.getX();
            double c1z = cor1.getZ();
            double c1y = cor1.getY();
            double c2x = cor2.getX();
            double c2z = cor2.getZ();
            double c2y = cor2.getY();
            double dis2 = calculateDistance2(c1x, c1z, c2x, c2z);
            double dis = Math.sqrt(dis2);
            MaLiLib.logger.error(String.format("center:%s,%s,%s radius:%s", c1x, c1y, c1z, dis));
            double startX = c1x;
            double startZ = c1z + Math.round(dis);
            List<double[]> posList = new ArrayList<>();
            posList.add(new double[]{startX, startZ});
            // 1/4圆
            while (startZ > c1z) {
                double[][] nextPos = {{startX + 1, startZ}, {startX + 1, startZ - 1}, {startX, startZ - 1}};
                double minDisSquare = Double.MAX_VALUE;
                for (int i = 0; i < 3; i++) {
                    double disSquare = Math.abs(calculateDistance2(nextPos[i][0], nextPos[i][1], c1x, c1z) - dis2);
                    if (disSquare < minDisSquare) {
                        minDisSquare = disSquare;
                        startX = nextPos[i][0];
                        startZ = nextPos[i][1];
                    }
                }
                posList.add(new double[]{startX, startZ});
            }
            //关于x轴对称 1/2圆
            int len = posList.size();
            for (int i = 0; i < len; ++i) {
                double[] pos = posList.get(i);
                double syncPosX = pos[0];
                double syncPosZ = 2 * c1z - pos[1];
                posList.add(new double[]{syncPosX, syncPosZ});
            }
            //关于z轴对称 整个圆
            Set<Long> seenPosLongSet = new HashSet<>();
            List<Box> boxList = new ArrayList<>();
            for (double[] pos : posList) {
                double curX = (int) pos[0];
                double curZ = (int) pos[1];
                double syncPosX = (int) (2 * c1x - pos[0]);
                double syncPosZ = (int) (pos[1]);

                int c1Y = (int) c1y;
                int c2Y = (int) c2y;

                BlockPos pos1 = new BlockPos((int) curX, c1Y, (int) curZ);
                BlockPos pos2 = new BlockPos((int) syncPosX, c2Y, (int) syncPosZ);
                // 添加到多选区域列表中
                if (onCircle) {//仅创建圆上节点,考虑y坐标
                    BlockPos pos11 = new BlockPos(pos1.getX(), c1Y, pos1.getZ());
                    BlockPos pos12 = new BlockPos(pos1.getX(), c2Y, pos1.getZ());
                    boxList.add(new Box(pos11, pos12, makeKey(pos1.getX(), pos1.getZ())));
//                    addOneBox(pos11, pos12, makeKey(pos1.getX(), pos1.getZ()));

                    BlockPos pos21 = new BlockPos(pos2.getX(), c1Y, pos2.getZ());
                    BlockPos pos22 = new BlockPos(pos2.getX(), c2Y, pos2.getZ());
                    boxList.add(new Box(pos21, pos22, makeKey(pos2.getX(), pos2.getZ())));
//                    addOneBox(pos21, pos22, makeKey(pos2.getX(), pos2.getZ()));
                    if (expand) {
                        seenPosLongSet.add(pos1.asLong());
                        seenPosLongSet.add(pos2.asLong());
                        expandPos(boxList, pos1, c1Y, c2Y,seenPosLongSet);
                        expandPos(boxList,pos2, c1Y, c2Y,seenPosLongSet);
                    }
                } else {
                    String name = "z_" + (int) curZ;// 按照z轴坐标去重
                    boxList.add(new Box(pos1, pos2, name));
//                    addOneBox(pos1, pos2, name);
                }
//                MaLiLib.logger.error(String.format("(%s, %s, %s)|(%s, %s, %s)", curX, cor1.getY(), curZ, syncPosX, cor1.getY(), syncPosZ));
            }
            seenPosLongSet.clear();
            MaLiLib.logger.error(String.format("createNewSubRegionBoxCircle boxList size: %s", boxList.size()));
            Map<String, Box> boxMap = mergeBoxList(boxList,true);
            Map<String, Box> boxMap2 = mergeBoxList(new ArrayList<>(boxMap.values()),false);
            MaLiLib.logger.error(String.format("createNewSubRegionBoxCircle merged size: %s", boxMap.size()));
            if (!onCircle) {// 按照z轴两个点去重,
                Map<String, Box> newBoxMap = new HashMap<>();
                for (Box box : boxMap2.values()) {
                    newBoxMap.put(makeKey(box.getPos1().getZ(), box.getPos2().getZ()), box);
                }
                this.subRegionBoxes.putAll(newBoxMap);
            } else { // 圆圈不存在两个box相同不需要去重
                this.subRegionBoxes.putAll(boxMap2);
            }
            MaLiLib.logger.error(String.format("createNewSubRegionBoxCircle subRegionBoxes size: %s", this.subRegionBoxes.size()));
        } catch (Exception e) {
            MaLiLib.logger.error("createNewSubRegionBoxCircle ", e);
        }
        return "createNewSubRegionBoxCircle";
    }

    protected boolean ifAnyNull(Box box) {
        return box.getPos1() == null || box.getPos2() == null;
    }

    protected boolean isBoxAdjX(Box box1, Box box2) {
        if (Math.abs(box2.getPos1().getX() - box1.getPos2().getX()) > 1 &&
                Math.abs(box1.getPos1().getX() - box2.getPos2().getX()) > 1
        ) {
            return false;
        }
        if (box1.getPos1().getZ() != box2.getPos1().getZ()) {
            return false;
        }
        if (box1.getPos2().getZ() != box2.getPos2().getZ()) {
            return false;
        }
        return true;
    }

    protected boolean isBoxAdjZ(Box box1, Box box2) {
        if (Math.abs(box2.getPos1().getZ() - box1.getPos2().getZ()) > 1 &&
                Math.abs(box1.getPos1().getZ() - box2.getPos2().getZ()) > 1
        ) {
            return false;
        }
        if (box1.getPos1().getX() != box2.getPos1().getX()) {
            return false;
        }
        if (box1.getPos2().getX() != box2.getPos2().getX()) {
            return false;
        }
        return true;
    }
    public int multipleMax(int ... args) {
        int ret = Integer.MIN_VALUE;
        for (int x: args) {
            ret = Math.max(ret, x);
        }
        return ret;
    }

    public int multipleMin(int ... args) {
        int ret = Integer.MAX_VALUE;
        for (int x: args) {
            ret = Math.min(ret, x);
        }
        return ret;
    }
    public List<Integer> getAllCoordinatesByType(CoordinateType type, Box ...boxes) {
        List<Integer> values = new ArrayList<>();
        for (Box box: boxes) {
            values.add(box.getCoordinate(Corner.CORNER_1,type));
            values.add(box.getCoordinate(Corner.CORNER_2,type));
        }
        return values;
    }

    protected Box mergeTwoBoxes(Box box1, Box box2) {
        Box box = new Box();
        /*
         *   |------|12   |-------|22 |------|12
         *   |      |     |       |   |      |
         * 11|------|   21|-------| 11|------|
         */
        int p1x = multipleMin(getAllCoordinatesByType(CoordinateType.X, box1, box2).stream().mapToInt(o -> o).toArray());
        int p1y = multipleMin(getAllCoordinatesByType(CoordinateType.Y, box1, box2).stream().mapToInt(o -> o).toArray());
        int p1z = multipleMin(getAllCoordinatesByType(CoordinateType.Z, box1, box2).stream().mapToInt(o -> o).toArray());

        int p2x = multipleMax(getAllCoordinatesByType(CoordinateType.X, box1, box2).stream().mapToInt(o -> o).toArray());
        int p2y = multipleMax(getAllCoordinatesByType(CoordinateType.Y, box1, box2).stream().mapToInt(o -> o).toArray());
        int p2z = multipleMax(getAllCoordinatesByType(CoordinateType.Z, box1, box2).stream().mapToInt(o -> o).toArray());

        box.setPos1(new BlockPos(p1x, p1y, p1z));
        box.setPos2(new BlockPos(p2x, p2y, p2z));
        return box;
    }

    protected Map<String,Box> mergeBoxList(List<Box> boxList,boolean xFirst) {
        Map<String, Box> boxMap = new HashMap<>();
        if (boxList == null || boxList.isEmpty()) {
            return boxMap;
        }
        Comparator<Box> xFirstFunction = (o1, o2) -> {
            if (ifAnyNull(o1) || ifAnyNull(o2)) {
                return 0;
            }
            if (o1.getPos1().getZ() != o2.getPos1().getZ()) {
                return o1.getPos1().getZ() - o2.getPos1().getZ();
            }
            return o1.getPos1().getX() - o2.getPos1().getX();
        };
        Comparator<Box> zFirstFunction = (o1, o2) -> {
            if (ifAnyNull(o1) || ifAnyNull(o2)) {
                return 0;
            }
            if (o1.getPos1().getX() != o2.getPos1().getX()) {
                return o1.getPos1().getX() - o2.getPos1().getX();
            }
            return o1.getPos1().getZ() - o2.getPos1().getZ();
        };
        Queue<Box> priorityQueue = new PriorityQueue<>(xFirst ? xFirstFunction : zFirstFunction);
        // 坐标归一
        List<Box> newBoxList = boxList.stream().map(box -> {
            BlockPos pos1 = box.getPos1();
            BlockPos pos2 = box.getPos1();
            if (pos1.getX() > pos2.getX() || pos1.getZ() > pos2.getZ()) {
                return new Box(pos2, pos1, box.getName());
            }
            return box;
        }).toList();
        priorityQueue.addAll(newBoxList);// 堆排序
        String keyFmt = "merge_%s";
        int ind = 1;
        while (priorityQueue.size() >= 2) {
            Box curBox = priorityQueue.remove();// one box
            Box nextBox = priorityQueue.remove();// next box
            // x axis, first x equal
            if (isBoxAdjX(curBox, nextBox)) {
                Box merged = mergeTwoBoxes(curBox, nextBox);
                priorityQueue.add(merged);
            } else if (isBoxAdjZ(curBox, nextBox)) { // z axis, second z equal
                Box merged = mergeTwoBoxes(curBox, nextBox);
                priorityQueue.add(merged);
            } else {
                priorityQueue.add(nextBox);
                boxMap.put(keyFmt.formatted(ind), curBox);
                ++ind;
            }
        }
        while (!priorityQueue.isEmpty()) {
            Box box = priorityQueue.remove();
            boxMap.put(keyFmt.formatted(ind), box);
            ++ind;
        }

        return boxMap;

    }

    protected int[][] DIRECTIONS = new int[][]{{0,1},{1,0},{0,-1},{-1,0}};

    protected void expandPos(List<Box> boxList, BlockPos pos, int c1Y, int c2Y,Set<Long> seenPosLongSet) {
        for (int[] dir : DIRECTIONS) {
            BlockPos nextPos = new BlockPos(pos.getX() + dir[0], pos.getY(), pos.getZ() + dir[1]);
            if (seenPosLongSet.contains(nextPos.asLong())) {
                continue;
            }
            seenPosLongSet.add(nextPos.asLong());
            BlockPos pos1 = new BlockPos(nextPos.getX(),c1Y, nextPos.getZ());
            BlockPos pos2 = new BlockPos(nextPos.getX(),c2Y, nextPos.getZ());
            boxList.add(new Box(pos1, pos2,makeKey(nextPos.getX(), nextPos.getZ())));
            //addOneBox(pos1, pos2,makeKey(nextPos.getX(), nextPos.getZ()));
        }
    }
    protected void addOneBox(BlockPos pos1, BlockPos pos2,String name) {
        Box box = new Box();
        box.setSelectedCorner(Corner.CORNER_1);
        box.setName(name);
        this.setSubRegionCornerPos(box, Corner.CORNER_1, pos1);
        this.setSubRegionCornerPos(box, Corner.CORNER_2, pos2);
        this.subRegionBoxes.put(name, box);
    }


    protected String makeKey(int x, int y, int z) {
        return String.format("%s_%s_%s", x, y, z);
    }

    protected String makeKey(int x,int z) {
        return String.format("%s_%s", x, z);
    }

    public void removeAllSubRegion() {
        Box currentSelected = this.getSelectedSubRegionBox();
        this.subRegionBoxes.clear();
        this.addSubRegionBox(currentSelected, false);
    }

    public static void main(String[] args) {
        BlockPos pos1 = new BlockPos(0,0,0);
        BlockPos pos2 = new BlockPos(0,0,500);
        new AreaSelection().createNewSubRegionBoxCircle(pos1, pos2, false, true);
    }

    public void clearCurrentSelectedCorner()
    {
        this.setCurrentSelectedCorner(Corner.NONE);
    }

    public void setCurrentSelectedCorner(Corner corner)
    {
        Box box = this.getSelectedSubRegionBox();

        if (box != null)
        {
            box.setSelectedCorner(corner);
        }
    }

    /**
     * Adds the given SelectionBox, if either replace is true, or there isn't yet a box by the same name.
     * @param box
     * @param replace
     * @return true if the box was successfully added, false if replace was false and there was already a box with the same name
     */
    public boolean addSubRegionBox(Box box, boolean replace)
    {
        if (replace || this.subRegionBoxes.containsKey(box.getName()) == false)
        {
            this.subRegionBoxes.put(box.getName(), box);
            this.markDirty();
            return true;
        }

        return false;
    }

    public void removeAllSubRegionBoxes()
    {
        this.subRegionBoxes.clear();
        this.markDirty();
    }

    public boolean removeSubRegionBox(String name)
    {
        boolean success = this.subRegionBoxes.remove(name) != null;
        this.markDirty();

        if (success && name.equals(this.currentBox))
        {
            this.currentBox = null;
        }

        return success;
    }

    public boolean removeSelectedSubRegionBox()
    {
        boolean success = this.currentBox != null ? this.subRegionBoxes.remove(this.currentBox) != null : false;
        this.currentBox = null;
        this.markDirty();
        return success;
    }

    public boolean renameSubRegionBox(String oldName, String newName)
    {
        return this.renameSubRegionBox(oldName, newName, null);
    }

    public boolean renameSubRegionBox(String oldName, String newName, @Nullable IMessageConsumer feedback)
    {
        Box box = this.subRegionBoxes.get(oldName);

        if (box != null)
        {
            if (this.subRegionBoxes.containsKey(newName))
            {
                if (feedback != null)
                {
                    feedback.addMessage(MessageType.ERROR, "litematica.error.area_editor.rename_sub_region.exists", newName);
                }

                return false;
            }

            this.subRegionBoxes.remove(oldName);
            box.setName(newName);
            this.subRegionBoxes.put(newName, box);

            if (this.currentBox != null && this.currentBox.equals(oldName))
            {
                this.currentBox = newName;
            }

            return true;
        }

        return false;
    }

    public void moveEntireSelectionTo(BlockPos newOrigin, boolean printMessage)
    {
        BlockPos old = this.getEffectiveOrigin();
        BlockPos diff = newOrigin.subtract(old);

        for (Box box : this.subRegionBoxes.values())
        {
            if (box.getPos1() != null)
            {
                this.setSubRegionCornerPos(box, Corner.CORNER_1, box.getPos1().add(diff));
            }

            if (box.getPos2() != null)
            {
                this.setSubRegionCornerPos(box, Corner.CORNER_2, box.getPos2().add(diff));
            }
        }

        if (this.getExplicitOrigin() != null)
        {
            this.setExplicitOrigin(newOrigin);
        }

        if (printMessage)
        {
            String oldStr = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
            String newStr = String.format("x: %d, y: %d, z: %d", newOrigin.getX(), newOrigin.getY(), newOrigin.getZ());
            InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.moved_selection", oldStr, newStr);
        }
    }

    public void moveSelectedElement(Direction direction, int amount)
    {
        Box box = this.getSelectedSubRegionBox();

        if (this.isOriginSelected())
        {
            if (this.getExplicitOrigin() != null)
            {
                this.setExplicitOrigin(this.getExplicitOrigin().offset(direction, amount));
            }
        }
        else if (box != null)
        {
            Corner corner = box.getSelectedCorner();

            if ((corner == Corner.NONE || corner == Corner.CORNER_1) && box.getPos1() != null)
            {
                BlockPos pos = this.getSubRegionCornerPos(box, Corner.CORNER_1).offset(direction, amount);
                this.setSubRegionCornerPos(box, Corner.CORNER_1, pos);
            }

            if ((corner == Corner.NONE || corner == Corner.CORNER_2) && box.getPos2() != null)
            {
                BlockPos pos = this.getSubRegionCornerPos(box, Corner.CORNER_2).offset(direction, amount);
                this.setSubRegionCornerPos(box, Corner.CORNER_2, pos);
            }
        }
    }

    public void setSelectedSubRegionCornerPos(BlockPos pos, Corner corner)
    {
        Box box = this.getSelectedSubRegionBox();

        if (box != null)
        {
            this.setSubRegionCornerPos(box, corner, pos);
        }
    }

    public void setSubRegionCornerPos(Box box, Corner corner, BlockPos pos)
    {
        if (corner == Corner.CORNER_1)
        {
            box.setPos1(pos);
            this.markDirty();
        }
        else if (corner == Corner.CORNER_2)
        {
            box.setPos2(pos);
            this.markDirty();
        }
    }

    public void setCoordinate(@Nullable Box box, Corner corner, CoordinateType type, int value)
    {
        if (box != null && corner != null && corner != Corner.NONE)
        {
            box.setCoordinate(value, corner, type);
            this.markDirty();
        }
        else if (this.explicitOrigin != null)
        {
            this.setExplicitOrigin(PositionUtils.getModifiedPosition(this.explicitOrigin, value, type));
        }
    }

    public BlockPos getSubRegionCornerPos(Box box, Corner corner)
    {
        return corner == Corner.CORNER_2 ? box.getPos2() : box.getPos1();
    }

    public AreaSelection copy()
    {
        return fromJson(this.toJson());
    }

    public static AreaSelection fromJson(JsonObject obj)
    {
        AreaSelection area = new AreaSelection();

        if (JsonUtils.hasArray(obj, "boxes"))
        {
            JsonArray arr = obj.get("boxes").getAsJsonArray();
            final int size = arr.size();

            for (int i = 0; i < size; i++)
            {
                JsonElement el = arr.get(i);

                if (el.isJsonObject())
                {
                    Box box = Box.fromJson(el.getAsJsonObject());

                    if (box != null)
                    {
                        area.subRegionBoxes.put(box.getName(), box);
                    }
                }
            }
        }

        if (JsonUtils.hasString(obj, "name"))
        {
            area.name = obj.get("name").getAsString();
        }

        if (JsonUtils.hasString(obj, "current"))
        {
            area.currentBox = obj.get("current").getAsString();
        }

        BlockPos pos = JsonUtils.blockPosFromJson(obj, "origin");

        if (pos != null)
        {
            area.setExplicitOrigin(pos);
        }
        else
        {
            area.updateCalculatedOrigin();
        }

        return area;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        for (Box box : this.subRegionBoxes.values())
        {
            JsonObject o = box.toJson();

            if (o != null)
            {
                arr.add(o);
            }
        }

        obj.add("name", new JsonPrimitive(this.name));

        if (arr.size() > 0)
        {
            if (this.currentBox != null)
            {
                obj.add("current", new JsonPrimitive(this.currentBox));
            }

            obj.add("boxes", arr);
        }

        if (this.getExplicitOrigin() != null)
        {
            obj.add("origin", JsonUtils.blockPosToJson(this.getExplicitOrigin()));
        }

        return obj;
    }
}
