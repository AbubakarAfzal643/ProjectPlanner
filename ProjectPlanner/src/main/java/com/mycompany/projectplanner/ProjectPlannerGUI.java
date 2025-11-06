package com.mycompany.projectplanner;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

public class ProjectPlannerGUI extends JFrame {

    private Project currentProject;
    private JTextField projectTitleField;
    private final JTable taskTable;
    private ProjectTableModel tableModel;
    private IDataRepository currentRepository;
    private JComboBox<String> dataSourceCombo;
    private JLabel statusLabel;

    private static final String DEFAULT_PROJECT_NAME = "New Project";
    private static final String SOURCE_FILE = "File System";
    private static final String SOURCE_DATABASE = "Database";

    public ProjectPlannerGUI() {
        setTitle("Project Planning Application - Layered Architecture");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 750);
        setLocationRelativeTo(null);

        currentProject = new Project();
        currentRepository = new FileDataRepository();
        tableModel = new ProjectTableModel(currentProject);
        taskTable = new JTable(tableModel);

        taskTable.setFillsViewportHeight(true);
        taskTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        taskTable.setRowHeight(25);

        setLayout(new BorderLayout(10, 10));

        add(createTopControlPanel(), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(taskTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Project Tasks and Resources"));
        add(scrollPane, BorderLayout.CENTER);

        add(createStatusPanel(), BorderLayout.SOUTH);

        // Initialize: Load from file and save to database
        initializeData();

        setVisible(true);
    }

    private void initializeData() {
        try {
            // Load from file
            FileDataRepository fileRepo = new FileDataRepository();
            if (fileRepo.isAvailable()) {
                currentProject = fileRepo.loadProject();
                currentProject.setName("Initial Project");
                
                // Save to database
                DatabaseRepository dbRepo = new DatabaseRepository();
                if (dbRepo.isAvailable()) {
                    dbRepo.saveProject(currentProject);
                    updateStatus("Data loaded from files and saved to database successfully!");
                } else {
                    updateStatus("Warning: Database not available. Using file system only.");
                }
                
                refreshTable();
            } else {
                updateStatus("No default files found. Starting with empty project.");
            }
        } catch (Exception e) {
            updateStatus("Error during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(new Color(0, 100, 0));
        panel.add(new JLabel("Status: "));
        panel.add(statusLabel);
        return panel;
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        System.out.println("Status: " + message);
    }

    private JPanel createTopControlPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left panel - File operations
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        leftPanel.add(new JButton(new AbstractAction("New Project") {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionNewProject();
            }
        }));
        leftPanel.add(new JButton(new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionSaveProject();
            }
        }));
        leftPanel.add(new JButton(new AbstractAction("Load") {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionLoadProject();
            }
        }));

        // Center panel - Project info
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.add(new JLabel("Project Title:", SwingConstants.RIGHT));
        projectTitleField = new JTextField(DEFAULT_PROJECT_NAME, 25);
        centerPanel.add(projectTitleField);
        
        centerPanel.add(new JLabel("   Data Source:"));
        dataSourceCombo = new JComboBox<>(new String[]{SOURCE_FILE, SOURCE_DATABASE});
        dataSourceCombo.addActionListener(e -> switchDataSource());
        centerPanel.add(dataSourceCombo);

        // Right panel - Operations
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        rightPanel.add(new JButton(new AbstractAction("Upload Files") {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionUploadFiles();
            }
        }));
        rightPanel.add(new JButton(new AbstractAction("Edit/Delete") {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionEditDelete();
            }
        }));
        rightPanel.add(new JButton(new AbstractAction("Analyze") {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionAnalyze();
            }
        }));
        rightPanel.add(new JButton(new AbstractAction("Visualize") {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionVisualize();
            }
        }));

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        return mainPanel;
    }

    private void switchDataSource() {
        String selectedSource = (String) dataSourceCombo.getSelectedItem();
        if (SOURCE_DATABASE.equals(selectedSource)) {
            currentRepository = new DatabaseRepository();
            if (!currentRepository.isAvailable()) {
                JOptionPane.showMessageDialog(this, 
                    "Database connection failed! Please check database configuration.\nFalling back to file system.",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
                dataSourceCombo.setSelectedItem(SOURCE_FILE);
                currentRepository = new FileDataRepository();
            } else {
                updateStatus("Switched to Database storage");
            }
        } else {
            currentRepository = new FileDataRepository();
            updateStatus("Switched to File System storage");
        }
    }

    private void actionNewProject() {
        currentProject = new Project();
        projectTitleField.setText(DEFAULT_PROJECT_NAME);
        currentProject.setName(DEFAULT_PROJECT_NAME);
        refreshTable();
        updateStatus("New project created");
        JOptionPane.showMessageDialog(this, "New project created. Use upload or load to add data.", 
            "New Project", JOptionPane.INFORMATION_MESSAGE);
    }

    private void actionSaveProject() {
        if (currentProject == null || currentProject.getTasks().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No project data to save!", 
                "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String projectName = projectTitleField.getText().trim();
            if (projectName.isEmpty()) {
                projectName = DEFAULT_PROJECT_NAME;
            }
            currentProject.setName(projectName);
            
            currentRepository.saveProject(currentProject);
            updateStatus("Project saved successfully to " + currentRepository.getRepositoryType());
            JOptionPane.showMessageDialog(this, 
                "Project '" + projectName + "' saved successfully to " + currentRepository.getRepositoryType(),
                "Save Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            updateStatus("Error saving project: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error saving project: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void actionLoadProject() {
        try {
            if (currentRepository instanceof DatabaseRepository) {
                // Show project selection dialog
                List<ProjectInfo> projects = currentRepository.getAllProjects();
                if (projects.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No projects found in database!", 
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                
                ProjectInfo selected = (ProjectInfo) JOptionPane.showInputDialog(
                    this,
                    "Select a project to load:",
                    "Load Project",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    projects.toArray(),
                    projects.get(0)
                );
                
                if (selected != null) {
                    currentProject = currentRepository.loadProject(selected.projectId);
                    projectTitleField.setText(currentProject.getName());
                    refreshTable();
                    updateStatus("Project loaded from database: " + currentProject.getName());
                }
            } else {
                // File repository
                currentProject = currentRepository.loadProject();
                projectTitleField.setText("Loaded from Files");
                refreshTable();
                updateStatus("Project loaded from file system");
            }
        } catch (Exception e) {
            updateStatus("Error loading project: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error loading project: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void actionUploadFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("."));
        fileChooser.setDialogTitle("Select Tasks.txt");
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                File tasksFile = selectedFile;
                File resourcesFile = new File(selectedFile.getParent(), "Resources.txt");

                if (!tasksFile.exists() || !resourcesFile.exists()) {
                    JOptionPane.showMessageDialog(this,
                        "Both Tasks.txt and Resources.txt must be in the same folder!",
                        "File Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Project tempProject = new Project();
                Map<Integer, List<Integer>> dependencyMap = FileUtilty.parseTasksFile(tasksFile, tempProject);
                Map<Resource, Map<Integer, Integer>> resourceAllocTemp = FileUtilty.parseResourcesFile(resourcesFile);
                FileUtilty.resolveProjectData(tempProject, dependencyMap, resourceAllocTemp);

                currentProject = tempProject;
                currentProject.setName(selectedFile.getParentFile().getName());
                projectTitleField.setText(currentProject.getName());
                refreshTable();
                
                updateStatus("Files uploaded successfully");
                JOptionPane.showMessageDialog(this, "Files loaded successfully! Click 'Save' to persist to " + 
                    currentRepository.getRepositoryType(), "Upload Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                updateStatus("Error uploading files: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error reading files: " + e.getMessage(), 
                    "File Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void actionEditDelete() {
        if (currentProject.tasksById.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No tasks to edit or delete!", 
                "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new EditDeleteDialog(this, currentProject, currentRepository).setVisible(true);
        refreshTable();
    }

    private void actionAnalyze() {
        if (currentProject.tasksById.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please load data before performing analysis.", 
                "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new AnalysisDialog(this, currentProject).setVisible(true);
    }

    private void actionVisualize() {
        if (currentProject.tasksById.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please load data before visualization.", 
                "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new GanttChartFrame(currentProject).setVisible(true);
    }

    private void refreshTable() {
        tableModel = new ProjectTableModel(currentProject);
        taskTable.setModel(tableModel);
        tableModel.fireTableDataChanged();
    }

    // Inner classes from original code

    private class ProjectTableModel extends AbstractTableModel {
        private final Project project;
        private final List<Task> taskList;
        private final String[] columnNames = {"Id", "Task", "Start", "End", "Dependencies", "Resources"};

        public ProjectTableModel(Project project) {
            this.project = project;
            this.taskList = project.tasksById.values().stream().collect(Collectors.toList());
        }

        @Override
        public int getRowCount() {
            return taskList.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Task task = taskList.get(rowIndex);
            switch (columnIndex) {
                case 0: return task.id;
                case 1: return task.title;
                case 2: return task.startTime.format(Task.Formater);
                case 3: return task.endTime.format(Task.Formater);
                case 4:
                    return task.dependencies.stream()
                            .map(d -> String.valueOf(d.id))
                            .collect(Collectors.joining(", "));
                case 5:
                    return project.teamForTask(task.id).stream()
                            .collect(Collectors.joining(", "));
                default: return "";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private class EditDeleteDialog extends JDialog {
        private Project project;
        private IDataRepository repository;
        private JComboBox<String> entityTypeCombo;
        private JComboBox<Object> entityCombo;
        private JButton deleteButton;
        private JButton updateButton;

        public EditDeleteDialog(JFrame parent, Project project, IDataRepository repository) {
            super(parent, "Edit/Delete Data", true);
            this.project = project;
            this.repository = repository;
            
            setSize(500, 300);
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout(10, 10));

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Entity type selection
            JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            typePanel.add(new JLabel("Select Type:"));
            entityTypeCombo = new JComboBox<>(new String[]{"Task", "Resource"});
            entityTypeCombo.addActionListener(e -> updateEntityList());
            typePanel.add(entityTypeCombo);
            mainPanel.add(typePanel);

            // Entity selection
            JPanel entityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            entityPanel.add(new JLabel("Select Item:"));
            entityCombo = new JComboBox<>();
            entityPanel.add(entityCombo);
            mainPanel.add(entityPanel);

            mainPanel.add(Box.createVerticalStrut(20));

            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            
            deleteButton = new JButton("Delete");
            deleteButton.addActionListener(e -> performDelete());
            buttonPanel.add(deleteButton);

            updateButton = new JButton("Update (Reload Required)");
            updateButton.addActionListener(e -> {
                JOptionPane.showMessageDialog(this, 
                    "For updates, please use file editing or direct database modification.\n" +
                    "Then reload the project.", 
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            });
            buttonPanel.add(updateButton);

            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> dispose());
            buttonPanel.add(closeButton);

            mainPanel.add(buttonPanel);

            add(mainPanel, BorderLayout.CENTER);

            updateEntityList();
        }

        private void updateEntityList() {
            entityCombo.removeAllItems();
            String type = (String) entityTypeCombo.getSelectedItem();
            
            if ("Task".equals(type)) {
                for (Task task : project.getTasks()) {
                    entityCombo.addItem("Task " + task.id + ": " + task.title);
                }
            } else {
                for (Resource resource : project.getResources()) {
                    entityCombo.addItem("Resource: " + resource.getName());
                }
            }
        }

        private void performDelete() {
            if (entityCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Please select an item to delete!");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete this item?", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
            
            if (confirm != JOptionPane.YES_OPTION) return;

            try {
                String selected = (String) entityCombo.getSelectedItem();
                String type = (String) entityTypeCombo.getSelectedItem();

                if ("Task".equals(type)) {
                    int taskId = Integer.parseInt(selected.split(":")[0].replace("Task ", "").trim());
                    repository.deleteTask(taskId, project.getProjectId());
                    project.tasksById.remove(taskId);
                    updateStatus("Task deleted successfully");
                } else {
                    String resourceName = selected.replace("Resource: ", "");
                    repository.deleteResource(resourceName, project.getProjectId());
                    project.resources.removeIf(r -> r.getName().equals(resourceName));
                    updateStatus("Resource deleted successfully");
                }

                JOptionPane.showMessageDialog(this, "Item deleted successfully!");
                updateEntityList();
                refreshTable();
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error deleting item: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private class AnalysisDialog extends JDialog {
        private final Project project;
        private final JTextArea resultArea;
        private final ButtonGroup analysisGroup;
        private final JRadioButton taskTeamRadio;
        private final JTextField taskIdField;

        public AnalysisDialog(JFrame parent, Project project) {
            super(parent, "Project Analysis Options", true);
            this.project = project;
            setSize(600, 500);
            setLayout(new BorderLayout(15, 15));
            setLocationRelativeTo(parent);

            JPanel optionsPanel = new JPanel();
            optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
            optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            analysisGroup = new ButtonGroup();

            JRadioButton completionRadio = new JRadioButton("Project completion time and duration", true);
            JRadioButton overlapRadio = new JRadioButton("Overlapping tasks with dependencies");
            taskTeamRadio = new JRadioButton("Resources and teams for Task ID:");
            JRadioButton effortRadio = new JRadioButton("Effort breakdown: Resources-wise");

            analysisGroup.add(completionRadio);
            analysisGroup.add(overlapRadio);
            analysisGroup.add(taskTeamRadio);
            analysisGroup.add(effortRadio);

            optionsPanel.add(completionRadio);
            optionsPanel.add(overlapRadio);

            taskIdField = new JTextField(5);
            taskIdField.setMaximumSize(new Dimension(80, 25));
            JPanel taskTeamPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            taskTeamPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            taskTeamPanel.add(taskTeamRadio);
            taskTeamPanel.add(taskIdField);
            optionsPanel.add(taskTeamPanel);

            optionsPanel.add(effortRadio);
            optionsPanel.add(Box.createVerticalStrut(20));

            JButton runButton = new JButton("Run Analysis");
            runButton.addActionListener(e -> runAnalysis());
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttonPanel.add(runButton);
            optionsPanel.add(buttonPanel);

            add(optionsPanel, BorderLayout.NORTH);

            resultArea = new JTextArea("Results will appear here...", 15, 40);
            resultArea.setEditable(false);
            resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(resultArea);
            scrollPane.setBorder(BorderFactory.createTitledBorder("Analysis Results"));
            add(scrollPane, BorderLayout.CENTER);

            runAnalysis();
        }

        private void runAnalysis() {
            String result = "Error: No analysis type selected.";

            if (getRadioButtonText("Project completion time and duration").isSelected()) {
                result = getProjectCompletionTime();
            } else if (getRadioButtonText("Overlapping tasks with dependencies").isSelected()) {
                result = getOverlappingTasks();
            } else if (taskTeamRadio.isSelected()) {
                result = getTeamForTask();
            } else if (getRadioButtonText("Effort breakdown: Resource-wise").isSelected()) {
                result = getEffortBreakdown();
            }

            resultArea.setText(result);
            resultArea.setCaretPosition(0);
        }

        private JRadioButton getRadioButtonText(String text) {
            for (AbstractButton button : Collections.list(analysisGroup.getElements())) {
                if (button instanceof JRadioButton && button.getText().equals(text)) {
                    return (JRadioButton) button;
                }
            }
            return null;
        }

        private String getProjectCompletionTime() {
            LocalDateTime start = project.projectStart();
            LocalDateTime end = project.projectEnd();
            Duration duration = project.projectDuration();

            StringBuilder sb = new StringBuilder();
            sb.append("--- Project Completion Time and Duration ---\n");
            sb.append(String.format("Earliest Start: %s\n", start != null ? start.format(Task.Formater) : "N/A"));
            sb.append(String.format("Latest End:     %s\n", end != null ? end.format(Task.Formater) : "N/A"));
            sb.append("Duration:\n");
            long totalHours = duration.get(ChronoUnit.SECONDS) / 3600;
            long totalMinutes = (duration.get(ChronoUnit.SECONDS) % 3600) / 60;

            sb.append(String.format("  - Total Time Span: %d hours and %d minutes\n", totalHours, totalMinutes));
            sb.append(String.format("  - Total Days Span: %.2f days\n", duration.toMinutes() / (60.0 * 24)));
            return sb.toString();
        }

        private String getOverlappingTasks() {
            List<String> overlaps = project.findOverlappingDependencyPairs();
            StringBuilder sb = new StringBuilder();
            sb.append("--- Overlapping Tasks (with dependencies) ---\n");
            if (overlaps.isEmpty()) sb.append("No overlapping dependent tasks detected.\n");
            else for (String overlap : overlaps) sb.append("- ").append(overlap).append("\n");
            return sb.toString();
        }

        private String getTeamForTask() {
            String taskIdStr = taskIdField.getText().trim();
            if (taskIdStr.isEmpty()) return "Please enter a Task ID for 'Resources and teams' analysis.";
            try {
                int taskId = Integer.parseInt(taskIdStr);
                List<String> team = project.teamForTask(taskId);
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("--- Team for Task %d: %s ---\n", taskId, project.getTaskById(taskId) != null ? project.getTaskById(taskId).title : "N/A"));
                if (team.isEmpty()) sb.append("No resources allocated to Task ").append(taskId).append(".\n");
                else for (String resource : team) sb.append("- ").append(resource).append("\n");
                return sb.toString();
            } catch (NumberFormatException e) {
                return "Invalid Task ID entered. Please enter a valid number.";
            }
        }

        private String getEffortBreakdown() {
            Map<String, Double> efforts = project.totalEffortHoursPerResource();
            StringBuilder sb = new StringBuilder();
            sb.append("--- Total Effort Per Resource (hours & person-days / 8h) ---\n");
            if (efforts.isEmpty()) sb.append("No resource allocations found.\n");
            else for (Map.Entry<String, Double> entry : efforts.entrySet()) {
                String name = entry.getKey();
                double hours = entry.getValue();
                double personDays = hours / 8.0;
                sb.append(String.format("%-15s: %.2f hours (%.2f person-days / 8h)\n", name, hours, personDays));
            }
            return sb.toString();
        }
    }

    private class GanttChartFrame extends JFrame {
        public GanttChartFrame(Project project) {
            setTitle("Project Visualization (Gantt Chart)");
            setSize(1000, 600);
            setLocationRelativeTo(null);
            add(new GanttPanel(project));
            setVisible(true);
        }
    }

    private class GanttPanel extends JPanel {
        private final List<Task> taskList;

        public GanttPanel(Project project) {
            this.taskList = project.tasksById.values().stream()
                    .sorted((t1, t2) -> t1.startTime.compareTo(t2.startTime))
                    .collect(Collectors.toList());
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (taskList.isEmpty()) {
                g2d.drawString("No tasks to visualize.", 20, 20);
                return;
            }

            int nameWidth = 150;
            int chartMargin = 20;
            int width = getWidth() - nameWidth - (chartMargin * 2);
            int barHeight = 25;
            int barSpacing = 15;
            int startX = nameWidth + chartMargin;
            int startY = chartMargin + 20;

            LocalDateTime projectStart = taskList.get(0).startTime;
            LocalDateTime projectEnd = taskList.stream()
                    .map(t -> t.endTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(projectStart.plusHours(1));
            long totalMinutes = ChronoUnit.MINUTES.between(projectStart, projectEnd);
            if (totalMinutes == 0) totalMinutes = 1;

            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawLine(startX, startY - 5, startX + width, startY - 5);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Start: " + projectStart.toLocalDate(), startX, chartMargin);
            g2d.drawString("End: " + projectEnd.toLocalDate(), startX + width - 100, chartMargin);

            int y = startY;
            for (int i = 0; i < taskList.size(); i++) {
                Task task = taskList.get(i);

                g2d.setColor(Color.BLACK);
                g2d.drawString(task.id + ". " + task.title, chartMargin, y + barHeight - 8);

                long taskStartTimeMinutes = ChronoUnit.MINUTES.between(projectStart, task.startTime);
                long taskDurationMinutes = ChronoUnit.MINUTES.between(task.startTime, task.endTime);

                int xPos = (int) (startX + (double) taskStartTimeMinutes / totalMinutes * width);
                int barW = (int) ((double) taskDurationMinutes / totalMinutes * width);

                Color barColor = new Color(50, 150, 255);
                g2d.setColor(barColor);
                g2d.fillRect(xPos, y, barW, barHeight);
                g2d.setColor(barColor.darker());
                g2d.drawRect(xPos, y, barW, barHeight);

                g2d.setColor(Color.WHITE);
                String durationStr = String.format("%.1f h", task.getDurationInHours());
                g2d.drawString("ID: " + task.id + "  ", xPos + 5, y + 15);
                g2d.drawString(durationStr, xPos + barW - g2d.getFontMetrics().stringWidth(durationStr) - 5, y + barHeight - 8);

                g2d.setColor(Color.RED.darker());
                for (Task dep : task.dependencies) {
                    int depIndex = taskList.indexOf(dep);
                    if (depIndex != -1) {
                        int depY = startY + depIndex * (barHeight + barSpacing);
                        g2d.drawLine(xPos, y + barHeight / 2, xPos - 5, y + barHeight / 2);
                        g2d.drawLine(xPos - 5, y + barHeight / 2, xPos - 5, depY + barHeight / 2);
                        g2d.drawLine(xPos - 5, depY + barHeight / 2, startX + width, depY + barHeight / 2);
                    }
                }

                y += barHeight + barSpacing;
            }
        }
    }
}