import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Main application class for the Restaurant Simulation.
 * Reads configuration, initializes entities and shared resources, runs the simulation,
 * and prints the final summary.
 * [cite: 68, 69, 70]
 */
public class RestSimApp {

    // Configuration Variables
    private static int numChefs;
    private static int numWaiters;
    private static int numTables;
    private static final Map<String, Integer> mealPrepTimes = new HashMap<>(); // Meal name -> prep time in minutes
    private static final List<Customer> customers = new ArrayList<>();

    // Shared Resources & Synchronization Primitives [cite: 66, 69]
    private static TableBuffer tableBuffer;
    private static Buffer orderedMealsBuf;
    private static Buffer cookedMealsBuf;

    // Simulation Clock & Statistics
    private static SimulationClock simulationClock;
    private static final AtomicLong totalTableWaitTimeMillis = new AtomicLong(0);
    private static final AtomicLong totalPrepTimeMillis = new AtomicLong(0);
    private static final AtomicInteger totalCustomersServed = new AtomicInteger(0);
    private static final AtomicInteger totalMealsPrepared = new AtomicInteger(0);
    private static final AtomicInteger totalCustomersEntered = new AtomicInteger(0);


    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java RestSimApp <input_config_file>");
            System.exit(1);
        }
        String configFile = args[0];

        simulationClock = new SimulationClock(); // Start the clock!
        long simulationStartTimeMillis = System.currentTimeMillis(); // Real start time


        try {
            readConfig(configFile); // [cite: 27]
        } catch (IOException e) {
            System.err.println("Error reading configuration file '" + configFile + "': " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Error in configuration file format: " + e.getMessage());
            System.exit(1);
        }

        // Initialize Shared Buffers [cite: 69]
        // Capacity for buffers - let's assume order buffer capacity = num tables, cooked buffer = num tables? (Can be adjusted)
        int orderBufferCapacity = numTables > 0 ? numTables : 5; // Sensible default if numTables is 0?
        int cookedBufferCapacity = numTables > 0 ? numTables : 5;

        tableBuffer = new TableBuffer(numTables);
        orderedMealsBuf = new Buffer(orderBufferCapacity);
        cookedMealsBuf = new Buffer(cookedBufferCapacity);


        // --- Simulation Start ---
        System.out.println("Simulation Started with " + numChefs + " Chefs, " + numWaiters + " Waiters, and " + numTables + " Tables."); // [cite: 32, 33]

        // Create Executor Services for different roles
        ExecutorService chefExecutor = Executors.newFixedThreadPool(numChefs);
        ExecutorService waiterExecutor = Executors.newFixedThreadPool(numWaiters);
        // CustKiosk handles arrival timing internally, so a cached pool is fine
        ExecutorService customerExecutor = Executors.newCachedThreadPool();


        // Start Chef Threads [cite: 69]
        List<Chef> chefList = new ArrayList<>();
        for (int i = 0; i < numChefs; i++) {
            Chef chef = new Chef(i + 1, orderedMealsBuf, cookedMealsBuf, mealPrepTimes, simulationClock, totalPrepTimeMillis, totalMealsPrepared);
            chefList.add(chef);
            chefExecutor.submit(chef);
        }

        // Start Waiter Threads [cite: 69]
        List<Waiter> waiterList = new ArrayList<>();
        for (int i = 0; i < numWaiters; i++) {
            Waiter waiter = new Waiter(i + 1, cookedMealsBuf, tableBuffer, simulationClock, totalCustomersServed);
            waiterList.add(waiter);
            waiterExecutor.submit(waiter);
        }

        // Start Customer Arrival Threads (via CustKiosk) [cite: 69, 78]
        totalCustomersEntered.set(customers.size()); // Track how many customers should arrive
        for (Customer customer : customers) {
            CustKiosk kiosk = new CustKiosk(customer, tableBuffer, orderedMealsBuf, simulationClock, totalTableWaitTimeMillis, simulationStartTimeMillis);
            customerExecutor.submit(kiosk);
        }

        // --- Simulation Monitoring and Termination ---
        // We need a way to decide when the simulation is over.
        // A simple approach: wait until all customers who arrived have been served.
        // This assumes no new customers arrive after the initial list.

        try {
            // Monitor until all expected customers have been served
            while (totalCustomersServed.get() < totalCustomersEntered.get()) {
                // Print status periodically (optional)
                // System.out.println("["+simulationClock.getFormattedTime()+"] Status: Served " + totalCustomersServed.get() + "/" + totalCustomersEntered.get());
                Thread.sleep(2000); // Check every 2 seconds
                // Add a timeout condition? What if a customer never gets served due to a bug?
                // For now, we assume the simulation will eventually complete.
            }

            System.out.println("[" + simulationClock.getFormattedTime() + "] All " + totalCustomersEntered.get() + " customers have been served. Shutting down...");

        } catch (InterruptedException e) {
            System.err.println("Simulation monitoring interrupted.");
            Thread.currentThread().interrupt();
        } finally {
            // --- Shutdown ---
            // Signal threads to stop by interrupting them
            System.out.println("[" + simulationClock.getFormattedTime() + "] Interrupting threads...");
            shutdownAndAwaitTermination(customerExecutor, "Customer"); // Kiosks finish quickly anyway
            shutdownAndAwaitTermination(chefExecutor, "Chef");
            shutdownAndAwaitTermination(waiterExecutor, "Waiter");

            long simulationEndTimeMillis = System.currentTimeMillis();
            long totalSimulationDurationMillis = simulationEndTimeMillis - simulationStartTimeMillis;


            // --- Final Summary --- [cite: 49, 53]
            System.out.println("\n[" + simulationClock.getFormattedTime() + "] [End of Simulation]"); // [cite: 49]
            System.out.println("\nSummary:");
            System.out.println("-----------------------------------------");
            int servedCount = totalCustomersServed.get();
            System.out.println("Total Customers Served: " + servedCount); // [cite: 50]

            double avgWaitSeconds = (servedCount > 0)
                    ? (totalTableWaitTimeMillis.get() / (double) servedCount) / 1000.0
                    : 0.0;
            // Convert seconds to minutes for output
            double avgWaitMinutes = avgWaitSeconds / 60.0;
            System.out.printf("Average Wait Time for Table: %.2f Minutes%n", avgWaitMinutes); // [cite: 50]


            int mealsPreparedCount = totalMealsPrepared.get();
            double avgPrepSeconds = (mealsPreparedCount > 0)
                    ? (totalPrepTimeMillis.get() / (double) mealsPreparedCount) / 1000.0
                    : 0.0;
            // Convert seconds to minutes for output
            double avgPrepMinutes = avgPrepSeconds / 60.0;
            System.out.printf("Average Order Preparation Time: %.2f Minutes%n", avgPrepMinutes); // [cite: 51]

            double totalSimMinutes = totalSimulationDurationMillis / (1000.0 * 60.0);
            System.out.printf("Total Simulation Time: %.2f Minutes%n", totalSimMinutes); // [cite: 52]
            System.out.println("-----------------------------------------");
        }
    }

    /**
     * Parses the configuration file.
     * [cite: 27, 28, 29, 30, 31]
     * @param filename Path to the configuration file.
     * @throws IOException If there's an error reading the file.
     * @throws IllegalArgumentException If the format is invalid.
     */
    private static void readConfig(String filename) throws IOException, IllegalArgumentException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;

            // 1. Read NC, NW, NT line
            line = reader.readLine();
            if (line == null) throw new IllegalArgumentException("Missing NC, NW, NT line.");
            parseCounts(line);

            // 2. Read meal prep times line
            line = reader.readLine();
            if (line == null) throw new IllegalArgumentException("Missing meal prep times line.");
            parsePrepTimes(line);

            // 3. Read customer lines
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) { // Ignore empty lines and comments
                    parseCustomer(line);
                }
            }
            if (customers.isEmpty()) {
                System.out.println("Warning: No customer information found in the config file.");
            }

        }
    }

    // Helper to parse NC=X NW=Y NT=Z
    private static void parseCounts(String line) throws IllegalArgumentException {
        try {
            Map<String, Integer> counts = parseKeyValuePairs(line);
            numChefs = counts.getOrDefault("NC", 0);
            numWaiters = counts.getOrDefault("NW", 0);
            numTables = counts.getOrDefault("NT", 0);
            if (numChefs <= 0 || numWaiters <= 0 || numTables <= 0) {
                throw new IllegalArgumentException("NC, NW, and NT must be positive integers.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid format for NC, NW, NT line: '" + line + "'. Expected 'NC=X NW=Y NT=Z'. " + e.getMessage(), e);
        }
    }


    // Helper to parse Meal=HH:MM ...
    private static void parsePrepTimes(String line) throws IllegalArgumentException {
        try {
            Map<String, String> times = parseKeyValuePairsString(line);
            for (Map.Entry<String, String> entry : times.entrySet()) {
                String mealName = entry.getKey();
                String timeStr = entry.getValue(); // Format HH:MM or just MM
                int minutes;
                if (timeStr.contains(":")) {
                    String[] parts = timeStr.split(":");
                    if (parts.length != 2) throw new IllegalArgumentException("Invalid time format for " + mealName + ": " + timeStr);
                    minutes = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                } else {
                    minutes = Integer.parseInt(timeStr); // Assume just minutes if no colon
                }
                if (minutes < 0) throw new IllegalArgumentException("Preparation time cannot be negative for " + mealName);
                mealPrepTimes.put(mealName, minutes);
            }
            if (mealPrepTimes.isEmpty()) {
                throw new IllegalArgumentException("No meal preparation times found.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid format for meal prep times line: '" + line + "'. " + e.getMessage(), e);
        }
    }


    // Helper to parse CustomerID=X ArrivalTime=HH:MM Order=Meal
    private static void parseCustomer(String line) throws IllegalArgumentException {
        try {
            Map<String, String> data = parseKeyValuePairsString(line);
            int id = Integer.parseInt(data.get("CustomerID"));
            String arrivalStr = data.get("ArrivalTime"); // HH:MM
            String order = data.get("Order");

            if (arrivalStr == null || order == null) {
                throw new IllegalArgumentException("Missing ArrivalTime or Order for CustomerID " + id);
            }

            // Convert HH:MM arrival time to minutes since simulation start (assuming 08:00 is time 0)
            String[] timeParts = arrivalStr.split(":");
            if (timeParts.length != 2) throw new IllegalArgumentException("Invalid ArrivalTime format: " + arrivalStr);
            int arrivalHour = Integer.parseInt(timeParts[0]);
            int arrivalMinute = Integer.parseInt(timeParts[1]);
            // Calculate minutes relative to 8:00 AM
            int arrivalTimeMinutes = (arrivalHour - 8) * 60 + arrivalMinute;
            if (arrivalTimeMinutes < 0) {
                System.out.println("Warning: Customer " + id + " arrival time " + arrivalStr + " is before 08:00. Treating as arrival at 08:00 (minute 0).");
                arrivalTimeMinutes = 0;
            }


            // Check if the ordered meal exists in the prep times
            if (!mealPrepTimes.containsKey(order)) {
                throw new IllegalArgumentException("Customer " + id + " ordered unknown meal '" + order + "'.");
            }

            customers.add(new Customer(id, arrivalTimeMinutes, order));

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid format for customer line: '" + line + "'. " + e.getMessage(), e);
        }
    }


    // Utility to parse "Key1=Val1 Key2=Val2" into a Map<String, Integer>
    private static Map<String, Integer> parseKeyValuePairs(String line) throws NumberFormatException {
        Map<String, Integer> map = new HashMap<>();
        String[] pairs = line.trim().split("\\s+"); // Split by whitespace
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(kv[0], Integer.parseInt(kv[1]));
            } else {
                throw new IllegalArgumentException("Invalid pair: " + pair);
            }
        }
        return map;
    }

    // Utility to parse "Key1=Val1 Key2=Val2" into a Map<String, String>
    private static Map<String, String> parseKeyValuePairsString(String line) {
        Map<String, String> map = new HashMap<>();
        // Handle potential spaces within values if needed, but basic split works for project spec
        String[] pairs = line.trim().split("\\s+(?=\\w+=)"); // Split on space only if followed by Key=
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2); // Split only on the first '='
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            } else {
                throw new IllegalArgumentException("Invalid pair format: " + pair);
            }
        }
        return map;
    }


    // Graceful shutdown logic for ExecutorService
    private static void shutdownAndAwaitTermination(ExecutorService pool, String poolName) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(10, TimeUnit.SECONDS))
                    System.err.println(poolName + " pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

}