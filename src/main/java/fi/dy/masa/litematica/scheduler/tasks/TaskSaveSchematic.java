package fi.dy.masa.litematica.scheduler.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.render.infohud.RenderPhase;
import fi.dy.masa.litematica.scheduler.TaskBase;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class TaskSaveSchematic extends TaskBase implements IInfoHudRenderer
{
    private final LitematicaSchematic schematic;
    private final BlockPos origin;
    private final ImmutableMap<String, Box> subRegions;
    private final Set<ChunkPos> requiredChunks = new HashSet<>();
    private final Set<UUID> existingEntities = new HashSet<>();
    private final List<String> infoHudLines = new ArrayList<>();
    @Nullable private final File dir;
    @Nullable private final String fileName;
    @Nullable private ICompletionListener completionListener;
    private final boolean takeEntities;
    private final boolean overrideFile;
    private boolean finished;

    public TaskSaveSchematic(LitematicaSchematic schematic, AreaSelection area, boolean takeEntities)
    {
        this(null, null, schematic, area, takeEntities, false);
    }

    public TaskSaveSchematic(@Nullable File dir, @Nullable String fileName, LitematicaSchematic schematic, AreaSelection area, boolean takeEntities, boolean overrideFile)
    {
        this.schematic = schematic;
        this.origin = area.getEffectiveOrigin();
        this.subRegions = area.getAllSubRegions();
        this.requiredChunks.addAll(PositionUtils.getTouchedChunksForBoxes(area.getAllSubRegionBoxes()));
        this.dir = dir;
        this.fileName = fileName;
        this.takeEntities = takeEntities;
        this.overrideFile = overrideFile;
        this.updateInfoHudLines();
        InfoHud.getInstance().addInfoHudRenderer(this, true);
    }

    public void setCompletionListener(ICompletionListener listener)
    {
        this.completionListener = listener;
    }

    @Override
    public boolean canExecute()
    {
        return Minecraft.getMinecraft().world != null;
    }

    @Override
    public boolean shouldRemove()
    {
        return this.canExecute() == false;
    }

    @Override
    public boolean execute()
    {
        Minecraft mc = Minecraft.getMinecraft();
        World world = WorldUtils.getBestWorld(mc);
        World worldClient = mc.world;

        if (world != null && worldClient != null)
        {
            Iterator<ChunkPos> iter = this.requiredChunks.iterator();
            int processed = 0;

            while (iter.hasNext())
            {
                ChunkPos pos = iter.next();
                int chunkX = pos.x;
                int chunkZ = pos.z;
                int count = 0;

                for (int cx = chunkX - 1; cx <= chunkX + 1; ++cx)
                {
                    for (int cz = chunkZ - 1; cz <= chunkZ + 1; ++cz)
                    {
                        if (worldClient.getChunkProvider().isChunkGeneratedAt(cx, cz))
                        {
                            ++count;
                        }
                    }
                }

                // All neighbor chunks loaded
                if (count == 9)
                {
                    ImmutableMap<String, StructureBoundingBox> volumes = PositionUtils.getBoxesWithinChunk(chunkX, chunkZ, this.subRegions);
                    this.schematic.takeBlocksFromWorldWithinChunk(world, pos.x, pos.z, volumes, this.subRegions);

                    if (this.takeEntities)
                    {
                        this.schematic.takeEntitiesFromWorldWithinChunk(world, chunkX, chunkZ,
                                volumes, this.subRegions, this.existingEntities, this.origin);
                    }

                    iter.remove();
                    processed++;
                }
            }

            if (processed > 0)
            {
                this.updateInfoHudLines();
            }
        }

        this.finished = this.requiredChunks.isEmpty();

        return this.finished;
    }

    @Override
    public void stop()
    {
        if (this.finished)
        {
            long time = (new Date()).getTime();
            this.schematic.getMetadata().setTimeCreated(time);
            this.schematic.getMetadata().setTimeModified(time);
            this.schematic.getMetadata().setTotalBlocks(this.schematic.getTotalBlocks());

            if (this.dir != null)
            {
                if (this.schematic.writeToFile(this.dir, this.fileName, this.overrideFile))
                {
                    InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.schematic_saved_as", this.fileName);
                }
                else
                {
                    InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.error.schematic_save_failed", this.fileName);
                }
            }
            // In-memory only
            else
            {
                String name = this.schematic.getMetadata().getName();
                SchematicHolder.getInstance().addSchematic(this.schematic, true);
                InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.in_memory_schematic_created", name);
            }

            if (this.completionListener != null)
            {
                this.completionListener.onTaskCompleted();
            }
        }
        else
        {
            InfoUtils.showGuiOrActionBarMessage(MessageType.WARNING, "litematica.message.error.schematic_save_interrupted");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);
    }

    private void updateInfoHudLines()
    {
        this.infoHudLines.clear();
        EntityPlayer player = Minecraft.getMinecraft().player;

        if (player != null)
        {
            List<ChunkPos> list = new ArrayList<>();
            list.addAll(this.requiredChunks);
            PositionUtils.CHUNK_POS_COMPARATOR.setReferencePosition(new BlockPos(player.getPositionVector()));
            PositionUtils.CHUNK_POS_COMPARATOR.setClosestFirst(true);
            Collections.sort(list, PositionUtils.CHUNK_POS_COMPARATOR);

            String pre = TextFormatting.WHITE.toString() + TextFormatting.BOLD.toString();
            String title = I18n.format("litematica.gui.label.schematic_verifier.missing_chunks", this.requiredChunks.size());
            this.infoHudLines.add(String.format("%s%s%s", pre, title, TextFormatting.RESET.toString()));

            int maxLines = Math.min(list.size(), Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue());

            for (int i = 0; i < maxLines; ++i)
            {
                ChunkPos pos = list.get(i);
                this.infoHudLines.add(String.format("cx: %5d, cz: %5d (x: %d, z: %d)", pos.x, pos.z, pos.x << 4, pos.z << 4));
            }
        }
    }

    @Override
    public boolean getShouldRenderText(RenderPhase phase)
    {
        return phase == RenderPhase.POST;
    }

    @Override
    public List<String> getText(RenderPhase phase)
    {
        return this.infoHudLines;
    }
}
