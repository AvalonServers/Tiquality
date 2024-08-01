package com.github.terminatornl.tiquality.interfaces;

public interface TPSConstrained {
    short getTargetTPS();
    void setTargetTPS(short tps);

    /**
     * Increments the tick counter based on the subject's target TPS
     * @return Whether the subject should tick
     */
    boolean tickFractional();
}
