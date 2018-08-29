package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.interfaces.TiqualityChunk;
import cf.terminator.tiquality.store.*;
import cf.terminator.tiquality.util.ForgeData;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Mixin(value = Chunk.class, priority = 1001)
public abstract class MixinChunk implements TiqualityChunk {

    @Shadow public abstract boolean isLoaded();
    @Shadow public abstract ChunkPos getPos();

    private final BiMap<Byte, PlayerTracker> trackerLookup = HashBiMap.create();
    private final ChunkStorage STORAGE = new ChunkStorage();

    /**
     * Gets the first free index for a player.
     *
     * There are 3 reserved values:
     *           0: No owner
     *          -1: No owner, but is always ticked by the server (Mob spawners, etc)
     *          -2: Reserved for potentional future usecase
     * @return the owner value
     */
    private byte getFirstFreeIndex(){
        byte i=1;
        while(trackerLookup.containsKey(i)){
            ++i;
            if(i == -2){
                throw new IllegalStateException("There are too many owners in this chunk: " + this);
            }
        }
        return i;
    }

    private byte getProfileIDforChunk(GameProfile profile){
        PlayerTracker tracker = TrackerHub.getPlayerTrackerSafeByProfile(profile);
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
    private void lagGoggles_refresh(){
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
    public void lagGoggles_setTrackedPosition(BlockPos pos, @Nonnull PlayerTracker tracker){
        STORAGE.set(pos, getProfileIDforChunk(tracker.getOwner()));
    }

    @Override
    public void lagGoggles_removeTracker(BlockPos pos){
        STORAGE.set(pos, (byte) 0);
    }

    @Override
    public void lagGoggles_writeToNBT(NBTTagCompound tag) {
        lagGoggles_refresh();
        NBTTagList list = tag.getTagList("Sections", 10);
        STORAGE.injectNBTAfter(list);
        tag.setTag("Sections", list);
        NBTTagList ownerList = new NBTTagList();
        for(Map.Entry<Byte, PlayerTracker> e : trackerLookup.entrySet()){
            UUID uuid = e.getValue().getOwner().getId();
            NBTTagCompound owner = new NBTTagCompound();
            owner.setByte("id", e.getKey());
            owner.setLong("uuidMost", uuid.getMostSignificantBits());
            owner.setLong("uuidLeast", uuid.getLeastSignificantBits());
            ownerList.appendTag(owner);
        }
        if(ownerList.tagCount() > 0) {
            tag.setTag("TiqualityCommand", ownerList);
        }
    }

    @Override
    public void lagGoggles_loadNBT(World world, NBTTagCompound tag) {
        STORAGE.loadFromNBT(tag.getTagList("Sections", 10));

        Iterator<NBTBase> list = tag.getTagList("TiqualityCommand",10).iterator();
        while(list.hasNext()){
            NBTTagCompound owner = (NBTTagCompound) list.next();
            UUID uuid = new UUID(owner.getLong("uuidMost"),owner.getLong("uuidLeast"));

            trackerLookup.forcePut(owner.getByte("id"),
                    TrackerHub.getPlayerTrackerSafeByProfile(ForgeData.getGameProfileByUUID(uuid)));

        }
    }

    @Override
    public @Nullable PlayerTracker lagGoggles_findTrackerByBlockPos(BlockPos pos){
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

}
