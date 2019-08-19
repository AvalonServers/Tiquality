package com.github.terminatornl.tiquality.monitor;

import com.github.terminatornl.tiquality.interfaces.Tracker;
import com.github.terminatornl.tiquality.tracking.TrackerManager;
import com.github.terminatornl.tiquality.util.Constants;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TickMaster {

    public static long TICK_DURATION = Constants.NS_IN_TICK_LONG; /* Is updated when reloading config. */
    private final MinecraftServer server;
    private long startTime = 0L;

    public TickMaster(MinecraftServer server) {
        this.server = server;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            startTime = System.nanoTime();

            GameProfile[] cache = server.getOnlinePlayerProfiles();


            /* First, we asses the amount of active PlayerTrackers. */
            Double totalWeight_1 = TrackerManager.foreach(new TrackerManager.Action<Double>() {
                @Override
                public void each(Tracker tracker) {
                    if (value == null) {
                        value = tracker.getMultiplier(cache);
                    } else {
                        value += tracker.getMultiplier(cache);
                    }
                }
            });

            /*
                We divide the tick time amongst users, based on whether they are online or not and config multiplier.
                Source for formula: https://math.stackexchange.com/questions/253392/weighted-division
            */
            final double totalWeight = totalWeight_1 != null ? Math.max(1, totalWeight_1) : 1;

            TrackerManager.foreach(new TrackerManager.Action<Object>() {
                @Override
                public void each(Tracker tracker) {
                    long time = Math.round(TICK_DURATION * (tracker.getMultiplier(cache) / totalWeight));
                    tracker.setNextTickTime(time);
                }
            });
            TrackerManager.foreach(new TrackerManager.Action<Object>() {
                @Override
                public void each(Tracker tracker) {
                    tracker.tick();
                }
            });

        } else if (e.phase == TickEvent.Phase.END) {
            TrackerManager.tickUntil(startTime + TICK_DURATION);
            TrackerManager.removeInactiveTrackers();
        }
    }

    public long getThisTickEndTime() {
        return startTime + TICK_DURATION;
    }
}
