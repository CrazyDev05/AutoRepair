package de.crazydev22.repair;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class AutoRepair extends JavaPlugin {
    private final NamespacedKey KEY = new NamespacedKey(this, "auto_repair");

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (var player : Bukkit.getOnlinePlayers()) {
                if (!player.getPersistentDataContainer().getOrDefault(KEY, PersistentDataType.BOOLEAN, false))
                    continue;
                int xp = (int) (getExpAtLevel(player.getLevel()) + (player.getExp() * player.getExpToLevel()));
                if (xp <= 0) continue;
                var serverPlayer = ((CraftPlayer) player).getHandle();
                xp -= repairPlayerGears(serverPlayer, xp);
                serverPlayer.giveExperiencePoints(-xp);
            }
        }, 0, 1);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!command.getName().equalsIgnoreCase("tool-heal"))
            return false;

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by a player!");
            return false;
        }

        var pdc = player.getPersistentDataContainer();
        boolean newRepair = !pdc.getOrDefault(KEY, PersistentDataType.BOOLEAN, false);
        pdc.set(KEY, PersistentDataType.BOOLEAN, newRepair);

        sender.sendMessage(newRepair ? "Your tools will now repair itself!" : "Your tools will not repair itself!");
        return true;
    }

    private static int repairPlayerGears(ServerPlayer player, int value) {
        Optional<EnchantedItemInUse> randomItemWith = EnchantmentHelper.getRandomItemWith(
                EnchantmentEffectComponents.REPAIR_WITH_XP, player, net.minecraft.world.item.ItemStack::isDamaged
        );
        if (randomItemWith.isPresent()) {
            ItemStack itemStack = randomItemWith.get().itemStack();
            int i = EnchantmentHelper.modifyDurabilityToRepairFromXp(player.serverLevel(), itemStack, value);
            int min = Math.min(i, itemStack.getDamageValue());
            itemStack.setDamageValue(itemStack.getDamageValue() - min);
            if (min > 0) {
                int i1 = value - min * value / i;
                if (i1 > 0) {
                    return repairPlayerGears(player, i1);
                }
            }

            return 0;
        } else {
            return value;
        }
    }

    private static long getExpAtLevel(int level) {
        if (level <= 16)
            return (long) (Math.pow(level, 2) + 6L * level + 0);
        else if (level <= 31)
            return (long) (2.5 * Math.pow(level, 2) - 40.5 * level + 360.0);
        else
            return (long) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220.0);
    }
}
