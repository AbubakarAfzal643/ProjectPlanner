package com.mycompany.projectplanner;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Task {

    // this class works on tasks.txt file

    public int id;
    public String title;
    public LocalDateTime startTime;
    public LocalDateTime endTime;

    // these are the list of dependencies
    List<Task> dependencies = new ArrayList<Task>();

    // these are the list for childTasks
    List<Task> children = new ArrayList<Task>();

    // time handling => string format
    public static DateTimeFormatter Formater = DateTimeFormatter.ofPattern("yyyyMMdd+HHmm");

    // parameterized constructor
    public Task(int id, String title, String start, String end) {
        this.id = id;
        this.title = title;
        // Parse time strings using the defined formatter
        this.startTime = LocalDateTime.parse(start.trim(), Formater);
        this.endTime = LocalDateTime.parse(end.trim(), Formater);

    }
    public double getDurationInHours() {
        Duration d = Duration.between(startTime, endTime); // time difference of start time and end time
        return d.toMinutes() / 60.0; // convert to minutes and then to hours
    }
    public boolean overLapsWith(Task other) {
        // find the latest of the two tasks start times
        LocalDateTime s = startTime.isAfter(other.startTime) ? startTime : other.startTime;
        // finds the earliest of the two tasks end times
        LocalDateTime e = endTime.isBefore(other.endTime) ? endTime : other.endTime;
        // true if laterStartTime < earlierEndTime : Checks for Overlap
        return s.isBefore(e);
    }

    @Override
    public String toString() {
        return "Task " + id + ": " + title;
    }
}
