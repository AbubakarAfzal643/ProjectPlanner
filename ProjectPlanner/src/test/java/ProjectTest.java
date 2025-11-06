package com.mycompany.projectplanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectTest {

    private Project project;

    // Format: yyyyMMdd+HHmm
    private Task createTask(int id, String title, String start, String end) {
        return new Task(id, title, start, end);
    }
    
    @BeforeEach
    void setUp() {
        project = new Project();
        project.setName("Test Project");
    }

    // --- Task Overlap Tests (Requirement: Overlapping Tasks) ---

    // test number 2
    @Test
    void testOverlap_FullOverlap() {
        // Task A: 10:00 to 14:00
        Task taskA = createTask(1, "A", "20251107+1000", "20251107+1400");
        // Task B: 11:00 to 13:00 (inside A)
        Task taskB = createTask(2, "B", "20251107+1100", "20251107+1300");

        assertTrue(taskA.overLapsWith(taskB), "Task B should overlap with Task A (full containment).");
        assertTrue(taskB.overLapsWith(taskA), "Task A should overlap with Task B (full containment).");
    }

    // test number 3
    @Test
    void testOverlap_PartialOverlap_Start() {
        // Task A: 10:00 to 14:00
        Task taskA = createTask(1, "A", "20251107+1000", "20251107+1400");
        // Task B: 09:00 to 11:00 (overlaps at start)
        Task taskB = createTask(2, "B", "20251107+0900", "20251107+1100");

        assertTrue(taskA.overLapsWith(taskB), "Tasks should overlap at the start.");
        assertTrue(taskB.overLapsWith(taskA), "Tasks should overlap at the start (commutative).");
    }
    
    // test number 4
    @Test
    void testOverlap_PartialOverlap_End() {
        // Task A: 10:00 to 14:00
        Task taskA = createTask(1, "A", "20251107+1000", "20251107+1400");
        // Task B: 13:00 to 15:00 (overlaps at end)
        Task taskB = createTask(2, "B", "20251107+1300", "20251107+1500");

        assertTrue(taskA.overLapsWith(taskB), "Tasks should overlap at the end.");
        assertTrue(taskB.overLapsWith(taskA), "Tasks should overlap at the end (commutative).");
    }

    // test number 5
    @Test
    void testNoOverlap_Sequential() {
        // Task A: 10:00 to 12:00
        Task taskA = createTask(1, "A", "20251107+1000", "20251107+1200");
        // Task B: 12:01 to 14:00 (Starts just after A ends)
        Task taskB = createTask(2, "B", "20251107+1201", "20251107+1400");

        assertFalse(taskA.overLapsWith(taskB), "Task A and B should not overlap (sequential).");
        assertFalse(taskB.overLapsWith(taskA), "Task B and A should not overlap (sequential).");
    }

    
    // test number 6
    @Test
    void testNoOverlap_AdjacentBoundary() {
        // Task A: 10:00 to 12:00
        Task taskA = createTask(1, "A", "20251107+1000", "20251107+1200");
        // Task B: 12:00 to 14:00 (Starts exactly when A ends)
        Task taskB = createTask(2, "B", "20251107+1200", "20251107+1400");

        // The logic s.isBefore(e) means they do not overlap at the exact boundary time.
        // The earliest end (e) is 12:00, the latest start (s) is 12:00. 12:00 is not before 12:00.
        assertFalse(taskA.overLapsWith(taskB), "Tasks should not overlap when exactly adjacent (A ends, B starts).");
        assertFalse(taskB.overLapsWith(taskA), "Tasks should not overlap when exactly adjacent (B starts, A ends).");
    }

    // test number 7
    @Test
    void testOverlap_SameStartEnd() {
        // Task A: 10:00 to 12:00
        Task taskA = createTask(1, "A", "20251107+1000", "20251107+1200");
        // Task B: 10:00 to 12:00
        Task taskB = createTask(2, "B", "20251107+1000", "20251107+1200");

        assertTrue(taskA.overLapsWith(taskB), "Tasks with identical times should overlap.");
    }

    // --- Project Completion Time Tests (Requirement: Project Completion Time) ---

    // test number 8
    @Test
    void testProjectStartAndEnd_SingleTask() {
        // Task A: 10:00 to 12:00
        Task taskA = createTask(1, "A", "20251107+1000", "20251107+1200");
        project.addTask(taskA);

        assertEquals(LocalDateTime.parse("20251107+1000", Task.Formater), project.projectStart(), "Project start should match single task start.");
        assertEquals(LocalDateTime.parse("20251107+1200", Task.Formater), project.projectEnd(), "Project end should match single task end.");
    }

    // test number 9
    @Test
    void testProjectStartAndEnd_MultipleTasks_DifferentTimes() {
        // Task A: 10:00 to 12:00 (Earliest Start)
        Task taskA = createTask(1, "A", "20251107+1000", "20251107+1200");
        // Task B: 11:00 to 15:00 (Latest End)
        Task taskB = createTask(2, "B", "20251107+1100", "20251107+1500");
        // Task C: 13:00 to 14:00 (Intermediate)
        Task taskC = createTask(3, "C", "20251107+1300", "20251107+1400");

        project.addTask(taskA);
        project.addTask(taskB);
        project.addTask(taskC);

        LocalDateTime expectedStart = LocalDateTime.parse("20251107+1000", Task.Formater);
        LocalDateTime expectedEnd = LocalDateTime.parse("20251107+1500", Task.Formater);

        assertEquals(expectedStart, project.projectStart(), "Project start should be the earliest task start time (Task A).");
        assertEquals(expectedEnd, project.projectEnd(), "Project end should be the latest task end time (Task B).");
    }

    // test number 10
    @Test
    void testProjectStartAndEnd_AcrossDays() {
        // Task A: 2025/11/07 10:00 to 12:00 (Earliest Start)
        Task taskA = createTask(1, "A", "20251107+1000", "20251107+1200");
        // Task B: 2025/11/08 10:00 to 15:00 (Latest End)
        Task taskB = createTask(2, "B", "20251108+1000", "20251108+1500");

        project.addTask(taskA);
        project.addTask(taskB);

        LocalDateTime expectedStart = LocalDateTime.parse("20251107+1000", Task.Formater);
        LocalDateTime expectedEnd = LocalDateTime.parse("20251108+1500", Task.Formater);

        assertEquals(expectedStart, project.projectStart(), "Project start should be the earliest task start time (Task A).");
        assertEquals(expectedEnd, project.projectEnd(), "Project end should be the latest task end time (Task B).");
    }
    
    // test number 1
    @Test
    void testProjectStartAndEnd_EmptyProject() {
        assertNull(project.projectStart(), "Project start should be null for an empty project.");
        assertNull(project.projectEnd(), "Project end should be null for an empty project.");
    }
}