package fi.dy.masa.litematica.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.MismatchType;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.SortCriteria;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntrySortable;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.PositionUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.LocalRandom;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.List;

public class WidgetSchematicVerificationResult extends WidgetListEntrySortable<BlockMismatchEntry>
{
    private static final LocalRandom RAND = new LocalRandom(0);

    public static final String HEADER_EXPECTED = "litematica.gui.label.schematic_verifier.expected";
    public static final String HEADER_FOUND = "litematica.gui.label.schematic_verifier.found";
    public static final String HEADER_COUNT = "litematica.gui.label.schematic_verifier.count";

    private static int maxNameLengthExpected;
    private static int maxNameLengthFound;
    private static int maxCountLength;

    private final BlockRenderManager blockModelShapes;
    private final GuiSchematicVerifier guiSchematicVerifier;
    private final WidgetListSchematicVerificationResults listWidget;
    private final SchematicVerifier verifier;
    private final BlockMismatchEntry mismatchEntry;
    @Nullable private final String header1;
    @Nullable private final String header2;
    @Nullable private final String header3;
    @Nullable private final BlockMismatchInfo mismatchInfo;
    private final int count;
    private final boolean isOdd;
    @Nullable private final ButtonGeneric buttonIgnore;

    public WidgetSchematicVerificationResult(int x, int y, int width, int height, boolean isOdd,
            WidgetListSchematicVerificationResults listWidget, GuiSchematicVerifier guiSchematicVerifier,
            BlockMismatchEntry entry, int listIndex)
    {
        super(x, y, width, height, entry, listIndex);

        this.columnCount = 3;
        this.blockModelShapes = this.mc.getBlockRenderManager();
        this.mismatchEntry = entry;
        this.guiSchematicVerifier = guiSchematicVerifier;
        this.listWidget = listWidget;
        this.verifier = guiSchematicVerifier.getPlacement().getSchematicVerifier();
        this.isOdd = isOdd;

        // Main header
        if (entry.header1 != null && entry.header2 != null)
        {
            this.header1 = entry.header1;
            this.header2 = entry.header2;
            this.header3 = GuiBase.TXT_BOLD + StringUtils.translate(HEADER_COUNT) + GuiBase.TXT_RST;
            this.mismatchInfo = null;
            this.count = 0;
            this.buttonIgnore = null;
        }
        // Category title
        else if (entry.header1 != null)
        {
            this.header1 = entry.header1;
            this.header2 = null;
            this.header3 = null;
            this.mismatchInfo = null;
            this.count = 0;
            this.buttonIgnore = null;
        }
        // Mismatch entry
        else
        {
            this.header1 = null;
            this.header2 = null;
            this.header3 = null;
            this.mismatchInfo = new BlockMismatchInfo(entry.blockMismatch.stateExpected, entry.blockMismatch.stateFound);
            this.count = entry.blockMismatch.count;

            if (entry.mismatchType != MismatchType.CORRECT_STATE)
            {
                this.buttonIgnore = this.createButton(this.x + this.width, y + 1, ButtonListener.ButtonType.IGNORE_MISMATCH);
            }
            else
            {
                this.buttonIgnore = null;
            }
        }
    }

