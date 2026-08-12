package com.spunish.velocity.platform;

import com.spunish.common.platform.MainThreadDispatcher;

/**
 * Velocity's API has no main-thread restriction — every call is safe from
 * any thread, so there is nothing to hop to.
 */
public final class VelocityMainThreadDispatcher implements MainThreadDispatcher {

    @Override
    public void runOnMainThread(Runnable task) {
        task.run();
    }
}
