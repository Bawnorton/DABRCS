package com.bawnorton.dabrsc;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.tcoded.folialib.FoliaLib;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class DABRCS extends JavaPlugin implements Listener {
    private static final String CONFIG_SYNC_CHANNEL = "do_a_barrel_roll:config_sync";

    private FoliaLib foliaLib;

    @Override
    public void onEnable() {
        foliaLib = new FoliaLib(this);

        // Ensure config file exists and defaults are actually written when missing
        saveDefaultConfig();
        getConfig().addDefault("allowThrusting", true);
        getConfig().addDefault("forceEnabled", false);
        getConfig().addDefault("sendChatFeedback", true);
        getConfig().options().copyDefaults(true);
        saveConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CONFIG_SYNC_CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CONFIG_SYNC_CHANNEL,
                (channel, player, message) -> {
                    ByteArrayDataInput in = ByteStreams.newDataInput(message);
                    int version = in.readInt();
                    boolean success = in.readBoolean();
                    if (getConfig().getBoolean("sendChatFeedback")) {
                        player.sendMessage("Response from DABR (" + version + "): Success:" + success);
                    }
                });

        Objects.requireNonNull(getCommand("dabrcs"), "unreachable")
                .setExecutor((commandSender, command, s, strings) -> {
                    if (!commandSender.isOp()) {
                        commandSender.sendMessage("You must be an operator to use this command.");
                        return true;
                    }

                    if (strings.length == 0) {
                        commandSender.sendMessage("Usage: /dabrcs <allowThrusting|forceEnabled> <true|false>");
                        return true;
                    }

                    if (strings.length == 1) {
                        if (strings[0].equals("allowThrusting")) {
                            commandSender.sendMessage("allowThrusting is currently " + getConfig().getBoolean("allowThrusting"));
                            return true;
                        }

                        if (strings[0].equals("forceEnabled")) {
                            commandSender.sendMessage("forceEnabled is currently " + getConfig().getBoolean("forceEnabled"));
                            return true;
                        }

                        commandSender.sendMessage("Usage: /dabrcs <allowThrusting|forceEnabled> <true|false>");
                        return true;
                    }

                    if (strings.length == 2) {
                        if (strings[0].equals("allowThrusting")) {
                            getConfig().set("allowThrusting", Boolean.parseBoolean(strings[1]));
                            saveConfig();
                            sendConfigToAllPlayers();
                            commandSender.sendMessage("Set allowThrusting to " + getConfig().getBoolean("allowThrusting"));
                            return true;
                        }

                        if (strings[0].equals("forceEnabled")) {
                            getConfig().set("forceEnabled", Boolean.parseBoolean(strings[1]));
                            saveConfig();
                            sendConfigToAllPlayers();
                            commandSender.sendMessage("Set forceEnabled to " + getConfig().getBoolean("forceEnabled"));
                            return true;
                        }

                        commandSender.sendMessage("Usage: /dabrcs <allowThrusting|forceEnabled> <true|false>");
                        return true;
                    }

                    commandSender.sendMessage("Usage: /dabrcs <allowThrusting|forceEnabled> <true|false>");
                    return true;
                });

        getLogger().info("Loaded DABRCS");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getLogger().info("Unloaded DABRCS");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 20 ms was likely too soon for client/mod channel readiness on join.
        foliaLib.getScheduler().runAtEntityLater(
                player,
                () -> {
                    getLogger().info("Sending join config to " + player.getName()
                            + " allowThrusting=" + getConfig().getBoolean("allowThrusting")
                            + " forceEnabled=" + getConfig().getBoolean("forceEnabled"));
                    sendConfig(player);
                },
                1L,
                TimeUnit.SECONDS
        );
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
        FileConfiguration config = getConfig();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        out.writeInt(4);
        out.writeBoolean(config.getBoolean("allowThrusting"));
        out.writeBoolean(config.getBoolean("forceEnabled"));
        out.writeBoolean(true);
        out.writeBoolean(false);
        out.writeBoolean(false);
        out.writeBoolean(false);
        out.writeInt(0);

        String value = "VANILLA";
        writeVarInt(out, value.length());
        out.write(value.getBytes(StandardCharsets.UTF_8));

        player.sendPluginMessage(this, CONFIG_SYNC_CHANNEL, out.toByteArray());
    }
}
