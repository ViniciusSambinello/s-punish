package com.spunish.common.platform;

/**
 * This instance's configured or derived identifier — recorded as
 * {@code origin_server} on every punishment applied here and used by the
 * sync consumer to ignore self-originated events (see {@code punishment/network-sync}).
 */
public interface ServerIdentity {

    String id();
}
