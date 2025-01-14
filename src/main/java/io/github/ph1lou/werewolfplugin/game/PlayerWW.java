package io.github.ph1lou.werewolfplugin.game;

import io.github.ph1lou.werewolfapi.Formatter;
import io.github.ph1lou.werewolfapi.ILover;
import io.github.ph1lou.werewolfapi.IPlayerWW;
import io.github.ph1lou.werewolfapi.MessageAction;
import io.github.ph1lou.werewolfapi.PotionModifier;
import io.github.ph1lou.werewolfapi.WereWolfAPI;
import io.github.ph1lou.werewolfapi.enums.RolesBase;
import io.github.ph1lou.werewolfapi.enums.Sound;
import io.github.ph1lou.werewolfapi.enums.StateGame;
import io.github.ph1lou.werewolfapi.enums.StatePlayer;
import io.github.ph1lou.werewolfapi.rolesattributs.IRole;
import io.github.ph1lou.werewolfapi.utils.BukkitUtils;
import io.github.ph1lou.werewolfapi.utils.NMSUtils;
import io.github.ph1lou.werewolfapi.versions.VersionUtils;
import io.github.ph1lou.werewolfplugin.roles.villagers.Villager;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


public class PlayerWW implements IPlayerWW {

    private StatePlayer state = StatePlayer.ALIVE;
    private final List<ILover> lovers = new ArrayList<>();
    private final Map<PotionModifier,Integer> potionModifiers = new HashMap<>();
    private final List<MessageAction> disconnectedMessages = new ArrayList<>();
    private final List<ItemStack> decoItems = new ArrayList<>();
    private int maxHealth = 20;
    private Location disconnectedLocation = null;
    private int disconnectedChangeHealth = 0;
    @Nullable
    private UUID mojangUUID = null;
    private final List<IPlayerWW> killer = new ArrayList<>();
    private final UUID uuid;
    private IRole role;
    private int disconnectedChangeMaxHealth = 0;
    private final List<ItemStack> itemsDeath = new ArrayList<>();
    private transient Location spawn;
    private int deathTime = 0;
    private int disconnectedTime = 0;
    private String name;
    private final WereWolfAPI game;
    private final List<IPlayerWW> playersKilled = new ArrayList<>();


    public PlayerWW(WereWolfAPI api, Player player) {
        this.spawn = player.getWorld().getSpawnLocation();
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.role = new Villager(api, this,
                RolesBase.VILLAGER.getKey());
        this.game = api;
        try {
            Object server = NMSUtils.getNMSClass("MinecraftServer")
                    .getMethod("getServer").invoke(null);
            Object userCache = server.getClass().getMethod("getUserCache").invoke(server);
            Object profile = userCache.getClass().getMethod("getProfile",String.class).invoke(userCache,name);
            this.mojangUUID = (UUID) profile.getClass().getMethod("getId").invoke(profile);
        } catch (NullPointerException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
        }
    }

    @Override
    public void addPlayerHealth(int health) {

        Player player = Bukkit.getPlayer(this.uuid);

        if (player != null) {
            player.setHealth(Math.min(player.getHealth() + health, VersionUtils.getVersionUtils().getPlayerMaxHealth(player)));
            return;
        }

        this.disconnectedChangeHealth += health;
    }

    @Override
    public void removePlayerHealth(int health) {

        Player player = Bukkit.getPlayer(this.uuid);

        if (player != null) {
            player.setHealth(player.getHealth() - health);
            return;
        }

        this.disconnectedChangeHealth -= health;

    }

    @Override
    public void addPlayerMaxHealth(int health) {

        Player player = Bukkit.getPlayer(uuid);

        this.maxHealth += health;

        if (player != null) {
            VersionUtils.getVersionUtils().addPlayerMaxHealth(player, health);
            return;
        }

        this.disconnectedChangeMaxHealth += health;
    }

    @Override
    public void removePlayerMaxHealth(int health) {

        Player player = Bukkit.getPlayer(uuid);

        this.maxHealth -= health;

        if (player != null) {
            VersionUtils.getVersionUtils().removePlayerMaxHealth(player, health);

            if (player.getHealth() > VersionUtils.getVersionUtils().getPlayerMaxHealth(player)) {
                player.setHealth(VersionUtils.getVersionUtils().getPlayerMaxHealth(player));
            }
            return;
        }

        this.disconnectedChangeMaxHealth -= health;
    }

    @Override
    public void sendMessageWithKey(String key, Object... args) {
        this.sendMessage(new TextComponent(game.translate(key, args)));
    }

    @Override
    public void sendMessageWithKey(String key, Sound sound, Object... args) {
        this.sendMessage(new TextComponent(game.translate(key, args)), sound);
    }

