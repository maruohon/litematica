package litematica.schematic.verifier;

import malilib.util.data.RunStatus;

public class VerifierStatus
{
    public final RunStatus status;
    public final int processedChunks;
    public final int totalChunks;
    public final int totalBlocks;
    public final int correctBlocks;

    public VerifierStatus(RunStatus status, int processedChunks, int totalChunks, int totalBlocks, int correctBlocks)
    {
        this.status = status;
        this.processedChunks = processedChunks;
        this.totalChunks = totalChunks;
        this.totalBlocks = totalBlocks;
        this.correctBlocks = correctBlocks;
    }
}
