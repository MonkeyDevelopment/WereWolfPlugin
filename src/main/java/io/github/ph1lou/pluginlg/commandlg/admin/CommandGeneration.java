package io.github.ph1lou.pluginlg.commandlg.admin;

import io.github.ph1lou.pluginlg.MainLG;
import io.github.ph1lou.pluginlg.game.GameManager;
import io.github.ph1lou.pluginlgapi.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandGeneration implements Commands {

    private final MainLG main;

    public CommandGeneration(MainLG main) {
        this.main = main;
    }


    @Override
    public void execute(CommandSender sender, String[] args) {

        GameManager game = main.currentGame;

        if (!sender.hasPermission("a.use") && !sender.hasPermission("a.generation.use") && !game.getHosts().contains(((Player) sender).getUniqueId())) {
            sender.sendMessage(game.translate("werewolf.check.permission_denied"));
            return;
        }
        game.generateMap(sender,game.getConfig().getBorderMax()/2);
    }
}