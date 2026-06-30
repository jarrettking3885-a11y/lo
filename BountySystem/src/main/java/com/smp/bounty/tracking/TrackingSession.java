package com.smp.bounty.tracking;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class TrackingSession {

    private final UUID trackerUuid;
    private final UUID targetUuid;
    private final String targetName;
    private final long startedAt;
    private BukkitTask task;

    public TrackingSession(UUID trackerUuid, UUID targetUuid, String targetName) {
        this.trackerUuid = trackerUuid;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.startedAt = System.currentTimeMillis();
    }

    public UUID getTrackerUuid() { return trackerUuid; }
    public UUID getTargetUuid() { return targetUuid; }
    public String getTargetName() { return targetName; }
    public long getStartedAt() { return startedAt; }

    public void setTask(BukkitTask task) { this.task = task; }

    public void cancel() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
}
