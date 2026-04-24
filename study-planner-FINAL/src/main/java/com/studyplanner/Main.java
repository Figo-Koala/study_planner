package com.studyplanner;

import com.studyplanner.ai.AIRecommendationService;
import com.studyplanner.ai.MockAIRecommendationService;
import com.studyplanner.ai.OnlineAIRecommendationService;
import com.studyplanner.exception.InvalidTaskException;
import com.studyplanner.exception.SchedulingConflictException;
import com.studyplanner.model.*;
import com.studyplanner.repository.TaskRepository;
import com.studyplanner.service.PlannerService;
import com.studyplanner.service.TaskQueryService;
import com.studyplanner.service.TaskService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/*
 * Main - Application entry point and console UI for the Intelligent Study Planner.
 * Initialises the TaskRepository (auto-creates study_planner.db on first run),
 * wires up service and AI layers, then runs the interactive console menu loop.
 * All user input/output is handled here via private static helper methods.
 *
 * Run with:
 *   mvn package
 *   java -jar target/study-planner.jar
 */
public class Main {

    private static TaskService      taskService;
    private static PlannerService   plannerService;
    private static TaskQueryService queryService;
    private static Scanner          scanner;
    private static AIRecommendationService aiService;

    private static final String DIV = "-".repeat(60);
    private static final String HDR = "=".repeat(60);

    // Bootstraps all layers and starts the menu loop.
    public static void main(String[] args) {
        try {
            TaskRepository repository = new TaskRepository();
            taskService    = new TaskService(repository);
            queryService   = new TaskQueryService(repository);
            plannerService = new PlannerService(repository,
                    new MockAIRecommendationService(), PlannerService.DEFAULT_DAILY_LIMIT);
            aiService      = new MockAIRecommendationService();
            scanner        = new Scanner(System.in);

            printWelcome();
            boolean running = true;
            while (running) {
                printMainMenu();
                int choice = readMenuChoice(9);
                running = dispatch(choice);
            }
            printGoodbye();
            scanner.close();

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            System.err.println("Ensure Java 17+ is installed and try again.");
            System.exit(1);
        }
    }

    // Routes the main menu choice to the correct handler. Returns false to exit.
    private static boolean dispatch(int choice) {
        return switch (choice) {
            case 1  -> { handleAddTask();           yield true; }
            case 2  -> { handleViewTasks();         yield true; }
            case 3  -> { handleUpdateTask();        yield true; }
            case 4  -> { handleDeleteTask();        yield true; }
            case 5  -> { handleToggleCompletion();  yield true; }
            case 6  -> { handleGenerateStudyPlan(); yield true; }
            case 7  -> { handleAISuggestions();     yield true; }
            case 8  -> { handleSwitchAIMode();      yield true; }
            case 9  -> { handleViewSummary();       yield true; }
            case 0  -> false;
            default -> { System.out.println("  Invalid option."); yield true; }
        };
    }

    // ── Menus ─────────────────────────────────────────────────────────────────

    private static void printWelcome() {
        System.out.println("\n" + HDR);
        System.out.println("  INTELLIGENT STUDY PLANNER");
        System.out.println("  AI-Assisted Task Prioritization");
        System.out.println(HDR);
        System.out.println("  AI Mode : " + aiService.getModeName());
        System.out.println("  Tasks   : " + taskService.getTaskCount());
        System.out.println(HDR);
    }

    private static void printMainMenu() {
        System.out.println("\n" + DIV);
        System.out.println("  MAIN MENU");
        System.out.println(DIV);
        System.out.println("  1. Add a new task");
        System.out.println("  2. View / filter / sort tasks");
        System.out.println("  3. Update an existing task");
        System.out.println("  4. Delete a task");
        System.out.println("  5. Mark task complete / incomplete");
        System.out.println("  6. Generate study plan");
        System.out.println("  7. AI suggestions (breakdown / priority)");
        System.out.println("  8. Switch AI mode  [" + aiService.getModeName() + "]");
        System.out.println("  9. View summary statistics");
        System.out.println("  0. Exit");
        System.out.println(DIV);
        System.out.print("  Enter choice: ");
    }

    // ── 1. Add task ───────────────────────────────────────────────────────────