    @Override
    public void sendMessageWithKey(String key, Formatter... formatters) {
        this.sendMessageWithKey(key,null,formatters);
    }

    @Override
    public void sendMessageWithKey(String key,@Nullable Sound sound, Formatter... formatters) {
        String message = game.translate(key);
        for(Formatter formatter :formatters){
            message = formatter.handle(message);
        }
        this.sendMessage(new TextComponent(message), sound);
    }

    @Override
    public void sendMessage(TextComponent textComponent) {
        this.sendMessage(textComponent, null);
    }

    @Override
    public void sendMessage(TextComponent textComponent,@Nullable Sound sound) {

        Player player = Bukkit.getPlayer(uuid);

        if (player != null) {
            player.spigot().sendMessage(textComponent);
            if (sound != null) {
                sound.play(player);
            }
            return;
        }

        disconnectedMessages.add(new MessageAction(textComponent, sound));
    }

    @Override
    public void addPotionModifier(PotionModifier potionModifier) {

        Player player = Bukkit.getPlayer(this.uuid);

        if(!potionModifier.isAdd()){
            new ArrayList<>(this.potionModifiers.keySet())
                    .stream()
                    .filter(potionModifier1 -> potionModifier1.getIdentifier().equals(potionModifier.getIdentifier()))
                    .filter(potionModifier1 -> potionModifier1.getPotionEffectType().equals(potionModifier.getPotionEffectType()))
                    .forEach(potionModifier1 -> {
                        int id = this.potionModifiers.remove(potionModifier1);

                        if(id != -1){
                            Bukkit.getScheduler().cancelTask(id);
                        }

                        if(player!=null){
                            player.removePotionEffect(potionModifier1.getPotionEffectType());
                            this.potionModifiers.keySet().stream()
                                    .filter(potionModifier2 -> potionModifier2.getPotionEffectType().equals(potionModifier1.getPotionEffectType()))
                                    .max(Comparator.comparing(potionModifier2 -> 20 * (potionModifier2.getTimer() - game.getTimer()) + potionModifier2.getDuration()))
                                    .ifPresent(potionModifier2 -> player.addPotionEffect(
                                            new PotionEffect(
                                                    potionModifier2.getPotionEffectType(),
                                                    20 * (potionModifier2.getTimer() - game.getTimer()) + potionModifier2.getDuration(),
                                                    potionModifier2.getAmplifier(),
                                                    false,
                                                    false)));
                        }
                    });

            return;
        }
        AtomicBoolean find = new AtomicBoolean(false);
        new ArrayList<>(this.potionModifiers.keySet())
                .stream()
                .filter(potionModifier1 -> potionModifier1.getPotionEffectType().equals(potionModifier.getPotionEffectType()))
                .forEach(potionModifier1 -> {
                    if(20 * (potionModifier1.getTimer() - game.getTimer()) +
                            potionModifier1.getDuration()   < potionModifier.getDuration()){

                        if(potionModifier1.getIdentifier().equals(potionModifier.getIdentifier())){
                            int id = this.potionModifiers.remove(potionModifier1);
                            if(id != -1){
                                Bukkit.getScheduler().cancelTask(id);
                            }
                        }
                        if(player!=null){
                            player.removePotionEffect(potionModifier.getPotionEffectType());
                        }
                    }
                    else if(potionModifier1.getIdentifier().equals(potionModifier.getIdentifier())){
                        find.set(true);
                    }
                });

        if(find.get()){
            return;
        }

        if(potionModifier.getDuration()<1000000000){
            potionModifier.setTimer(game.getTimer());
            this.potionModifiers.put(potionModifier,BukkitUtils.scheduleSyncDelayedTask(() -> {
                if(!this.game.isState(StateGame.END)){
                    this.addPotionModifier(PotionModifier.remove(potionModifier.getPotionEffectType(),
                            potionModifier.getIdentifier()));
                }
            },potionModifier.getDuration()));
        }
        else {
            this.potionModifiers.put(potionModifier,-1);
        }

        if(player!=null){
            player.addPotionEffect(new PotionEffect(potionModifier.getPotionEffectType(),
                    potionModifier.getDuration(),
                    potionModifier.getAmplifier(),
                    false,
                    false));
        }

    }

    @Override
    public void clearPotionEffects() {
        this.potionModifiers.clear();

        Player player = Bukkit.getPlayer(this.uuid);

        if(player!=null){
            player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
        }
    }

    @Override
    public Set<? extends PotionModifier> getPotionModifiers() {

        return this.potionModifiers.keySet();
    }

    @Override
    public void teleport(Location location) {

        Player player = Bukkit.getPlayer(uuid);

        if (player != null) {
            player.teleport(location);
            return;
        }

        disconnectedLocation = location.clone();

    }

