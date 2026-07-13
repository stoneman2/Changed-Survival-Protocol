package net.stonenibbler.changed_survive_protocol.common.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.stonenibbler.changed_survive_protocol.common.collapse.FeralBodySpawner;
import net.stonenibbler.changed_survive_protocol.common.data.CSPCapabilities;
import net.stonenibbler.changed_survive_protocol.common.data.CSPPlayerData;
import net.stonenibbler.changed_survive_protocol.common.event.CSPPlayerEvents;
import net.stonenibbler.changed_survive_protocol.common.event.CSPTransfurEvents;
import net.stonenibbler.changed_survive_protocol.common.util.CSPTransfurState;

public final class CSPCommands {
    private static final SimpleCommandExceptionType MISSING_PLAYER_DATA = new SimpleCommandExceptionType(
            Component.literal("CSP player data is not available. Try rejoining; report this if it persists."));
    private static final SimpleCommandExceptionType INVALID_LUCIDITY_FORM = new SimpleCommandExceptionType(
            Component.literal("This command requires a non-suit transfur that uses CSP lucidity."));

    private CSPCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("csp")
                .requires(source -> source.hasPermission(2))
                .then(percentGroup("infection",
                        data -> data.getInfectionPercent(),
                        (data, value) -> data.setInfectionPercent(value),
                        CSPPlayerData::clearInfection))
                .then(percentGroup("coverage",
                        CSPPlayerData::getCoverage,
                        CSPPlayerData::setCoverage,
                        CSPPlayerData::clearCoverage))
                .then(percentGroup("lucidity",
                        CSPPlayerData::getLucidity,
                        CSPPlayerData::setLucidity,
                        (data) -> data.setLucidity(100.0D)))
                .then(Commands.literal("stabilize")
                        .executes(context -> mutateLucidity(context.getSource(), data -> {
                            data.setStabilizedLatex(true);
                            data.setLucidityActive(false);
                            data.setUnstableLatex(false);
                            data.setUnstableLatexTicks(0);
                            data.setLucidityDrainMultiplier(1.0D);
                        }, "stabilized latex")))
                .then(Commands.literal("unstabilize")
                        .executes(context -> mutateLucidity(context.getSource(), data -> {
                            data.setStabilizedLatex(false);
                            data.setLucidityActive(true);
                            data.setUnstableLatex(true);
                        }, "unstabilized latex")))
                .then(Commands.literal("forcecollapse")
                        .executes(context -> forceCollapse(context.getSource())))
                .then(Commands.literal("clearall")
                        .executes(context -> mutate(context.getSource(), CSPPlayerData::reset, "cleared CSP data")))
                .then(Commands.literal("forcetransfur")
                        .executes(context -> forceTransfur(context.getSource()))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> percentGroup(String name, Getter getter, Setter setter, Clearer clearer) {
        return Commands.literal(name)
                .then(Commands.literal("get")
                        .executes(context -> get(context.getSource(), name, getter)))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                .executes(context -> mutate(context.getSource(), data -> setter.set(data, DoubleArgumentType.getDouble(context, "value")),
                                        "set " + name + " to " + format(DoubleArgumentType.getDouble(context, "value"))))))
                .then(Commands.literal("clear")
                        .executes(context -> mutate(context.getSource(), clearer::clear, "cleared " + name)));
    }

    private static int get(CommandSourceStack source, String name, Getter getter) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CSPPlayerData data = data(player);
        source.sendSuccess(() -> Component.literal(name + ": " + format(getter.get(data))), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int mutateLucidity(CommandSourceStack source, Mutator mutator, String message) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CSPPlayerData data = lucidityData(player);
        mutator.mutate(data);
        CSPPlayerEvents.sync(player);
        source.sendSuccess(() -> Component.literal(message), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int mutate(CommandSourceStack source, Mutator mutator, String message) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CSPPlayerData data = data(player);
        mutator.mutate(data);
        CSPPlayerEvents.sync(player);
        source.sendSuccess(() -> Component.literal(message), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int forceCollapse(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CSPPlayerData data = lucidityData(player);
        FeralBodySpawner.forceCollapse(player, data);
        source.sendSuccess(() -> Component.literal("forced lucidity collapse"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int forceTransfur(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CSPPlayerData data = data(player);
        data.setInfected(true);
        data.setInfectionPercent(100.0D);
        CSPTransfurEvents.forceUncontrolledTransfur(player, data);
        CSPPlayerEvents.sync(player);
        source.sendSuccess(() -> Component.literal("forced uncontrolled CSP transfur"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static CSPPlayerData data(ServerPlayer player) throws CommandSyntaxException {
        return CSPCapabilities.get(player).resolve().orElseThrow(MISSING_PLAYER_DATA::create);
    }

    private static CSPPlayerData lucidityData(ServerPlayer player) throws CommandSyntaxException {
        if (!CSPTransfurState.usesLucidity(player)) {
            throw INVALID_LUCIDITY_FORM.create();
        }
        return data(player);
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    @FunctionalInterface
    private interface Getter {
        double get(CSPPlayerData data);
    }

    @FunctionalInterface
    private interface Setter {
        void set(CSPPlayerData data, double value);
    }

    @FunctionalInterface
    private interface Clearer {
        void clear(CSPPlayerData data);
    }

    @FunctionalInterface
    private interface Mutator {
        void mutate(CSPPlayerData data);
    }
}
