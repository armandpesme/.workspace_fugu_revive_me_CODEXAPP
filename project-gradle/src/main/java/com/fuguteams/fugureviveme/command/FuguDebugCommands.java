package com.fuguteams.fugureviveme.command;

import com.fuguteams.fugureviveme.FuguReviveMe;
import com.fuguteams.fugureviveme.server.FuguKnockoutRuntime;
import com.fuguteams.fugureviveme.state.KoRecord;
import com.fuguteams.fugureviveme.state.ReviveState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = FuguReviveMe.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FuguDebugCommands {
    private static final int REQUIRED_PERMISSION_LEVEL = 4;

    private FuguDebugCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(root());
    }

    static LiteralArgumentBuilder<CommandSourceStack> root() {
        return Commands.literal("fugurevive")
                .requires(source -> source.hasPermission(REQUIRED_PERMISSION_LEVEL))
                .then(Commands.literal("test")
                        .then(Commands.literal("kotemporaire")
                                .then(Commands.argument("cible", EntityArgument.player())
                                        .executes(context -> forceKo(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "cible"),
                                                ReviveState.TEMPORARY_KO))))
                        .then(Commands.literal("kopermanant")
                                .then(Commands.argument("cible", EntityArgument.player())
                                        .executes(context -> forceKo(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "cible"),
                                                ReviveState.PROLONGED_KO)))));
    }

    private static int forceKo(CommandSourceStack source, ServerPlayer target, ReviveState state) {
        FuguKnockoutRuntime runtime = FuguKnockoutRuntime.get();
        int durationTicks = state == ReviveState.TEMPORARY_KO
                ? runtime.config().temporaryDurationTicks()
                : runtime.config().prolongedDurationTicks();
        KoRecord record = createTestRecord(
                state,
                durationTicks,
                target.serverLevel().dimension().location(),
                target.blockPosition(),
                target.serverLevel().getGameTime());
        runtime.revive().updateRecord(target.getUUID(), record);
        source.sendSuccess(() -> Component.translatable(
                "commands.fugu_revive_me.test_ko.success",
                target.getGameProfile().getName(),
                state.name()), true);
        return Command.SINGLE_SUCCESS;
    }

    static KoRecord createTestRecord(
            ReviveState state,
            int durationTicks,
            ResourceLocation dimension,
            BlockPos position,
            long serverGameTime
    ) {
        if (state != ReviveState.TEMPORARY_KO && state != ReviveState.PROLONGED_KO) {
            throw new IllegalArgumentException("Test KO command only supports temporary or prolonged states");
        }
        return new KoRecord(
                state,
                serverGameTime + durationTicks,
                0,
                dimension,
                position,
                Optional.empty());
    }
}
