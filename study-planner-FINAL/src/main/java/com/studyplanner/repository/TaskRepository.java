package com.studyplanner.repository;

import com.studyplanner.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/*
 * TaskRepository - Stores all tasks using a Map<String, Task> (key = taskId).
 * Also persists tasks to a local SQLite database via JDBC so data survives restarts.
 * On startup, tasks are loaded from the database into the map.
 * Each create/update/delete operation updates both the map and the database.
 *
 * Database: SQLite, auto-created as study_planner.db in the working directory.
 * Pass ":memory:" to the constructor for an in-memory database (used in tests).
 */
public class TaskRepository {

    private final Map<String, Task> tasks;
    private final String dbUrl;

    // Creates the repository using the default database file study_planner.db.
    public TaskRepository() {
        this("study_planner.db");
    }

    // Creates the repository with a custom database path (use ":memory:" for tests).
    public TaskRepository(String dbPath) {
        this.tasks = new LinkedHashMap<>();
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        createTable();
        loadFromDatabase();
    }

    // Creates the tasks table if it does not already exist.
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS tasks (" +
                "task_id TEXT PRIMARY KEY, task_type TEXT, title TEXT, " +
                "course_name TEXT, deadline TEXT, estimated_mins INTEGER, " +
                "difficulty INTEGER, importance INTEGER, completion INTEGER, " +
                "submission_type TEXT, has_deliverable INTEGER, " +
                "topic_list TEXT, is_cumulative INTEGER, page_count INTEGER)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("DB init error: " + e.getMessage());
        }
    }

    // Reads all rows from the database and loads them into the map.
    private void loadFromDatabase() {
        String sql = "SELECT * FROM tasks";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Task t = rowToTask(rs);
                if (t != null) tasks.put(t.getTaskId(), t);
            }
        } catch (SQLException e) {
            System.err.println("DB load error: " + e.getMessage());
        }
    }

    /*
     * Converts one ResultSet row into the correct Task subtype.
     * Reads task_type to determine which subclass to create,
     * then reads the common fields and any nullable subtype columns.
     */
    private Task rowToTask(ResultSet rs) throws SQLException {
        String id       = rs.getString("task_id");
        String type     = rs.getString("task_type");
        String title    = rs.getString("title");
        String course   = rs.getString("course_name");
        LocalDate dl    = LocalDate.parse(rs.getString("deadline"));
        int mins        = rs.getInt("estimated_mins");
        int diff        = rs.getInt("difficulty");
        int imp         = rs.getInt("importance");
        boolean done    = rs.getInt("completion") == 1;

        Task task;
        switch (type) {
            case "ASSIGNMENT":
                task = new AssignmentTask(id, title, course, dl, mins, diff, imp,
                        rs.getString("submission_type"),
                        rs.getInt("has_deliverable") == 1);
                break;
            case "EXAM":
                String topicsStr = rs.getString("topic_list");
                List<String> topics = (topicsStr != null && !topicsStr.isEmpty())
                        ? new ArrayList<>(Arrays.asList(topicsStr.split(","))) : new ArrayList<>();
                task = new ExamTask(id, title, course, dl, mins, diff, imp,
                        topics, rs.getInt("is_cumulative") == 1);
                break;
            case "READING":
                task = new ReadingTask(id, title, course, dl, mins, diff, imp,
                        rs.getInt("page_count"));
                break;
            default:
                return null;
        }
        task.setCompletionStatus(done);
        return task;
    }

    /*
     * Saves a new task to the map and inserts it into the database.
     * Returns false if a task with the same ID already exists.
     */
    public boolean save(Task task) {
        if (task == null || tasks.containsKey(task.getTaskId())) return false;

        String sql = "INSERT INTO tasks VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindTask(ps, task);
            ps.executeUpdate();
            tasks.put(task.getTaskId(), task);
            return true;
        } catch (SQLException e) {
            System.err.println("DB save error: " + e.getMessage());
            return false;
        }
    }

    /*
     * Updates an existing task in the map and database.
     * Returns false if the task does not exist.
     */
    public boolean update(Task task) {
        if (task == null || !tasks.containsKey(task.getTaskId())) return false;

        String sql = "UPDATE tasks SET task_type=?,title=?,course_name=?,deadline=?," +
                "estimated_mins=?,difficulty=?,importance=?,completion=?," +
                "submission_type=?,has_deliverable=?,topic_list=?,is_cumulative=?," +
                "page_count=? WHERE task_id=?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // bind all fields except task_id (which goes last as WHERE clause value)
            ps.setString(1,  task.getTaskType());
            ps.setString(2,  task.getTitle());
            ps.setString(3,  task.getCourseName());
            ps.setString(4,  task.getDeadline().toString());
            ps.setInt(5,     task.getEstimatedMinutes());
            ps.setInt(6,     task.getDifficulty());
            ps.setInt(7,     task.getImportance());
            ps.setInt(8,     task.isCompletionStatus() ? 1 : 0);
            bindSubtypeColumns(ps, task, 9);
            ps.setString(14, task.getTaskId());
            ps.executeUpdate();
            tasks.put(task.getTaskId(), task);
            return true;
        } catch (SQLException e) {
            System.err.println("DB update error: " + e.getMessage());
            return false;
        }
    }

    /*
     * Removes the task with the given ID from the map and database.
     * Returns false if the task does not exist.
     */
    public boolean delete(String taskId) {
        if (taskId == null || !tasks.containsKey(taskId)) return false;

        String sql = "DELETE FROM tasks WHERE task_id=?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskId);
            ps.executeUpdate();
            tasks.remove(taskId);
            return true;
        } catch (SQLException e) {
            System.err.println("DB delete error: " + e.getMessage());
            return false;
        }
    }

    // Returns the task with the given ID, or null if not found.
    public Task findById(String taskId) {
        return taskId != null ? tasks.get(taskId) : null;
    }

    // Returns all tasks as a new list.
    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }

    // Returns true if a task with the given ID exists.
    public boolean exists(String taskId) {
        return taskId != null && tasks.containsKey(taskId);
    }

    // Returns the total number of tasks stored.
    public int count() { return tasks.size(); }

    // Removes all tasks from the map and database.
    public void clear() {
        String sql = "DELETE FROM tasks";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            tasks.clear();
        } catch (SQLException e) {
            System.err.println("DB clear error: " + e.getMessage());
        }
    }

    // Binds all 14 parameters for an INSERT statement.
    private void bindTask(PreparedStatement ps, Task task) throws SQLException {
        ps.setString(1, task.getTaskId());
        ps.setString(2, task.getTaskType());
        ps.setString(3, task.getTitle());
        ps.setString(4, task.getCourseName());
        ps.setString(5, task.getDeadline().toString());
        ps.setInt(6,    task.getEstimatedMinutes());
        ps.setInt(7,    task.getDifficulty());
        ps.setInt(8,    task.getImportance());
        ps.setInt(9,    task.isCompletionStatus() ? 1 : 0);
        bindSubtypeColumns(ps, task, 10);
    }

    // Binds the 5 nullable subtype columns starting at the given position.
    private void bindSubtypeColumns(PreparedStatement ps, Task task, int start) throws SQLException {
        if (task instanceof AssignmentTask a) {
            ps.setString(start,     a.getSubmissionType());
            ps.setInt(start + 1,    a.isHasDeliverable() ? 1 : 0);
            ps.setNull(start + 2,   Types.VARCHAR);
            ps.setNull(start + 3,   Types.INTEGER);
            ps.setNull(start + 4,   Types.INTEGER);
        } else if (task instanceof ExamTask e) {
            ps.setNull(start,       Types.VARCHAR);
            ps.setNull(start + 1,   Types.INTEGER);
            ps.setString(start + 2, String.join(",", e.getTopicList()));
            ps.setInt(start + 3,    e.isCumulative() ? 1 : 0);
            ps.setNull(start + 4,   Types.INTEGER);
        } else if (task instanceof ReadingTask r) {
            ps.setNull(start,       Types.VARCHAR);
            ps.setNull(start + 1,   Types.INTEGER);
            ps.setNull(start + 2,   Types.VARCHAR);
            ps.setNull(start + 3,   Types.INTEGER);
            ps.setInt(start + 4,    r.getPageCount());
        }
    }
}
