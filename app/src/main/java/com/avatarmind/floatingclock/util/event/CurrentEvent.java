package com.avatarmind.floatingclock.util.event;

public class CurrentEvent {
    long currentTime = 0;

    public CurrentEvent(long currentTime) {
        this.currentTime = currentTime;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }
}
