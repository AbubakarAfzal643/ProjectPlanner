Project Planning and Analysis Application

This project is a standalone desktop application built with Java Swing for managing, analyzing, and visualizing project tasks, dependencies, and resource allocations. It provides a robust interface for loading data from text files, performing critical path and effort breakdown analysis, and generating a simple Gantt Chart visualization.

✨ Features

File-Based Input: Easily load project data from structured Tasks.txt and Resources.txt files.

Tabular View: Displays all loaded tasks in a clear, interactive JTable, including start/end times, dependencies, and resource assignments.

Advanced Analysis: The Analyze feature provides four key project management reports:

Project completion time (total duration).

Detection of overlapping tasks with dependencies (scheduling conflicts).

Resource team allocation details for any specific Task ID.

Total effort breakdown per resource in person-hours/days.

Visualization: Generates a basic Gantt Chart to visually represent the project timeline and task durations.

MVC Architecture: The codebase separates the data models (Task, Resource, Project) from the GUI (ProjectPlannerGUI) and file handling (FileUtility).

🛠️ Prerequisites

To run this application, you need:

Java Development Kit (JDK) 11 or higher

A basic Java development environment (e.g., VS Code, IntelliJ IDEA, Eclipse)

🚀 Getting Started

1. Repository Setup

Clone the repository to your local machine:

git clone [https://github.com/your-username/your-repo-name.git](https://github.com/your-username/your-repo-name.git)
cd ProjectPlanner


2. Project Structure

Ensure your project structure and data files match the expected format:

.
├── src/
│   ├── projectplanner/
│   │   ├── Main.java             # Application entry point
│   │   ├── ProjectPlannerGUI.java # Main GUI (View/Controller)
│   │   ├── Project.java          # Project Model (Business Logic)
│   │   ├── Task.java             # Task Entity
│   │   ├── Resource.java         # Resource Entity
│   │   ├── FileUtility.java      # File Parsing Logic
│   │   ├── Tasks.txt             # Example Task Data (Input)
│   │   └── Resources.txt         # Example Resource Data (Input)


3. Running the Application

Compile and run the Main.java class. Most IDEs will handle this automatically.

# Example command line compilation (adjust paths as necessary)
javac -d bin src/projectplanner/*.java
java -cp bin projectplanner.Main


⚙️ Data File Format

The application relies on two plain text files for input. The files must be located in the same directory (or the project root, depending on your running environment).

1. Tasks.txt Format

Each line represents one task:

TaskID, Title, StartDateTime, EndDateTime, DependencyIDs (comma/space separated)


Field

Description

Format Example

TaskID

Unique integer identifier.

1

Title

Short description of the task.

Initial research and analysis

Start/EndDateTime

Timestamp for task execution.

YYYYMMDD+HHMM (e.g., 20251013+0800)

Dependencies

IDs of tasks that must be completed first.

2, 4

2. Resources.txt Format

Each line represents one resource (person):

ResourceName, TaskID:Allocation%, TaskID:Allocation%, ...


Field

Description

Format Example

ResourceName

Name of the team member.

Ahmed

Allocations

A list of task allocations.

1:50, 3:100 (Task 1: 50%, Task 3: 100%)

🖼️ User Interface & Workflow

Launch: Run the Main.java file to open the Project Planning Application window.

Load Data: Click Upload Tasks or Upload Resources and select one of the input files. The application will automatically attempt to find the companion file in the same directory.

Review: The central table will populate with the imported task and resource data.

Analyze: Click the Analyze button to open the analysis dialog. Select a radio button option (e.g., Effort Breakdown) and click Run Analysis to view the report in the text area.

Visualize: Click the Visualize button to view the simple Gantt Chart representation of the project timeline.

👨‍💻 Core Classes and Responsibilities

Class

Responsibility

ProjectPlannerGUI.java

Main application window, handles all user interaction, table display, and launches sub-dialogs/frames.

Project.java

Central data model. Manages collections of Tasks and Resources and contains all analytical methods (projectDuration(), findOverlappingDependencyPairs(), totalEffortHoursPerResource()).

Task.java

Defines a single project task, including ID, timeline, and dependency links. Calculates its own duration.

Resource.java

Defines a project resource and tracks its percentage allocation across various tasks.

FileUtility.java

Provides static methods to safely read and parse data from Tasks.txt and Resources.txt into the Model objects.
