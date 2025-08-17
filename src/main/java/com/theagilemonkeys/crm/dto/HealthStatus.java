package com.theagilemonkeys.crm.dto;

import java.util.List;

public class HealthStatus {
    private String status;
    private List<ComponentStatus> components;

    public HealthStatus(String status, List<ComponentStatus> components) {
        this.status = status;
        this.components = components;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ComponentStatus> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentStatus> components) {
        this.components = components;
    }
}
