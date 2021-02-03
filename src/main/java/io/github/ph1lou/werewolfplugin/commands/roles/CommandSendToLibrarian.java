package io.github.ph1lou.werewolfplugin.commands.roles;

import io.github.ph1lou.werewolfapi.Commands;
import io.github.ph1lou.werewolfapi.PlayerWW;
import io.github.ph1lou.werewolfapi.WereWolfAPI;
import io.github.ph1lou.werewolfapi.enums.RolesBase;
import io.github.ph1lou.werewolfapi.enums.StatePlayer;
import io.github.ph1lou.werewolfapi.events.LibrarianGiveBackEvent;
import io.github.ph1lou.werewolfapi.rolesattributs.AffectedPlayers;
import io.github.ph1lou.werewolfapi.rolesattributs.Storage;
import io.github.ph1lou.werewolfplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandSendToLibrarian implements Commands {

    private final Main main;

    public CommandSendToLibrarian(Main main) {
        this.main = main;
    }

    @Override
    public void execute(Player player, String[] args) {

        WereWolfAPI game = main.getWereWolfAPI();
        UUID uuid = player.getUniqueId();
        PlayerWW playerWW = game.getPlayerWW(uuid);

        if (playerWW == null) return;


        if (args.length == 0) {
            playerWW.sendMessageWithKey("werewolf.check.parameters", 1);
            return;
        }

        AtomicBoolean find = new AtomicBoolean(false);

        StringBuilder sb2 = new StringBuilder();

        for (String w : args) {
            sb2.append(w).append(" ");
        }
        game.getPlayerWW()
                .stream()
                .filter(playerWW1 -> playerWW1.isState(StatePlayer.ALIVE))
                .filter(playerWW1 -> playerWW1.isKey(RolesBase.LIBRARIAN.getKey()))
                .map(PlayerWW::getRole)
                .filter(roles -> ((AffectedPlayers) roles).getAffectedPlayers().contains(playerWW))
                .forEach(roles -> {

                    ((AffectedPlayers) roles).removeAffectedPlayer(playerWW);
                    LibrarianGiveBackEvent librarianGiveBackEvent =
                            new LibrarianGiveBackEvent(playerWW,
                                    roles.getPlayerWW(),
                                    sb2.toString());

                    Bukkit.getPluginManager().callEvent(librarianGiveBackEvent);

                    if (librarianGiveBackEvent.isCancelled()) {
                        playerWW.sendMessageWithKey("werewolf.check.cancel");
                        return;
                    }

                    ((Storage) roles).getStorage().add(sb2.toString());

                    playerWW.sendMessageWithKey("werewolf.role.librarian.contribute");
                    find.set(true);
                    librarianGiveBackEvent.getTargetWW().sendMessageWithKey(
                            "werewolf.role.librarian.contribution",
                            player.getName(),
                            librarianGiveBackEvent.getInfo());
                });


        if (!find.get()) {
            playerWW.sendMessageWithKey("werewolf.role.librarian.prohibit");
        }


    }
}
