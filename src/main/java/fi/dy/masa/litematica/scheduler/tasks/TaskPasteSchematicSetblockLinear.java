package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;
import java.util.Set;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.StringUtils;

public class TaskPasteSchematicSetblockLinear extends TaskPasteSchematicSetblock
{
    protected final ChunkPos centerChunk;
    protected final PasteAxis axis;
    protected IntBoundingBox enclosingBox;
    protected ChunkPos currentChunk;
    protected int currentX;
    protected int currentY;
    protected int currentZ;
    protected int minX;
    protected int minY;
    protected int minZ;
    protected int maxX;
    protected int maxY;
    protected int maxZ;
    protected int currentBoxIndex;

    public TaskPasteSchematicSetblockLinear(SchematicPlacement placement, boolean changedBlocksOnly, BlockPos playerPos)
    {
        super(placement, changedBlocksOnly);

        this.axis = (PasteAxis) Configs.Generic.PASTE_LINEAR_AXIS.getOptionListValue();
        this.centerChunk = new ChunkPos(playerPos.getX() >> 4, playerPos.getZ() >> 4);

        this.addTouchedBoxesLinear(placement);
    }

    @Override
    protected void addTouchedBoxes(SchematicPlacement placement)
    {
        // NO-OP
    }

    protected void addTouchedBoxesLinear(SchematicPlacement placement)
    {
        Set<ChunkPos> touchedChunks = placement.getTouchedChunks();
        int chunkRadius = Configs.Generic.PASTE_LINEAR_CHUNK_RAD.getIntegerValue();

        if (chunkRadius < 0)
        {
            chunkRadius = this.mc.options.viewDistance;
        }

        int minCX = this.centerChunk.x - chunkRadius;
        int minCZ = this.centerChunk.z - chunkRadius;
        int maxCX = this.centerChunk.x + chunkRadius;
        int maxCZ = this.centerChunk.z + chunkRadius;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (ChunkPos pos : touchedChunks)
        {
            if (pos.x < minCX || pos.x > maxCX || pos.z < minCZ || pos.z > maxCZ)
            {
                continue;
            }

            for (IntBoundingBox box : placement.getBoxesWithinChunk(pos.x, pos.z).values())
            {
                minX = Math.min(minX, box.minX);
                minY = Math.min(minY, box.minY);
                minZ = Math.min(minZ, box.minZ);
                maxX = Math.max(maxX, box.maxX);
                maxY = Math.max(maxY, box.maxY);
                maxZ = Math.max(maxZ, box.maxZ);
                this.boxesInChunks.put(pos, box);
            }

            this.chunks.add(pos);
        }

        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.currentX = minX;
        this.currentY = minY;
        this.currentZ = minZ;
        this.enclosingBox = IntBoundingBox.createProper(minX, minY, minZ, maxX, maxY, maxZ);
        this.currentChunk = new ChunkPos(minX >> 4, minZ >> 4);

        this.updateInfoHudLines();
    }

