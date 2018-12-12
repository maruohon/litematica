package fi.dy.masa.litematica.util;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.text.TextFormatting;

public class LayerRange
{
    public static final int WORLD_HORIZONTAL_SIZE_MAX =  30000000;
    public static final int WORLD_HORIZONTAL_SIZE_MIN = -30000000;
    public static final int WORLD_VERTICAL_SIZE_MAX = 255;
    public static final int WORLD_VERTICAL_SIZE_MIN = 0;

    private LayerMode layerMode = LayerMode.ALL;
    private EnumFacing.Axis axis = EnumFacing.Axis.Y;
    private int layerSingle = 0;
    private int layerAbove = 0;
    private int layerBelow = 0;
    private int layerRangeMin = 0;
    private int layerRangeMax = 0;

    public LayerMode getLayerMode()
    {
        return this.layerMode;
    }

    public EnumFacing.Axis getAxis()
    {
        return this.axis;
    }

    public int getLayerSingle()
    {
        return this.layerSingle;
    }

    public int getLayerAbove()
    {
        return this.layerAbove;
    }

    public int getLayerBelow()
    {
        return this.layerBelow;
    }

    public int getLayerRangeMin()
    {
        return this.layerRangeMin;
    }

    public int getLayerRangeMax()
    {
        return this.layerRangeMax;
    }

    public int getLayerMin()
    {
        switch (this.layerMode)
        {
            case ALL:
                return this.getWorldMinValueForAxis(this.axis);
            case SINGLE_LAYER:
                return this.layerSingle;
            case ALL_ABOVE:
                return this.layerAbove;
            case ALL_BELOW:
                return this.getWorldMinValueForAxis(this.axis);
            case LAYER_RANGE:
                return this.layerRangeMin;
            default:
                return WORLD_HORIZONTAL_SIZE_MIN;
        }
    }

    public int getLayerMax()
    {
        switch (this.layerMode)
        {
            case ALL:
                return this.getWorldMaxValueForAxis(this.axis);
            case SINGLE_LAYER:
                return this.layerSingle;
            case ALL_ABOVE:
                return this.getWorldMaxValueForAxis(this.axis);
            case ALL_BELOW:
                return this.layerBelow;
            case LAYER_RANGE:
                return this.layerRangeMax;
            default:
                return WORLD_HORIZONTAL_SIZE_MAX;
        }
    }

    public int getCurrentLayerValue(boolean isSecondValue)
    {
        switch (this.layerMode)
        {
            case SINGLE_LAYER:
                return this.layerSingle;

            case ALL_ABOVE:
                return this.layerAbove;

            case ALL_BELOW:
                return this.layerBelow;

            case LAYER_RANGE:
                return isSecondValue ? this.layerRangeMax : this.layerRangeMin;

            default:
                return 0;
        }
    }

    public int getWorldMinValueForAxis(EnumFacing.Axis axis)
    {
        switch (axis)
        {
            case Y:
                return WORLD_VERTICAL_SIZE_MIN;
            case X:
            case Z:
            default:
                return WORLD_HORIZONTAL_SIZE_MIN;
        }
    }

    public int getWorldMaxValueForAxis(EnumFacing.Axis axis)
    {
        switch (axis)
        {
            case Y:
                return WORLD_VERTICAL_SIZE_MAX;
            case X:
            case Z:
            default:
                return WORLD_HORIZONTAL_SIZE_MAX;
        }
    }

    public void setLayerMode(LayerMode mode)
    {
        this.setLayerMode(mode, true);
    }

    public void setLayerMode(LayerMode mode, boolean printMessage)
    {
        this.layerMode = mode;

        WorldUtils.markAllSchematicChunksForRenderUpdate();

        if (printMessage)
        {
            String val = TextFormatting.GREEN.toString() + mode.getDisplayName();
            StringUtils.printActionbarMessage("litematica.message.set_layer_mode_to", val);
        }
    }

    public void setAxis(EnumFacing.Axis axis)
    {
        this.axis = axis;

        WorldUtils.markAllSchematicChunksForRenderUpdate();
        String val = TextFormatting.GREEN.toString() + axis.getName();
        StringUtils.printActionbarMessage("litematica.message.set_layer_axis_to", val);
    }

