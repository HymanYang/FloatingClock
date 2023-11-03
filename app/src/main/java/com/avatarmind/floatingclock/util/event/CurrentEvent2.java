package com.avatarmind.floatingclock.util.event;

public class CurrentEvent2 {
    long currentTime = 0;

    int type = 0 ;

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public CurrentEvent2(long currentTime) {
        this.currentTime = currentTime;
    }

    public CurrentEvent2(long currentTime, int type) {
        this.currentTime = currentTime;
        this.type = type;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }
}
