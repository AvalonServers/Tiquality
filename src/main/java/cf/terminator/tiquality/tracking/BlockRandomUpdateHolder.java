package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.interfaces.TiqualitySimpleTickable;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class BlockRandomUpdateHolder implements TiqualitySimpleTickable {

    private final Block block;
    private final World world;
    private final BlockPos pos;
    private final IBlockState state;
    private final Random rand;

    public BlockRandomUpdateHolder(Block block, World world, BlockPos pos, IBlockState state, Random rand) {
        this.block = block;
        this.world = world;
        this.pos = pos;
        this.state = state;
        this.rand = rand;
    }

    /**
     * Method to actually run the update on the tickable.
     */
    @Override
    public void doUpdateTick() {
        TiqualityChunk chunk = ((TiqualityWorld) world).getChunk(pos);
        if(chunk.isChunkLoaded()) {
            block.randomTick(world, pos, state, rand);
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

    /**
     * Gets the type of this Tickable
     *
     * @return the type
     */
    @Override
    public TickType getType() {
        return TickType.BLOCK_RANDOM;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || o instanceof BlockRandomUpdateHolder == false){
            return false;
        }
        BlockRandomUpdateHolder other = (BlockRandomUpdateHolder) o;
        return other.pos.equals(pos);
    }

    @Override
    public int hashCode(){
        return pos.hashCode();
    }
}