    public void setLayerSingle(int layer)
    {
        this.markAffectedLayersForRenderUpdate();
        this.layerSingle = this.getClampedValue(layer);
        this.markAffectedLayersForRenderUpdate();
    }

    public void setLayerAbove(int layer)
    {
        this.markAffectedLayersForRenderUpdate();
        this.layerAbove = this.getClampedValue(layer);
        this.markAffectedLayersForRenderUpdate();
    }

    public void setLayerBelow(int layer)
    {
        this.markAffectedLayersForRenderUpdate();
        this.layerBelow = this.getClampedValue(layer);
        this.markAffectedLayersForRenderUpdate();
    }

    public void setLayerRangeMin(int layer)
    {
        this.setLayerRangeMin(layer, false);
    }

    private void setLayerRangeMin(int layer, boolean force)
    {
        this.markAffectedLayersForRenderUpdate();
        this.layerRangeMin = this.getClampedValue(layer);

        if (force == false)
        {
            this.layerRangeMin = MathHelper.clamp(this.layerRangeMin, this.layerRangeMin, this.layerRangeMax);
        }

        this.markAffectedLayersForRenderUpdate();
    }

    public void setLayerRangeMax(int layer)
    {
        this.setLayerRangeMax(layer, false);
    }

    private void setLayerRangeMax(int layer, boolean force)
    {
        this.markAffectedLayersForRenderUpdate();
        this.layerRangeMax = this.getClampedValue(layer);

        if (force == false)
        {
            this.layerRangeMax = MathHelper.clamp(this.layerRangeMax, this.layerRangeMin, this.layerRangeMax);
        }

        this.markAffectedLayersForRenderUpdate();
    }

    public void setToPosition(Entity entity)
    {
        int pos = 0;

        switch (this.axis)
        {
            case X:
                pos = (int) entity.posX;
                break;
            case Y:
                pos = (int) entity.posY;
                break;
            case Z:
                pos = (int) entity.posZ;
                break;
        }

        switch (this.layerMode)
        {
            case SINGLE_LAYER:
                this.setLayerSingle(pos);
                break;
            case ALL_ABOVE:
                this.setLayerAbove(pos);
                break;
            case ALL_BELOW:
                this.setLayerBelow(pos);
                break;
            case LAYER_RANGE:
                this.setLayerRangeMin(pos, true);
                this.setLayerRangeMax(pos, true);
                break;
            default:
        }
    }

    private void markAffectedLayersForRenderUpdate()
    {
        int val1;
        int val2;

        switch (this.layerMode)
        {
            case ALL:
                WorldUtils.markAllSchematicChunksForRenderUpdate();
                return;
            case SINGLE_LAYER:
            {
                val1 = this.layerSingle - 1;
                val2 = this.layerSingle + 1;
                break;
            }
            case ALL_ABOVE:
            {
                val1 = this.layerAbove - 1;
                val2 = this.axis == EnumFacing.Axis.Y ? WORLD_VERTICAL_SIZE_MAX : WORLD_HORIZONTAL_SIZE_MAX;
                break;
            }
            case ALL_BELOW:
            {
                val1 = this.axis == EnumFacing.Axis.Y ? WORLD_VERTICAL_SIZE_MIN : WORLD_HORIZONTAL_SIZE_MIN;
                val2 = this.layerBelow + 1;
                break;
            }
            case LAYER_RANGE:
            {
                val1 = this.layerRangeMin - 1;
                val2 = this.layerRangeMax + 1;
                break;
            }
            default:
                return;
        }

        switch (this.axis)
        {
            case X:
                WorldUtils.markSchematicChunksForRenderUpdateBetweenX(val1, val2);
                break;
            case Y:
                WorldUtils.markSchematicChunksForRenderUpdateBetweenY(val1, val2);
                break;
            case Z:
                WorldUtils.markSchematicChunksForRenderUpdateBetweenZ(val1, val2);
                break;
        }
    }

