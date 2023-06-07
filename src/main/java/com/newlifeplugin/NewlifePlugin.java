package com.newlifeplugin;

import org.bukkit.Bukkit;
import com.destroystokyo.paper.MaterialTags;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.meta.AxolotlBucketMeta;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public final class NewlifePlugin extends JavaPlugin implements Listener {
    private HashMap<String, String> teams;
    private Random random;
    private HashMap<Player, Boolean> isNegative63Y = new HashMap<>();


    Inventory missionInventory = Bukkit.createInventory(null, 27, "Missions");
    private Map<Player, Entity> playerDeaths = new HashMap<>();

    private HashMap<Player, Boolean> isNotSafeToSleep;
    private HashMap<Player, Boolean> isReadyToSleep;

    private Player wolfOwner;

    public class Mission {
        String missinName;
        String difficulty;
        String missionDetail;
        Material displayMaterial;
        String clearedTeam;
    }

    public List<Mission> missions = new ArrayList<Mission>() {

    };
    private HashMap<Player, Boolean> isUsedTotem;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        teams = new HashMap<>();
        random = new Random();

        isNegative63Y = new HashMap<>();
        isNotSafeToSleep = new HashMap<>();
        isReadyToSleep = new HashMap<>();

        isUsedTotem = new HashMap<>();

        // Declare Variables
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            isNegative63Y.put(player, false);
            isNotSafeToSleep.put(player, false);
            isReadyToSleep.put(player, false);
        }

        // Set initial team names
        teams.forEach((key, value) -> getConfig().set("teams." + key, value));
        saveConfig();

        // Modify game rules
        World world = Bukkit.getWorlds().get(0); // Assuming only one world
        world.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(2000);

        missions.add(createMission("금 수집가", "난이도 하", "금 블럭 10개를 모으세요.", Material.GOLD_NUGGET, "미달성"));
        missions.add(createMission("금 부자", "난이도 중", "금 블럭 32개를 모으세요.", Material.GOLD_INGOT, "미달성"));
        missions.add(createMission("금 억만장자", "난이도 상", "금 블럭 64개를 모으세요.", Material.GOLD_BLOCK, "미달성"));
        missions.add(createMission("풍선처럼 가볍게", "난이도 상", "세상의 가장 아래에서 가장 위로 땅을 밟지 않고 올라가세요.", Material.ELYTRA, "미달성"));
        missions.add(createMission("전생의 원수", "난이도 중", "가장 최근에 자신을 죽인 대상을 죽이세요.", Material.WOODEN_SWORD, "미달성"));
        missions.add(createMission("건물 사이에 피어난 장미", "난이도 하", "마을에 장미를 심으세요.", Material.POPPY, "미달성"));
        missions.add(createMission("아무도 없어요?", "난이도 하", "버려진 마을을 발견하세요.", Material.COBWEB, "미달성"));
        missions.add(createMission("\'석\'박사", "난이도 중", "\'석\'으로 끝나는 모든 아이템을 수집하세요.", Material.LODESTONE, "미달성"));
        missions.add(createMission("제발 잠 좀 자자", "난이도 하", "잠을 방해하는 녀석을 제거하세요.", Material.WHITE_BED, "미달성"));
        missions.add(createMission("물개", "난이도 상", "늑대와 함께 바다 신전을 공략하세요.", Material.PRISMARINE_SHARD, "미달성"));
        missions.add(createMission("옆 신사분께서 쏘셨습니다", "난이도 중", "화염구를 다른 대상에게 선물하세요.", Material.GHAST_TEAR, "미달성"));
        missions.add(createMission("폭적폭", "난이도 중", "크리퍼에게 자폭 외의 폭발을 알려주세요.", Material.GUNPOWDER, "미달성"));
        missions.add(createMission("갓챠!", "난이도 상", "푸른 아홀로틀을 발견하세요.", Material.AXOLOTL_BUCKET, "미달성"));
        missions.add(createMission("쓸모없는 토템", "난이도 중", "불사의 토템을 무용지물로 만드세요.", Material.TOTEM_OF_UNDYING, "미달성"));
        missions.add(createMission("충전 완료", "난이도 중", "충전된 크리퍼를 발견하세요.", Material.CREEPER_HEAD, "미달성"));
        missions.add(createMission("어린 왕자", "난이도 상", "여우와 함께 죽음을 피하세요.", Material.SWEET_BERRIES, "미달성"));

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
        if (event.getPlayer().getInventory().getChestplate() != null && event.getPlayer().getInventory().getChestplate().getType() == Material.ELYTRA) {
            event.getPlayer().getInventory().setChestplate(new ItemStack(Material.AIR));
            event.getPlayer().sendMessage("겉날개를 착용할 수 없습니다.");
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

        } else if (command.equals("/info")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(playerDeaths.keySet().toString());
            event.getPlayer().sendMessage(playerDeaths.values().toString());

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
            if (item != null) {
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
            for (Mission m : missions) {
                if (m.missinName == "아무도 없어요?" && m.clearedTeam == "미달성") {
                    m.clearedTeam = teams.get(player.getName());
                }
            }
        }

        Location playerLocation = player.getLocation();
        int playerY = playerLocation.getBlockY();

        // Check if the player has reached the lowest position
        if (playerY == -63) {
            isNegative63Y.replace(player, true);
        }

        if (isNegative63Y.get(player)) {
            // Check if the player stepped on a block
            if (playerLocation.getBlock().getType() != Material.AIR) {
                isNegative63Y.replace(player, false);
            }
            // Check if the player has reached the highest position without stepping on a block
            if (playerY == 319 && playerLocation.getBlock().getType() == Material.AIR) {
                isNegative63Y.replace(player, false);
                for (Mission m : missions) {
                    if (m.missinName == "풍선처럼 가볍게" && m.clearedTeam == "미달성") {
                        m.clearedTeam = teams.get(player.getName());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (isUsedTotem.containsKey(player) && isUsedTotem.get(player)) {
            // Mission cleared
            for (Mission m : missions) {
                if (m.missinName == "쓸모없는 토템" && m.clearedTeam == "미달성") {
                    m.clearedTeam = teams.get(player.getName());
                }
            }
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
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!isNegative63Y.containsKey(player)) {
            isNegative63Y.put(player, false);
        }
        if (!isNotSafeToSleep.containsKey(player)) {
            isNotSafeToSleep.put(player, false);
        }
        if (!isReadyToSleep.containsKey(player)) {
            isReadyToSleep.put(player, false);
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

        Entity entity = event.getEntity();

        if (entity instanceof EnderDragon) {
            Bukkit.broadcastMessage("The Ender Dragon has been defeated!");

            new BukkitRunnable() {
                @Override
                public void run() {
                    teleportFromEnd();
                }
            }.runTaskLater(this, 5 * 60 * 20); // 5 minutes * 60 seconds * 20 ticks
        }

        if (isNotSafeToSleep.containsKey(event.getEntity().getKiller())) {
            isReadyToSleep.replace(event.getEntity().getKiller(), true);
        } else {
            isReadyToSleep.put(event.getEntity().getKiller(), true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if the placed block is a poppy and if it's in a village
        if (block.getType() == Material.POPPY && block.getLocation().getWorld().getName().endsWith("_village")) {
            // Player has cleared the mission
            for (Mission m : missions) {
                if (m.missinName == "건물 사이에 피어난 장미" && m.clearedTeam == "미달성") {
                    m.clearedTeam = teams.get(player.getName());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();

        // Check if it's not safe to sleep due to monsters nearby
        if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.NOT_SAFE) {
            isNotSafeToSleep.replace(event.getPlayer(), true);
            player.sendMessage("You cannot sleep; there are monsters nearby!");
        }
        if (isReadyToSleep.containsKey(event.getPlayer()) && isReadyToSleep.get(event.getPlayer())) {
            if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
                for (Mission m : missions) {
                    if (m.missinName == "제발 잠 좀 자자" && m.clearedTeam == "미달성") {
                        m.clearedTeam = teams.get(player.getName());
                    }
                }
                isNotSafeToSleep.replace(event.getPlayer(), false);
                isReadyToSleep.replace(event.getPlayer(), false);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();

        // 피해를 받은 엔티티가 LivingEntity인 경우에 대한 처리
        if (entity instanceof LivingEntity) {
            if (damager instanceof  Fireball && entity instanceof LivingEntity) {

                Fireball fireball = (Fireball) damager;

                if (fireball.getShooter() instanceof Player) {
                    Player player = (Player) fireball.getShooter();
                    for (Mission m : missions) {
                        if (m.missinName == "옆 신사분께서 쏘셨습니다" && m.clearedTeam == "미달성") {
                            m.clearedTeam = teams.get(player.getName());
                        }
                    }
                }
            }

            // 대미지를 받은 후의 체력을 확인하여 체력이 0 이하인 경우에만 처리
            if (((LivingEntity) entity).getHealth() - event.getFinalDamage() <= 0) {

                // 여기서부터는 "물개" 미션 클리어 판정입니다.
                if (entity instanceof ElderGuardian && damager instanceof Wolf) {
                    Wolf wolf = (Wolf) damager;
                    for (Player player : getServer().getOnlinePlayers()) {
                        if (player == wolf.getOwner()) {
                            wolfOwner = player;
                        }
                    }

                    if (wolfOwner != null) {
                        // Mission cleared
                        for (Mission m : missions) {
                            if (m.missinName == "물개" && m.clearedTeam == "미달성") {
                                m.clearedTeam = teams.get(wolfOwner.getName());
                            }
                        }
                    }
                }
                // 여기서부터는 "전생의 원수" 미션 클리어 판정입니다.
                if (damager instanceof Player) {
                    if (playerDeaths.containsKey(damager)) {
                        if (playerDeaths.get(damager) == entity) {
                            for (Mission m : missions) {
                                if (m.missinName == "전생의 원수" && m.clearedTeam == "미달성") {
                                    m.clearedTeam = teams.get(damager.getName());
                                }
                            }
                        }
                    }
                }

                if (entity instanceof Player) {
                    playerDeaths.put((Player) entity, damager);
                }
                // 여기서부터는 "폭적폭" 미션 클리어 판정입니다.
                if (entity instanceof Creeper) {
                    Creeper creeper = (Creeper) entity;
                    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                            if (player.getLocation().distance(event.getEntity().getLocation()) <= 16) {
                                // Mission cleared
                                for (Mission m : missions) {
                                    if (m.missinName == "폭적폭" && m.clearedTeam == "미달성") {
                                        m.clearedTeam = teams.get(player.getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        double health = player.getHealth();

        // Check if player's health drops to 0 or below without void damage
        if (health - event.getFinalDamage() <= 0 && event.getFinalDamage() != 0 && event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            ItemStack offHandItem = player.getInventory().getItemInOffHand();

            // Check if player has a Totem of Undying in the main hand or off-hand
            if ((mainHandItem.getType() == Material.TOTEM_OF_UNDYING) || (offHandItem.getType() == Material.TOTEM_OF_UNDYING)) {
                // Player has used a Totem of Undying
                isUsedTotem.put(player, true);
                player.sendMessage("5초 안에 죽으면 미션이 클리어됩니다.");
                getServer().getScheduler().runTaskLater(this, () -> isUsedTotem.put(player, false), 100L); // Set to false after 5 seconds (100 ticks)
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check if the player interacts with a monster
        if (isNotSafeToSleep.get(event.getPlayer()) && event.getClickedBlock() != null) {
            Entity clickedEntity = event.getClickedBlock().getWorld().spawn(event.getClickedBlock().getLocation(), Monster.class);
            if (clickedEntity instanceof Monster) {
                // Player has killed a monster
                isNotSafeToSleep.replace(event.getPlayer(), false);
            }
            clickedEntity.remove();
        }
    }


    private void enchantItemRandomly(ItemStack item, Player player) {
        for (Enchantment i : item.getEnchantments().keySet()) {
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
                player.sendMessage("오버월드로 이동되었습니다.");
            }
        }
    }

    private void checkGoldRich1(Player player) {

        int requiredAmount = 10;
        ItemStack targetItem = new ItemStack(Material.GOLD_BLOCK, requiredAmount);

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(targetItem) && item.getAmount() >= requiredAmount) {
                for (Mission m : missions) {
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
                for (Mission m : missions) {
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
                for (Mission m : missions) {
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

        boolean isCollectAllSEOK = true;

        for (Material material : requiredMaterials) {
            if (!hasMaterial(inventory, material)) {
                isCollectAllSEOK = false;
                break;
            }
        }
        if (isCollectAllSEOK) {
            for (Mission m : missions) {
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

