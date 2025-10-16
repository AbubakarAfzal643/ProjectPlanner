package projectplanner;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
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

    private static final String DEFAULT_PROJECT_NAME = "New Project";

    public ProjectPlannerGUI() {
        setTitle("Project Planning Application");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);

        currentProject = new Project();
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

        loadSampleData();

        setVisible(true);
    }

    private void loadSampleData() {
        try {
            File tasksFile = new File("./Tasks.txt");
            File resourcesFile = new File("./Resources.txt");

            if (!tasksFile.exists() || !resourcesFile.exists()) {
                tasksFile = new File("src/projectplanner/Tasks.txt");
                resourcesFile = new File("src/projectplanner/Resources.txt");
            }

            if (tasksFile.exists() && resourcesFile.exists()) {
                Project tempProject = new Project();
                Map<Integer, List<Integer>> dependencyMap = FileUtility.parseTasksFile(tasksFile, tempProject);
                Map<Resource, Map<Integer, Integer>> resourceAllocTemp = FileUtility.parseResourcesFile(resourcesFile);
                FileUtility.resolveProjectData(tempProject, dependencyMap, resourceAllocTemp);

                currentProject = tempProject;
                tableModel = new ProjectTableModel(currentProject);
                taskTable.setModel(tableModel);
                tableModel.fireTableDataChanged();
                projectTitleField.setText("New Project");

            } else {
                System.out.println("Default Tasks.txt or Resources.txt not found. Starting with empty project.");
            }
        } catch (Exception e) {
            System.err.println("Error loading default data: " + e.getMessage());
        }
    }

    private JPanel createTopControlPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
        leftPanel.add(new JButton(new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        }));

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.add(new JLabel("Project Title:", SwingConstants.RIGHT));
        projectTitleField = new JTextField(DEFAULT_PROJECT_NAME, 30);
        centerPanel.add(projectTitleField);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        rightPanel.add(new JButton(new AbstractAction("Upload Tasks") {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionUploadFile("Tasks.txt");
            }
        }));
        rightPanel.add(new JButton(new AbstractAction("Upload Resources") {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionUploadFile("Resources.txt");
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

    private void actionNewProject() {
        currentProject = new Project();
        projectTitleField.setText(DEFAULT_PROJECT_NAME);
        tableModel = new ProjectTableModel(currentProject);
        taskTable.setModel(tableModel);
        tableModel.fireTableDataChanged();
        JOptionPane.showMessageDialog(this, "New project created. Use upload buttons to load data.", "New Project", JOptionPane.INFORMATION_MESSAGE);
    }

    private void actionSaveProject() {
        if (currentProject == null || currentProject.getTasks().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No project data to save!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Folder to Save Project Files");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) return;

        File folder = chooser.getSelectedFile();
        File tasksFile = new File(folder, "Tasks.txt");
        File resourcesFile = new File(folder, "Resources.txt");

        try {
            saveTasksFile(tasksFile);
            saveResourcesFile(resourcesFile);

            JOptionPane.showMessageDialog(this, "Project saved successfully to:\n" + folder.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving project: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveTasksFile(File file) throws IOException {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(file)) {
            pw.println("# ID, Title, Start, End, Dependencies...");
            for (Task t : currentProject.getTasks()) {
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

    private void saveResourcesFile(File file) throws IOException {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(file)) {
            pw.println("# ResourceName, TaskId:Percentage, ...");
            for (Resource r : currentProject.getResources()) {
                StringBuilder sb = new StringBuilder();
                sb.append(r.getName());
                for (Map.Entry<Task, Integer> e : r.getAllocations().entrySet()) {
                    sb.append(", ").append(e.getKey().id).append(":").append(e.getValue());
                }
                pw.println(sb);
            }
        }
    }

    private void actionUploadFile(String fileNameHint) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("."));
        fileChooser.setDialogTitle("Select " + fileNameHint);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                boolean isTasksUpload = fileNameHint.equals("Tasks.txt");

                File tasksFile = isTasksUpload ? selectedFile : new File(selectedFile.getParent(), "Tasks.txt");
                File resourcesFile = isTasksUpload ? new File(selectedFile.getParent(), "Resources.txt") : selectedFile;

                if (!tasksFile.exists() || !resourcesFile.exists()) {
                    JOptionPane.showMessageDialog(this,
                            "Missing companion file. Please ensure both Tasks.txt and Resources.txt are in the same folder, or load them one by one.",
                            "File Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Project tempProject = new Project();
                Map<Integer, List<Integer>> dependencyMap = FileUtility.parseTasksFile(tasksFile, tempProject);
                Map<Resource, Map<Integer, Integer>> resourceAllocTemp = FileUtility.parseResourcesFile(resourcesFile);
                FileUtility.resolveProjectData(tempProject, dependencyMap, resourceAllocTemp);

                currentProject = tempProject;
                tableModel = new ProjectTableModel(currentProject);
                taskTable.setModel(tableModel);
                tableModel.fireTableDataChanged();

                projectTitleField.setText("Project: " + selectedFile.getParentFile().getName());

                JOptionPane.showMessageDialog(this, fileNameHint + " loaded successfully!", "Upload Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error processing project data: " + e.getMessage(), "Processing Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void actionAnalyze() {
        if (currentProject.tasksById.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please load tasks and resources before performing analysis.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new AnalysisDialog(this, currentProject).setVisible(true);
    }

    private void actionVisualize() {
        if (currentProject.tasksById.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please load tasks before visualization.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new GanttChartFrame(currentProject).setVisible(true);
    }

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
            JRadioButton effortRadio = new JRadioButton("Effort breakdown: Resource-wise");

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
                g2d.drawString("ID: " + task.id, xPos + 5, y + 15);
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
