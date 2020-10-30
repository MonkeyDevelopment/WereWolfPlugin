package io.github.ph1lou.werewolfplugin.commands.admin.ingame;

import io.github.ph1lou.werewolfapi.Commands;
import io.github.ph1lou.werewolfapi.ModerationManagerAPI;
import io.github.ph1lou.werewolfapi.enumlg.StateGame;
import io.github.ph1lou.werewolfapi.enumlg.StatePlayer;
import io.github.ph1lou.werewolfapi.events.ModeratorEvent;
import io.github.ph1lou.werewolfplugin.Main;
import io.github.ph1lou.werewolfplugin.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CommandModerator implements Commands {


    private final Main main;

    public CommandModerator(Main main) {
        this.main = main;
    }

    @Override
    public void execute(Player player, String[] args) {


        GameManager game = (GameManager) main.getWereWolfAPI();
        ModerationManagerAPI moderationManager = game.getModerationManager();
        Player moderator = Bukkit.getPlayer(args[0]);

        if (moderator == null) {
            player.sendMessage(game.translate("werewolf.check.offline_player"));
            return;
        }

        UUID argUUID = moderator.getUniqueId();

        if (moderationManager.getModerators().contains(argUUID)) {
            Bukkit.broadcastMessage(game.translate("werewolf.commands.admin.moderator.remove", args[0]));
            moderationManager.getModerators().remove(argUUID);

            if (game.isState(StateGame.LOBBY)) {
                game.finalJoin(moderator);
            }
            Bukkit.getPluginManager().callEvent(new ModeratorEvent(argUUID, false));
            return;
        }

        if (!game.isState(StateGame.LOBBY)) {
            if (game.getPlayersWW().containsKey(argUUID) && !game.getPlayersWW().get(argUUID).isState(StatePlayer.DEATH)) {
                player.sendMessage(game.translate("werewolf.commands.admin.moderator.player_living"));
                return;
            }
        } else {
            if (game.getPlayersWW().containsKey(argUUID)) {
                game.getScore().removePlayerSize();
                game.getPlayersWW().remove(argUUID);
            } else {
                moderationManager.getQueue().remove(argUUID);
            }
            game.getModerationManager().checkQueue();
        }
        moderator.setGameMode(GameMode.SPECTATOR);
        moderationManager.getModerators().add(argUUID);
        Bukkit.broadcastMessage(game.translate("werewolf.commands.admin.moderator.add", args[0]));
        Bukkit.getPluginManager().callEvent(new ModeratorEvent(argUUID, true));
    }
}
