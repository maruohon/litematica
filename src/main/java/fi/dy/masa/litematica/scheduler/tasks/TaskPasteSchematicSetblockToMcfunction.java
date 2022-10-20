package fi.dy.masa.litematica.scheduler.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import net.minecraft.client.network.ClientPlayerEntity;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;

public class TaskPasteSchematicSetblockToMcfunction extends TaskPasteSchematicPerChunkCommand
{
    protected final String fileNameBase;
    protected final int maxCommandsPerFile = 64000;
    protected BufferedWriter writer;
    protected int fileIndex = 1;
    protected int commandsInCurrentFile;

    public TaskPasteSchematicSetblockToMcfunction(Collection<SchematicPlacement> placements, LayerRange range, boolean changedBlocksOnly)
    {
        super(placements, range, changedBlocksOnly);

        String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date(System.currentTimeMillis()));
        this.fileNameBase = "paste_commands_" + date + "_";
        this.maxCommandsPerTick = 64000;

        this.openNextOutputFile();
    }

    @Override
    protected void sendCommand(String command, ClientPlayerEntity player)
    {
        if (this.writer == null || this.commandsInCurrentFile > this.maxCommandsPerFile)
        {
            this.openNextOutputFile();
        }

        try
        {
            this.writer.write(command);
            this.writer.newLine();
            ++this.commandsInCurrentFile;
            ++this.sentCommandsThisTick;
        }
        catch (IOException e)
        {
            Litematica.logger.error("Exception while writing paste commands to file", e);
            this.phase = TaskPhase.FINISHED;
            this.finished = true;
        }
    }

    @Override
    public void stop()
    {
        this.closeCurrentFile();
        super.stop();
    }

    protected void openNextOutputFile()
    {
        this.closeCurrentFile();

        try
        {
            String fileName = this.fileNameBase + String.format("%02d", this.fileIndex) + ".mcfunction";
            File file = new File(DataManager.getCurrentConfigDirectory(), fileName);
            this.writer = new BufferedWriter(new FileWriter(file));
            ++this.fileIndex;
            this.commandsInCurrentFile = 0;
        }
        catch (IOException ignore) {}
    }

    protected void closeCurrentFile()
    {
        try
        {
            if (this.writer != null)
            {
                this.writer.close();
            }
        }
        catch (IOException ignore) {}
    }
}
