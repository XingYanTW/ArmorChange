package me.xingyan.armorchange;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent; // 修正此行
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ArmorChangeListener implements Listener {

    private boolean isArmor(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type.name().endsWith("_HELMET") ||
               type.name().endsWith("_CHESTPLATE") ||
               type.name().endsWith("_LEGGINGS") ||
               type.name().endsWith("_BOOTS");
    }

    // 1. 处理点击、拖动、shift点击等
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory() instanceof PlayerInventory)) return;

        ItemStack newItem = event.getCursor();
        ItemStack oldItem = event.getCurrentItem();

        // 检查是否是盔甲槽
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            if (isArmor(newItem) && event.getAction() != InventoryAction.COLLECT_TO_CURSOR) {
                // 装备
                player.getServer().getPluginManager().callEvent(new ArmorEquipEvent(player, newItem));
            }
            if (isArmor(oldItem) && event.getAction() != InventoryAction.COLLECT_TO_CURSOR) {
                // 卸下
                player.getServer().getPluginManager().callEvent(new ArmorUnequipEvent(player, oldItem));
            }
        }
        // shift点击从物品栏装备
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && isArmor(event.getCurrentItem())) {
            player.getServer().getPluginManager().callEvent(new ArmorEquipEvent(player, event.getCurrentItem()));
        }
    }

    // 2. 拖动
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        for (int slot : event.getRawSlots()) {
            if (slot >= 5 && slot <= 8 && isArmor(event.getOldCursor())) {
                player.getServer().getPluginManager().callEvent(new ArmorEquipEvent(player, event.getOldCursor()));
            }
        }
    }

    // 3. 热键交换
    @EventHandler
    public void onHotbarSwap(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (isArmor(hotbarItem)) {
                player.getServer().getPluginManager().callEvent(new ArmorEquipEvent(player, hotbarItem));
            }
            ItemStack armorItem = event.getCurrentItem();
            if (isArmor(armorItem)) {
                player.getServer().getPluginManager().callEvent(new ArmorUnequipEvent(player, armorItem));
            }
        }
    }

    // 4. 右键装备
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        // 只处理主手，避免副手重复触发
        if (event.getHand() != EquipmentSlot.HAND) return;
        // 只处理右键
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        if (event.getItem() != null && isArmor(event.getItem())) {
            Player player = event.getPlayer();
            // 只在玩家未打开界面且盔甲槽为空或不同装备时触发
            if (player.getOpenInventory().getType() == InventoryType.CRAFTING) {
                Material type = event.getItem().getType();
                ItemStack currentArmor = switch (type) {
                    case LEATHER_HELMET, CHAINMAIL_HELMET, IRON_HELMET, GOLDEN_HELMET, DIAMOND_HELMET, NETHERITE_HELMET ->
                        player.getInventory().getHelmet();
                    case LEATHER_CHESTPLATE, CHAINMAIL_CHESTPLATE, IRON_CHESTPLATE, GOLDEN_CHESTPLATE, DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE ->
                        player.getInventory().getChestplate();
                    case LEATHER_LEGGINGS, CHAINMAIL_LEGGINGS, IRON_LEGGINGS, GOLDEN_LEGGINGS, DIAMOND_LEGGINGS, NETHERITE_LEGGINGS ->
                        player.getInventory().getLeggings();
                    case LEATHER_BOOTS, CHAINMAIL_BOOTS, IRON_BOOTS, GOLDEN_BOOTS, DIAMOND_BOOTS, NETHERITE_BOOTS ->
                        player.getInventory().getBoots();
                    default -> null;
                };
                // 只有當盔甲槽為空或裝備不同時才觸發
                if (currentArmor == null || currentArmor.getType() == Material.AIR || !currentArmor.isSimilar(event.getItem())) {
                    player.getServer().getPluginManager().callEvent(new ArmorEquipEvent(player, event.getItem()));
                }
            }
        }
    }

    // 5. Q键丢弃
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isArmor(item)) {
            event.getPlayer().getServer().getPluginManager().callEvent(new ArmorUnequipEvent(event.getPlayer(), item));
        }
    }

    // 6. 由发射器装备
    @EventHandler
    public void onDispenser(BlockDispenseArmorEvent event) {
        if (event.getTargetEntity() instanceof Player player && isArmor(event.getItem())) {
            player.getServer().getPluginManager().callEvent(new ArmorEquipEvent(player, event.getItem()));
        }
    }

    // 7. 玩家死亡卸下
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (isArmor(item)) {
                player.getServer().getPluginManager().callEvent(new ArmorUnequipEvent(player, item));
            }
        }
    }

    // 8. 盔甲损坏
    @EventHandler
    public void onArmorBreak(PlayerItemBreakEvent event) {
        ItemStack item = event.getBrokenItem();
        if (isArmor(item)) {
            event.getPlayer().getServer().getPluginManager().callEvent(new ArmorUnequipEvent(event.getPlayer(), item));
        }
    }
}
