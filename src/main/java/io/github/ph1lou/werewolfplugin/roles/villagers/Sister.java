package io.github.ph1lou.werewolfplugin.roles.villagers;


import io.github.ph1lou.werewolfapi.DescriptionBuilder;
import io.github.ph1lou.werewolfapi.IPlayerWW;
import io.github.ph1lou.werewolfapi.WereWolfAPI;
import io.github.ph1lou.werewolfapi.enums.RolesBase;
import io.github.ph1lou.werewolfapi.enums.StatePlayer;
import io.github.ph1lou.werewolfapi.enums.TimersBase;
import io.github.ph1lou.werewolfapi.events.game.life_cycle.FinalDeathEvent;
import io.github.ph1lou.werewolfapi.events.game.timers.WereWolfListEvent;
import io.github.ph1lou.werewolfapi.events.roles.sister.SisterDeathEvent;
import io.github.ph1lou.werewolfapi.rolesattributs.IAffectedPlayers;
import io.github.ph1lou.werewolfapi.rolesattributs.IRole;
import io.github.ph1lou.werewolfapi.rolesattributs.RoleVillage;
import io.github.ph1lou.werewolfapi.utils.Utils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class Sister extends RoleVillage implements IAffectedPlayers {

    final List<IPlayerWW> killerWWS = new ArrayList<>();

    public Sister(WereWolfAPI api, IPlayerWW playerWW, String key) {
        super(api, playerWW, key);
    }


    @Override
    public @NotNull String getDescription() {


        return new DescriptionBuilder(game, this)
                .setDescription(() -> game.translate("werewolf.role.sister.description"))
                .setEffects(() -> game.translate("werewolf.role.sister.effect",
                        game.getConfig().getDistanceSister()))
                .addExtraLines(() -> {
                    if (game.getConfig().getTimerValue(TimersBase.WEREWOLF_LIST.getKey()) > 0) {
                        return game.translate("werewolf.role.sister.sisters_list", Utils.conversion(game.getConfig().getTimerValue(TimersBase.WEREWOLF_LIST.getKey())));
                    } else {
                        return game.translate("werewolf.role.sister.sisters_list", this.getSister());
                    }
                })
                .build();
    }


    @Override
    public void recoverPower() {

    }


    @EventHandler
    public void onWerewolfList(WereWolfListEvent event) {
        getPlayerWW().sendMessageWithKey("werewolf.role.sister.sisters_list", this.getSister());
    }

    private String getSister() {

        StringBuilder list = new StringBuilder();

        game.getPlayerWW()
                .stream()
                .filter(playerWW -> playerWW.isState(StatePlayer.ALIVE))
                .filter(playerWW -> !playerWW.getRole().equals(this))
                .filter(playerWW -> playerWW.isKey(RolesBase.SISTER.getKey()))
                .forEach(playerWW -> list.append(playerWW.getName()).append(" "));
        return list.toString();
    }

    @Override
    public void second() {

        Player sister = Bukkit.getPlayer(getPlayerUUID());

        if (sister == null) {
            return;
        }
        if (!getPlayerWW().isState(StatePlayer.ALIVE)) {
            return;
        }

        Location location = sister.getLocation();

        boolean recoverResistance = game.getPlayerWW()
                .stream()
                .filter(playerWW -> playerWW.isState(StatePlayer.ALIVE))
                .map(IPlayerWW::getRole)
                .filter(roles -> !roles.equals(this))
                .filter(roles -> roles.isKey(RolesBase.SISTER.getKey()))
                .map(IRole::getPlayerUUID)
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(player -> {
                    try {
                        return location.distance(player.getLocation()) < 20;
                    } catch (Exception ignored) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null) != null;


        if (recoverResistance) {
            
            sister.getActivePotionEffects()
                    .stream()
                    .filter(potionEffect -> potionEffect.getDuration()
                            < 100)
                    .map(PotionEffect::getType)
                    .filter(potionEffectType -> potionEffectType.equals(
                            PotionEffectType.DAMAGE_RESISTANCE))
                    .forEach(sister::removePotionEffect);
            sister.addPotionEffect(new PotionEffect(
                    PotionEffectType.DAMAGE_RESISTANCE,
                    100,
                    0,
                    false,
                    false));
        }


    }

    @EventHandler
    public void onSisterDeathReveal(SisterDeathEvent event) {

        if (event.isCancelled()) return;

        if (!getPlayerWW().isState(StatePlayer.ALIVE)) return;

        event.getAllSisters().add(getPlayerWW());

        IPlayerWW sisterWW = event.getSister();
        IPlayerWW killerWW = event.getKiller();
        TextComponent textComponent = new TextComponent(game.translate("werewolf.role.sister.choice"));

        TextComponent name = new TextComponent(
                game.translate("werewolf.role.sister.name"));
        name.setClickEvent(
                new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        String.format("/ww %s %s",
                                game.translate("werewolf.role.sister.command_name"),
                                killerWW == null ? "pve" : killerWW.getUUID().toString())));
        name.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(game.translate("werewolf.role.sister.see_name"))
                                .create()));

        textComponent.addExtra(name);

        textComponent.addExtra(game.translate("werewolf.role.sister.or"));

        TextComponent role =
                new TextComponent(game.translate("werewolf.role.sister.role"));

        role.setClickEvent(
                new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        String.format("/ww %s %s",
                                game.translate("werewolf.role.sister.command_role"),
                                killerWW == null ? "pve" : killerWW.getUUID().toString())));

        role.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(game.translate("werewolf.role.sister.see_role"))
                                .create()));

        textComponent.addExtra(role);

        getPlayerWW().sendMessage(textComponent);

        sisterWW.getLastKiller().ifPresent(killerWWS::add);

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSisterDeathRevealEnd(SisterDeathEvent event) {
        if (event.getAllSisters().isEmpty()) event.setCancelled(true);
    }

    @EventHandler
    public void onSisterDeath(FinalDeathEvent event) {

        IPlayerWW playerWW = event.getPlayerWW();

        if (!playerWW.equals(getPlayerWW())) return;

        Bukkit.getPluginManager().callEvent(new SisterDeathEvent(playerWW,
                new HashSet<>(), getPlayerWW().getLastKiller().isPresent() ? getPlayerWW().getLastKiller().get() : null));

    }

    @Override
    public void addAffectedPlayer(IPlayerWW playerWW) {
        killerWWS.add(playerWW);
    }

    @Override
    public void removeAffectedPlayer(IPlayerWW playerWW) {
        killerWWS.remove(playerWW);
    }

    @Override
    public void clearAffectedPlayer() {
        killerWWS.clear();
    }

    @Override
    public List<IPlayerWW> getAffectedPlayers() {
        return killerWWS;
    }
}
