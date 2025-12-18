package me.soham.pillarfortune;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class PillarFortune extends JavaPlugin implements Listener {

    private BukkitRunnable countdownTask;
    private int countdown = 30;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("PillarFortune enabled");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        World world = event.getPlayer().getWorld();

        if (world.getPlayers().size() >= 2 && countdownTask == null) {
            startCountdown(world);
        }
    }

    private void startCountdown(World world) {
        countdown = 30;

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {

                // Stop countdown if players drop below 2
                if (world.getPlayers().size() < 2) {
                    world.sendMessage(
                            Component.text("Not enough players. Countdown stopped.")
                                    .color(NamedTextColor.RED)
                    );
                    countdownTask = null;
                    cancel();
                    return;
                }

                // Start game
                if (countdown == 0) {
                    cancel();
                    countdownTask = null;
                    startGame(world);
                    return;
                }

                // Countdown message
                world.sendMessage(
                        Component.text("Game starts in " + countdown + "s")
                                .color(NamedTextColor.YELLOW)
                );

                countdown--;
            }
        };

        countdownTask.runTaskTimer(this, 0L, 20L);
    }

    private void startGame(World world) {
        List<Player> players = world.getPlayers();

        Location center = new Location(world, 0, 100, 0);
        double radius = 25;
        int height = 12;

        for (int i = 0; i < players.size(); i++) {
            double angle = 2 * Math.PI * i / players.size();

            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            Location base = new Location(world, x, center.getY(), z);

            // Build pillar
            for (int y = 0; y < height; y++) {
                base.clone().add(0, y, 0).getBlock().setType(Material.BEDROCK);
            }

            // Teleport player
            players.get(i).teleport(base.clone().add(0, height + 1, 0));
        }

        world.sendMessage(
                Component.text("Pillar of Fortune started!")
                        .color(NamedTextColor.GREEN)
        );
    }
}
