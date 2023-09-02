package com.bawnorton.dabrsc;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class DABRCS extends JavaPlugin implements Listener {
    private static final String HANDSHAKE_CHANNEL = "do_a_barrel_roll:handshake";

    FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        getLogger().info("Loaded DABRCS");

        config.addDefault("allowThrusting", true);
        config.addDefault("forceEnabled", false);
        saveConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, HANDSHAKE_CHANNEL);

        Objects.requireNonNull(getCommand("dabrcs"), "unreachable").setExecutor((commandSender, command, s, strings) -> {
            if(!commandSender.isOp()) {
                commandSender.sendMessage("You must be an operator to use this command.");
                return true;
            }

            if (strings.length == 0) {
                commandSender.sendMessage("Usage: /dabrcs <allowThrusting|forceEnabled> <true|false>");
                return true;
            }

            if (strings.length == 1) {
                if (strings[0].equals("allowThrusting")) {
                    commandSender.sendMessage("allowThrusting: " + config.getBoolean("allowThrusting"));
                    return true;
                }

                if (strings[0].equals("forceEnabled")) {
                    commandSender.sendMessage("forceEnabled: " + config.getBoolean("forceEnabled"));
                    return true;
                }

                commandSender.sendMessage("Usage: /dabrcs <allowThrusting|forceEnabled> <true|false>");
                return true;
            }

            if (strings.length == 2) {
                if (strings[0].equals("allowThrusting")) {
                    config.set("allowThrusting", Boolean.parseBoolean(strings[1]));
                    saveConfig();
                    sendConfigToAllPlayers();
                    commandSender.sendMessage("Set allowThrusting to " + config.getBoolean("allowThrusting"));
                    return true;
                }

                if (strings[0].equals("forceEnabled")) {
                    config.set("forceEnabled", Boolean.parseBoolean(strings[1]));
                    saveConfig();
                    sendConfigToAllPlayers();
                    commandSender.sendMessage("Set forceEnabled to " + config.getBoolean("forceEnabled"));
                    return true;
                }

                commandSender.sendMessage("Usage: /dabrcs <allowThrusting|forceEnabled> <true|false>");
                return true;
            }

            commandSender.sendMessage("Usage: /dabrcs <allowThrusting|forceEnabled> <true|false>");
            return true;
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("Unloaded DABRCS");
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        getServer().getScheduler().runTaskLater(this, () -> sendConfig(event.getPlayer()), 20L);

    }

    private void writeVarInt(ByteArrayDataOutput out, int value) {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        out.writeByte(value);
    }

    private void sendConfigToAllPlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            sendConfig(player);
        }
    }

    private void sendConfig(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        String config = String.format("{\"allowThrusting\": %s, \"forceEnabled\": %s}", getConfig().getBoolean("allowThrusting"), getConfig().getBoolean("forceEnabled"));

        out.writeInt(3);
        writeVarInt(out, config.length());
        out.write(config.getBytes(StandardCharsets.UTF_8));
        out.writeBoolean(true);

        DABRCS plugin = JavaPlugin.getPlugin(DABRCS.class);
        player.sendPluginMessage(plugin, HANDSHAKE_CHANNEL, out.toByteArray());
    }
}
