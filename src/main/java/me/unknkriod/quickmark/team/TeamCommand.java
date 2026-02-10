package me.unknkriod.quickmark.team;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.unknkriod.quickmark.gui.screen.TeamManagementScreen;
import me.unknkriod.quickmark.network.NetworkSender;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

/**
 * Registers and handles client-side commands for team management (/qm invite, /qm leave).
 */
public class TeamCommand {
    private static final SuggestionProvider<FabricClientCommandSource> PLAYER_SUGGESTIONS =
            (context, builder) -> suggestOnlinePlayers(builder, MinecraftClient.getInstance());

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("qm")
                .then(literal("gui")
                        .executes(context -> {
                            MinecraftClient.getInstance().setScreen(new TeamManagementScreen());
                            return 1;
                        })
                )
                .then(literal("invite")
                        .then(argument("player", StringArgumentType.word())
                                .suggests(PLAYER_SUGGESTIONS)
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "player");
                                    invitePlayer(playerName);
                                    return 1;
                                })
                        )
                )
                .then(literal("leave")
                        .executes(context -> {
                            leaveTeam();
                            return 1;
                        })
                )
        );
    }

    /**
     * Suggests online players not in the team for invitation.
     */
    private static CompletableFuture<Suggestions> suggestOnlinePlayers(
            SuggestionsBuilder builder, MinecraftClient client) {

        String input = builder.getRemaining().toLowerCase();

        if (client.getNetworkHandler() != null) {
            Collection<String> playerNames = client.getNetworkHandler().getPlayerList()
                    .stream()
                    .map(PlayerListEntry::getProfile)
                    .map(GameProfile::getName)
                    .filter(Objects::nonNull)
                    .filter(name -> !name.equals(client.player.getName().getString())) // Exclude self
                    .filter(name -> !TeamManager.isPlayerInTeam(name)) // Exclude teammates
                    .toList();

            for (String name : playerNames) {
                if (name.toLowerCase().startsWith(input)) {
                    builder.suggest(name);
                }
            }
        }

        return builder.buildFuture();
    }

    private static void invitePlayer(String playerName) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (!TeamManager.isLeader(player.getUuid())) {
            player.sendMessage(Text.translatableWithFallback("quickmark.command.not_leader", "Only the team leader can invite players"), false);
            return;
        }

        UUID targetId = null;
        if (client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(playerName);
            if (entry != null) {
                targetId = entry.getProfile().getId();
            }
        }

        if (player.getUuid().equals(targetId)) {
            player.sendMessage(Text.literal("You cannot invite yourself"), false);
            return;
        }

        // Check existing invitations
        if (TeamManager.hasOutgoingInvitation(targetId)) {
            long remainingTime = TeamManager.getRemainingTimeForOutgoingInvitation(targetId);
            if (remainingTime > 0) {
                player.sendMessage(Text.translatableWithFallback("quickmark.command.invite.pending",
                        "Invitation for " + playerName + " has already been sent. Try again in " + (remainingTime/1000) + " seconds",
                        playerName, remainingTime/1000), false);
                return;
            }
        }

        if (TeamManager.hasIncomingInvitation(targetId)) {
            player.sendMessage(Text.translatableWithFallback("quickmark.command.invite.conflict",
                    "Player " + playerName + " has already sent you an invitation. Respond to it first",
                    playerName), false);
            return;
        }

        if (targetId != null) {
            TeamManager.sendInvitation(targetId);
            player.sendMessage(Text.translatableWithFallback("quickmark.command.invite.sent", "Invitation sent to " + playerName, playerName), false);
        } else {
            player.sendMessage(Text.translatableWithFallback("quickmark.command.invite.not_found", "Player " + playerName + " not found", playerName), false);
        }
    }

    private static void leaveTeam() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        TeamManager.removePlayer(player.getUuid());
        NetworkSender.sendTeamUpdate();
        player.sendMessage(Text.translatableWithFallback("quickmark.command.leave.success", "You have left the team"), false);
    }
}