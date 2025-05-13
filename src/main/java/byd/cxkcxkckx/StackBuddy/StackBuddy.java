package byd.cxkcxkckx.StackBuddy;
        
import top.mrxiaom.pluginbase.BukkitPlugin;

public class StackBuddy extends BukkitPlugin {
    public static StackBuddy getInstance() {
        return (StackBuddy) BukkitPlugin.getInstance();
    }

    public StackBuddy() {
        super(options()
                .bungee(false)
                .adventure(false)
                .database(false)
                .reconnectDatabaseWhenReloadConfig(false)
                .scanIgnore("top.mrxiaom.example.libs")
        );
        // this.scheduler = new FoliaLibScheduler(this);
    }

    @Override
    protected void afterEnable() {
        saveDefaultConfig();
        String language = getConfig().getString("language", "zh_CN");
        java.io.File langFile = new java.io.File(getDataFolder(), "lang/" + language + ".yml");
        if (!langFile.exists()) {
            saveResource("lang/" + language + ".yml", false);
        }
        org.bukkit.configuration.file.YamlConfiguration langConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(langFile);
        getLogger().info(langConfig.getString("messages.plugin_loaded", "StackBuddy loaded"));
        int maxStackSize = getConfig().getInt("max_stack_size", 10);
        boolean enableLogging = getConfig().getBoolean("enable_logging", true);
        boolean enableExcludedMounts = getConfig().getBoolean("enable_excluded_mounts", true);
        java.util.List<String> excludedMounts = getConfig().getStringList("excluded_mounts");
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
                if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
                org.bukkit.entity.Player player = event.getPlayer();
                if (player.getInventory().getItemInMainHand().getType() != org.bukkit.Material.AIR) return;
                org.bukkit.entity.Entity target = event.getRightClicked();
                if (target instanceof org.bukkit.entity.LivingEntity) {
                    org.bukkit.entity.LivingEntity living = (org.bukkit.entity.LivingEntity) target;
                    if (enableExcludedMounts && excludedMounts.contains(living.getType().toString())) {
                        String msg = langConfig.getString("messages.entity_excluded", "This entity cannot be picked up");
                        player.sendMessage(msg);
                        return;
                    }
                    if (player.isSneaking()) {
                        // 如果玩家正在蹲下，则让所有骑乘的生物散开
                        org.bukkit.entity.Entity vehicle = player;
                        while (vehicle.getPassengers().size() > 0) {
                            vehicle.getPassengers().forEach(passenger -> passenger.leaveVehicle());
                            vehicle = vehicle.getPassengers().get(0);
                        }
                        String msg = langConfig.getString("messages.entities_dispersed", "").replace("%player%", player.getName());
                        if (enableLogging) {
                            getLogger().info(msg);
                        }
                        player.sendMessage(msg);
                    } else {
                        // 检查当前堆叠数量是否超过最大值
                        int currentStackSize = 0;
                        org.bukkit.entity.Entity top = player;
                        while (top.getPassengers().size() > 0) {
                            currentStackSize++;
                            top = top.getPassengers().get(0);
                        }
                        if (currentStackSize >= maxStackSize) {
                            player.sendMessage(langConfig.getString("messages.max_stack_reached", "").replace("%max%", String.valueOf(maxStackSize)));
                            if (enableLogging) {
                                getLogger().info(langConfig.getString("messages.max_stack_reached", "").replace("%max%", String.valueOf(maxStackSize)));
                            }
                            return;
                        }
                        // 否则，将目标生物骑乘到玩家头上
                        living.leaveVehicle();
                        top.addPassenger(living);
                        String msg = langConfig.getString("messages.entity_stacked", "").replace("%player%", player.getName()).replace("%entity%", living.getType().toString());
                        if (enableLogging) {
                            getLogger().info(msg);
                        }
                        player.sendMessage(msg);
                    }
                }
            }

            @org.bukkit.event.EventHandler
            public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
                org.bukkit.entity.Player player = event.getPlayer();
                if (player.isSneaking()) {
                    // 如果玩家正在蹲下，则让所有骑乘的生物散开
                    org.bukkit.entity.Entity vehicle = player;
                    while (vehicle.getPassengers().size() > 0) {
                        vehicle.getPassengers().forEach(passenger -> passenger.leaveVehicle());
                        vehicle = vehicle.getPassengers().get(0);
                    }
                    String msg = langConfig.getString("messages.entities_dispersed", "").replace("%player%", player.getName());
                    if (enableLogging) {
                        getLogger().info(msg);
                    }
                    player.sendMessage(msg);
                }
            }
        }, this);
    }
}
