package com.github.terminatornl.tiquality.mixin;

import com.github.terminatornl.tiquality.interfaces.TPSConstrained;
import com.github.terminatornl.tiquality.interfaces.UpdateTyped;
import com.github.terminatornl.tiquality.tracking.UpdateType;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nonnull;

@Mixin(value = Block.class, priority = 1001)
public class MixinBlock implements UpdateTyped, TPSConstrained {
    private UpdateType tiqualityUpdateType = UpdateType.DEFAULT;
    private short tiqualityTargetTPS = 20;
    private float tiqualityFractionalTick = 0;

    @Override
    public @Nonnull
    UpdateType getUpdateType() {
        return tiqualityUpdateType;
    }

    @Override
    public void setUpdateType(@Nonnull UpdateType type) {
        tiqualityUpdateType = type;
    }

    @Override
    public short getTargetTPS() { return tiqualityTargetTPS; }

    @Override
    public void setTargetTPS(short tps) {
        if (tps <= 0 || tps > 20) tps = 20; // clamp
        tiqualityTargetTPS = tps;
    }

    @Override
    public boolean tickFractional() {
        tiqualityFractionalTick += tiqualityTargetTPS / (float) 20;
        if (tiqualityFractionalTick >= 1) {
            tiqualityFractionalTick %= 1;
            return true;
        } else {
            return false;
        }
    }
}