    // Prompts for task type and delegates to the correct builder.
    private static void handleAddTask() {
        System.out.println("\n" + DIV + "\n  ADD NEW TASK\n" + DIV);
        System.out.println("  1. Assignment   2. Exam   3. Reading");
        int typeChoice = readInt("  Select type (1-3): ", 1, 3);
        try {
            Task task = switch (typeChoice) {
                case 1 -> buildAssignment();
                case 2 -> buildExam();
                case 3 -> buildReading();
                default -> throw new InvalidTaskException("Invalid type.", "type");
            };
            taskService.createTask(task);
            System.out.println("\n  Task added. ID: " + task.getTaskId()
                    + "  Priority: " + String.format("%.1f", task.calculatePriorityScore()));
        } catch (InvalidTaskException e) {
            System.out.println("\n  Error: " + e.getMessage());
        }
        pressEnterToContinue();
    }

    // Reads the 7 fields common to all task types.
    private static String[] readCommonFields() {
        String id       = readNonBlankString("  Task ID (e.g. CS101-HW1): ");
        String title    = readNonBlankString("  Title: ");
        String course   = readNonBlankString("  Course name: ");
        String deadline = readDate("  Deadline (yyyy-MM-dd): ").toString();
        String mins     = String.valueOf(readNonNegativeInt("  Estimated minutes: "));
        String diff     = String.valueOf(readInt("  Difficulty (1-5): ", 1, 5));
        String imp      = String.valueOf(readInt("  Importance (1-5): ", 1, 5));
        return new String[]{id, title, course, deadline, mins, diff, imp};
    }

    // Builds an AssignmentTask from user input.
    private static AssignmentTask buildAssignment() throws InvalidTaskException {
        String[] c   = readCommonFields();
        String sub   = readOptionalString("  Submission type (Enter to skip): ");
        boolean del  = readBoolean("  Has a deliverable? (y/n): ");
        if (sub.isEmpty()) sub = "Standard";
        return new AssignmentTask(c[0], c[1], c[2], LocalDate.parse(c[3]),
                Integer.parseInt(c[4]), Integer.parseInt(c[5]), Integer.parseInt(c[6]), sub, del);
    }

    // Builds an ExamTask from user input.
    private static ExamTask buildExam() throws InvalidTaskException {
        String[] c      = readCommonFields();
        String[] topics = readCommaSeparatedList("  Topics (comma-separated, Enter to skip): ");
        boolean cumul   = readBoolean("  Cumulative exam? (y/n): ");
        ExamTask exam   = new ExamTask(c[0], c[1], c[2], LocalDate.parse(c[3]),
                Integer.parseInt(c[4]), Integer.parseInt(c[5]), Integer.parseInt(c[6]),
                new ArrayList<>(), cumul);
        for (String t : topics) if (!t.isBlank()) exam.addTopic(t.trim());
        return exam;
    }

    // Builds a ReadingTask from user input.
    private static ReadingTask buildReading() throws InvalidTaskException {
        String[] c = readCommonFields();
        int pages  = readNonNegativeInt("  Number of pages (0 if unknown): ");
        return new ReadingTask(c[0], c[1], c[2], LocalDate.parse(c[3]),
                Integer.parseInt(c[4]), Integer.parseInt(c[5]), Integer.parseInt(c[6]), pages);
    }

    // ── 2. View tasks ─────────────────────────────────────────────────────────

    // Shows a sub-menu with sorting, filtering, and grouping options.
    private static void handleViewTasks() {
        System.out.println("\n" + DIV + "\n  VIEW TASKS\n" + DIV);
        System.out.println("  1. All tasks          2. Sort by deadline");
        System.out.println("  3. Sort by priority   4. Filter by course");
        System.out.println("  5. Completed only     6. Pending only");
        System.out.println("  7. Group by course    8. Group by type");
        System.out.println("  9. Task details       0. Back");
        int choice = readMenuChoice(9);
        switch (choice) {
            case 1 -> queryService.printTaskList(queryService.getAllTasks(), "All Tasks");
            case 2 -> queryService.printTaskList(queryService.getAllTasksSortedByDeadline(), "By Deadline");
            case 3 -> queryService.printTaskList(queryService.getAllTasksSortedByPriority(), "By Priority");
            case 4 -> {
                String course = readNonBlankString("  Course name: ");
                queryService.printTaskList(queryService.filterByCourseName(course), "Course: " + course);
            }
            case 5 -> queryService.printTaskList(queryService.filterByCompletionStatus(true), "Completed");
            case 6 -> queryService.printTaskList(queryService.filterByCompletionStatus(false), "Pending");
            case 7 -> queryService.printGroupedTasks(queryService.groupByCourse(), "By Course");
            case 8 -> queryService.printGroupedTasks(queryService.groupByTaskType(), "By Type");
            case 9 -> viewSingleTask();
            case 0 -> { return; }
        }
        pressEnterToContinue();
    }

