package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.tracking.ChunkStorage;
import cf.terminator.tiquality.tracking.TrackerBase;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Mixin(value = Chunk.class, priority = 1001)
public abstract class MixinChunk implements TiqualityChunk {

    @Shadow public abstract boolean isLoaded();
    @Shadow public abstract ChunkPos getPos();

    private final BiMap<Byte, TrackerBase> trackerLookup = HashBiMap.create();
    private final ChunkStorage STORAGE = new ChunkStorage();

    /**
     * Gets the first free index for a player.
     *
     * There are 6 reserved values:
     *           0: No owner
     *          -1: Reserved for potentional future use-case
     *          -2: Reserved for potentional future use-case
     *          -3: Reserved for potentional future use-case
     *          -4: Reserved for potentional future use-case
     *          -5: Reserved for potentional future use-case
     * @return the owner value
     */
    private byte getFirstFreeIndex(){
        byte i=1;
        while(trackerLookup.containsKey(i)){
            ++i;
            if(i == -5){
                throw new IllegalStateException("There are too many owners in this chunk: " + this);
            }
        }
        return i;
    }

    private byte getIDbyTracker(TrackerBase tracker){
        Byte owner_id = trackerLookup.inverse().get(tracker);
        if(owner_id == null){
            owner_id = getFirstFreeIndex();
            trackerLookup.put(owner_id, tracker);
        }
        return owner_id;
    }

    /**
     * Removes unused block owners.
     */
    private void tiquality_refresh(){
        Set<Byte> ownersToKeep = new TreeSet<>();
        for(byte[] data : STORAGE.getAll()){
            for(byte b : data){
                if(b == 0){
                    continue;
                }
                if(ownersToKeep.contains(b) == false){
                    ownersToKeep.add(b);
                }
            }
        }

        trackerLookup.keySet().retainAll(ownersToKeep);
    }

    @Override
    public Chunk getMinecraftChunk(){
        return (Chunk) (Object) this;
    }

    @Override
    public synchronized void tiquality_setTrackedPosition(BlockPos pos, TrackerBase tracker){

        TiqualityEvent.SetBlockTrackerEvent event = new TiqualityEvent.SetBlockTrackerEvent(this, pos, tracker);

        if(MinecraftForge.EVENT_BUS.post(event) /* if cancelled */){
            return;
        }
        tracker = event.getTracker();

        if(tracker == null){
            STORAGE.set(pos, (byte) 0);
        }else {
            byte id = getIDbyTracker(tracker);
            STORAGE.set(pos, id);
            tracker.associateChunk(this);
            trackerLookup.forcePut(id, tracker);
        }
    }

    @Override
    public void tiquality_writeToNBT(NBTTagCompound tag) {
        tiquality_refresh();
        NBTTagList list = tag.getTagList("Sections", 10);
        STORAGE.injectNBTAfter(list);
        tag.setTag("Sections", list);
        NBTTagList trackerList = new NBTTagList();
        for(Map.Entry<Byte, TrackerBase> e : trackerLookup.entrySet()){
            if(e.getValue().shouldSaveToDisk() == false){
                continue;
            }
            NBTTagCompound trackerData = new NBTTagCompound();
            trackerData.setByte("chunk_id", e.getKey());
            trackerData.setTag("tracker", TrackerBase.getTrackerTag(e.getValue()));
            trackerList.appendTag(trackerData);
        }
        if(trackerList.tagCount() > 0) {
            tag.setTag("Tiquality", trackerList);
        }
    }

    @Override
    public void tiquality_loadNBT(World world, NBTTagCompound tag) {
        STORAGE.loadFromNBT(tag.getTagList("Sections", 10));

        for (NBTBase nbtBase : tag.getTagList("Tiquality", 10)) {
            NBTTagCompound trackerData = (NBTTagCompound) nbtBase;
            TrackerBase tracker = TrackerBase.getTracker(this, trackerData.getCompoundTag("tracker"));
            if(tracker != null){
                trackerLookup.forcePut(trackerData.getByte("chunk_id"), tracker);
            }else{
                Tiquality.LOGGER.debug("Failed to load tracker in chunk: ", this);
            }
        }
    }

    @Override
    public synchronized @Nullable TrackerBase tiquality_findTrackerByBlockPos(BlockPos pos){
        return trackerLookup.get(STORAGE.get(pos));
    }

    @Override
    public boolean isChunkLoaded(){
        return isLoaded();
    }

    @Override
    public int compareTo(@Nonnull TiqualityChunk other){
        ChunkPos thisPos = this.getPos();
        ChunkPos otherPos = ((Chunk) other).getPos();

        int xComp = Integer.compare(thisPos.x, otherPos.x);
        return xComp != 0 ? xComp : Integer.compare(thisPos.z, otherPos.z);
    }

    @Inject(method = "onLoad", at=@At("HEAD"))
    private void onLoad(CallbackInfo ci){
        for(TrackerBase tracker : trackerLookup.values()){
            tracker.associateChunk(this);
        }
    }

    @Inject(method = "onUnload", at=@At("HEAD"))
    private void onUnLoad(CallbackInfo ci){
        for(TrackerBase tracker : trackerLookup.values()){
            tracker.disAssociateChunk(this);
        }
    }

}
