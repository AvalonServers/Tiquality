package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import cf.terminator.tiquality.interfaces.TiqualityWorld;
import cf.terminator.tiquality.tracking.TickLogger;
import cf.terminator.tiquality.tracking.TrackerBase;
import cf.terminator.tiquality.tracking.TrackerManager;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(Entity.class)
public abstract class MixinEntity implements TiqualityEntity {

    @Shadow public abstract void onUpdate();
    @Shadow public abstract UUID getPersistentID();
    @Shadow public double posX;
    @Shadow public double posY;
    @Shadow public double posZ;
    @Shadow public World world;
    private TrackerBase tracker = null;

    @Override
    public void doUpdateTick() {
        this.onUpdate();
    }

    @Override
    public BlockPos getPos() {
        return new BlockPos(this.posX, this.posY, this.posZ);
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public TickLogger.Location getLocation() {
        return new TickLogger.Location(this);
    }

    /**
     * Gets the type of this Tickable
     * @return the type
     */
    @Override
    public TickType getType() {
        return TickType.ENTITY;
    }

    @Override
    public @Nullable TrackerBase getTracker() {
        return tracker;
    }

    @Override
    public void setTracker(@Nullable TrackerBase tracker) {
        TiqualityEvent.SetEntityTrackerEvent event = new TiqualityEvent.SetEntityTrackerEvent(this, tracker);
        if(MinecraftForge.EVENT_BUS.post(event) /* is cancelled */){
            return;
        }
        this.tracker = event.getTracker();
    }

    @Inject(method = "writeToNBT", at = @At("HEAD"))
    private void TiqualityOnWrite(NBTTagCompound compound, CallbackInfoReturnable<NBTTagCompound> cir){
        if(tracker != null && tracker.shouldSaveToDisk() == true) {
            compound.setTag("Tiquality", TrackerBase.getTrackerTag(tracker));
        }
    }

    @Inject(method = "readFromNBT", at = @At("HEAD"))
    private void TiqualityOnRead(NBTTagCompound compound, CallbackInfo ci){
        if(compound.hasKey("Tiquality")) {
            tracker = TrackerManager.getTracker((TiqualityWorld) world, compound.getCompoundTag("Tiquality"));
        }
    }
}
