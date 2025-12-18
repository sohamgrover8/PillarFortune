package me.soham.pillarfortune;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PillarFortune extends JavaPlugin implements Listener {

    private BukkitRunnable countdownTask;
    private BukkitRunnable lavaTask;

    private boolean gameRunning = false;

    private int countdown;
    private int lavaY;

    private final Set<Player> alivePlayers = new HashSet<>();

    // CONFIG VALUES
    private int minPlayers;
    private int countdownSeconds;
    private int platformRadius;
    private int pillarHeight;
    private int spawnRadius;
    private int lavaStartY;
    private int lavaIntervalSeconds;

    private Location center;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("PillarFortune enabled");
    }

    // ================= CONFIG =================
    private void loadConfig() {
        minPlayers = getConfig().getInt("game.min-players");
        countdownSeconds = getConfig().getInt("game.countdown-seconds");

        platformRadius = getConfig().getInt("platform.platform-radius");
        spawnRadius = getConfig().getInt("pillars.spawn-radius");
        pillarHeight = getConfig().getInt("pillars.height");

        lavaStartY = getConfig().getInt("lava.start-y");
        lavaIntervalSeconds = getConfig().getInt("lava.rise-interval-seconds");

        World world = Bukkit.getWorlds().get(0);
        center = new Location(
                world,
                getConfig().getInt("platform.center-x"),
                getConfig().getInt("platform.center-y"),
                getConfig().getInt("platform.center-z")
        );
    }

    // ================= JOIN =================
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        if (gameRunning) {
            p.setGameMode(GameMode.SPECTATOR);
            p.teleport(center.clone().add(0, 10, 0));
            return;
        }

        p.setGameMode(GameMode.ADVENTURE);

        if (p.getWorld().getPlayers().size() >= minPlayers && countdownTask == null) {
            startCountdown(p.getWorld());
        }
    }

    // ================= COUNTDOWN =================
    private void startCountdown(World world) {
        countdown = countdownSeconds;

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {

                if (world.getPlayers().size() < minPlayers) {
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

                for (Player p : world.getPlayers()) {
                    p.sendActionBar(
                            Component.text("Starting in " + countdown + "s")
                                    .color(NamedTextColor.YELLOW)
                    );
                }
                countdown--;
            }
        };
        countdownTask.runTaskTimer(this, 0L, 20L);
    }

    // ================= GAME START =================
    private void startGame(World world) {
        gameRunning = true;

        alivePlayers.clear();
        alivePlayers.addAll(world.getPlayers());

        for (Player p : alivePlayers) {
            p.setGameMode(GameMode.SURVIVAL);
        }

        List<Player> players = alivePlayers.stream().toList();

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);

            double angle = 2 * Math.PI * i / players.size();
            Location base = new Location(
                    world,
                    center.getX() + spawnRadius * Math.cos(angle),
                    center.getY(),
                    center.getZ() + spawnRadius * Math.sin(angle)
            );

            for (int y = 0; y < pillarHeight; y++) {
                base.clone().add(0, y, 0).getBlock().setType(Material.BEDROCK);
            }

            p.teleport(base.clone().add(0, pillarHeight + 1, 0));
        }

        world.sendMessage(
                Component.text("Pillar of Fortune started!")
                        .color(NamedTextColor.GREEN)
        );

        startLava(world);
    }

    // ================= LAVA =================
    private void startLava(World world) {
        lavaY = lavaStartY;

        lavaTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (int x = -platformRadius; x <= platformRadius; x++) {
                    for (int z = -platformRadius; z <= platformRadius; z++) {

                        if (x * x + z * z > platformRadius * platformRadius) continue;

                        Location l = new Location(
                                world,
                                center.getX() + x,
                                lavaY,
                                center.getZ() + z
                        );

                        if (l.getBlock().getType() == Material.AIR) {
                            l.getBlock().setType(Material.LAVA);
                        }
                    }
                }
                lavaY++;
            }
        };
        lavaTask.runTaskTimer(this, 0L, lavaIntervalSeconds * 20L);
    }

    // ================= DEATH =================
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        if (!alivePlayers.remove(p)) return;

        p.setGameMode(GameMode.SPECTATOR);
        checkWin(p.getWorld());
    }

    // ================= WIN + RESTART =================
    private void checkWin(World world) {
        if (alivePlayers.size() != 1) return;

        Player winner = alivePlayers.iterator().next();

        world.sendMessage(
                Component.text(winner.getName() + " won Pillar of Fortune!")
                        .color(NamedTextColor.GOLD)
        );

        if (lavaTask != null) {
            lavaTask.cancel();
            lavaTask = null;
        }

        Bukkit.getScheduler().runTaskLater(this, () -> restartGame(world), 200L);
    }

    private void restartGame(World world) {
        gameRunning = false;
        alivePlayers.clear();

        for (Player p : world.getPlayers()) {
            p.teleport(center);
            p.setGameMode(GameMode.ADVENTURE);
        }
    }
}
