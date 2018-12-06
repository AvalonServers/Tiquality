package cf.terminator.tiquality.interfaces;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface TiqualityWorld {

    /**
     * Gets the minecraft world
     * @return the chunk
     */
    @Nonnull
    World getMinecraftWorld();

    /**
     * Optimized way of getting a chunk using a BlockPos
     * @param pos the position of the block
     * @return the chunk
     */
    @Nonnull TiqualityChunk getChunk(BlockPos pos);

    /**
     * Optimized way of getting the TrackerBase using a BlockPos.
     * Don't forget PlayerTrackers reside inside chunks, so it still has to grab the chunk.
     * If you need to use the chunk later on, this is not for you.
     *
     * @param pos the position of the block
     * @return the tracker
     */
    @Nullable
    Tracker getTracker(BlockPos pos);

    /**
     * Optimized way of setting the TrackerBase using a BlockPos.
     * Don't forget PlayerTrackers reside inside chunks, so it still has to grab the chunk.
     * If you need to use the chunk later on, this is not for you.
     *
     * @param pos the position of the block
     * @param tracker the TrackerBase
     */
    void setTracker(BlockPos pos, Tracker tracker);

    /**
     * Sets the tracker in a cuboid area
     * @param start start coord (All lower)
     * @param end end coord (All lower)
     * @param tracker the tracker to add
     * @param callback a task to run on completion
     */
    void setTrackerCuboidAsync(BlockPos start, BlockPos end, Tracker tracker, Runnable callback);

    /**
     * Sets the tracker in a cuboid area
     * @param start start coord (All lower)
     * @param end end coord (All lower)
     * @param tracker the tracker to add
     * @param callback a task to run on completion
     * @param beforeRun a task to run before work starts
     */
    void setTrackerCuboidAsync(BlockPos start, BlockPos end, Tracker tracker, Runnable callback, Runnable beforeRun);

    /**
     * Gets all entities in this world
     * @param trackersOnly set this to true if you're only intrested in entities which have a tracker associated.
     *                    If this is true, you are also able to edit the list. If this is false, you are returned an unmodifiable list
     * @return a list of entities, or an empty list if there are none
     */
    @Nonnull
    List<TiqualityEntity> getEntities(boolean trackersOnly);


    /**
     * Gets the minecraft chunk provider
     * @return the minecraft chunk provider
     */
    IChunkProvider getMinecraftChunkProvider();
}