    public boolean moveLayer(int amount)
    {
        String strTo = TextFormatting.GREEN.toString() + this.axis.getName().toLowerCase() + " = ";

        switch (this.layerMode)
        {
            case ALL:
                return false;
            case SINGLE_LAYER:
            {
                this.setLayerSingle(this.layerSingle + amount);

                String val = strTo + this.layerSingle;
                StringUtils.printActionbarMessage("litematica.message.set_layer_to", val);
                break;
            }
            case ALL_ABOVE:
            {
                this.setLayerAbove(this.layerAbove + amount);
                String val = strTo + this.layerAbove;
                StringUtils.printActionbarMessage("litematica.message.moved_min_layer_to", val);
                break;
            }
            case ALL_BELOW:
            {
                this.setLayerBelow(this.layerBelow + amount);
                String val = strTo + this.layerBelow;
                StringUtils.printActionbarMessage("litematica.message.moved_max_layer_to", val);
                break;
            }
            case LAYER_RANGE:
            {
                EntityPlayer player = Minecraft.getInstance().player;

                if (player != null)
                {
                    double playerPos = this.axis == Axis.Y ? player.posY : (this.axis == Axis.X ? player.posX : player.posZ);
                    double min = this.layerRangeMin + 0.5D;
                    double max = this.layerRangeMax + 0.5D;
                    String val1;

                    if (Math.abs(playerPos - min) < Math.abs(playerPos - max) || playerPos < min)
                    {
                        this.setLayerRangeMin(this.layerRangeMin + amount);
                        val1 = I18n.format("litematica.message.layer_range.range_min");
                    }
                    else
                    {
                        this.setLayerRangeMax(this.layerRangeMax + amount);
                        val1 = I18n.format("litematica.message.layer_range.range_max");
                    }

                    String val2 = String.valueOf(amount);
                    String val3 = this.axis.getName().toLowerCase();
                    StringUtils.printActionbarMessage("litematica.message.moved_layer_range", val1, val2, val3);
                }

                break;
            }
            default:
        }

        return true;
    }

    private int getClampedValue(int value)
    {
        if (this.axis == EnumFacing.Axis.Y)
        {
            return MathHelper.clamp(value, WORLD_VERTICAL_SIZE_MIN, WORLD_VERTICAL_SIZE_MAX);
        }
        else
        {
            return MathHelper.clamp(value, WORLD_HORIZONTAL_SIZE_MIN, WORLD_HORIZONTAL_SIZE_MAX);
        }
    }

