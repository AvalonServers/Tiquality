package com.github.terminatornl.tiquality.profiling;

import com.github.terminatornl.tiquality.util.Constants;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.*;

import static com.github.terminatornl.tiquality.util.Utils.TWO_DECIMAL_FORMATTER;

public class ProfileReport implements IMessage {

    private final long startTimeNanos;
    private final long endTimeNanos;
    private final TreeSet<AnalyzedComponent> analyzedComponents = new TreeSet<>();
    private final TreeMap<String, TickTime> classTimes = new TreeMap<>();
    private final ITextComponent identifier;
    private double serverTPS;
    private double trackerTPS;
    private int serverTicks;
    private int trackerTicks;
    private long grantedNanos;
    private long totalNanosUsed = 0L;
    private NavigableSet<Map.Entry<String, TickTime>> classTimesSorted = null;

    public ProfileReport(long startTimeNanos, long endTimeNanos, TickLogger logger, ITextComponent identifier, Collection<AnalyzedComponent> analyzedComponents) {
        long totalTimeNanos = endTimeNanos - startTimeNanos;
        this.startTimeNanos = startTimeNanos;
        this.endTimeNanos = endTimeNanos;
        this.serverTicks = logger.getServerTicks();
        this.trackerTicks = logger.getTrackerTicks();

        double idealTicks = totalTimeNanos / Constants.NS_IN_TICK_DOUBLE;
        this.serverTPS = this.serverTicks / idealTicks * 20D;
        this.trackerTPS = this.trackerTicks / idealTicks * 20D;


        this.grantedNanos = logger.getGrantedNanos();
        this.analyzedComponents.addAll(analyzedComponents);
        this.identifier = identifier;
        for (AnalyzedComponent component : this.analyzedComponents) {
            /*
                Total nanoseconds used
             */
            totalNanosUsed += component.getTimes().getNanosConsumed();

            /*
                Class times
             */
            TickTime time = classTimes.get(component.getReferencedClass());
            if (time == null) {
                classTimes.put(component.getReferencedClass(), new TickTime(component.getTimes()));
            } else {
                time.add(component.getTimes());
            }
        }
    }

    public ITextComponent getIdentifier() {
        return identifier;
    }

    public double getServerTPS() {
        return serverTPS;
    }

    public double getTrackerTPS() {
        return trackerTPS;
    }

    public int getServerTicks() {
        return serverTicks;
    }

    public int getTrackerTicks() {
        return trackerTicks;
    }

    public String getTrackerImpactPercentage(TickTime time) {
        double factor = (double) time.getNanosConsumed() / (double) this.grantedNanos;
        return TWO_DECIMAL_FORMATTER.format(Math.round(factor * 10000D) / 100D);
    }

    public String getServerImpactPercentage(TickTime time) {
        double nanosPassedOnServer = (Constants.NS_IN_TICK_DOUBLE * (double) serverTicks * serverTPS / 20D);

        double factor = (double) time.getNanosConsumed() / nanosPassedOnServer;
        return TWO_DECIMAL_FORMATTER.format(Math.round(factor * 10000D) / 100D);
    }

    public double getMuPerTick(TickTime time) {
        return ((double) time.getNanosConsumed() / 1000) / (double) trackerTicks;
    }

    public double getCallsPerTick(TickTime time) {
        return ((double) time.getCalls()) / (double) trackerTicks;
    }

    public NavigableSet<AnalyzedComponent> getAnalyzedComponents() {
        return Collections.unmodifiableNavigableSet(analyzedComponents);
    }

    public NavigableMap<String, TickTime> getClassTimes() {
        return Collections.unmodifiableNavigableMap(classTimes);
    }

    public NavigableSet<Map.Entry<String, TickTime>> getClassTimesSorted() {
        if (classTimesSorted != null) {
            return classTimesSorted;
        }
        TreeSet<Map.Entry<String, TickTime>> set = new TreeSet<>(Comparator.comparing(Map.Entry::getValue));
        set.addAll(classTimes.entrySet());
        classTimesSorted = Collections.unmodifiableNavigableSet(set);
        return classTimesSorted;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.serverTPS = buf.readDouble();
        this.trackerTPS = buf.readDouble();
        this.serverTicks = buf.readInt();
        this.trackerTicks = buf.readInt();
        this.grantedNanos = buf.readLong();

        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            analyzedComponents.add(new AnalyzedComponent(buf));
        }

        int size2 = buf.readInt();
        for (int i = 0; i < size2; i++) {
            classTimes.put(ByteBufUtils.readUTF8String(buf), new TickTime(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(serverTPS);
        buf.writeDouble(trackerTPS);
        buf.writeInt(serverTicks);
        buf.writeInt(trackerTicks);
        buf.writeLong(grantedNanos);

        buf.writeInt(analyzedComponents.size());
        for (AnalyzedComponent entry : analyzedComponents) {
            entry.toBytes(buf);
        }

        buf.writeInt(classTimes.size());
        for (Map.Entry<String, TickTime> entry : classTimes.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, entry.getKey());
            entry.getValue().toBytes(buf);
        }
    }

    public long getTotalNanosUsed() {
        return totalNanosUsed;
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public long getEndTimeNanos() {
        return endTimeNanos;
    }
}
