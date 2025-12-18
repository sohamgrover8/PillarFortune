package me.soham.pillarfortune;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
                if (world.getPlayers().size() < 2) {
                    world.sendMessage(ChatColor.RED + "Not enough players. Countdown stopped.");
                    countdownTask = null;
                    cancel();
                    return;
                }

                if (countdown == 0) {
                    cancel();
                    countdownTask = null;
                    startGame(world);
                    return;
                }

                world.sendMessage(ChatColor.YELLOW + "Game starts in " + countdown + "s");
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

            for (int y = 0; y < height; y++) {
                base.clone().add(0, y, 0).getBlock().setType(Material.BEDROCK);
            }

            players.get(i).teleport(base.clone().add(0, height + 1, 0));
        }

        world.sendMessage(ChatColor.GREEN + "Pillar of Fortune started!");
    }
}