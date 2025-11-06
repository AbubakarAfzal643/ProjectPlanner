package com.mycompany.projectplanner;

import java.util.*;

public class Resource {

    public String name;
    public Map<Task, Integer> allocations = new LinkedHashMap<>();

    public Resource(String name) {
        this.name = name;
    }

    public void addAllocations(Task t, int pct) {
        allocations.put(t, pct);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Task, Integer> getAllocations() {
        return allocations;
    }
}
