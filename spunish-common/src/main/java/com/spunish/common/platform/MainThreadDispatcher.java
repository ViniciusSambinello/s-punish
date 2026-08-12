package com.spunish.common.platform;

/**
 * Hops back to the platform's main thread for the parts of a listener/command
 * flow (opening a GUI, calling most Bukkit API) that require it — everything
 * upstream of that call (resolution, storage, service logic) runs off it.
 */
public interface MainThreadDispatcher {

    void runOnMainThread(Runnable task);
}
