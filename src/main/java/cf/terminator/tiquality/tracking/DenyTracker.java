package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.TrackerAlreadyExistsException;
import cf.terminator.tiquality.interfaces.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Used to remember when not to tick blocks, this prevents scanning the block list to improve performance at cost of ram.
 *
 * This does not persist, and will 'slowly' repopulate when chunks are reloaded.
 */
public class DenyTracker implements Tracker {

    public static final DenyTracker INSTANCE = new DenyTracker();
    private static boolean IS_FIRST_RUN = true;

    public static void unlinkAll(){
        /*
         * This populates throughout the game, no need in removing stuff that does not exists!
         */
        if(IS_FIRST_RUN){
            IS_FIRST_RUN = false;
            return;
        }
        for(World world : FMLCommonHandler.instance().getMinecraftServerInstance().worlds){
            IChunkProvider iProvider = world.getChunkProvider();
            if(iProvider instanceof ChunkProviderServer){
                ChunkProviderServer provider = (ChunkProviderServer) iProvider;
                Tiquality.SCHEDULER.schedule(new Runnable() {
                    @Override
                    public void run() {
                        for(Chunk chunk : provider.getLoadedChunks()){
                            ((TiqualityChunk) chunk).replaceTracker(INSTANCE, null);
                        }
                    }
                });
            }
        }
    }

    @Override
    public Tracker load(TiqualityWorld world, NBTTagCompound nbt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean shouldSaveToDisk() {
        return false;
    }

    @Override
    public NBTTagCompound getNBT() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TickLogger getTickLogger() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProfileEnabled(boolean shouldProfile) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public TickLogger stopProfiler() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNextTickTime(long granted_ns) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getMultiplier(GameProfile[] cache) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getRemainingTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean needsTick() {
        return false;
    }

    /**
     * Void the tick
     * @param tickable t
     */
    @Override
    public void tickTileEntity(TiqualitySimpleTickable tickable) {

    }

    /**
     * Void the tick
     * @param entity e
     */
    @Override
    public void tickEntity(TiqualityEntity entity) {

    }

    /**
     * Void the tick
     * @param block b
     * @param world w
     * @param pos p
     * @param state s
     * @param rand r
     */
    @Override
    public void doBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {

    }

    /**
     * Void the tick
     * @param block b
     * @param world w
     * @param pos p
     * @param state s
     * @param rand r
     */
    @Override
    public void doRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {

    }

    @Override
    public void grantTick() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void associateChunk(TiqualityChunk chunk) {

    }

    @Override
    public void associateDelegatingTracker(Tracker tracker) {

    }

    @Override
    public void removeDelegatingTracker(Tracker tracker) {

    }

    @Nonnull
    @Override
    public List<GameProfile> getAssociatedPlayers() {
        return null;
    }

    @Nonnull
    @Override
    public TextComponentString getInfo() {
        return new TextComponentString(TextFormatting.RED + "TICK-DENIED");
    }

    @Nonnull
    @Override
    public String getIdentifier() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean shouldUnload() {
        return false;
    }

    @Override
    public void onUnload() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(@Nonnull Object o) {
        return 0;
    }

    @Override
    public void checkCollision(@Nonnull Tracker tracker) throws TrackerAlreadyExistsException {

    }

    @Override
    public void setHolder(TrackerHolder holder) {

    }

    @Override
    public TrackerHolder getHolder() {
        return null;
    }
}
