package Model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UsageSession implements Serializable {
    private String username;
    private int machineID;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public UsageSession(String username, int machineID, LocalDateTime startTime, LocalDateTime endTime) {
        this.username = username;
        this.machineID = machineID;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getMachineID() {
        return machineID;
    }

    public void setMachineID(int machineID) {
        this.machineID = machineID;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
