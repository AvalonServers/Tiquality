package cf.terminator.tiquality.tracking;

import cf.terminator.tiquality.Tiquality;
import cf.terminator.tiquality.api.TrackerAlreadyExistsException;
import cf.terminator.tiquality.concurrent.ThreadSafeSet;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.interfaces.Tracker;
import cf.terminator.tiquality.util.PersistentData;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@SuppressWarnings("WeakerAccess")
public class TrackerManager {

    /**
     * Holds a list of all registered trackers
     * See: cf.terminator.tiquality.api.Tracking#registerCustomTracker(java.lang.Class)
     */
    public static final HashMap<String, Class<? extends Tracker>> REGISTERED_TRACKER_TYPES = new HashMap<>();

    /**
     * Variable holding all PlayerTrackers.
     */
    private static final ThreadSafeSet<TrackerHolder> TRACKER_LIST = new ThreadSafeSet<>(new CopyOnWriteArraySet<>());

    /**
     * Loop over the protected set.
     */
    public static <T> T foreach(Action<T> foreach){
        TRACKER_LIST.lock();
        try {
            for (TrackerHolder tracker : TRACKER_LIST) {
                foreach.each(tracker.getTracker());
                if (foreach.stop) {
                    return foreach.value;
                }
            }
            return foreach.value;
        }finally {
            TRACKER_LIST.unlock();
        }
    }

    /**
     * Ticks all scheduled objects until either time runs out or all objects have been ticked.
     * THIS MAY ONLY BE CALLED ON THE MAIN THREAD!
     * @param time the time (System.nanoTime()) when the ticking should stop.
     */
    public static void tickUntil(long time){
        TRACKER_LIST.lock();
        boolean hasWork = true;
        while(System.nanoTime() < time && hasWork) {
            hasWork = false;
            for(TrackerHolder holder : TRACKER_LIST){
                Tracker tracker = holder.getTracker();
                if(tracker.isDone() == false){
                    hasWork = true;
                    tracker.grantTick();
                }
            }
        }
        TRACKER_LIST.unlock();
    }

    /**
     * Removes trackers which do not tick anymore due to their tickables being unloaded
     */
    public static void removeInactiveTrackers(){
        TRACKER_LIST.lock();

        Set<TrackerHolder> removables = new HashSet<>();

        for (TrackerHolder holder : TRACKER_LIST){
            Tracker tracker = holder.getTracker();
            if ((tracker.isDone() && tracker.isLoaded() == false) || tracker.shouldUnload()) {
                tracker.onUnload();
                removables.add(holder);
            }
        }
        TRACKER_LIST.removeAll(removables);
        TRACKER_LIST.unlock();
    }

    /**
     * Checks if a Tracker already exists with the same unique ID, if it does: Return the old one and discard the new.
     * If not, return the new one and tracking it.
     * @param input the new Tracker
     * @return input
     * @throws TrackerAlreadyExistsException if the tracker already exists, indicative of a programming error.
     */
    public static <T extends Tracker> TrackerHolder<T> addTracker(@Nonnull TrackerHolder<T> input) throws TrackerAlreadyExistsException {
        TRACKER_LIST.lock();
        try{
            for (TrackerHolder holder : TRACKER_LIST) {
                try {
                    holder.getTracker().checkColission(input.getTracker());
                }catch (TrackerAlreadyExistsException e){
                    Tiquality.LOGGER.warn("TRACKER ALREADY EXISTS: " + e.getNewTracker().toString() + " and: " + e.getOldTracker().toString());
                    e.printStackTrace();
                }
            }
            TRACKER_LIST.add(input);
        }finally {
            TRACKER_LIST.unlock();
        }
        return input;
    }

    @Nullable
    public static <T extends Tracker> T getTrackerByID(long id){
        TRACKER_LIST.lock();
        try{
            for (TrackerHolder tracker : TRACKER_LIST) {
                if (id == tracker.getId()) {
                    //noinspection unchecked
                    return (T) tracker.getTracker();
                }
            }
            return null;
        }finally {
            TRACKER_LIST.unlock();
        }
    }

    /**
     * Instantiates a new tracker using an NBT compound tag.
     * If the tracker already exists, a reference to the pre-existing tracker is given.
     * @param tagCompound The NBT tag compound
     * @return the tracker
     */
    @Nullable
    public static TrackerHolder getTracker(TiqualityWorld world, NBTTagCompound tagCompound){
        String type = tagCompound.getString("type");
        if(type.equals("")){
            return null;
        }
        long id = tagCompound.getLong("id");
        TRACKER_LIST.lock();
        try {
            for (TrackerHolder holder : TRACKER_LIST) {
                if (holder.getId() == id) {
                    return holder;
                }
            }

            Class<? extends Tracker> clazz = REGISTERED_TRACKER_TYPES.get(type);
            if(clazz == null){
            /*
                Either a mod author completely forgot to call cf.terminator.tiquality.api.Tracking.registerCustomTracker(),
                or a mod providing a tracker has been removed since last load.
             */
                return null;
            }
            Tracker newTracker;
            try {
                newTracker =  clazz.getDeclaredConstructor(TiqualityWorld.class, NBTTagCompound.class).newInstance(world, tagCompound.getCompoundTag("data"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return addTracker(new TrackerHolder<>(newTracker, id));
        }finally {
            TRACKER_LIST.unlock();
        }
    }


    /**
     * There's a theoretical maximum of 1.8446744e+19 different Trackers per server. This should suffice.
     */
    static long NEXT_TRACKER_ID;
    static {
        if(PersistentData.NEXT_FREE_TRACKER_ID.isSet() == false){
            PersistentData.NEXT_FREE_TRACKER_ID.setLong(Long.MIN_VALUE);
        }
        NEXT_TRACKER_ID = PersistentData.NEXT_FREE_TRACKER_ID.getLong();
    }

    public static long generateUniqueTrackerID(){
        synchronized (PersistentData.NEXT_FREE_TRACKER_ID) {
            long granted = NEXT_TRACKER_ID++;
            PersistentData.NEXT_FREE_TRACKER_ID.setLong(NEXT_TRACKER_ID);
            return granted;
        }
    }

    public static NBTTagCompound getTrackerTag(TrackerHolder holder){
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", holder.getTracker().getIdentifier());
        tag.setLong("id", holder.getId());
        tag.setTag("data", holder.getTracker().getNBT());
        return tag;
    }

    public static abstract class Action<T>{
        public T value = null;
        private boolean stop = false;

        public void stop(T value){
            this.stop = true;
            this.value = value;
        }
        public abstract void each(Tracker tracker);
    }
}