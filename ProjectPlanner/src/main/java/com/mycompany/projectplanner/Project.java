package com.mycompany.projectplanner;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class Project {

    // all tasks are keyed by id, using LinkedHashMap to maintain insertion order
    public Map<Integer, Task> tasksById = new LinkedHashMap<Integer, Task>();
    // all resources in a list
    public List<Resource> resources = new ArrayList<Resource>();

    // optional project name field for display/saving
    private String name = "Untitled Project";
    
    // Project ID for database operations
    private int projectId = -1;


    public void addTask(Task t) {
        tasksById.put(t.id, t);
    }

    public void addResource(Resource r) {
        resources.add(r);
    }

    public Task getTaskById(int id) {
        return tasksById.get(id);
    }


    public LocalDateTime projectStart() {
        LocalDateTime min = null;
        for (Task t : tasksById.values()) {
            if (min == null || t.startTime.isBefore(min)) {
                min = t.startTime;
            }
        }
        return min;
    }

    public LocalDateTime projectEnd() {
        LocalDateTime max = null;
        for (Task t : tasksById.values()) {
            if (max == null || t.endTime.isAfter(max)) {
                max = t.endTime;
            }
        }
        return max;
    }

    public Duration projectDuration() {
        LocalDateTime start = projectStart();
        LocalDateTime end = projectEnd();
        if (start == null || end == null) {
            return Duration.ZERO;
        }
        return Duration.between(start, end);
    }


    public List<String> findOverlappingDependencyPairs() {
        List<String> result = new ArrayList<>();
        for (Task t : tasksById.values()) {
            for (Task dep : t.dependencies) {
                if (t.startTime.isBefore(dep.endTime) && t.overLapsWith(dep)) {
                    result.add("Task " + t.id + " overlaps with its dependency Task " + dep.id);
                }
            }
        }
        return result;
    }

    public List<String> teamForTask(int taskId) {
        List<String> names = new ArrayList<>();
        Task task = tasksById.get(taskId);
        if (task == null) return names;

        for (Resource r : resources) {
            if (r.allocations.containsKey(task)) {
                names.add(r.name + " (" + r.allocations.get(task) + "%)");
            }
        }
        return names;
    }

    public Map<String, Double> totalEffortHoursPerResource() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Resource r : resources) {
            double total = 0.0;
            for (Map.Entry<Task, Integer> e : r.allocations.entrySet()) {
                double taskHours = e.getKey().getDurationInHours();
                total += taskHours * (e.getValue() / 100.0);
            }
            map.put(r.name, total);
        }
        return map;
    }


    /** Return all tasks as a list */
    public List<Task> getTasks() {
        return new ArrayList<>(tasksById.values());
    }

    /** Return all resources */
    public List<Resource> getResources() {
        return resources;
    }

    /** Get or set project name (for UI display) */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    /** Get or set project ID (for database operations) */
    public int getProjectId() {
        return projectId;
    }
    
    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }
}