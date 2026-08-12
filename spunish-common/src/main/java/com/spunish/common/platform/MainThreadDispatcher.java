package com.spunish.common.platform;

public interface MainThreadDispatcher {

    void runOnMainThread(Runnable task);
}
