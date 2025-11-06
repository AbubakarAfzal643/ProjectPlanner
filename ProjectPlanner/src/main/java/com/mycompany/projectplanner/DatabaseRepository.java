package com.mycompany.projectplanner;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class DatabaseRepository implements IDataRepository {
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            DatabaseConfig.DB_URL,
            DatabaseConfig.DB_USER,
            DatabaseConfig.DB_PASSWORD
        );
    }
    
    @Override
    public void saveProject(Project project) throws Exception {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                int projectId = getOrCreateProject(conn, project.getName());
                project.setProjectId(projectId);
                clearProjectData(conn, projectId);
                
                
                saveTasks(conn, project, projectId);
                saveTaskDependencies(conn, project, projectId);
                
                
                saveResources(conn, project, projectId);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    @Override
    public Project loadProject(int projectId) throws Exception {
        try (Connection conn = getConnection()) {
            Project project = new Project();
            
            // Load project info
            String projectSql = "SELECT ProjectName FROM Projects WHERE ProjectId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(projectSql)) {
                pstmt.setInt(1, projectId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    project.setName(rs.getString("ProjectName"));
                    project.setProjectId(projectId);
                } else {
                    throw new Exception("Project not found with ID: " + projectId);
                }
            }
            
            // at startup 
            loadTasks(conn, project, projectId);
            loadTaskDependencies(conn, project, projectId);
            loadResources(conn, project, projectId);
            
            return project;
        }
    }
    
    @Override
    public Project loadProject() throws Exception {
        List<ProjectInfo> projects = getAllProjects();
        if (projects.isEmpty()) {
            throw new Exception("No projects found in database");
        }
        return loadProject(projects.get(0).projectId);
    }
    
    @Override
    public List<ProjectInfo> getAllProjects() throws Exception {
        List<ProjectInfo> projects = new ArrayList<>();
        
        try (Connection conn = getConnection()) {
            String sql = "SELECT ProjectId, ProjectName, CreatedDate FROM Projects ORDER BY CreatedDate DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    projects.add(new ProjectInfo(
                        rs.getInt("ProjectId"),
                        rs.getString("ProjectName"),
                        rs.getTimestamp("CreatedDate").toString()
                    ));
                }
            }
        }
        
        return projects;
    }
    
    @Override
    public void updateTask(Task task, int projectId) throws Exception {
        try (Connection conn = getConnection()) {
            String sql = "UPDATE Tasks SET Title = ?, StartTime = ?, EndTime = ? WHERE TaskId = ? AND ProjectId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, task.title);
                pstmt.setTimestamp(2, Timestamp.valueOf(task.startTime));
                pstmt.setTimestamp(3, Timestamp.valueOf(task.endTime));
                pstmt.setInt(4, task.id);
                pstmt.setInt(5, projectId);
                pstmt.executeUpdate();
            }
        }
    }
    
    @Override
    public void deleteTask(int taskId, int projectId) throws Exception {
        try (Connection conn = getConnection()) {
            // Dependencies will be deleted automatically due to CASCADE
            String sql = "DELETE FROM Tasks WHERE TaskId = ? AND ProjectId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, taskId);
                pstmt.setInt(2, projectId);
                pstmt.executeUpdate();
            }
        }
    }
    
    @Override
    public void updateResource(Resource resource, int projectId) throws Exception {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Find or create resource
                int resourceId = getOrCreateResource(conn, resource.getName(), projectId);
                
                // Delete existing allocations
                String deleteSql = "DELETE FROM ResourceAllocations WHERE ResourceId = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                    pstmt.setInt(1, resourceId);
                    pstmt.executeUpdate();
                }
                
                // Insert new allocations
                String insertSql = "INSERT INTO ResourceAllocations (ResourceId, TaskId, AllocationPercentage) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    for (Map.Entry<Task, Integer> entry : resource.getAllocations().entrySet()) {
                        pstmt.setInt(1, resourceId);
                        pstmt.setInt(2, entry.getKey().id);
                        pstmt.setInt(3, entry.getValue());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
                
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    @Override
    public void deleteResource(String resourceName, int projectId) throws Exception {
        try (Connection conn = getConnection()) {
            String sql = "DELETE FROM Resources WHERE ResourceName = ? AND ProjectId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, resourceName);
                pstmt.setInt(2, projectId);
                pstmt.executeUpdate();
            }
        }
    }
    
    @Override
    public boolean isAvailable() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getRepositoryType() {
        return "SQL Server Database";
    }
    
    // Private helper methods
    
    private int getOrCreateProject(Connection conn, String projectName) throws SQLException {
        // Check if project exists
        String checkSql = "SELECT ProjectId FROM Projects WHERE ProjectName = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setString(1, projectName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("ProjectId");
            }
        }
        
        // Create new project
        String insertSql = "INSERT INTO Projects (ProjectName) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, projectName);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Failed to create project");
        }
    }
    
    private void clearProjectData(Connection conn, int projectId) throws SQLException {
        // Delete in correct order due to foreign keys
        String[] tables = {"TaskDependencies", "ResourceAllocations", "Resources", "Tasks"};
        for (String table : tables) {
            String sql = "DELETE FROM " + table + " WHERE " + 
                        (table.equals("TaskDependencies") ? "TaskId IN (SELECT TaskId FROM Tasks WHERE ProjectId = ?)" :
                         table.equals("ResourceAllocations") ? "TaskId IN (SELECT TaskId FROM Tasks WHERE ProjectId = ?)" :
                         "ProjectId = ?");
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, projectId);
                pstmt.executeUpdate();
            }
        }
    }
    
    private void saveTasks(Connection conn, Project project, int projectId) throws SQLException {
        String sql = "INSERT INTO Tasks (TaskId, ProjectId, Title, StartTime, EndTime) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Task task : project.getTasks()) {
                pstmt.setInt(1, task.id);
                pstmt.setInt(2, projectId);
                pstmt.setString(3, task.title);
                pstmt.setTimestamp(4, Timestamp.valueOf(task.startTime));
                pstmt.setTimestamp(5, Timestamp.valueOf(task.endTime));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    
    private void saveTaskDependencies(Connection conn, Project project, int projectId) throws SQLException {
        String sql = "INSERT INTO TaskDependencies (TaskId, DependsOnTaskId) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Task task : project.getTasks()) {
                for (Task dep : task.dependencies) {
                    pstmt.setInt(1, task.id);
                    pstmt.setInt(2, dep.id);
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();
        }
    }
    
    private void saveResources(Connection conn, Project project, int projectId) throws SQLException {
        for (Resource resource : project.getResources()) {
            // Insert resource
            String resSql = "INSERT INTO Resources (ProjectId, ResourceName) VALUES (?, ?)";
            int resourceId;
            try (PreparedStatement pstmt = conn.prepareStatement(resSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, projectId);
                pstmt.setString(2, resource.getName());
                pstmt.executeUpdate();
                
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    resourceId = rs.getInt(1);
                } else {
                    throw new SQLException("Failed to insert resource");
                }
            }
            
            // Insert allocations
            String allocSql = "INSERT INTO ResourceAllocations (ResourceId, TaskId, AllocationPercentage) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(allocSql)) {
                for (Map.Entry<Task, Integer> entry : resource.getAllocations().entrySet()) {
                    pstmt.setInt(1, resourceId);
                    pstmt.setInt(2, entry.getKey().id);
                    pstmt.setInt(3, entry.getValue());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        }
    }
    
    private void loadTasks(Connection conn, Project project, int projectId) throws SQLException {
        String sql = "SELECT TaskId, Title, StartTime, EndTime FROM Tasks WHERE ProjectId = ? ORDER BY TaskId";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("TaskId");
                String title = rs.getString("Title");
                LocalDateTime start = rs.getTimestamp("StartTime").toLocalDateTime();
                LocalDateTime end = rs.getTimestamp("EndTime").toLocalDateTime();
                
                Task task = new Task(id, title, 
                    start.format(Task.Formater), 
                    end.format(Task.Formater));
                project.addTask(task);
            }
        }
    }
    
    private void loadTaskDependencies(Connection conn, Project project, int projectId) throws SQLException {
        String sql = "SELECT td.TaskId, td.DependsOnTaskId FROM TaskDependencies td " +
                     "INNER JOIN Tasks t ON td.TaskId = t.TaskId WHERE t.ProjectId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int taskId = rs.getInt("TaskId");
                int depId = rs.getInt("DependsOnTaskId");
                
                Task task = project.getTaskById(taskId);
                Task dep = project.getTaskById(depId);
                
                if (task != null && dep != null) {
                    task.dependencies.add(dep);
                    dep.children.add(task);
                }
            }
        }
    }
    
    private void loadResources(Connection conn, Project project, int projectId) throws SQLException {
        String sql = "SELECT r.ResourceId, r.ResourceName, ra.TaskId, ra.AllocationPercentage " +
                     "FROM Resources r " +
                     "LEFT JOIN ResourceAllocations ra ON r.ResourceId = ra.ResourceId " +
                     "WHERE r.ProjectId = ? ORDER BY r.ResourceId";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            
            Map<Integer, Resource> resourceMap = new LinkedHashMap<>();
            
            while (rs.next()) {
                int resourceId = rs.getInt("ResourceId");
                String resourceName = rs.getString("ResourceName");
                
                Resource resource = resourceMap.get(resourceId);
                if (resource == null) {
                    resource = new Resource(resourceName);
                    resourceMap.put(resourceId, resource);
                    project.addResource(resource);
                }
                
                int taskId = rs.getInt("TaskId");
                if (!rs.wasNull()) {
                    int percentage = rs.getInt("AllocationPercentage");
                    Task task = project.getTaskById(taskId);
                    if (task != null) {
                        resource.addAllocations(task, percentage);
                    }
                }
            }
        }
    }
    
    private int getOrCreateResource(Connection conn, String resourceName, int projectId) throws SQLException {
        String checkSql = "SELECT ResourceId FROM Resources WHERE ResourceName = ? AND ProjectId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setString(1, resourceName);
            pstmt.setInt(2, projectId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("ResourceId");
            }
        }
        
        String insertSql = "INSERT INTO Resources (ProjectId, ResourceName) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, projectId);
            pstmt.setString(2, resourceName);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Failed to create resource");
        }
    }
}