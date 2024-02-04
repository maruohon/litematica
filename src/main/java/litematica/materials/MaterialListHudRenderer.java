package litematica.materials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

import malilib.config.value.HudAlignment;
import malilib.gui.util.GuiUtils;
import malilib.render.RenderContext;
import malilib.render.ShapeRenderUtils;
import malilib.render.buffer.VanillaWrappingVertexBuilder;
import malilib.render.buffer.VertexBuilder;
import malilib.util.StringUtils;
import malilib.util.data.Color4f;
import malilib.util.data.ItemType;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.RenderWrap;
import malilib.util.inventory.InventoryScreenUtils;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.render.infohud.IInfoHudRenderer;
import litematica.render.infohud.RenderPhase;

public class MaterialListHudRenderer implements IInfoHudRenderer
{
    protected final MaterialListBase materialList;
    protected final MaterialListSorter sorter;
    protected boolean shouldRender;
    protected long lastUpdateTime;

    public MaterialListHudRenderer(MaterialListBase materialList)
    {
        this.materialList = materialList;
        this.sorter = new MaterialListSorter(materialList);
    }

    @Override
    public boolean getShouldRenderText(RenderPhase phase)
    {
        return false;
    }

    @Override
    public boolean getShouldRenderCustom()
    {
        return this.shouldRender;
    }

    @Override
    public boolean shouldRenderInGuis()
    {
        return Configs.Generic.RENDER_MATERIALS_IN_GUI.getBooleanValue();
    }

    public void toggleShouldRender()
    {
        this.shouldRender = ! this.shouldRender;
    }

    @Override
    public List<String> getText(RenderPhase phase)
    {
        return Collections.emptyList();
    }

    protected void refreshList(long refreshInterval)
    {
        long currentTime = System.nanoTime();

        if (currentTime - this.lastUpdateTime > refreshInterval * 1000000L && GameWrap.getClientPlayer() != null)
        {
            MaterialListUtils.updateAvailableCounts(this.materialList.getAllMaterials());
            List<MaterialListEntry> list = this.materialList.getMissingMaterials(true);
            list.sort(this.sorter);
            this.lastUpdateTime = currentTime;
        }
    }

    @Override
    public int render(int xOffset, int yOffset, HudAlignment alignment, RenderContext ctx)
    {
        this.refreshList(2000L);

        List<MaterialListEntry> list = this.materialList.getMissingMaterials(false);

        if (list.size() == 0)
        {
            return 0;
        }

        FontRenderer font = GameWrap.getClient().fontRenderer;
        final double scale = Configs.InfoOverlays.MATERIAL_LIST_HUD_SCALE.getDoubleValue();
        final int maxLines = Configs.InfoOverlays.MATERIAL_LIST_HUD_MAX_LINES.getIntegerValue();
        int bgMargin = 2;
        int lineHeight = 16;
        int contentHeight = (Math.min(list.size(), maxLines) * lineHeight) + bgMargin + 10;
        int maxTextLength = 0;
        int maxCountLength = 0;
        int posX = xOffset + bgMargin;
        int posY = yOffset + bgMargin;
        int bgColor = 0xA0000000;
        int textColor = 0xFFFFFFFF;
        boolean useBackground = true;
        boolean useShadow = false;
        final int size = Math.min(list.size(), maxLines);

        // Only Chuck Norris can divide by zero
        if (scale == 0d)
        {
            return 0;
        }

        for (int i = 0; i < size; ++i)
        {
            MaterialListEntry entry = list.get(i);
            maxTextLength = Math.max(maxTextLength, font.getStringWidth(entry.getStack().getDisplayName()));
            long multiplier = this.materialList.getMultiplier();
            long count = multiplier == 1L ? entry.getMissingCount() - entry.getAvailableCount() : entry.getTotalCount();
            count *= multiplier;
            String strCount = this.getFormattedCountString(count, entry.getStack().getMaxStackSize());
            maxCountLength = Math.max(maxCountLength, font.getStringWidth(strCount));
        }

        final int maxLineLength = maxTextLength + maxCountLength + 30;

        if (alignment == HudAlignment.TOP_RIGHT || alignment == HudAlignment.BOTTOM_RIGHT)
        {
            posX = (int) ((GuiUtils.getScaledWindowWidth() / scale) - maxLineLength - xOffset - bgMargin);
        }
        else if (alignment == HudAlignment.CENTER)
        {
            posX = (int) ((GuiUtils.getScaledWindowWidth() / scale / 2) - (maxLineLength / 2) - xOffset);
        }

        if (scale != 1 && scale != 0)
        {
            yOffset = (int) (yOffset / scale);
        }

        posY = GuiUtils.getHudPosY(posY, yOffset, contentHeight, scale, alignment);
        posY += GuiUtils.getHudOffsetForPotions(alignment, scale, GameWrap.getClientPlayer());

        if (scale != 1d)
        {
            RenderWrap.pushMatrix(ctx);
            RenderWrap.scale(scale, scale, scale, ctx);
        }

        if (useBackground)
        {
            ShapeRenderUtils.renderRectangle(posX - bgMargin, posY - bgMargin,
                                             0, maxLineLength + bgMargin * 2, contentHeight + bgMargin, bgColor, ctx);
        }

        String title = StringUtils.translate("litematica.title.hud.material_list");

        if (useShadow)
        {
            font.drawStringWithShadow(title, posX + 2, posY + 2, textColor);
        }
        else
        {
            font.drawString(title, posX + 2, posY + 2, textColor);
        }

        posY += 12;
        final int itemCountTextColor = Configs.Colors.MATERIAL_LIST_HUD_ITEM_COUNTS.getIntegerValue();
        int x = posX + 18;
        int y = posY + 4;

        for (int i = 0; i < size; ++i)
        {
            MaterialListEntry entry = list.get(i);
            String text = entry.getStack().getDisplayName();
            long multiplier = this.materialList.getMultiplier();
            long count = multiplier == 1L ? entry.getMissingCount() - entry.getAvailableCount() : entry.getTotalCount();
            count *= multiplier;
            String strCount = this.getFormattedCountString(count, entry.getStack().getMaxStackSize());
            int cntLen = font.getStringWidth(strCount);
            int cntPosX = posX + maxLineLength - cntLen - 2;

            if (useShadow)
            {
                font.drawStringWithShadow(text, x, y, textColor);
                font.drawStringWithShadow(strCount, cntPosX, y, itemCountTextColor);
            }
            else
            {
                font.drawString(text, x, y, textColor);
                font.drawString(strCount, cntPosX, y, itemCountTextColor);
            }

            y += lineHeight;
        }

        x = posX;
        y = posY;

        RenderWrap.enableRescaleNormal();
        RenderWrap.setupBlendSeparate();
        RenderWrap.enableGuiItemLighting(ctx);

        for (int i = 0; i < size; ++i)
        {
            GameWrap.getClient().getRenderItem().renderItemAndEffectIntoGUI(GameWrap.getClientPlayer(), list.get(i).getStack(), x, y);
            y += lineHeight;
        }

        RenderWrap.disableItemLighting();
        RenderWrap.disableRescaleNormal();
        RenderWrap.disableBlend();

        if (scale != 1d)
        {
            RenderWrap.popMatrix(ctx);
        }

        return contentHeight + 4;
    }

