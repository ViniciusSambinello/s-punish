package com.spunish.paper.platform;

import com.spunish.common.platform.MainThreadDispatcher;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

public final class PaperMainThreadDispatcher implements MainThreadDispatcher {

    private final Plugin plugin;
    private final Server server;

    public PaperMainThreadDispatcher(Plugin plugin, Server server) {
        this.plugin = plugin;
        this.server = server;
    }

    @Override
    public void runOnMainThread(Runnable task) {
        if (server.isPrimaryThread()) {
            task.run();
        } else {
            server.getScheduler().runTask(plugin, task);
        }
    }
}
