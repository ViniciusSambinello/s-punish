package com.spunish.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

@Plugin(id = "spunish", name = "s-punish", version = "0.1.0-SNAPSHOT",
        description = "Network-wide punishment enforcement at the proxy edge.")
public final class SPunishVelocityPlugin {

    private final ProxyServer server;
    private final Path dataDirectory;
    private SPunishVelocityServices services;

    @Inject
    public SPunishVelocityPlugin(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            services = SPunishVelocityServices.bootstrap(server, dataDirectory);
        } catch (SPunishVelocityBootstrapException e) {
            Logger.getLogger("s-punish").log(Level.SEVERE, "s-punish could not be enabled: " + e.getMessage(), e);
            return;
        }
        server.getEventManager().register(this, new BanLoginListener(services));
    }

    public SPunishVelocityServices services() {
        return services;
    }
}