    protected String getFormattedCountString(long count, int maxStackSize)
    {
        long stacks = count / maxStackSize;
        long remainder = count % maxStackSize;
        double boxCount = (double) count / (27D * maxStackSize);

        if (count > maxStackSize)
        {
            if (boxCount >= 1.0)
            {
                return String.format("%d (%.2f %s)", count, boxCount, StringUtils.translate("litematica.gui.label.material_list.abbr.shulker_box"));
            }
            else if (Configs.InfoOverlays.MATERIAL_LIST_HUD_STACKS.getBooleanValue())
            {
                if (remainder > 0)
                {
                    return String.format("%d (%d x %d + %d)", count, stacks, maxStackSize, remainder);
                }
                else
                {
                    return String.format("%d (%d x %d)", count, stacks, maxStackSize);
                }
            }
        }

        return String.format("%d", count);
    }

    public static void renderSlotHighlights(GuiContainer gui)
    {
        MaterialListBase materialList = DataManager.getMaterialList();

        if (materialList != null)
        {
            materialList.getHudRenderer().refreshList(2000L);
            List<MaterialListEntry> list = materialList.getMissingMaterials(false);

            if (list.isEmpty() == false)
            {
                HashMap<ItemType, MaterialListEntry> map = new HashMap<>();
                list.forEach((entry) -> map.put(entry.getItemType(), entry));
                List<Pair<Slot, Color4f>> highlightedSlots = getHighlightedSlots(gui, map);

                if (highlightedSlots.isEmpty() == false)
                {
                    RenderWrap.disableTexture2D();
                    RenderWrap.setupBlendSeparate();
                    int guiX = InventoryScreenUtils.getGuiPosX(gui);
                    int guiY = InventoryScreenUtils.getGuiPosY(gui);
                    VertexBuilder builder = VanillaWrappingVertexBuilder.coloredQuads();

                    for (Pair<Slot, Color4f> pair : highlightedSlots)
                    {
                        Slot slot = pair.getLeft();
                        Color4f color = pair.getRight();
                        ShapeRenderUtils.renderOutlinedRectangle(guiX + slot.xPos, guiY + slot.yPos, 1f,
                                                                 16, 16, color.intValue, color.intValue | 0xFF000000, builder);
                    }

                    builder.draw();
                    RenderWrap.enableTexture2D();
                }
            }
        }
    }

    private static List<Pair<Slot, Color4f>> getHighlightedSlots(GuiContainer gui, HashMap<ItemType, MaterialListEntry> materialListEntries)
    {
        List<Pair<Slot, Color4f>> highlightedSlots = new ArrayList<>();

        for (int slotNum = 0; slotNum < gui.inventorySlots.inventorySlots.size(); ++slotNum)
        {
            Slot slot = gui.inventorySlots.inventorySlots.get(slotNum);

            if (slot.isEnabled() && slot.getHasStack() && (slot.inventory instanceof InventoryPlayer) == false)
            {
                ItemType type = new ItemType(slot.getStack(), false, false);
                MaterialListEntry entry = materialListEntries.get(type);

                // The item in the slot is on the material list's missing items list
                if (entry != null)
                {
                    long available = entry.getAvailableCount();
                    Color4f color;

                    if (available == 0L)
                    {
                        color = Configs.Colors.MATERIAL_LIST_SLOT_HL_NONE.getColor();
                    }
                    else if (available < entry.getStack().getMaxStackSize())
                    {
                        color = Configs.Colors.MATERIAL_LIST_SLOT_HL_LT_STACK.getColor();
                    }
                    else
                    {
                        color = Configs.Colors.MATERIAL_LIST_SLOT_HL_NOT_ENOUGH.getColor();
                    }

                    highlightedSlots.add(Pair.of(slot, color));
                }
            }
        }

        return highlightedSlots;
    }
}
