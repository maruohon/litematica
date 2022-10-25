package litematica.network;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.Unpooled;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import malilib.network.PacketSplitter;
import malilib.network.message.BasePacketHandler;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.game.wrap.GameUtils;
import malilib.util.game.wrap.NbtWrap;
import malilib.util.nbt.NbtUtils;
import litematica.scheduler.TaskScheduler;
import litematica.schematic.ISchematic;
import litematica.schematic.util.SchematicSaveSettings;
import litematica.selection.AreaSelection;
import litematica.task.MultiplayerCreateSchematicTask;

public class SchematicSavePacketHandler extends BasePacketHandler
{
    public static final SchematicSavePacketHandler INSTANCE = new SchematicSavePacketHandler();

    protected final ResourceLocation channelId = new ResourceLocation("litematica:save");
    protected final List<ResourceLocation> channels = ImmutableList.of(this.channelId);
    protected final HashMap<UUID, MultiplayerCreateSchematicTask> pendingSaveTasks = new HashMap<>();

    protected SchematicSavePacketHandler()
    {
        this.usePacketSplitter = true;
    }

    @Override
    public List<ResourceLocation> getChannels()
    {
        return this.channels;
    }

    @Override
    public void onPacketReceived(PacketBuffer buf)
    {
        try
        {
            NBTTagCompound tag = buf.readCompoundTag();
            UUID taskId = NbtUtils.readUUID(tag);

            if (taskId != null)
            {
                MultiplayerCreateSchematicTask task = this.pendingSaveTasks.get(taskId);

                if (task != null)
                {
                    task.onReceiveData(tag);
                    this.removeSaveTask(taskId);
                }
            }
        }
        catch (Exception e)
        {
            MessageDispatcher.error("litematica.message.error.schematic_save.server_side.failed_reading_data");
            MessageDispatcher.error(e.getMessage());
        }
    }

    public void removeSaveTask(UUID taskId)
    {
        this.pendingSaveTasks.remove(taskId);
    }

    public void requestSchematicSaveAllAtOnce(AreaSelection selection,
                                              SchematicSaveSettings settings,
                                              Consumer<ISchematic> listener)
    {
        this.requestSchematicSave(selection, settings, "AllAtOnce", listener);
    }

    public void requestSchematicSavePerChunk(AreaSelection selection,
                                             SchematicSaveSettings settings,
                                             Consumer<ISchematic> listener)
    {
        this.requestSchematicSave(selection, settings, "PerChunk", listener);
    }

    protected void requestSchematicSave(AreaSelection selection,
                                        SchematicSaveSettings settings,
                                        String saveMethod,
                                        Consumer<ISchematic> listener)
    {
        NetHandlerPlayClient handler = GameUtils.getClient().getConnection();
        UUID taskId = UUID.randomUUID();

        if (handler != null && this.sendSaveRequestPacket(selection, settings, taskId, saveMethod, handler))
        {
            MultiplayerCreateSchematicTask task = new MultiplayerCreateSchematicTask(selection, taskId, listener);
            this.pendingSaveTasks.put(taskId, task);
            TaskScheduler.getInstanceClient().scheduleTask(task, 10);
        }
    }

    protected boolean sendSaveRequestPacket(AreaSelection selection,
                                            SchematicSaveSettings settings,
                                            UUID taskId,
                                            String saveMethod,
                                            NetHandlerPlayClient handler)
    {
        NBTTagCompound dataTypesTag = new NBTTagCompound();
        NbtWrap.putBoolean(dataTypesTag, "Blocks", settings.saveBlocks.getBooleanValue());
        NbtWrap.putBoolean(dataTypesTag, "BlockEntities", settings.saveBlockEntities.getBooleanValue());
        NbtWrap.putBoolean(dataTypesTag, "BlockTicks", settings.saveBlockTicks.getBooleanValue());
        NbtWrap.putBoolean(dataTypesTag, "Entities", settings.saveEntities.getBooleanValue());
        NbtWrap.putBoolean(dataTypesTag, "ExposedBlocksOnly", settings.exposedBlocksOnly.getBooleanValue());

        NBTTagCompound tag = new NBTTagCompound();
        NbtUtils.writeUUID(tag, taskId);
        NbtWrap.putString(tag, "SaveMethod", saveMethod);
        NbtWrap.putTag(tag, "RequestedData", dataTypesTag);
        NbtWrap.putTag(tag, "Regions", selection.getSubRegionsAsCompound());

        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeCompoundTag(tag);
        PacketSplitter.send(this.channelId, buf, handler);

        return true;
    }
}
