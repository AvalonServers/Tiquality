package cf.terminator.tiquality.integration.griefprevention;

import cf.terminator.tiquality.api.event.TiqualityEvent;
import cf.terminator.tiquality.integration.griefprevention.event.GPClaimCreatedFullyEvent;
import cf.terminator.tiquality.interfaces.TiqualityEntity;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Set;

public class EventHandler {

    public static final EventHandler INSTANCE = new EventHandler();

    private EventHandler(){

    }

    @SubscribeEvent
    public void onClaimCreate(GPClaimCreatedFullyEvent e){
        if(GriefPreventionHook.isValidClaim(e.getClaim()) == false){
            return;
        }
        GriefPreventionTracker tracker = GriefPreventionHook.findOrGetTrackerByClaim(e.getClaim());
        tracker.setBlockTrackers(null,null);
    }

    @SubscribeEvent
    public void onSetTracker(TiqualityEvent.SetBlockTrackerEvent e){
        if(e.getTracker() instanceof GriefPreventionTracker){
            return;
        }
        ClaimManager claimManager = GriefPrevention.getApi().getClaimManager((World) e.getMinecraftWorld());
        Location<World> pos = new Location<>((World) e.getMinecraftWorld(), e.getPos().getX(), e.getPos().getY(), e.getPos().getZ());

        Claim claim = claimManager.getClaimAt(pos);
        if(GriefPreventionHook.isValidClaim(claim) == false){
            return;
        }
        GriefPreventionTracker tracker = GriefPreventionHook.findOrGetTrackerByClaim(claim);
        e.setTracker(tracker);
    }

    @SubscribeEvent
    public void onSetTracker(TiqualityEvent.SetChunkTrackerEvent e){
        if(e.getTracker() instanceof GriefPreventionTracker){
            return;
        }
        Chunk chunk = (Chunk) e.getChunk();
        ClaimManager claimManager = GriefPrevention.getApi().getClaimManager((World) chunk.getWorld());

        long chunkLong = ChunkPos.asLong(chunk.x, chunk.z);

        Set<Claim> claimSet = claimManager.getChunksToClaimsMap().get(chunkLong);
        if(claimSet == null){
            /* There are no claims in this chunk. */
            return;
        }
        for(Claim claim : claimSet){
            if(GriefPreventionHook.isValidClaim(claim)){
                e.setPerBlockMode();
                return;
            }
        }

    }

    @SubscribeEvent
    public void onSpawn(EntityJoinWorldEvent e){
        ClaimManager manager = GriefPrevention.getApi().getClaimManager((World) e.getWorld());
        Location<World> pos = new Location<>((World) e.getWorld(), e.getEntity().posX, e.getEntity().posY, e.getEntity().posZ);

        Claim claim = manager.getClaimAt(pos);
        if(GriefPreventionHook.isValidClaim(claim) == false){
            return;
        }

        GriefPreventionTracker tracker = GriefPreventionHook.findOrGetTrackerByClaim(claim);
        ((TiqualityEntity) e.getEntity()).setTracker(tracker);
    }
}
