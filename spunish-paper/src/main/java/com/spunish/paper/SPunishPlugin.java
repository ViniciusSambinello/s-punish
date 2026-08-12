package com.spunish.paper;

import com.spunish.paper.listener.ChatListener;
import com.spunish.paper.listener.CommandBlockListener;
import com.spunish.paper.listener.MuteWarningCooldown;
import com.spunish.paper.listener.PreLoginListener;
import com.spunish.paper.listener.QuitListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SPunishPlugin extends JavaPlugin {

    private SPunishServices services;

    @Override
    public void onEnable() {
        try {
            services = SPunishServices.bootstrap(this);
        } catch (SPunishBootstrapException e) {
            getLogger().severe("SPunish could not be enabled: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        MuteWarningCooldown muteWarningCooldown = new MuteWarningCooldown();
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PreLoginListener(services), this);
        pluginManager.registerEvents(new ChatListener(services, muteWarningCooldown), this);
        pluginManager.registerEvents(new CommandBlockListener(services, getServer(), muteWarningCooldown), this);
        pluginManager.registerEvents(new QuitListener(services, muteWarningCooldown), this);

        // Commands (section 8) and GUIs (section 9) register here too.

        getLogger().info("SPunish enabled.");
    }

    @Override
    public void onDisable() {
        if (services != null) {
            services.close();
            services = null;
        }
        getLogger().info("SPunish disabled.");
    }

    public SPunishServices services() {
        return services;
    }
}
