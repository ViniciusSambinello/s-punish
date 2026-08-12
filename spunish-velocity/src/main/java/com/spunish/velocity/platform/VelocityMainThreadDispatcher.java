package com.spunish.velocity.platform;

import com.spunish.common.platform.MainThreadDispatcher;

public final class VelocityMainThreadDispatcher implements MainThreadDispatcher {

    @Override
    public void runOnMainThread(Runnable task) {
        task.run();
    }
}
