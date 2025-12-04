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

  private final FileConfiguration config = this.getConfig();
  private FoliaLib foliaLib = null;

  @Override
  public void onEnable() {
    foliaLib = new FoliaLib(this);

    this.config.addDefault("allowThrusting", true);
    this.config.addDefault("forceEnabled", false);
    this.config.addDefault("sendChatFeedback", true);
    this.saveConfig();

    this.getServer().getPluginManager().registerEvents(this, this);
    this.getServer().getMessenger().registerOutgoingPluginChannel(this, CONFIG_SYNC_CHANNEL);
    this.getServer().getMessenger().registerIncomingPluginChannel(this, CONFIG_SYNC_CHANNEL, (channel, player, message) -> {
      ByteArrayDataInput in = ByteStreams.newDataInput(message);
      int version = in.readInt();
      boolean success = in.readBoolean();
      if (this.config.getBoolean("sendChatFeedback")) player.sendMessage("Response from DABR (" + version + "): Success:" + success);
    });

    Objects.requireNonNull(getCommand("dabrcs"), "unreachable").setExecutor((commandSender, command, s, strings) -> {
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
          commandSender.sendMessage("allowThrusting is currently " + this.config.getBoolean("allowThrusting"));
          return true;
        }

        if (strings[0].equals("forceEnabled")) {
          commandSender.sendMessage("forceEnabled is currently " + this.config.getBoolean("forceEnabled"));
          return true;
        }

        commandSender.sendMessage("Usage: /dabrcs <allowThrusting|forceEnabled> <true|false>");
        return true;
      }

      if (strings.length == 2) {
        if (strings[0].equals("allowThrusting")) {
          this.config.set("allowThrusting", Boolean.parseBoolean(strings[1]));
          this.saveConfig();
          this.sendConfigToAllPlayers();
          commandSender.sendMessage("Set allowThrusting to " + this.config.getBoolean("allowThrusting"));
          return true;
        }

        if (strings[0].equals("forceEnabled")) {
          this.config.set("forceEnabled", Boolean.parseBoolean(strings[1]));
          this.saveConfig();
          this.sendConfigToAllPlayers();
          commandSender.sendMessage("Set forceEnabled to " + this.config.getBoolean("forceEnabled"));
          return true;
        }

        commandSender.sendMessage("Usage: /dabrcs <allowThrusting|forceEnabled> <true|false>");
        return true;
      }

      commandSender.sendMessage("Usage: /dabrcs <allowThrusting|forceEnabled> <true|false>");
      return true;
    });
    this.getLogger().info("Loaded DABRCS");
  }

  @Override
  public void onDisable() {
    this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    this.getLogger().info("Unloaded DABRCS");
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    this.foliaLib.getScheduler().runTimerAsync(() -> this.sendConfig(player), 1, 20L, TimeUnit.MILLISECONDS);
  }

  private void writeVarInt(ByteArrayDataOutput out, int value) {
    while ((value & -128) != 0) {
      out.writeByte(value & 127 | 128);
      value >>>= 7;
    }

    out.writeByte(value);
  }

  private void sendConfigToAllPlayers() {
    for (Player player : this.getServer().getOnlinePlayers()) this.sendConfig(player);
  }

  private void sendConfig(Player player) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();

    out.writeInt(4);
    out.writeBoolean(this.config.getBoolean("allowThrusting"));
    out.writeBoolean(this.config.getBoolean("forceEnabled"));
    out.writeBoolean(true);
    out.writeBoolean(false);
    out.writeBoolean(false);
    out.writeBoolean(false);
    out.writeInt(0);

    String value = "VANILLA";
    this.writeVarInt(out, value.length());
    out.write(value.getBytes(StandardCharsets.UTF_8));

    DABRCS plugin = JavaPlugin.getPlugin(DABRCS.class);
    player.sendPluginMessage(plugin, CONFIG_SYNC_CHANNEL, out.toByteArray());
  }
}
