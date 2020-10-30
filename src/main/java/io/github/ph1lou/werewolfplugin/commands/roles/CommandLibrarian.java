package io.github.ph1lou.werewolfplugin.commands.roles;

import io.github.ph1lou.werewolfapi.Commands;
import io.github.ph1lou.werewolfapi.PlayerWW;
import io.github.ph1lou.werewolfapi.WereWolfAPI;
import io.github.ph1lou.werewolfapi.enumlg.StatePlayer;
import io.github.ph1lou.werewolfapi.events.LibrarianRequestEvent;
import io.github.ph1lou.werewolfapi.rolesattributs.AffectedPlayers;
import io.github.ph1lou.werewolfapi.rolesattributs.LimitedUse;
import io.github.ph1lou.werewolfapi.rolesattributs.Roles;
import io.github.ph1lou.werewolfplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CommandLibrarian implements Commands {


    private final Main main;

    public CommandLibrarian(Main main) {
        this.main = main;
    }

    @Override
    public void execute(Player player, String[] args) {

        WereWolfAPI game = main.getWereWolfAPI();
        String playername = player.getName();
        UUID uuid = player.getUniqueId();
        PlayerWW plg = game.getPlayersWW().get(uuid);
        Roles librarian = plg.getRole();

        if (args.length != 1) {
            player.sendMessage(game.translate("werewolf.check.player_input"));
            return;
        }

        if (args[0].toLowerCase().equals(playername.toLowerCase())) {
            player.sendMessage(game.translate("werewolf.check.not_yourself"));
            return;
        }

        Player selectionPlayer = Bukkit.getPlayer(args[0]);

        if (selectionPlayer == null) {
            player.sendMessage(game.translate("werewolf.check.offline_player"));
            return;
        }

        UUID argUUID = selectionPlayer.getUniqueId();

        if (!game.getPlayersWW().containsKey(argUUID) || !game.getPlayersWW().get(argUUID).isState(StatePlayer.ALIVE)) {
            player.sendMessage(game.translate("werewolf.check.player_not_found"));
            return;
        }

        if(((AffectedPlayers)librarian).getAffectedPlayers().contains(argUUID)){
            player.sendMessage(game.translate("werewolf.role.librarian.waiting"));
            return;
        }


       if (((LimitedUse)librarian).getUse() >= 3) {
            player.sendMessage(game.translate("werewolf.check.power"));
            return;
        }

        ((LimitedUse) librarian).setUse(((LimitedUse) librarian).getUse() + 1);
        LibrarianRequestEvent librarianRequestEvent=new LibrarianRequestEvent(uuid,argUUID);
        Bukkit.getPluginManager().callEvent(librarianRequestEvent);

        if(librarianRequestEvent.isCancelled()){
            player.sendMessage(game.translate("werewolf.check.cancel"));
            return;
        }

        ((AffectedPlayers) librarian).addAffectedPlayer(argUUID);

        selectionPlayer.sendMessage(game.translate("werewolf.role.librarian.message"));
        player.sendMessage(game.translate("werewolf.role.librarian.perform",selectionPlayer.getName()));
    }
}