    public static void setMaxNameLengths(List<BlockMismatch> mismatches)
    {
        maxNameLengthExpected = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate(HEADER_EXPECTED) + GuiBase.TXT_RST);
        maxNameLengthFound    = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate(HEADER_FOUND) + GuiBase.TXT_RST);
        maxCountLength = 7 * StringUtils.getStringWidth("8");

        for (BlockMismatch entry : mismatches)
        {
            ItemStack stack = ItemUtils.getItemForState(entry.stateExpected);
            String name = BlockMismatchInfo.getDisplayName(entry.stateExpected, stack);
            maxNameLengthExpected = Math.max(maxNameLengthExpected, StringUtils.getStringWidth(name));

            stack = ItemUtils.getItemForState(entry.stateFound);
            name = BlockMismatchInfo.getDisplayName(entry.stateFound, stack);
            maxNameLengthFound = Math.max(maxNameLengthFound, StringUtils.getStringWidth(name));
        }

        maxCountLength = Math.max(maxCountLength, StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate(HEADER_COUNT) + GuiBase.TXT_RST));
    }

    private ButtonGeneric createButton(int x, int y, ButtonListener.ButtonType type)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, true, type.getDisplayName());
        return this.addButton(button, new ButtonListener(type, this.mismatchEntry, this.guiSchematicVerifier));
    }

    @Override
    protected int getCurrentSortColumn()
    {
        return this.verifier.getSortCriteria().ordinal();
    }

    @Override
    protected boolean getSortInReverse()
    {
        return this.verifier.getSortInReverse();
    }

    @Override
    protected int getColumnPosX(int column)
    {
        int x1 = this.x + 4;
        int x2 = x1 + maxNameLengthExpected + 40; // including item icon
        int x3 = x2 + maxNameLengthFound + 40;

        return switch (column) {
            case 1 -> x2;
            case 2 -> x3;
            case 3 -> x3 + maxCountLength + 20;
            default -> x1;
        };
    }

    @Override
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton)
    {
        if (super.onMouseClickedImpl(mouseX, mouseY, mouseButton))
        {
            return true;
        }

        if (this.mismatchEntry.type != BlockMismatchEntry.Type.HEADER)
        {
            return false;
        }

        int column = this.getMouseOverColumn(mouseX, mouseY);

        switch (column) {
            case 0 -> this.verifier.setSortCriteria(SortCriteria.NAME_EXPECTED);
            case 1 -> this.verifier.setSortCriteria(SortCriteria.NAME_FOUND);
            case 2 -> this.verifier.setSortCriteria(SortCriteria.COUNT);
            default -> {
                return false;
            }
        }

        // Re-create the widgets
        this.listWidget.refreshEntries();

        return true;
    }

    @Override
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton)
    {
        return (this.buttonIgnore == null || mouseX < this.buttonIgnore.getX()) && super.canSelectAt(mouseX, mouseY, mouseButton);
    }

    protected boolean shouldRenderAsSelected()
    {
        if (this.mismatchEntry.type == BlockMismatchEntry.Type.CATEGORY_TITLE)
        {
            return this.verifier.isMismatchCategorySelected(this.mismatchEntry.mismatchType);
        }
        else if (this.mismatchEntry.type == BlockMismatchEntry.Type.DATA)
        {
            return this.verifier.isMismatchEntrySelected(this.mismatchEntry.blockMismatch);
        }

        return false;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, DrawContext context)
    {
        selected = this.shouldRenderAsSelected();

        // Default color for even entries
        int color = 0xA0303030;

        // Draw a lighter background for the hovered and the selected entry
        if (selected)
        {
            color = 0xA0707070;
        }
        else if (this.isMouseOver(mouseX, mouseY))
        {
            color = 0xA0505050;
        }
        // Draw a slightly darker background for odd entries
        else if (this.isOdd)
        {
            color = 0xA0101010;
        }

        RenderUtils.drawRect(this.x, this.y, this.width, this.height, color);

        if (selected)
        {
            RenderUtils.drawOutline(this.x, this.y, this.width, this.height, 0xFFE0E0E0);
        }

        int x1 = this.getColumnPosX(0);
        int x2 = this.getColumnPosX(1);
        int x3 = this.getColumnPosX(2);
        int y = this.y + 7;
        color = 0xFFFFFFFF;

        if (this.header1 != null && this.header2 != null)
        {
            this.drawString(x1, y, color, this.header1, context);
            this.drawString(x2, y, color, this.header2, context);
            this.drawString(x3, y, color, this.header3, context);

            this.renderColumnHeader(mouseX, mouseY, Icons.ARROW_DOWN, Icons.ARROW_UP);
        }
        else if (this.header1 != null)
        {
            this.drawString(this.x + 4, this.y + 7, color, this.header1, context);
        }
        else if (this.mismatchInfo != null &&
                (this.mismatchEntry.mismatchType != MismatchType.CORRECT_STATE ||
                        !this.mismatchEntry.blockMismatch.stateExpected.isAir()))
        {
            this.drawString(x1 + 20, y, color, this.mismatchInfo.nameExpected, context);

            if (this.mismatchEntry.mismatchType != MismatchType.CORRECT_STATE)
            {
                this.drawString(x2 + 20, y, color, this.mismatchInfo.nameFound, context);
            }

            this.drawString(x3, y, color, String.valueOf(this.count), context);

            y = this.y + 3;
            RenderUtils.drawRect(x1, y, 16, 16, 0x20FFFFFF); // light background for the item

            boolean useBlockModelConfig = Configs.Visuals.SCHEMATIC_VERIFIER_BLOCK_MODELS.getBooleanValue();
            boolean hasModelExpected = this.mismatchInfo.stateExpected.getRenderType() == BlockRenderType.MODEL;
            boolean hasModelFound    = this.mismatchInfo.stateFound.getRenderType() == BlockRenderType.MODEL;
            boolean isAirItemExpected = this.mismatchInfo.stackExpected.isEmpty();
            boolean isAirItemFound    = this.mismatchInfo.stackFound.isEmpty();
            boolean useBlockModelExpected = hasModelExpected && (isAirItemExpected || useBlockModelConfig || this.mismatchInfo.stateExpected.getBlock() == Blocks.FLOWER_POT);
            boolean useBlockModelFound    = hasModelFound    && (isAirItemFound    || useBlockModelConfig || this.mismatchInfo.stateFound.getBlock() == Blocks.FLOWER_POT);

            //RenderUtils.enableDiffuseLightingGui3D();

            BakedModel model;

            if (useBlockModelExpected)
            {
                model = this.blockModelShapes.getModel(this.mismatchInfo.stateExpected);
                renderModelInGui(x1, y, 1, model, this.mismatchInfo.stateExpected, context, this.mc);
            }
            else
            {
                context.drawItem(this.mismatchInfo.stackExpected, x1, y);
                context.drawTooltip(this.textRenderer, Text.of(this.mismatchInfo.nameExpected), x1, y);
            }

            if (this.mismatchEntry.mismatchType != MismatchType.CORRECT_STATE)
            {
                RenderUtils.drawRect(x2, y, 16, 16, 0x20FFFFFF); // light background for the item

                if (useBlockModelFound)
                {
                    model = this.blockModelShapes.getModel(this.mismatchInfo.stateFound);
                    renderModelInGui(x2, y, 1, model, this.mismatchInfo.stateFound, context, this.mc);
                }
                else
                {
                    context.drawItem(this.mismatchInfo.stackFound, x1, y);
                    context.drawTooltip(this.textRenderer, Text.of(this.mismatchInfo.nameFound), x1, y);
                }
            }

            //RenderUtils.disableDiffuseLighting();
            RenderSystem.disableBlend();
        }

        super.render(mouseX, mouseY, selected, context);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, DrawContext context)
    {
        if (this.mismatchInfo != null && this.buttonIgnore != null && mouseX < this.buttonIgnore.getX())
        {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 200);

            int x = mouseX + 10;
            int y = mouseY;
            int width = this.mismatchInfo.getTotalWidth();
            int height = this.mismatchInfo.getTotalHeight();

            if (x + width > GuiUtils.getCurrentScreen().width)
            {
                x = mouseX - width - 10;
            }

            if (y + height > GuiUtils.getCurrentScreen().height)
            {
                y = mouseY - height - 2;
            }

            this.mismatchInfo.render(x, y, this.mc, context);

            context.getMatrices().pop();
        }
    }

    public static class BlockMismatchInfo
    {
        private final BlockState stateExpected;
        private final BlockState stateFound;
        private final ItemStack stackExpected;
        private final ItemStack stackFound;
        private final String blockRegistrynameExpected;
        private final String blockRegistrynameFound;
        private final String nameExpected;
        private final String nameFound;
        private final int totalWidth;
        private final int totalHeight;
        private final int columnWidthExpected;

        public BlockMismatchInfo(BlockState stateExpected, BlockState stateFound)
        {
            this.stateExpected = stateExpected;
            this.stateFound = stateFound;

            this.stackExpected = ItemUtils.getItemForState(this.stateExpected);
            this.stackFound = ItemUtils.getItemForState(this.stateFound);

            Block blockExpected = this.stateExpected.getBlock();
            Block blockFound = this.stateFound.getBlock();
            Identifier rl1 = Registries.BLOCK.getId(blockExpected);
            Identifier rl2 = Registries.BLOCK.getId(blockFound);

            this.blockRegistrynameExpected = rl1.toString();
            this.blockRegistrynameFound = rl2.toString();

            this.nameExpected = getDisplayName(stateExpected, this.stackExpected);
            this.nameFound =    getDisplayName(stateFound,    this.stackFound);

            List<String> propsExpected = BlockUtils.getFormattedBlockStateProperties(this.stateExpected, " = ");
            List<String> propsFound = BlockUtils.getFormattedBlockStateProperties(this.stateFound, " = ");

            int w1 = Math.max(StringUtils.getStringWidth(this.nameExpected) + 20, StringUtils.getStringWidth(this.blockRegistrynameExpected));
            int w2 = Math.max(StringUtils.getStringWidth(this.nameFound) + 20, StringUtils.getStringWidth(this.blockRegistrynameFound));
            w1 = Math.max(w1, fi.dy.masa.litematica.render.RenderUtils.getMaxStringRenderLength(propsExpected));
            w2 = Math.max(w2, fi.dy.masa.litematica.render.RenderUtils.getMaxStringRenderLength(propsFound));

            this.columnWidthExpected = w1;
            this.totalWidth = this.columnWidthExpected + w2 + 40;
            this.totalHeight = Math.max(propsExpected.size(), propsFound.size()) * (StringUtils.getFontHeight() + 2) + 60;
        }

        public static String getDisplayName(BlockState state, ItemStack stack)
        {
            Block block = state.getBlock();
            String key = block.getTranslationKey() + ".name";
            String name = StringUtils.translate(key);
            name = !key.equals(name) ? name : stack.getName().getString();

            return name;
        }

        public int getTotalWidth()
        {
            return this.totalWidth;
        }

        public int getTotalHeight()
        {
            return this.totalHeight;
        }

        public void render(int x, int y, MinecraftClient mc, DrawContext context)
        {
            if (this.stateExpected != null && this.stateFound != null)
            {
                context.getMatrices().push();

                RenderUtils.drawOutlinedBox(x, y, this.totalWidth, this.totalHeight, 0xFF000000, GuiBase.COLOR_HORIZONTAL_BAR);

                int x1 = x + 10;
                int x2 = x + this.columnWidthExpected + 30;
                y += 4;

                TextRenderer textRenderer = mc.textRenderer;
                String pre = GuiBase.TXT_WHITE + GuiBase.TXT_BOLD;
                String strExpected = pre + StringUtils.translate("litematica.gui.label.schematic_verifier.expected") + GuiBase.TXT_RST;
                String strFound = pre + StringUtils.translate("litematica.gui.label.schematic_verifier.found") + GuiBase.TXT_RST;
                context.drawText(textRenderer, strExpected, x1, y, 0xFFFFFFFF, true);
                context.drawText(textRenderer, strFound, x2, y, 0xFFFFFFFF, true);

                y += 12;

                boolean useBlockModelConfig = Configs.Visuals.SCHEMATIC_VERIFIER_BLOCK_MODELS.getBooleanValue();
                boolean hasModelExpected = this.stateExpected.getRenderType() == BlockRenderType.MODEL;
                boolean hasModelFound    = this.stateFound.getRenderType() == BlockRenderType.MODEL;
                boolean isAirItemExpected = this.stackExpected.isEmpty();
                boolean isAirItemFound    = this.stackFound.isEmpty();
                boolean useBlockModelExpected = hasModelExpected && (isAirItemExpected || useBlockModelConfig || this.stateExpected.getBlock() == Blocks.FLOWER_POT);
                boolean useBlockModelFound    = hasModelFound    && (isAirItemFound    || useBlockModelConfig || this.stateFound.getBlock() == Blocks.FLOWER_POT);
                BlockRenderManager blockModelShapes = mc.getBlockRenderManager();

                //mc.getRenderItem().zLevel += 100;
                RenderUtils.drawRect(x1, y, 16, 16, 0x50C0C0C0); // light background for the item
                RenderUtils.drawRect(x2, y, 16, 16, 0x50C0C0C0); // light background for the item

                int iconY = y;

                //RenderSystem.disableBlend();
                //RenderUtils.disableDiffuseLighting();

                context.drawText(textRenderer, this.nameExpected, x1 + 20, y + 4, 0xFFFFFFFF, true);
                context.drawText(textRenderer, this.nameFound, x1 + 20, y + 4, 0xFFFFFFFF, true);

                y += 20;
                context.drawText(textRenderer, this.blockRegistrynameExpected, x1, y, 0xFF4060FF, true);
                context.drawText(textRenderer, this.blockRegistrynameFound, x2, y, 0xFF4060FF, true);

                y += StringUtils.getFontHeight() + 4;

                List<String> propsExpected = BlockUtils.getFormattedBlockStateProperties(this.stateExpected, " = ");
                List<String> propsFound = BlockUtils.getFormattedBlockStateProperties(this.stateFound, " = ");
                RenderUtils.renderText(x1, y, 0xFFB0B0B0, propsExpected, context);
                RenderUtils.renderText(x2, y, 0xFFB0B0B0, propsFound, context);

                BakedModel model;

                //TODO: RenderSystem.disableLighting();
                //RenderUtils.enableDiffuseLightingGui3D();

                if (useBlockModelExpected)
                {
                    model = blockModelShapes.getModel(this.stateExpected);
                    renderModelInGui(x1, iconY, 1, model, this.stateExpected, context, mc);
                }
                else
                {
                    context.drawItem(this.stackExpected, x1, iconY);
                    context.drawTooltip(textRenderer, this.stackExpected.toHoverableText(), x1, iconY);
                }

                if (useBlockModelFound)
                {
                    model = blockModelShapes.getModel(this.stateFound);
                    renderModelInGui(x2, iconY, 1, model, this.stateFound, context, mc);
                }
                else
                {
                    context.drawItem(this.stackFound, x2, iconY);
                    context.drawTooltip(textRenderer, this.stackFound.toHoverableText(), x2, iconY);
                }

                //mc.getRenderItem().zLevel -= 100;

                context.getMatrices().pop();
            }
        }
    }

    public static void renderModelInGui(int x, int y, float z,
                                        BakedModel model, BlockState state,
                                        DrawContext context, MinecraftClient mc)
    {
        if (state.getBlock() == Blocks.AIR)
        {
            return;
        }

        RenderUtils.bindTexture(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        mc.getTextureManager().getTexture(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).setFilter(false, false);

        RenderUtils.setupBlend();
        RenderUtils.color(1f, 1f, 1f, 1f);

        context.getMatrices().push();
        context.getMatrices().translate(x + 8.0, y + 8.0, z + 100.0);
        context.getMatrices().scale(16, -16, 16);

        Quaternionf rot = new Quaternionf().rotationXYZ(30 * (float) (Math.PI / 180.0), 225 * (float) (Math.PI / 180.0), 0.0F);
        context.getMatrices().multiply(rot);
        //matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30));
        //matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(225));
        context.getMatrices().scale(0.625f, 0.625f, 0.625f);
        context.getMatrices().translate(-0.5, -0.5, -0.5);

        renderModel(model, state, context);

        context.getMatrices().pop();
    }

    private static void renderModel(BakedModel model, BlockState state, DrawContext context)
    {
        if (!model.isBuiltin())
        {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(buffer);
            VertexConsumer vertexConsumer = immediate.getBuffer(RenderLayer.getTranslucent());
            MatrixStack.Entry matrixEntry = context.getMatrices().peek();

            int l = LightmapTextureManager.pack(15, 15);
            int[] light = new int[] { l, l, l, l };
            float[] brightness = new float[] { 0.75f, 0.75f, 0.75f, 1.0f };

            RenderSystem.setShader(GameRenderer::getRenderTypeTranslucentProgram);
            DiffuseLighting.enableGuiDepthLighting();

            for (Direction face : PositionUtils.ALL_DIRECTIONS)
            {
                RAND.setSeed(0);
                renderQuads(model.getQuads(state, face, RAND), brightness, light, matrixEntry, vertexConsumer);
            }

            RAND.setSeed(0);
            renderQuads(model.getQuads(state, null, RAND), brightness, light, matrixEntry, vertexConsumer);

            //tessellator.draw();
            immediate.draw();
        }
    }

    private static void renderQuads(List<BakedQuad> quads, float[] brightness, int[] light,
                                    MatrixStack.Entry matrixEntry, VertexConsumer vertexConsumer)
    {
        for (BakedQuad quad : quads)
        {
            renderQuad(quad, brightness, light, matrixEntry, vertexConsumer);
        }
    }

    private static void renderQuad(BakedQuad quad, float[] brightness, int[] light,
                                   MatrixStack.Entry matrixEntry, VertexConsumer vertexConsumer)
    {
        vertexConsumer.quad(matrixEntry, quad, brightness, 1.0f, 1.0f, 1.0f, light, OverlayTexture.DEFAULT_UV, true);
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final ButtonType type;
        private final GuiSchematicVerifier guiSchematicVerifier;
        private final BlockMismatchEntry mismatchEntry;

        public ButtonListener(ButtonType type, BlockMismatchEntry mismatchEntry, GuiSchematicVerifier guiSchematicVerifier)
        {
            this.type = type;
            this.mismatchEntry = mismatchEntry;
            this.guiSchematicVerifier = guiSchematicVerifier;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == ButtonType.IGNORE_MISMATCH)
            {
                this.guiSchematicVerifier.getPlacement().getSchematicVerifier().ignoreStateMismatch(this.mismatchEntry.blockMismatch);
                this.guiSchematicVerifier.initGui();
            }
        }

        public enum ButtonType
        {
            IGNORE_MISMATCH ("litematica.gui.button.schematic_verifier.ignore");

            private final String translationKey;

            ButtonType(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getDisplayName()
            {
                return StringUtils.translate(this.translationKey);
            }
        }
    }
}
