package com.mycompany.projectplanner;

import java.util.List;

public interface IDataRepository {

    void saveProject(Project project) throws Exception;
    Project loadProject(int projectId) throws Exception;
    Project loadProject() throws Exception;
    List<ProjectInfo> getAllProjects() throws Exception;
    void updateTask(Task task, int projectId) throws Exception;
    void deleteTask(int taskId, int projectId) throws Exception;
    void updateResource(Resource resource, int projectId) throws Exception;
    void deleteResource(String resourceName, int projectId) throws Exception;
    boolean isAvailable();
    String getRepositoryType();
}

class ProjectInfo {
    public int projectId;
    public String projectName;
    public String createdDate;
    
    public ProjectInfo(int projectId, String projectName, String createdDate) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.createdDate = createdDate;
    }
    
    @Override
    public String toString() {
        return projectName + " (ID: " + projectId + ", Created: " + createdDate + ")";
    }
}