    public boolean isPositionWithinRange(BlockPos pos)
    {
        return this.isPositionWithinRange(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean isPositionWithinRange(int x, int y, int z)
    {
        switch (this.layerMode)
        {
            case ALL:
                return true;

            case SINGLE_LAYER:
            {
                switch (this.axis)
                {
                    case X:
                        return x == this.layerSingle;
                    case Y:
                        return y == this.layerSingle;
                    case Z:
                        return z == this.layerSingle;
                }

                break;
            }

            case ALL_ABOVE:
            {
                switch (this.axis)
                {
                    case X:
                        return x >= this.layerAbove;
                    case Y:
                        return y >= this.layerAbove;
                    case Z:
                        return z >= this.layerAbove;
                }

                break;
            }

            case ALL_BELOW:
            {
                switch (this.axis)
                {
                    case X:
                        return x <= this.layerBelow;
                    case Y:
                        return y <= this.layerBelow;
                    case Z:
                        return z <= this.layerBelow;
                }

                break;
            }

            case LAYER_RANGE:
            {
                switch (this.axis)
                {
                    case X:
                        return x >= this.layerRangeMin && x <= this.layerRangeMax;
                    case Y:
                        return y >= this.layerRangeMin && y <= this.layerRangeMax;
                    case Z:
                        return z >= this.layerRangeMin && z <= this.layerRangeMax;
                }

                break;
            }
        }

        return false;
    }

    public boolean isPositionAtRenderEdgeOnSide(BlockPos pos, EnumFacing side)
    {
        switch (this.axis)
        {
            case X:
                return (side == EnumFacing.WEST && pos.getX() == this.getLayerMin()) || (side == EnumFacing.EAST && pos.getX() == this.getLayerMax());
            case Y:
                return (side == EnumFacing.DOWN && pos.getY() == this.getLayerMin()) || (side == EnumFacing.UP && pos.getY() == this.getLayerMax());
            case Z:
                return (side == EnumFacing.NORTH && pos.getZ() == this.getLayerMin()) || (side == EnumFacing.SOUTH && pos.getZ() == this.getLayerMax());
            default:
                return false;
        }
    }

    public boolean intersects(SubChunkPos pos)
    {
        switch (this.axis)
        {
            case X:
            {
                final int xMin = (pos.getX() << 4);
                final int xMax = (pos.getX() << 4) + 15;
                return (xMax < this.getLayerMin() || xMin > this.getLayerMax()) == false;
            }
            case Y:
            {
                final int yMin = (pos.getY() << 4);
                final int yMax = (pos.getY() << 4) + 15;
                return (yMax < this.getLayerMin() || yMin > this.getLayerMax()) == false;
            }
            case Z:
            {
                final int zMin = (pos.getZ() << 4);
                final int zMax = (pos.getZ() << 4) + 15;
                return (zMax < this.getLayerMin() || zMin > this.getLayerMax()) == false;
            }
            default:
                return false;
        }
    }

    public boolean intersects(MutableBoundingBox box)
    {
        switch (this.axis)
        {
            case X:
            {
                return (box.maxX < this.getLayerMin() || box.minX > this.getLayerMax()) == false;
            }
            case Y:
            {
                return (box.maxY < this.getLayerMin() || box.minY > this.getLayerMax()) == false;
            }
            case Z:
            {
                return (box.maxZ < this.getLayerMin() || box.minZ > this.getLayerMax()) == false;
            }
            default:
                return false;
        }
    }

    @Nullable
    public MutableBoundingBox getClampedRenderBoundingBox(MutableBoundingBox box)
    {
        if (this.intersects(box) == false)
        {
            return null;
        }

        switch (this.axis)
        {
            case X:
            {
                final int xMin = Math.max(box.minX, this.getLayerMin());
                final int xMax = Math.min(box.maxX, this.getLayerMax());
                return MutableBoundingBox.createProper(xMin, box.minY, box.minZ, xMax, box.maxY, box.maxZ);
            }
            case Y:
            {
                final int yMin = Math.max(box.minY, this.getLayerMin());
                final int yMax = Math.min(box.maxY, this.getLayerMax());
                return MutableBoundingBox.createProper(box.minX, yMin, box.minZ, box.maxX, yMax, box.maxZ);
            }
            case Z:
            {
                final int zMin = Math.max(box.minZ, this.getLayerMin());
                final int zMax = Math.min(box.maxZ, this.getLayerMax());
                return MutableBoundingBox.createProper(box.minX, box.minY, zMin, box.maxX, box.maxY, zMax);
            }
            default:
                return null;
        }
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("mode", new JsonPrimitive(this.layerMode.name()));
        obj.add("axis", new JsonPrimitive(this.axis.name()));
        obj.add("layer_single", new JsonPrimitive(this.layerSingle));
        obj.add("layer_above", new JsonPrimitive(this.layerAbove));
        obj.add("layer_below", new JsonPrimitive(this.layerBelow));
        obj.add("layer_range_min", new JsonPrimitive(this.layerRangeMin));
        obj.add("layer_range_max", new JsonPrimitive(this.layerRangeMax));

        return obj;
    }

    public static LayerRange fromJson(JsonObject obj)
    {
        LayerRange range = new LayerRange();

        range.layerMode = LayerMode.fromStringStatic(JsonUtils.getString(obj, "mode"));
        range.axis = EnumFacing.Axis.byName(JsonUtils.getString(obj, "axis"));
        if (range.axis == null) { range.axis = EnumFacing.Axis.Y; }

        range.layerSingle = JsonUtils.getInteger(obj, "layer_single");
        range.layerAbove = JsonUtils.getInteger(obj, "layer_above");
        range.layerBelow = JsonUtils.getInteger(obj, "layer_below");
        range.layerRangeMin = JsonUtils.getInteger(obj, "layer_range_min");
        range.layerRangeMax = JsonUtils.getInteger(obj, "layer_range_max");

        return range;
    }
}
