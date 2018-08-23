package fi.dy.masa.litematica.gui;

import java.util.Collections;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.ICompletionListener;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.data.SchematicVerifier;
import fi.dy.masa.litematica.data.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.data.SchematicVerifier.MismatchType;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.litematica.gui.base.GuiListBase;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResults;
import fi.dy.masa.litematica.render.InfoHud;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class GuiSchematicVerifier   extends GuiListBase<BlockMismatchEntry, WidgetSchematicVerificationResult, WidgetSchematicVerificationResults>
                                    implements ISelectionListener<BlockMismatchEntry>, ICompletionListener
{
    private final SchematicPlacement placement;
    @Nullable
    private BlockMismatchEntry selectedDataEntry;
    private int id;
    // static to remember the mode over GUI close/open cycles
    private static MismatchType resultMode = MismatchType.ALL;

    public GuiSchematicVerifier(SchematicPlacement placement)
    {
        super(10, 60);

        this.title = I18n.format("litematica.gui.title.schematic_verifier");
        this.placement = placement;
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 94;
    }

    @Override
    public void initGui()
    {
        WidgetSchematicVerificationResult.resetNameLengths();

        super.initGui();

        int x = 12;
        int y = 20;
        int buttonWidth;
        this.id = 0;
        String label;
        ButtonGeneric button;

        x += this.createButton(x, y, -1, ButtonListener.Type.START) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.STOP) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.RESET_VERIFIER) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.RESET_IGNORED) + 4;
        this.createButton(x, y, -1, ButtonListener.Type.TOGGLE_INFO_HUD);
        y += 22;

        x = 12;
        x += this.createButton(x, y, -1, ButtonListener.Type.SET_RESULT_MODE_ALL) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.SET_RESULT_MODE_WRONG_BLOCKS) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.SET_RESULT_MODE_WRONG_STATES) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.SET_RESULT_MODE_EXTRA) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.SET_RESULT_MODE_MISSING) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.SET_RESULT_MODE_CORRECT) + 4;

        y = this.height - 36;
        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = I18n.format(type.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));
    }

    private int createButton(int x, int y, int width, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this.placement, this);
        boolean enabled = true;
        String label = "";

        switch (type)
        {
            case SET_RESULT_MODE_ALL:
                label = MismatchType.ALL.getDisplayname();
                enabled = resultMode != MismatchType.ALL;
                break;

            case SET_RESULT_MODE_WRONG_BLOCKS:
                label = MismatchType.WRONG_BLOCK.getDisplayname();
                enabled = resultMode != MismatchType.WRONG_BLOCK;
                break;

            case SET_RESULT_MODE_WRONG_STATES:
                label = MismatchType.WRONG_STATE.getDisplayname();
                enabled = resultMode != MismatchType.WRONG_STATE;
                break;

            case SET_RESULT_MODE_EXTRA:
                label = MismatchType.EXTRA.getDisplayname();
                enabled = resultMode != MismatchType.EXTRA;
                break;

            case SET_RESULT_MODE_MISSING:
                label = MismatchType.MISSING.getDisplayname();
                enabled = resultMode != MismatchType.MISSING;
                break;

            case SET_RESULT_MODE_CORRECT:
                label = MismatchType.CORRECT_STATE.getDisplayname();
                enabled = resultMode != MismatchType.CORRECT_STATE;
                break;

            case START:
                if (this.placement.getSchematicVerifier().isPaused())
                {
                    label = I18n.format("litematica.gui.button.schematic_verifier.resume");
                }
                else
                {
                    label = I18n.format("litematica.gui.button.schematic_verifier.start");
                    enabled = this.placement.getSchematicVerifier().isActive() == false;
                }
                break;

            case STOP:
                label = I18n.format("litematica.gui.button.schematic_verifier.stop");
                enabled = this.placement.getSchematicVerifier().isActive();
                break;

            case RESET_VERIFIER:
            {
                label = I18n.format("litematica.gui.button.schematic_verifier.reset_verifier");
                SchematicVerifier verifier = this.placement.getSchematicVerifier();
                enabled = verifier.isActive() || verifier.isPaused() || verifier.isFinished();
                break;
            }

            case RESET_IGNORED:
                label = I18n.format("litematica.gui.button.schematic_verifier.reset_ignored");
                enabled = this.placement.getSchematicVerifier().getIgnoredMismatches().size() > 0;
                break;

            case TOGGLE_INFO_HUD:
            {
                boolean val = InfoHud.getInstance().isEnabled();
                String str = (val ? TXT_GREEN : TXT_RED) + I18n.format("litematica.message.value." + (val ? "on" : "off")) + TXT_RST;
                label = I18n.format("litematica.gui.button.schematic_verifier.toggle_info_hud", str);
                //int buttonWidth = this.mc.fontRenderer.getStringWidth(label);
                //x = this.width - buttonWidth + 10;
                break;
            }

            default:
        }

        if (width == -1)
        {
            width = this.mc.fontRenderer.getStringWidth(label) + 10;
        }

        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, width, 20, label);
        button.enabled = enabled;
        this.addButton(button, listener);

        return width;
    }

    public SchematicPlacement getPlacement()
    {
        return this.placement;
    }

    public MismatchType getResultMode()
    {
        return resultMode;
    }

    private void setResultMode(MismatchType type)
    {
        resultMode = type;
    }

    @Override
    public void onTaskCompleted()
    {
        if (this.mc.currentScreen == this)
        {
            this.initGui();
        }
    }

    @Override
    public void onSelectionChange(@Nullable BlockMismatchEntry entry)
    {
        if (entry != null)
        {
            SchematicVerifier verifier = this.placement.getSchematicVerifier();

            // Clear the selection when clicking again on the selected entry
            if (this.selectedDataEntry == entry)
            {
                this.selectedDataEntry = null;
                this.widget.clearSelection();
                this.placement.getSchematicVerifier().clearActiveMismatchRenderPositions();
            }
            // Main header - show the currently missing/unseen chunks
            else if (entry.type == BlockMismatchEntry.Type.HEADER)
            {
                verifier.updateRequiredChunksStringList();
            }
            // Category title - show all mismatches of that type
            else if (entry.type == BlockMismatchEntry.Type.CATEGORY_TITLE)
            {
                this.selectedDataEntry = entry;
                verifier.updateMismatchOverlaysForType(entry.mismatchType, null);
            }
            // A specific mismatch pair - show only those state pairs
            else if (entry.type == BlockMismatchEntry.Type.DATA)
            {
                this.selectedDataEntry = entry;
                verifier.updateMismatchOverlaysForType(entry.mismatchType, entry.blockMismatch);
            }
        }
    }

    @Override
    protected ISelectionListener<BlockMismatchEntry> getSelectionListener()
    {
        return this;
    }

    @Override
    protected WidgetSchematicVerificationResults createListWidget(int listX, int listY)
    {
        return new WidgetSchematicVerificationResults(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    public static class BlockMismatchEntry
    {
        public final Type type;
        @Nullable
        public final MismatchType mismatchType;
        @Nullable
        public final BlockMismatch blockMismatch;
        @Nullable
        public final String header1;
        @Nullable
        public final String header2;

        public BlockMismatchEntry(MismatchType mismatchType, String title)
        {
            this.type = Type.CATEGORY_TITLE;
            this.mismatchType = mismatchType;
            this.blockMismatch = null;
            this.header1 = title;
            this.header2 = null;
        }

        public BlockMismatchEntry(String header1, String header2)
        {
            this.type = Type.HEADER;
            this.mismatchType = null;
            this.blockMismatch = null;
            this.header1 = header1;
            this.header2 = header2;
        }

        public BlockMismatchEntry(MismatchType mismatchType, BlockMismatch blockMismatch)
        {
            this.type = Type.DATA;
            this.mismatchType = mismatchType;
            this.blockMismatch = blockMismatch;
            this.header1 = null;
            this.header2 = null;
        }

        public enum Type
        {
            HEADER,
            CATEGORY_TITLE,
            DATA;
        }
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiSchematicVerifier parent;
        private final Type type;

        public ButtonListener(Type type, SchematicPlacement placement, GuiSchematicVerifier parent)
        {
            this.parent = parent;
            this.type = type;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            Minecraft mc = Minecraft.getMinecraft();

            switch (this.type)
            {
                case SET_RESULT_MODE_ALL:
                    this.parent.setResultMode(MismatchType.ALL);
                    break;

                case SET_RESULT_MODE_WRONG_BLOCKS:
                    this.parent.setResultMode(MismatchType.WRONG_BLOCK);
                    break;

                case SET_RESULT_MODE_WRONG_STATES:
                    this.parent.setResultMode(MismatchType.WRONG_STATE);
                    break;

                case SET_RESULT_MODE_EXTRA:
                    this.parent.setResultMode(MismatchType.EXTRA);
                    break;

                case SET_RESULT_MODE_MISSING:
                    this.parent.setResultMode(MismatchType.MISSING);
                    break;

                case SET_RESULT_MODE_CORRECT:
                    this.parent.setResultMode(MismatchType.CORRECT_STATE);
                    break;

                case START:
                    WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

                    if (world != null)
                    {
                        SchematicVerifier verifier = this.parent.placement.getSchematicVerifier();

                        if (this.parent.placement.getSchematicVerifier().isPaused())
                        {
                            verifier.resume();
                        }
                        else
                        {
                            verifier.startVerification(mc.world, world, this.parent.placement, this.parent);
                        }
                    }
                    else
                    {
                        this.parent.addMessage(InfoType.ERROR, "litematica.error.generic.schematic_world_not_loaded");
                    }
                    break;

                case STOP:
                    this.parent.placement.getSchematicVerifier().stopVerification();
                    DataManager.removeSchematicVerificationTask();
                    break;

                case RESET_VERIFIER:
                    this.parent.placement.getSchematicVerifier().reset();
                    DataManager.removeSchematicVerificationTask();
                    break;

                case RESET_IGNORED:
                    this.parent.placement.getSchematicVerifier().setIgnoredStateMismatches(Collections.emptyList());
                    break;

                case TOGGLE_INFO_HUD:
                    InfoHud.getInstance().toggleEnabled();
                    break;
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        public enum Type
        {
            SET_RESULT_MODE_ALL,
            SET_RESULT_MODE_WRONG_BLOCKS,
            SET_RESULT_MODE_WRONG_STATES,
            SET_RESULT_MODE_EXTRA,
            SET_RESULT_MODE_MISSING,
            SET_RESULT_MODE_CORRECT,
            START,
            STOP,
            RESET_VERIFIER,
            RESET_IGNORED,
            TOGGLE_INFO_HUD;
        }
    }
}
