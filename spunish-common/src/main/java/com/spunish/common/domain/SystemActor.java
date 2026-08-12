package com.spunish.common.domain;

/**
 * Marker actor for automatic actions taken by the plugin itself — for example,
 * revoking a punishment that a {@code spunish.punish.override} application superseded.
 */
public record SystemActor() implements Actor {

    public static final SystemActor INSTANCE = new SystemActor();
}
