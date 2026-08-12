package com.spunish.common.platform;

/**
 * This instance's identifier, recorded as {@code origin_server} on every
 * punishment applied here and used to ignore self-originated network-sync events.
 */
public interface ServerIdentity {

    String id();
}
