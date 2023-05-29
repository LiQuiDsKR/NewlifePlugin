package com.newlifeplugin;

import com.destroystokyo.paper.MaterialTags;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public final class NewlifePlugin extends JavaPlugin implements Listener {
    private HashMap<String, String> teams;
        private Random random;
    private boolean flag01 = false;

    Inventory missionInventory = Bukkit.createInventory(null, 27, "Missions");
    private Map<UUID, UUID> playerDeaths = new HashMap<>();
    public Entity killer;
    public Player killedPlayer;
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
        missions.add (createMission("풍선처럼 가볍게", "난이도 상", "세상의 가장 아래에서 가장 위로 땅을 밟지 않고 올라가세요.", Material.ELYTRA, "미달성"));
        missions.add (createMission("전생의 원수", "난이도 중", "가장 최근에 자신을 죽인 대상을 죽이세요.", Material.WOODEN_SWORD, "미달성"));
        missions.add (createMission("건물 사이에 피어난 장미", "난이도 하", "마을에 장미를 심으세요.", Material.POPPY, "미달성"));
        missions.add (createMission("아무도 없어요?", "난이도 하", "버려진 마을을 발견하세요.", Material.COBWEB, "미달성"));
        missions.add (createMission("\'석\'박사", "난이도 중", "\'석\'으로 끝나는 모든 아이템을 수집하세요.", Material.LODESTONE, "미달성"));
        missions.add (createMission("제발 잠 좀 자자", "난이도 하", "잠을 방해하는 녀석을 제거하세요.", Material.WHITE_BED, "미달성"));
        missions.add (createMission("물개", "난이도 상", "늑대와 함께 바다 신전을 공략하세요.", Material.PRISMARINE_SHARD, "미달성"));
        missions.add (createMission("옆 신사분께서 쏘셨습니다", "난이도 중", "화염구를 다른 대상에게 선물하세요.", Material.GHAST_TEAR, "미달성"));
        missions.add (createMission("폭적폭", "난이도 중", "크리퍼에게 자폭 외의 폭발을 알려주세요.", Material.GUNPOWDER, "미달성"));
        missions.add (createMission("갓챠!", "난이도 상", "푸른 아홀로틀을 발견하세요.", Material.AXOLOTL_BUCKET, "미달성"));
        missions.add (createMission("쓸모없는 토템", "난이도 중", "불사의 토템을 무용지물로 만드세요.", Material.TOTEM_OF_UNDYING, "미달성"));
        missions.add (createMission("충전 완료", "난이도 중", "충전된 크리퍼를 발견하세요.", Material.CREEPER_HEAD, "미달성"));
        missions.add (createMission("어린 왕자", "난이도 상", "여우와 함께 죽음을 피하세요.", Material.SWEET_BERRIES, "미달성"));

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
        InventoryMissionCheck(event.getPlayer());
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

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();

        // Check if the player is in a zombie village biome
        if (location.getBlock().getBiome() == Biome.PLAINS && location.getBlock().getBiome().name().toLowerCase().contains("zombie")) {
            // Player has arrived at a zombie village
            player.sendMessage("Mission cleared!");
            for (Mission m :missions) {
                if (m.missinName == "아무도 없어요?" && m.clearedTeam == "미달성") {
                    m.clearedTeam = teams.get(killedPlayer.getName());
                }
            }
        }

        Location playerLocation = player.getLocation();
        int playerY = playerLocation.getBlockY();

        // Check if the player has reached the lowest position
        if (playerY == -63) {
            flag01 = true;
            player.sendMessage("you are y-63");
        }

        if (flag01 == true) {
            // Check if the player stepped on a block
            if (playerLocation.getBlock().getType() != Material.AIR) {
                flag01 = false;
            }
            // Check if the player has reached the highest position without stepping on a block
            if (playerY == 319 && playerLocation.getBlock().getType() == Material.AIR) {
                flag01 = false;
                player.sendMessage("you are y319");
                for (Mission m :missions) {
                    if (m.missinName == "풍선처럼 가볍게" && m.clearedTeam == "미달성") {
                        m.clearedTeam = teams.get(player.getName());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Entity killer = victim.getKiller();

        if (killer instanceof LivingEntity) {
            playerDeaths.put(victim.getUniqueId(), killer.getUniqueId());
        }
    }

    private void InventoryMissionCheck(Player player) {
        checkGoldRich1(player);
        checkGoldRich2(player);
        checkGoldRich3(player);
        checkSEOKDoctor(player);
    }
    private void openMissionUI(Player player) {
        updateMission(player);
        player.openInventory(missionInventory);
    }
    private void updateMission(Player player) {
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

    @EventHandler
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
        Entity entity = event.getEntity();

        if (entity instanceof Player) {
            Player player = (Player) entity;
            killedPlayer = (Player) entity;

            UUID playerVictimUUID = player.getUniqueId();
            UUID killerUUID = playerDeaths.get(playerVictimUUID);

            if (killerUUID != null) {
                killer = Bukkit.getEntity(killerUUID);
            }
        }
        if (event.getEntity().getKiller() == killedPlayer) {
            for (Mission m :missions) {
                if (m.missinName == "전생의 원수" && m.clearedTeam == "미달성") {
                    m.clearedTeam = teams.get(killedPlayer.getName());
                }
            }
        }
    }@EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if the placed block is a poppy and if it's in a village
        if (block.getType() == Material.POPPY && block.getLocation().getWorld().getName().endsWith("_village")) {
            // Player has cleared the mission
            for (Mission m :missions) {
                if (m.missinName == "건물 사이에 피어난 장미" && m.clearedTeam == "미달성") {
                    m.clearedTeam = teams.get(killedPlayer.getName());
                }
            }
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

    private void checkBottomToTop(Player player) {

    }
    public void checkSEOKDoctor(Player player) {
        PlayerInventory inventory = player.getInventory();

        // List of required materials
        Material[] requiredMaterials = {
                Material.GLOWSTONE,
                Material.LAPIS_LAZULI,
                Material.GOLD_ORE,
                Material.COPPER_ORE,
                Material.IRON_ORE,
                Material.LODESTONE,
                Material.NETHER_QUARTZ_ORE,
                Material.NETHER_GOLD_ORE,
                Material.DEEPSLATE_DIAMOND_ORE,
                Material.DIAMOND_ORE,
                Material.DEEPSLATE_LAPIS_ORE,
                Material.LAPIS_ORE,
                Material.DEEPSLATE_EMERALD_ORE,
                Material.DEEPSLATE_REDSTONE_ORE,
                Material.DEEPSLATE_GOLD_ORE,
                Material.DEEPSLATE_COPPER_ORE,
                Material.DEEPSLATE_IRON_ORE,
                Material.DEEPSLATE_COAL_ORE,
                Material.COAL_ORE,
                Material.CRYING_OBSIDIAN,
                Material.OBSIDIAN,
                Material.POINTED_DRIPSTONE,
                Material.CALCITE
        };

        boolean flag02 = true;

        for (Material material : requiredMaterials) {
            if (!hasMaterial(inventory, material)) {
                flag02 = false;
                break;
            }
        }
        if (flag02) {
            for (Mission m :missions) {
                if (m.missinName == "\'석\'박사" && m.clearedTeam == "미달성") {
                    m.clearedTeam = teams.get(player.getName());
                }
            }
        }

    }
    private static boolean hasMaterial(PlayerInventory inventory, Material material) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                return true; // Player has the material
            }
        }
        return false; // Player doesn't have the material
    }
}