    @Override
    public boolean execute()
    {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        ClientWorld worldClient = this.mc.world;
        ClientPlayerEntity player = this.mc.player;
        BlockPos.Mutable posMutable = new BlockPos.Mutable();
        ChunkPos pos = new ChunkPos(this.currentX >> 4, this.currentZ >> 4);
        Chunk chunkSchematic = worldSchematic.getChunkProvider().getChunk(pos.x, pos.z);
        Chunk chunkClient = worldClient.getChunk(pos.x, pos.z);
        boolean chunkChanged = true;
        this.sentCommandsThisTick = 0;

        if (this.sentCommandsTotal == 0)
        {
            this.mc.player.sendChatMessage("/gamerule sendCommandFeedback false");
        }

        while (this.sentCommandsThisTick < this.maxCommandsPerTick)
        {
            if (chunkChanged)
            {
                if (this.canProcessChunk(pos, worldSchematic, worldClient) == false)
                {
                    if (this.advanceToNextChunk())
                    {
                        this.summonAllEntities(worldSchematic, worldClient, player);
                        this.finished = true;
                        return true;
                    }

                    pos = new ChunkPos(this.currentX >> 4, this.currentZ >> 4);
                    chunkSchematic = worldSchematic.getChunkProvider().getChunk(pos.x, pos.z);
                    chunkClient = worldClient.getChunk(pos.x, pos.z);

                    continue;
                }
            }

            int cx = (this.currentX >> 4);
            int cz = (this.currentZ >> 4);
            chunkChanged = cx != pos.x || cz != pos.z;

            if (chunkChanged)
            {
                pos = new ChunkPos(cx, cz);
                chunkSchematic = worldSchematic.getChunkProvider().getChunk(pos.x, pos.z);
                chunkClient = worldClient.getChunk(pos.x, pos.z);
            }

            posMutable.set(this.currentX, this.currentY, this.currentZ);
            this.handleBlock(posMutable, chunkSchematic, chunkClient, player);

            if (this.advancePosition())
            {
                this.summonAllEntities(worldSchematic, worldClient, player);
                this.finished = true;
                return true;
            }
        }

        return false;
    }

    protected boolean advanceToNextChunk()
    {
        if (this.axis == PasteAxis.X)
        {
            this.currentX += 16 - (this.currentX & 0xF);
        }
        else
        {
            this.currentZ += 16 - (this.currentZ & 0xF);
        }

        return this.wrapPositions();
    }

    protected boolean advancePosition()
    {
        if (this.axis == PasteAxis.X)
        {
            ++this.currentX;
        }
        else
        {
            ++this.currentZ;
        }

        return this.wrapPositions();
    }

    protected boolean wrapPositions()
    {
        if (this.axis == PasteAxis.X)
        {
            if (this.currentX > this.maxX)
            {
                this.currentX = this.minX;

                if (++this.currentZ >= this.maxZ)
                {
                    this.currentZ = this.minZ;

                    if (++this.currentY > this.maxY)
                    {
                        return true;
                    }
                }
            }
        }
        else
        {
            if (this.currentZ > this.maxZ)
            {
                this.currentZ = this.minZ;

                if (++this.currentX >= this.maxX)
                {
                    this.currentX = this.minX;

                    if (++this.currentY > this.maxY)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected void summonAllEntities(WorldSchematic worldSchematic, ClientWorld worldClient, ClientPlayerEntity player)
    {
        for (ChunkPos pos : this.chunks)
        {
            if (this.canProcessChunk(pos, worldSchematic, worldClient))
            {
                List<IntBoundingBox> boxes = this.boxesInChunks.get(pos);

                for (IntBoundingBox box : boxes)
                {
                    this.summonEntities(box, worldSchematic, player);
                }
            }
        }
    }

    public static enum PasteAxis implements IConfigOptionListEntry
    {
        X ("x", "X"),
        Z ("z", "Z");

        private final String configString;
        private final String translationKey;

        private PasteAxis(String configString, String translationKey)
        {
            this.configString = configString;
            this.translationKey = translationKey;
        }

        @Override
        public String getStringValue()
        {
            return this.configString;
        }

        @Override
        public String getDisplayName()
        {
            return StringUtils.translate(this.translationKey);
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward)
        {
            int id = this.ordinal();

            if (forward)
            {
                if (++id >= values().length)
                {
                    id = 0;
                }
            }
            else
            {
                if (--id < 0)
                {
                    id = values().length - 1;
                }
            }

            return values()[id % values().length];
        }

        @Override
        public PasteAxis fromString(String name)
        {
            return fromStringStatic(name);
        }

        public static PasteAxis fromStringStatic(String name)
        {
            for (PasteAxis mode : PasteAxis.values())
            {
                if (mode.configString.equalsIgnoreCase(name))
                {
                    return mode;
                }
            }

            return PasteAxis.X;
        }
    }
}
