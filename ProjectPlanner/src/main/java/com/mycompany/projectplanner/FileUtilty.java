package com.mycompany.projectplanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class FileUtilty {

    // get data from tasks.txt and create objects from it
    public static Map<Integer, List<Integer>> parseTasksFile(File tasksFile, Project project) throws IOException {
        Map<Integer, List<Integer>> dependencyMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(tasksFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(",");
                // Ensure there are at least 4 required fields (ID, Title, Start, End)
                if (parts.length < 4) continue;

                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String title = parts[1].trim();
                    String start = parts[2].trim();
                    String end = parts[3].trim();
                    Task t = new Task(id, title, start, end); // creating obvjects here
                    project.addTask(t);

                    List<Integer> deps = new ArrayList<>();
                    // Handle dependencies (parts[4] and beyond)
                    for (int i = 4; i < parts.length; i++) {
                        String s = parts[i].trim();
                        if (!s.isEmpty()) {
                            // Split dependencies by whitespace or comma (just in case)
                            String[] depParts = s.split("[\\s,]+");
                            for (String dp : depParts) {
                                dp = dp.trim();
                                if (dp.isEmpty()) continue;
                                deps.add(Integer.parseInt(dp));
                            }
                        }
                    }
                    dependencyMap.put(id, deps);
                } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
                    System.err.println("Skipping invalid task line: " + line + ". Error: " + e.getMessage());
                }
            }
        }
        return dependencyMap;
    }
    public static Map<Resource, Map<Integer, Integer>> parseResourcesFile(File resourcesFile) throws IOException {
        Map<Resource, Map<Integer, Integer>> map = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(resourcesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(",");
                if (parts.length < 1) continue;

                Resource r = new Resource(parts[0]);
                Map<Integer, Integer> tmp = new LinkedHashMap<>();
                for (int i = 1; i < parts.length; i++) {
                    String p = parts[i].trim();
                    if (p.isEmpty()) continue;
                    String[] kv = p.split(":");
                    if (kv.length == 2) {
                        try {
                            int tid = Integer.parseInt(kv[0].trim());
                            int pct = Integer.parseInt(kv[1].trim());
                            tmp.put(tid, pct);
                        } catch (NumberFormatException ex) {
                            System.err.println("Skipping invalid allocation token: " + p);
                        }
                    }
                }
                map.put(r, tmp);
            }
        }
        return map;
    }

    public static void resolveProjectData(Project project, Map<Integer, List<Integer>> dependencyMap, Map<Resource, Map<Integer, Integer>> resourceAllocTemp) {
        // this will  connect task dependencies first
        for (Map.Entry<Integer, List<Integer>> e : dependencyMap.entrySet()) {
            Task t = project.getTaskById(e.getKey());
            if (t == null) continue;
            for (Integer depId : e.getValue()) {
                Task dep = project.getTaskById(depId);
                if (dep != null) {
                    t.dependencies.add(dep);
                    dep.children.add(t);
                }
            }
        }

        //then assighn resources to tasks
        for (Map.Entry<Resource, Map<Integer, Integer>> e : resourceAllocTemp.entrySet()) {
            Resource r = e.getKey();
            for (Map.Entry<Integer, Integer> alloc : e.getValue().entrySet()) {
                Task t = project.getTaskById(alloc.getKey());
                if (t != null) {
                    r.addAllocations(t, alloc.getValue());
                }
            }
            project.addResource(r);
        }
    }
}