    @Override
    public List<ItemStack> getItemDeath() {
        return this.itemsDeath;
    }

    @Override
    public void setItemDeath(ItemStack[] itemStacks) {
        itemsDeath.clear();
        itemsDeath.addAll(new ArrayList<>(Arrays.asList(itemStacks)));
    }

    @Override
    public void clearItemDeath() {
        this.itemsDeath.clear();
    }

    @Override
    public void setState(StatePlayer state) {
        this.state = state;
    }

	@Override
	public boolean isState(StatePlayer state) {
		return(this.state==state);
	}

    @Override
    public void addOneKill(IPlayerWW playerWW) {
        this.playersKilled.add(playerWW);
    }

    @Override
    public List<? extends IPlayerWW> getPlayersKills() {
        return this.playersKilled;
    }

    @Override
    public UUID getMojangUUID() {

        if (this.mojangUUID != null) {
            return this.mojangUUID;
        }
        return this.uuid;
    }

    @Override
    public IRole getRole() {
        return (this.role);
    }

    @Override
    public void setRole(IRole role) {
        this.role = role;
        this.role.setPlayerWW(this);
    }

    @Override
	public void setSpawn(Location spawn) {
		this.spawn=spawn;
	}

    @Override
    public Location getSpawn() {
        return (this.spawn);
    }

    @Override
    public List<ILover> getLovers() {
        return (this.lovers);
    }

    @Override
    public void addLover(ILover lover) {
        lovers.add(lover);
    }

    @Override
    public void removeLover(ILover lover) {
        lovers.remove(lover);
    }

    @Override
    public void addKiller(IPlayerWW killerUUID) {
        this.killer.add(killerUUID);
    }

    @Override
    public List<? extends IPlayerWW> getKillers() {
        return (this.killer);
    }

	@Override
	public void setDeathTime(int deathTime) {
		this.deathTime =deathTime;
	}

	@Override
	public int getDeathTime() {
		return(this.deathTime);
	}

    @Nullable
    @Override
    public Optional<IPlayerWW> getLastKiller() {
        return this.killer.isEmpty() ?
                Optional.empty() :
                this.killer.get(this.killer.size() - 1) == null ?
                        Optional.empty() :
                        Optional.of(this.killer.get(this.killer.size() - 1));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public @NotNull StatePlayer getState() {
        return state;
    }

    @Override
    public int getDisconnectedTime() {
        return disconnectedTime;
    }

    @Override
    public void setDisconnectedTime(int disconnectedTime) {
        this.disconnectedTime = disconnectedTime;
    }

    @Override
    public @NotNull UUID getUUID() {
        return uuid;
    }


    @Override
    public void updateAfterReconnect(Player player) {

        this.decoItems.forEach(this::addItem);

        addPlayerMaxHealth(this.disconnectedChangeMaxHealth);
        addPlayerHealth(this.disconnectedChangeHealth);

        this.updatePotionEffects(player);

        this.disconnectedMessages.forEach(messageAction -> {
            player.spigot().sendMessage(messageAction.getMessage());
            messageAction.getSound().ifPresent(sound -> sound.play(player));
        });

        player.teleport(this.disconnectedLocation);

        this.disconnectedChangeHealth = 0;
        this.disconnectedChangeMaxHealth = 0;
        this.disconnectedMessages.clear();
        this.decoItems.clear();
    }

    public void updatePotionEffects(Player player) {
        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));

        this.potionModifiers.keySet()
                .stream()
                .filter(PotionModifier::isAdd)
                .forEach(potionModifier -> {
                    int duration = potionModifier.getDuration();
                    if(duration<1000000000){
                        duration-=(this.game.getTimer()-potionModifier.getTimer())*20;
                    }
                    player.addPotionEffect(
                            new PotionEffect(
                                    potionModifier.getPotionEffectType(),
                                    duration,
                                    potionModifier.getAmplifier(),
                                    false,
                                    false));
                });
    }

    @Override
    public void setDisconnectedLocation(Location location) {
        disconnectedLocation = location;
    }

    @Override
    public void addItem(ItemStack itemStack) {

        if (itemStack == null) return;

        Player player = Bukkit.getPlayer(uuid);

        if (player != null) {
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItem(player.getLocation(), itemStack);
            } else {
                player.getInventory().addItem(itemStack);
                player.updateInventory();
            }
            return;
        }

        decoItems.add(itemStack);
    }

    @Override
    public int getMaxHealth() {
        return maxHealth;
    }

    @Override
    public @NotNull Location getLocation() {

        Player player = Bukkit.getPlayer(uuid);

        if (player == null) return disconnectedLocation;

        return player.getLocation().clone();
    }


}

