package com.mycompany.projectplanner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileDataRepository implements IDataRepository {
    
    private String tasksFilePath;
    private String resourcesFilePath;
    
    public FileDataRepository() {
        this.tasksFilePath = "./Tasks.txt";
        this.resourcesFilePath = "./Resources.txt";
    }
    
    public FileDataRepository(String tasksFilePath, String resourcesFilePath) {
        this.tasksFilePath = tasksFilePath;
        this.resourcesFilePath = resourcesFilePath;
    }
    
    @Override
    public void saveProject(Project project) throws Exception {
        File tasksFile = new File(tasksFilePath);
        File resourcesFile = new File(resourcesFilePath);
        
        saveTasksFile(project, tasksFile);
        saveResourcesFile(project, resourcesFile);
    }
    
    @Override
    public Project loadProject(int projectId) throws Exception {
        return loadProject();
    }
    
    @Override
    public Project loadProject() throws Exception {
        File tasksFile = new File(tasksFilePath);
        File resourcesFile = new File(resourcesFilePath);
        
        // Try alternative paths if files don't exist
        if (!tasksFile.exists() || !resourcesFile.exists()) {
            tasksFile = new File("src/projectplanner/Tasks.txt");
            resourcesFile = new File("src/projectplanner/Resources.txt");
        }
        
        if (!tasksFile.exists() || !resourcesFile.exists()) {
            throw new IOException("Tasks.txt or Resources.txt not found");
        }
        
        Project project = new Project();
        Map<Integer, List<Integer>> dependencyMap = FileUtilty.parseTasksFile(tasksFile, project);
        Map<Resource, Map<Integer, Integer>> resourceAllocTemp = FileUtilty.parseResourcesFile(resourcesFile);
        FileUtilty.resolveProjectData(project, dependencyMap, resourceAllocTemp);
        
        return project;
    }
    
    @Override
    public List<ProjectInfo> getAllProjects() throws Exception {
        List<ProjectInfo> projects = new ArrayList<>();
        projects.add(new ProjectInfo(1, "File-based Project", "N/A"));
        return projects;
    }
    
    @Override
    public void updateTask(Task task, int projectId) throws Exception {
        Project project = loadProject();
        project.tasksById.put(task.id, task);
        saveProject(project);
    }
    
    @Override
    public void deleteTask(int taskId, int projectId) throws Exception {
        Project project = loadProject();
        project.tasksById.remove(taskId);
        
        // Remove from dependencies and children
        for (Task t : project.tasksById.values()) {
            t.dependencies.removeIf(dep -> dep.id == taskId);
            t.children.removeIf(child -> child.id == taskId);
        }
        
        // Remove resource allocations
        for (Resource r : project.resources) {
            r.allocations.entrySet().removeIf(entry -> entry.getKey().id == taskId);
        }
        
        saveProject(project);
    }
    
    @Override
    public void updateResource(Resource resource, int projectId) throws Exception {
        Project project = loadProject();
        
        // Find and update resource
        Resource existing = project.resources.stream()
            .filter(r -> r.name.equals(resource.name))
            .findFirst()
            .orElse(null);
            
        if (existing != null) {
            existing.allocations = resource.allocations;
        } else {
            project.resources.add(resource);
        }
        
        saveProject(project);
    }
    
    @Override
    public void deleteResource(String resourceName, int projectId) throws Exception {
        Project project = loadProject();
        project.resources.removeIf(r -> r.name.equals(resourceName));
        saveProject(project);
    }
    
    @Override
    public boolean isAvailable() {
        File tasksFile = new File(tasksFilePath);
        File resourcesFile = new File(resourcesFilePath);
        return tasksFile.exists() && resourcesFile.exists();
    }
    
    @Override
    public String getRepositoryType() {
        return "File System";
    }
    
    // Helper methods
    private void saveTasksFile(Project project, File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(file)) {
            pw.println("# ID, Title, Start, End, Dependencies...");
            for (Task t : project.getTasks()) {
                StringBuilder sb = new StringBuilder();
                sb.append(t.id).append(", ")
                  .append(t.title).append(", ")
                  .append(t.startTime.format(Task.Formater)).append(", ")
                  .append(t.endTime.format(Task.Formater));
                
                if (!t.dependencies.isEmpty()) {
                    sb.append(", ");
                    for (int i = 0; i < t.dependencies.size(); i++) {
                        sb.append(t.dependencies.get(i).id);
                        if (i < t.dependencies.size() - 1) sb.append(" ");
                    }
                }
                pw.println(sb);
            }
        }
    }
    
    private void saveResourcesFile(Project project, File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(file)) {
            pw.println("# ResourceName, TaskId:Percentage, ...");
            for (Resource r : project.getResources()) {
                StringBuilder sb = new StringBuilder();
                sb.append(r.getName());
                for (Map.Entry<Task, Integer> e : r.getAllocations().entrySet()) {
                    sb.append(", ").append(e.getKey().id).append(":").append(e.getValue());
                }
                pw.println(sb);
            }
        }
    }
    
    public void setFilePaths(String tasksPath, String resourcesPath) {
        this.tasksFilePath = tasksPath;
        this.resourcesFilePath = resourcesPath;
    }
}