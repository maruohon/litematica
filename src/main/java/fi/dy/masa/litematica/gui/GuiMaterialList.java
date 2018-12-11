package fi.dy.masa.litematica.gui;

import java.io.File;
import java.util.List;
import com.mumfrey.liteloader.core.LiteLoader;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.button.ButtonOnOff;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.render.InfoHud;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.data.DataDump;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetInfoIcon;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class GuiMaterialList extends GuiListBase<MaterialListEntry, WidgetMaterialListEntry, WidgetListMaterialList>
{
    private final MaterialListBase materialList;
    private int id;

    public GuiMaterialList(MaterialListBase materialList)
    {
        super(10, 44);

        this.materialList = materialList;
        this.title = this.materialList.getDisplayName();

        Minecraft mc = Minecraft.getMinecraft();

        MaterialListUtils.updateAvailableCounts(this.materialList.getMaterialsAll(), mc.player);
        WidgetMaterialListEntry.setMaxNameLength(materialList.getMaterialsAll(), mc);

        // Remember the last opened material list, for the hotkey
        if (DataManager.getMaterialList() == null)
        {
            DataManager.setMaterialList(materialList);
        }
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 80;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 12;
        int y = 24;
        int buttonWidth;
        this.id = 0;
        String label;
        ButtonGeneric button;

        this.addWidget(new WidgetInfoIcon(this.width - 23, 12, this.zLevel, Icons.INFO_11, "litematica.info.material_list"));

        x += this.createButton(x, y, -1, ButtonListener.Type.REFRESH_LIST) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.LIST_TYPE) + 4;
        x += this.createButtonOnOff(x, y, -1, this.materialList.getHideAvailable(), ButtonListener.Type.HIDE_AVAILABLE) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.TOGGLE_INFO_HUD) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.WRITE_TO_FILE) + 4;
        y += 22;

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
        ButtonListener listener = new ButtonListener(type, this);
        String label = "";

        if (type == ButtonListener.Type.TOGGLE_INFO_HUD)
        {
            boolean val = InfoHud.getInstance().isEnabled();
            String str = (val ? TXT_GREEN : TXT_RED) + I18n.format("litematica.message.value." + (val ? "on" : "off")) + TXT_RST;
            label = type.getDisplayName(str);
        }
        else if (type == ButtonListener.Type.LIST_TYPE)
        {
            label = type.getDisplayName(this.materialList.getMaterialListType().getDisplayName());
        }
        else
        {
            label = type.getDisplayName();
        }

        if (width == -1)
        {
            width = this.mc.fontRenderer.getStringWidth(label) + 10;
        }

        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, width, 20, label);
        this.addButton(button, listener);

        return width;
    }

    private int createButtonOnOff(int x, int y, int width, boolean isCurrentlyOn, ButtonListener.Type type)
    {
        ButtonOnOff button = ButtonOnOff.create(x, y, width, false, type.getTranslationKey(), isCurrentlyOn);
        this.addButton(button, new ButtonListener(type, this));
        return button.getButtonWidth();
    }

    public MaterialListBase getMaterialList()
    {
        return this.materialList;
    }

    @Override
    protected WidgetListMaterialList createListWidget(int listX, int listY)
    {
        return new WidgetListMaterialList(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiMaterialList parent;
        private final Type type;

        public ButtonListener(Type type, GuiMaterialList parent)
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
            switch (this.type)
            {
                case REFRESH_LIST:
                    this.parent.materialList.refreshMaterialList();
                    break;

                case LIST_TYPE:
                    BlockInfoListType type = this.parent.materialList.getMaterialListType();
                    this.parent.materialList.setMaterialListType((BlockInfoListType) type.cycle(mouseButton == 0));
                    this.parent.materialList.refreshMaterialList();
                    break;

                case HIDE_AVAILABLE:
                    this.parent.materialList.setHideAvailable(! this.parent.materialList.getHideAvailable());
                    this.parent.materialList.refreshMaterialList();
                    break;

                case TOGGLE_INFO_HUD:
                    break;

                case WRITE_TO_FILE:
                    File dir = new File(LiteLoader.getCommonConfigFolder(), Reference.MOD_ID);
                    DataDump dump = new DataDump(2, DataDump.Format.ASCII);
                    this.addLinesToDump(dump, this.parent.materialList.getMaterialsAll());
                    File file = DataDump.dumpDataToFile(dir, "material_list", dump.getLines());

                    if (file != null)
                    {
                        String key = "litematica.message.material_list_written_to_file";
                        this.parent.addMessage(MessageType.SUCCESS, key, file.getName());
                        StringUtils.sendOpenFileChatMessage(this.parent.mc.player, key, file);
                    }
                    break;
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        private void addLinesToDump(DataDump dump, List<MaterialListEntry> materials)
        {
            for (MaterialListEntry entry : materials)
            {
                dump.addData(entry.getStack().getDisplayName(), String.valueOf(entry.getCountTotal()));
            }

            dump.addTitle("Item", "Total");
            dump.setColumnProperties(1, DataDump.Alignment.RIGHT, true); // total
            dump.setSort(true);
            dump.setUseColumnSeparator(true);
        }

        public enum Type
        {
            REFRESH_LIST        ("litematica.gui.button.material_list.refresh_list"),
            LIST_TYPE           ("litematica.gui.button.material_list.list_type"),
            HIDE_AVAILABLE      ("litematica.gui.button.material_list.hide_available"),
            TOGGLE_INFO_HUD     ("litematica.gui.button.material_list.toggle_info_hud"),
            WRITE_TO_FILE       ("litematica.gui.button.material_list.write_to_file");

            private final String translationKey;

            private Type(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getTranslationKey()
            {
                return this.translationKey;
            }

            public String getDisplayName(Object... args)
            {
                return I18n.format(this.translationKey, args);
            }
        }
    }
}
