package fr.openmc.core.utils;

import fr.openmc.core.OMCPlugin;
import lombok.Getter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPermsAPI {
    @Getter private static LuckPerms api;
    private static boolean hasLuckPerms;

    public LuckPermsAPI(OMCPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            hasLuckPerms = false;
            return;
        } else {
            hasLuckPerms = true;
        }

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            plugin.lpApi = provider.getProvider();
            api = provider.getProvider();
        }
    }

    public static boolean hasLuckPerms() {
        return hasLuckPerms;
    }

    public static String getPrefix(Player player) {
        if (!hasLuckPerms) return "";

        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";

        return user.getCachedData().getMetaData(QueryOptions.defaultContextualOptions()).getPrefix();
    }
}