    // Reads a task ID and prints its full details.
    private static void viewSingleTask() {
        String id = readNonBlankString("  Task ID: ");
        try {
            Task t = taskService.getTask(id);
            System.out.println("\n" + DIV);
            System.out.println("  ID         : " + t.getTaskId());
            System.out.println("  Type       : " + t.getTaskType());
            System.out.println("  Title      : " + t.getTitle());
            System.out.println("  Course     : " + t.getCourseName());
            System.out.println("  Deadline   : " + t.getDeadline());
            System.out.println("  Est. time  : " + t.getEstimatedMinutes() + " min");
            System.out.println("  Difficulty : " + t.getDifficulty() + "/5");
            System.out.println("  Importance : " + t.getImportance() + "/5");
            System.out.println("  Status     : " + (t.isCompletionStatus() ? "Completed" : "Pending"));
            System.out.printf( "  Priority   : %.2f / 100%n", t.calculatePriorityScore());
            System.out.println("  Details    : " + t.getTypeSpecificDetails());
        } catch (InvalidTaskException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    // ── 3. Update task ────────────────────────────────────────────────────────

    // Reads an existing task, prompts for new values (Enter = keep current), then saves.
    private static void handleUpdateTask() {
        System.out.println("\n" + DIV + "\n  UPDATE TASK\n" + DIV);
        String id = readNonBlankString("  Task ID to update: ");
        try {
            Task t = taskService.getTask(id);
            System.out.println("  Current: " + t.getTitle() + " | " + t.getDeadline());
            System.out.println("  (Press Enter to keep current value)\n");

            String title  = readWithDefault("  New title",    t.getTitle());
            String course = readWithDefault("  New course",   t.getCourseName());
            String dlRaw  = readWithDefault("  New deadline", t.getDeadline().toString());
            String mRaw   = readWithDefault("  New minutes",  String.valueOf(t.getEstimatedMinutes()));
            String dRaw   = readWithDefault("  New difficulty (1-5)", String.valueOf(t.getDifficulty()));
            String iRaw   = readWithDefault("  New importance (1-5)", String.valueOf(t.getImportance()));

            t.setTitle(title);
            t.setCourseName(course);
            t.setDeadline(LocalDate.parse(dlRaw));
            t.setEstimatedMinutes(Integer.parseInt(mRaw));
            t.setDifficulty(Integer.parseInt(dRaw));
            t.setImportance(Integer.parseInt(iRaw));
            taskService.updateTask(t);
            System.out.println("\n  Task updated.");
        } catch (InvalidTaskException e) {
            System.out.println("\n  Error: " + e.getMessage());
        } catch (NumberFormatException | DateTimeParseException e) {
            System.out.println("\n  Invalid input — update cancelled.");
        }
        pressEnterToContinue();
    }

    // ── 4. Delete task ────────────────────────────────────────────────────────

    // Confirms with the user before deleting a task by ID.
    private static void handleDeleteTask() {
        System.out.println("\n" + DIV + "\n  DELETE TASK\n" + DIV);
        String id = readNonBlankString("  Task ID to delete: ");
        try {
            Task t = taskService.getTask(id);
            System.out.println("  About to delete: " + t.getTitle());
            if (readBoolean("  Confirm? (y/n): ")) {
                taskService.deleteTask(id);
                System.out.println("  Task deleted.");
            } else {
                System.out.println("  Cancelled.");
            }
        } catch (InvalidTaskException e) {
            System.out.println("  Error: " + e.getMessage());
        }
        pressEnterToContinue();
    }

    // ── 5. Toggle completion ──────────────────────────────────────────────────

    // Flips the completion status of the given task.
    private static void handleToggleCompletion() {
        System.out.println("\n" + DIV + "\n  MARK COMPLETE / INCOMPLETE\n" + DIV);
        String id = readNonBlankString("  Task ID: ");
        try {
            Task t = taskService.getTask(id);
            System.out.println("  Current: " + (t.isCompletionStatus() ? "Completed" : "Pending"));
            taskService.toggleCompletion(id);
            Task updated = taskService.getTask(id);
            System.out.println("  New status: " + (updated.isCompletionStatus() ? "Completed" : "Pending"));
        } catch (InvalidTaskException e) {
            System.out.println("  Error: " + e.getMessage());
        }
        pressEnterToContinue();
    }

    // ── 6. Generate study plan ────────────────────────────────────────────────

    // Prompts for plan length and whether to include AI notes, then generates and prints the plan.
    private static void handleGenerateStudyPlan() {
        System.out.println("\n" + DIV + "\n  GENERATE STUDY PLAN\n" + DIV);
        System.out.println("  1. 7-day plan   2. 14-day plan   3. Custom   4. With AI notes");
        int choice = readMenuChoice(4);
        int days   = switch (choice) {
            case 2  -> 14;
            case 3  -> readInt("  Number of days: ", 1, 90);
            default -> 7;
        };
        boolean withAI = (choice == 4);

        if (taskService.getTaskCount() == 0) {
            System.out.println("\n  No tasks added yet.");
            pressEnterToContinue();
            return;
        }
        try {
            plannerService.setAiService(withAI ? aiService : null);
            StudyPlan plan = plannerService.generateStudyPlan(days, withAI);
            System.out.println(plan);
            System.out.println(plannerService.generateWorkloadSummary(plan));
        } catch (SchedulingConflictException e) {
            System.out.println("\n  SCHEDULING CONFLICT: " + e.getMessage());
            System.out.println("  Conflict dates: " + e.getConflictDates());
            System.out.println("  Tip: spread tasks over more days or reduce estimated times.");
        }
        pressEnterToContinue();
    }

    // ── 7. AI suggestions ─────────────────────────────────────────────────────

    // Shows sub-menu for priority explanation, breakdown, or top-5 list.
    private static void handleAISuggestions() {
        System.out.println("\n" + DIV + "\n  AI SUGGESTIONS [" + aiService.getModeName() + "]\n" + DIV);
        System.out.println("  1. Priority explanation   2. Study breakdown   3. Top 5 tasks   0. Back");
        int choice = readMenuChoice(3);
        switch (choice) {
            case 1 -> {
                String id = readNonBlankString("  Task ID: ");
                try {
                    System.out.println("\n" + aiService.generatePriorityExplanation(taskService.getTask(id)));
                } catch (InvalidTaskException e) { System.out.println("  Error: " + e.getMessage()); }
            }
            case 2 -> {
                String id = readNonBlankString("  Task ID: ");
                try {
                    System.out.println("\n" + aiService.generateBreakdown(taskService.getTask(id)));
                } catch (InvalidTaskException e) { System.out.println("  Error: " + e.getMessage()); }
            }
            case 3 -> queryService.printTaskList(queryService.getTopPriorityTasks(5), "Top 5 Priority Tasks");
            case 0 -> { return; }
        }
        pressEnterToContinue();
    }

    // ── 8. Switch AI mode ─────────────────────────────────────────────────────

    // Lets the user switch between mock (offline) and online OpenAI mode.
    private static void handleSwitchAIMode() {
        System.out.println("\n" + DIV + "\n  SWITCH AI MODE\n" + DIV);
        System.out.println("  Current: " + aiService.getModeName());
        System.out.println("  1. Offline Mock   2. Online OpenAI   0. Cancel");
        int choice = readMenuChoice(2);
        switch (choice) {
            case 1 -> {
                aiService = new MockAIRecommendationService();
                plannerService.setAiService(aiService);
                System.out.println("  Switched to: " + aiService.getModeName());
            }
            case 2 -> {
                String key = loadApiKey();
                aiService  = new OnlineAIRecommendationService(key);
                plannerService.setAiService(aiService);
                System.out.println("  Switched to: " + aiService.getModeName());
                if (key == null || key.isBlank())
                    System.out.println("  No API key found. Add openai.api.key to config.properties.");
            }
            case 0 -> System.out.println("  Mode unchanged.");
        }
        pressEnterToContinue();
    }

    // Reads openai.api.key from config.properties if the file exists.
    private static String loadApiKey() {
        try {
            java.util.Properties props = new java.util.Properties();
            java.io.File file = new java.io.File("config.properties");
            if (file.exists()) {
                props.load(new java.io.FileInputStream(file));
                return props.getProperty("openai.api.key", "");
            }
        } catch (Exception e) {
            System.out.println("  Could not read config.properties: " + e.getMessage());
        }
        return "";
    }

    // ── 9. Summary ────────────────────────────────────────────────────────────

    // Prints task counts, course list, and top 3 pending tasks by priority.
    private static void handleViewSummary() {
        System.out.println("\n" + DIV + "\n  SUMMARY STATISTICS\n" + DIV);
        System.out.printf("  Total tasks : %d%n", taskService.getTaskCount());
        System.out.printf("  Pending     : %d%n", queryService.countPendingTasks());
        System.out.printf("  Completed   : %d%n", queryService.countCompletedTasks());
        System.out.printf("  AI mode     : %s%n", aiService.getModeName());
        List<String> courses = queryService.getDistinctCourseNames();
        if (!courses.isEmpty())
            System.out.println("  Courses     : " + String.join(", ", courses));
        List<Task> top3 = queryService.getTopPriorityTasks(3);
        if (!top3.isEmpty()) {
            System.out.println("\n  Top 3 Pending by Priority:");
            for (int i = 0; i < top3.size(); i++) {
                Task t = top3.get(i);
                System.out.printf("    %d. %-30s Due: %-12s Pri: %.1f%n",
                        i + 1, t.getTitle(), t.getDeadline(), t.calculatePriorityScore());
            }
        }
        pressEnterToContinue();
    }

    // ── Goodbye ───────────────────────────────────────────────────────────────

    private static void printGoodbye() {
        System.out.println("\n" + HDR);
        System.out.println("  Thank you for using the Intelligent Study Planner!");
        System.out.println("  Good luck with your studies.");
        System.out.println(HDR);
    }

    // ── Input helpers ─────────────────────────────────────────────────────────

    // Reads a non-blank string, re-prompting until the user types something.
    private static String readNonBlankString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) return line;
            System.out.println("  Input must not be blank.");
        }
    }

    // Reads an optional string — returns empty string if the user presses Enter.
    private static String readOptionalString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    // Reads an integer in the inclusive range [min, max], re-prompting on bad input.
    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= min && val <= max) return val;
                System.out.println("  Enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid number.");
            }
        }
    }

    // Reads a menu choice in range [0, max].
    private static int readMenuChoice(int max) {
        return readInt("  Choice: ", 0, max);
    }

    // Reads a non-negative integer, re-prompting on bad input.
    private static int readNonNegativeInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= 0) return val;
                System.out.println("  Enter a number >= 0.");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid number.");
            }
        }
    }

    // Reads a LocalDate in yyyy-MM-dd format, re-prompting on bad format.
    private static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return LocalDate.parse(scanner.nextLine().trim());
            } catch (DateTimeParseException e) {
                System.out.println("  Use format yyyy-MM-dd (e.g. 2026-05-01).");
            }
        }
    }

    // Reads y/n and returns true for 'y', false for 'n', re-prompting otherwise.
    private static boolean readBoolean(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim().toLowerCase();
            if (line.equals("y")) return true;
            if (line.equals("n")) return false;
            System.out.println("  Enter y or n.");
        }
    }

    // Reads a comma-separated line and returns the tokens as a String array.
    private static String[] readCommaSeparatedList(String prompt) {
        System.out.print(prompt);
        String line = scanner.nextLine().trim();
        if (line.isEmpty()) return new String[0];
        return line.split("\\s*,\\s*");
    }

    // Reads a line and returns the default value if the user presses Enter without typing.
    private static String readWithDefault(String prompt, String defaultVal) {
        System.out.print(prompt + " [" + defaultVal + "]: ");
        String line = scanner.nextLine().trim();
        return line.isEmpty() ? defaultVal : line;
    }

    // Waits for the user to press Enter before returning.
    private static void pressEnterToContinue() {
        System.out.print("\n  Press Enter to continue...");
        scanner.nextLine();
    }
}
