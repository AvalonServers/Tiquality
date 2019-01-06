package cf.terminator.tiquality.tracking.update;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.tracking.TickLogger;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class BlockUpdateHolder implements TiqualitySimpleTickable {

    private final Block block;
    private final World world;
    private final BlockPos pos;
    private final Random rand;

    public BlockUpdateHolder(Block block, World world, BlockPos pos, Random rand) {
        this.block = block;
        this.world = world;
        this.pos = pos;
        this.rand = rand;
    }

    /**
     * Method to actually run the update on the tickable.
     */
    @Override
    public void doUpdateTick() {
        TiqualityChunk chunk = ((TiqualityWorld) world).getTiqualityChunk(pos);
        if(chunk.isChunkLoaded()) {
            Tiquality.TICK_EXECUTOR.onBlockTick(block, world, pos, chunk.getMinecraftChunk().getBlockState(pos), rand);
        }
    }

    /**
     * Method to get the position of the object
     */
    @Override
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Method to get the world of the object
     */
    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public TickLogger.Location getLocation() {
        return new TickLogger.Location(world, pos);
    }

    @Override
    public void tiquality_mark() {
        ((TiqualityWorld) world).tiquality_mark(pos);
    }

    @Override
    public void tiquality_unMark() {
        ((TiqualityWorld) world).tiquality_unMark(pos);
    }

    @Override
    public boolean tiquality_isMarked() {
        return ((TiqualityWorld) world).tiquality_isMarked(pos);
    }

    /**
     * Gets the type of this Tickable
     *
     * @return the type
     */
    @Override
    public TickType getType() {
        return TickType.BLOCK;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || o instanceof BlockUpdateHolder == false){
            return false;
        }
        BlockUpdateHolder other = (BlockUpdateHolder) o;
        return other.pos.equals(pos);
    }

    @Override
    public int hashCode(){
        return pos.hashCode();
    }
}
