### Project Planning and Analysis Application

A standalone desktop application built with **Java Swing** for managing, analyzing, and visualizing project tasks, dependencies, and resource allocations. It provides a robust interface for loading project data from text files, performing critical analysis, and generating a simple Gantt Chart visualization.

---

## 💡 Key Features

- **File-Based Input**: Load project data from structured `Tasks.txt` and `Resources.txt` files.
- **DB view** : Load tasks and resources to database after fetching from text files.
- **Tabular View**: Displays all tasks in a clear, interactive JTable, including start/end times, dependencies, and resource assignments.
- **Advanced Analysis**:
  - Project completion time (total duration)
  - Detection of overlapping tasks with dependencies (scheduling conflicts)
  - Resource allocation details for specific Task IDs
  - Total effort breakdown per resource in person-hours/days
- **Visualization**: Generates a basic Gantt Chart to visually represent the project timeline and task durations.
- **Layered Architecture**: Separates data models (`Task`, `Resource`, `Project`) from the GUI (`ProjectPlannerGUI`) and file handling (`FileUtility`) and also database connection for SQL server.

---

## ⚙️ Setup and Prerequisites

To run this application, you need:

- **Java Development Kit (JDK) 11 or higher**
- A Java IDE (e.g., VS Code, IntelliJ IDEA, Eclipse)

### 1. Repository Setup
git clone https://github.com/AbubakarAfzal643/ProjectPlanner.git  <br>
cd ProjectPlanner <br>


### 2. Project Structure

.<br>
├── src/ <br>
│   ├── projectplanner/<br>
│   │   ├── Main.java             ## Application entry point<br>
│   │   ├── ProjectPlannerGUI.java ## Main GUI (View/Controller)<br>
│   │   ├── Project.java          ## Project Model (Business Logic)<br>
│   │   ├── Task.java             ## Task Entity<br>
│   │   ├── Resource.java         ## Resource Entity<br>
│   │   ├── FileUtility.java      ## File Parsing Logic<br>
│   │   └── ... (Other classes)<br>
├── Tasks.txt                     ## Example Task Data (Input)<br>
└── Resources.txt                 ## Example Resource Data (Input)<br>

### 3. Running the Application

Compile and run the Main.java class: <br>

javac -d bin src/projectplanner/*.java <br>
java -cp bin projectplanner.Mai <br>


### 📁 Data File Formats


# 1. Tasks.txt Format

Each line represents a task: <br>
TaskID, Title, StartDateTime, EndDateTime, DependencyIDs<br>


# 2. Resources.txt Format

Each line represents a resource (person): <br>
ResourceName, TaskID:Allocation%, TaskID:Allocation%, ... <br>


### 🖥️ User Workflow

- Launch: Run Main.java to open the application.
- Load Data: Click Upload Tasks or Upload Resources and select the input files.
- Review: Tasks and resources populate in the central table.
- Analyze: Click Analyze, choose a report (e.g., Effort Breakdown), and view results.
- Visualize: Click Visualize to see the Gantt Chart representation.






