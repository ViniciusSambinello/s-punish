package com.spunish.paper;

import com.spunish.common.domain.PunishmentCategory;
import com.spunish.paper.command.HistoryCommand;
import com.spunish.paper.command.MenuOpener;
import com.spunish.paper.command.PlayerResolver;
import com.spunish.paper.command.PunishCommand;
import com.spunish.paper.command.RecordCommand;
import com.spunish.paper.command.ReloadCommand;
import com.spunish.paper.command.UnpunishCommand;
import com.spunish.paper.gui.MenuListener;
import com.spunish.paper.gui.PaperMenuOpener;
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
        pluginManager.registerEvents(new MenuListener(), this);

        registerCommands();

        getLogger().info("SPunish enabled.");
    }

    private void registerCommands() {
        PlayerResolver playerResolver = new PlayerResolver(getServer(), services.profileRepository());
        MenuOpener menuOpener = new PaperMenuOpener(services);

        registerCommand("punish", "Apply a punishment to a player.",
                new PunishCommand(services, playerResolver, menuOpener));
        registerCommand("unban", "Revoke a player's active ban.",
                new UnpunishCommand(services, playerResolver, PunishmentCategory.BAN, "unban"));
        registerCommand("unmute", "Revoke a player's active mute.",
                new UnpunishCommand(services, playerResolver, PunishmentCategory.MUTE, "unmute"));
        registerCommand("record", "View a staff punishment report.",
                new RecordCommand(services, playerResolver, menuOpener));
        registerCommand("history", "View a player's punishment history.",
                new HistoryCommand(services, playerResolver, menuOpener));
        registerCommand("spunishreload", "Reload reasons.yml and messages.yml.",
                new ReloadCommand(services));
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
