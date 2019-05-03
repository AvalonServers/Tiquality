package cf.terminator.tiquality.mixin;

import cf.terminator.tiquality.tracking.TickHub;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(value = World.class, priority = 1001)
public class MixinWorldForge {

    @Redirect(method="immediateBlockTick", at = @At(value = "INVOKE", target = "net/minecraft/block/Block.updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"), require = 1)
    private void onBlockTick(Block block, World worldIn, BlockPos pos, IBlockState state, Random rand){
        TickHub.onBlockTick(block, worldIn, pos, state, rand);
    }

    @Redirect(method = "updateEntities", require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ITickable;update()V"))
    private void onUpdateTileEntities(ITickable tile) {
        TickHub.onTileEntityTick(tile);
    }

    @Redirect(method = "updateEntities", require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onUpdate()V"))
    private void onUpdateEntities(Entity entity){
        TickHub.onEntityTick(entity);
    }
}