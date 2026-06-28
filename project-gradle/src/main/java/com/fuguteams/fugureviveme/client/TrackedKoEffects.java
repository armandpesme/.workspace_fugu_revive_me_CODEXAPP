package com.fuguteams.fugureviveme.client;

import com.fuguteams.fugureviveme.network.ClientboundTrackedKoVisual;
import com.fuguteams.fugureviveme.state.ReviveActionType;
import com.fuguteams.fugureviveme.state.ReviveState;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

final class TrackedKoEffects {
    private static final Set<Integer> POSE_PUSHED = new HashSet<>();

    private TrackedKoEffects() {
        throw new AssertionError("TrackedKoEffects is a static utility and must not be instantiated");
    }

    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || level.getGameTime() % 10L != 0L) {
            return;
        }
        for (Player player : level.players()) {
            if (player == minecraft.player) {
                continue;
            }
            ClientReviveStateStore.get().trackedVisual(player.getId())
                    .filter(TrackedKoEffects::isVisibleKo)
                    .ifPresent(visual -> spawnParticles(level, player, visual));
        }
    }

    static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player) {
            return;
        }
        Optional<ClientboundTrackedKoVisual> visual = ClientReviveStateStore.get().trackedVisual(player.getId())
                .filter(TrackedKoEffects::isVisibleKo);
        if (visual.isEmpty()) {
            return;
        }
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(0.0D, -0.18D, 0.0D);
        float tilt = visual.get().state() == ReviveState.FULLY_DOWNED ? 82.0F : 64.0F;
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(tilt));
        POSE_PUSHED.add(player.getId());
    }

    static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (POSE_PUSHED.remove(player.getId())) {
            event.getPoseStack().popPose();
        }
    }

    private static boolean isVisibleKo(ClientboundTrackedKoVisual visual) {
        return visual.state() == ReviveState.TEMPORARY_KO
                || visual.state() == ReviveState.PROLONGED_KO
                || visual.state() == ReviveState.FULLY_DOWNED;
    }

    private static void spawnParticles(ClientLevel level, Player player, ClientboundTrackedKoVisual visual) {
        ParticleOptions particle = visual.state() == ReviveState.FULLY_DOWNED ? ParticleTypes.SMOKE : ParticleTypes.SOUL;
        double x = player.getX();
        double y = player.getY() + 0.25D;
        double z = player.getZ();
        level.addParticle(particle, x, y, z, 0.0D, 0.03D, 0.0D);
        if (visual.actionType() == ReviveActionType.ALLY_REVIVE || visual.actionType() == ReviveActionType.SELF_REVIVE) {
            level.addParticle(ParticleTypes.ENCHANT, x, y + 0.35D, z, 0.0D, 0.05D, 0.0D);
        }
    }
}
