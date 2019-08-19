package com.github.terminatornl.tiquality.tracking;

import java.util.LinkedList;

public class TickWallet {

    private final LinkedList<TickWallet> subWallets = new LinkedList<>();
    private long remainingTime = 0L;

    public void setRemainingTime(long time) {
        remainingTime = time;
    }

    /**
     * Gets the time of this wallet PLUS all subwallets.
     *
     * @return the time left in nanoseconds
     */
    public long getTimeLeft() {
        long time = remainingTime;
        for (TickWallet subWallet : subWallets) {
            time += subWallet.remainingTime;
        }
        return time;
    }

    /**
     * Attempts to take time, and only accesses the subwallets if it's own time ran out.
     *
     * @param time time in ns
     */
    public void consume(long time) {
        if (remainingTime > 0) {
            remainingTime -= time;
        } else {
            for (TickWallet subWallet : subWallets) {
                if (subWallet.remainingTime > 0) {
                    subWallet.remainingTime -= time;
                    return;
                }
            }
        }
    }

    public void clearWallets() {
        subWallets.clear();
    }

    public void addWallet(TickWallet wallet) {
        subWallets.add(wallet);
    }


}
