package cf.terminator.tiquality.interfaces;

import cf.terminator.tiquality.tracking.TickLogger;
import cf.terminator.tiquality.tracking.TrackerHolder;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.List;
import java.util.Random;

public interface Tracker {

    /**
     * Use this to determine if you need to read NBT data.
     * You can also return an existing tracker.
     *
     * However, if you do return an existing tracker, update checkColission accordingly.
     * @return Tracker
     */
    Tracker load(TiqualityWorld world, NBTTagCompound trackerTag);

    /**
     * Consume tick time
     * @param time
     */
    default void consume(long time){
        if(canProfile()){
            throw new RuntimeException("You must override the consume method!");
        }
    }

    boolean shouldSaveToDisk();

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    @Nonnull NBTTagCompound getNBT();

    void setProfileEnabled(boolean shouldProfile);

    @Nullable TickLogger stopProfiler();

    boolean canProfile();

    boolean isProfiling();

    void setNextTickTime(long granted_ns);

    /**
     * Tick this tracker every tick, right after every tracker has been assigned
     * tick time. This can be used for inter-communication between trackers.
     */
    default void tick() {

    }

    /**
     * Gets the tick time multiplier for the TrackerBase.
     * This is used to distribute tick time in a more controlled manner.
     * @param cache The current online player cache
     * @return the multiplier
     */
    double getMultiplier(GameProfile[] cache);

    long getRemainingTime();

    boolean needsTick();

    void tickSimpleTickable(TiqualitySimpleTickable tileEntity);

    void tickEntity(TiqualityEntity entity);

    void doBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand);

    void doRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand);

    void grantTick();

    void addTickableToQueue(TiqualitySimpleTickable tickable);

    void associateChunk(TiqualityChunk chunk);
    void associateDelegatingTracker(Tracker tracker);
    void removeDelegatingTracker(Tracker tracker);

    /**
     * Gets the associated players for this tracker
     * @return a list of all players involved with this tracker.
     */
    @Nonnull
    List<GameProfile> getAssociatedPlayers();

    String toString();

    /**
     * @return the info describing this TrackerBase (Like the owner)
     */
    @Nonnull
    TextComponentString getInfo();

    @Nonnull
    String getIdentifier();

    /**
     * Required to check for colission with unloaded trackers.
     * @return int the hash code, just like Object#hashCode().
     */
    int getHashCode();

    boolean shouldUnload();

    @OverridingMethodsMustInvokeSuper
    void onUnload();

    /**
     * This is called when a holder has been assigned to this tracker.
     * You only need to do this if you want your tracker to be written to disk.
     * @param holder holder
     */
    void setHolder(TrackerHolder holder);

    /**
     * Simply return the holder you've received in setHolder.
     * You only need to do this if you want your tracker to be written to disk.
     * @return holder
     */
    <T extends Tracker> TrackerHolder<T> getHolder();

    /**
     * Fired whenever a block is changed that is owned by this tracker
     * In general, you don't have to use this.
     *
     * @param world the world
     * @param pos the position
     * @param state the new block state
     */
    default void notifyBlockStateChange(TiqualityWorld world, BlockPos pos, IBlockState state){

    }

    /**
     * Notify this tracker about it's performance falling behind.
     * @param ratio the tracker's speed compared to the server tick time.
     */
    default void notifyFallingBehind(double ratio) {

    }

    boolean isLoaded();

    /**
     * Checks if the tracker is equal to one already in the database.
     * Allows for flexibility for loading.
     * @param tag tag
     * @return equals
     */
    boolean equalsSaved(NBTTagCompound tag);
}
