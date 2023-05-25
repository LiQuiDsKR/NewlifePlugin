package com.newlifeplugin;

import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class NewlifePlugin extends JavaPlugin implements Listener {
    private HashMap<String, String> teams;
    private Random random;

    Inventory missionInventory = Bukkit.createInventory(null, 27, "Missions");

    public class Mission {
        String missinName;
        String difficulty;
        String missionDetail;
        Material displayMaterial;
        String clearedTeam;
    }
    public List<Mission> missions = new ArrayList<Mission>() {

    };

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        teams = new HashMap<>();
        random = new Random();

        // Set initial team names
        teams.forEach((key, value) -> getConfig().set("teams." + key, value));
        saveConfig();

        // Modify game rules
        World world = Bukkit.getWorlds().get(0); // Assuming only one world
        world.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(2000);

        missions.add (createMission("금 수집가", "난이도 하", "금 블럭 10개를 모으세요.", Material.GOLD_NUGGET, "미달성"));
        missions.add (createMission("금 부자", "난이도 중", "금 블럭 32개를 모으세요.", Material.GOLD_INGOT, "미달성"));
        missions.add (createMission("금 억만장자", "난이도 상", "금 블럭 64개를 모으세요.", Material.GOLD_BLOCK, "미달성"));
        missions.add (createMission("\'석\'박사", "난이도 상", "\'석\'으로 끝나는 모든 아이템을 수집하세요.", Material.LODESTONE, "미달성"));

    }

    private Mission createMission(String missionName, String difficulty, String missionDetail, Material displayMaterial, String clearedTeam) {
        Mission mission = new Mission();
        mission.missinName = missionName;
        mission.difficulty = difficulty;
        mission.missionDetail = missionDetail;
        mission.displayMaterial = displayMaterial;
        mission.clearedTeam = clearedTeam;

        return mission;
    }

    private ItemStack createMissionItem(String missionName, String difficulty, String missionDetail, Material displayMaterial, String clearedTeam) {
        ItemStack item = new ItemStack(displayMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§r" + ChatColor.WHITE + missionName);
        List<String> lores = new ArrayList<String>() {
        };
        if (difficulty == "난이도 하") {
            lores.add("§r" + ChatColor.GREEN + difficulty);
        } else if (difficulty == "난이도 중") {
            lores.add("§r" + ChatColor.GOLD + difficulty);
        } else if (difficulty == "난이도 상") {
            lores.add("§r" + ChatColor.RED + difficulty);
        }
            lores.add("§r" + ChatColor.GRAY + missionDetail);
        lores.add("§r" + ChatColor.BLUE + clearedTeam);

        meta.setLore(lores);
        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onPlayerInventorySlotChanged(PlayerInventorySlotChangeEvent event) {
        if (event.getPlayer().getInventory().getChestplate() != null && event.getPlayer().getInventory().getChestplate().getType() == Material.ELYTRA){
            event.getPlayer().getInventory().setChestplate(new ItemStack(Material.AIR));
            event.getPlayer().sendMessage("You can't equip Elytra!");
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();



        if (command.equals("/team1")) {
            event.setCancelled(true);
            String playerName = event.getPlayer().getName();
            teams.put(playerName, "team1");
        } else if (command.equals("/team2")) {
            event.setCancelled(true);
            String playerName = event.getPlayer().getName();
            teams.put(playerName, "team2");
        } else if (command.equals("/check")) {
            event.setCancelled(true);
        } else if (command.equals("/teaminfo")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(teams.keySet().toString());
            event.getPlayer().sendMessage(teams.values().toString());

        } else if (command.startsWith("/team rename")) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            String[] parts = command.split(" ");
            if (parts.length == 3 && parts[2].length() > 0) {
                String newTeamName = parts[2];
                for (String playerName : teams.keySet()) {
                    if (teams.get(playerName) == teams.get(player.getName())) {
                        teams.replace(playerName, newTeamName);
                    }
                }
            }
        } else if (command.equals("/reinforce")) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null ) {
                if (MaterialTags.ENCHANTABLE.isTagged(item)) {
                    if (player.getLevel() >= 1) {
                        enchantItemRandomly(item, event.getPlayer());
                        player.setLevel(player.getLevel() - 1);
                        player.sendMessage(ChatColor.GREEN + "마법 부여에 성공했습니다.");
                    } else {
                        player.sendMessage(ChatColor.RED + "마법 부여에 필요한 레벨이 모자랍니다.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "해당 아이템은 마법을 부여할 수 없습니다.");
                }

            } else {
                player.sendMessage(ChatColor.RED + "마법 부여를 하려면 손에 아이템을 들고 명령어를 입력하세요!");
            }
        } else if (command.equals("/mission")) {
            event.setCancelled(true);
            openMissionUI(event.getPlayer());
        }
    }

    private void missionClearCheck(Player player) {
        checkGoldRich1(player);
        checkGoldRich2(player);
        checkGoldRich3(player);
    }
    private void checkGoldRich1(Player player) {

        int requiredAmount = 10;
        ItemStack targetItem = new ItemStack(Material.GOLD_BLOCK, requiredAmount);

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(targetItem) && item.getAmount() >= requiredAmount) {
                for (Mission m :missions) {
                    if (m.missinName == "금 수집가" && m.clearedTeam == "미달성") {
                        m.clearedTeam = teams.get(player.getName());
                    }
                }
            }
        }
    }
    private void checkGoldRich2(Player player) {

        int requiredAmount = 32;
        ItemStack targetItem = new ItemStack(Material.GOLD_BLOCK, requiredAmount);

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(targetItem) && item.getAmount() >= requiredAmount) {
                for (Mission m :missions) {
                    if (m.missinName == "금 부자" && m.clearedTeam == "미달성") {
                        m.clearedTeam = teams.get(player.getName());
                    }
                }
            }
        }
    }
    private void checkGoldRich3(Player player) {

        int requiredAmount = 64;
        ItemStack targetItem = new ItemStack(Material.GOLD_BLOCK, requiredAmount);

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(targetItem) && item.getAmount() >= requiredAmount) {
                for (Mission m :missions) {
                    if (m.missinName == "금 억만장자" && m.clearedTeam == "미달성") {
                        m.clearedTeam = teams.get(player.getName());
                    }
                }
            }
        }
    }
    private void openMissionUI(Player player) {
        updateMission();
        player.openInventory(missionInventory);
    }
    private void updateMission() {

        Inventory inventory = missionInventory;
        inventory.clear();
        for (Mission m : missions) {
            missionInventory.addItem(createMissionItem(m.missinName, m.difficulty, m.missionDetail, m.displayMaterial, m.clearedTeam));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase("Missions")) {
            event.setCancelled(true);
        }
    }

    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            Bukkit.broadcastMessage("The Ender Dragon has been defeated!");

            new BukkitRunnable() {
                @Override
                public void run() {
                    teleportFromEnd();
                }
            }.runTaskLater(this, 5 * 60 * 20); // 5 minutes * 60 seconds * 20 ticks
        }
    }



    private void enchantItemRandomly(ItemStack item, Player player) {
        for (Enchantment i:item.getEnchantments().keySet()) {
            item.removeEnchantment(i);
        }
        for (int i = 0; i < random.nextInt(5) + 1; i++) {
            setEnchantment(item);
        }
    }

    void setEnchantment(ItemStack item) {
        try {
            Enchantment temp;
            temp = getRandomEnchantment();
            item.addEnchantment(temp, random.nextInt(temp.getMaxLevel() + 1));
        } catch (IllegalArgumentException e) {
            setEnchantment(item);
        }
    }


    private Enchantment getRandomEnchantment() {
        Enchantment[] enchantments = Enchantment.values();
        return enchantments[random.nextInt(enchantments.length)];
    }

    private void teleportFromEnd() {
        World endWorld = Bukkit.getWorlds().get(1); // Assuming the End world is the second world loaded
        List<Player> playersInEnd = endWorld.getPlayers();

        if (!playersInEnd.isEmpty()) {
            for (Player player : playersInEnd) {
                player.setVelocity(new Vector(0, 0, 0));
                player.teleport(player.getBedSpawnLocation());
                player.sendMessage("You have been teleported back to the Overworld.");
            }
        }
    }
}

