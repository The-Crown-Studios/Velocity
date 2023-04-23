package com.velocitypowered.proxy.thread;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public class VelocityThread extends Thread {

    public VelocityThread(@NotNull String name) {
        super(name);
    }

}