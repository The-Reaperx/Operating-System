import java.util.Vector;
import java.util.Optional;

/**
 * A specialized buffer to manage available restaurant tables.
 * Uses semaphores to control access to available tables.
 * Implements a custom consumeAt method to release a specific table.
 * [cite: 17, 66, 85, 86]
 */
public class TableBuffer {
    private final Vector<Table> tables; // Holds all table objects
    private final Semaphore availableTablesSemaphore; // Counts available tables
    private final Semaphore mutex; // For safe access to the tables vector if needed for specific operations beyond semaphore control

    /**
     * Creates a TableBuffer with a specified number of tables.
     * @param numberOfTables The total number of tables in the restaurant.
     */
    public TableBuffer(int numberOfTables) {
        if (numberOfTables <= 0) {
            throw new IllegalArgumentException("Number of tables must be positive");
        }
        this.tables = new Vector<>(numberOfTables);
        for (int i = 0; i < numberOfTables; i++) {
            tables.add(new Table(i + 1)); // Table IDs start from 1
        }
        this.availableTablesSemaphore = new Semaphore(numberOfTables); // All tables are initially available
        this.mutex = new Semaphore(1); // For potential complex modifications, though acquire/release are primary sync
    }

    /**
     * Acquires an available table for a customer.
     * Blocks if no tables are available. Finds the first available table.
     * @param customerId The ID of the customer who will occupy the table.
     * @return The Table object that was acquired.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public Table acquireTable(int customerId) throws InterruptedException {
        availableTablesSemaphore.acquire(); // Wait for a table to become available

        Table acquiredTable = null;
        mutex.acquire(); // Lock to safely find and mark a table as occupied
        try {
            // Find the first non-occupied table (should always find one due to semaphore)
            for (Table table : tables) {
                if (table.occupy(customerId)) { // Try to occupy
                    acquiredTable = table;
                    break;
                }
            }
            if (acquiredTable == null) {
                // This state should theoretically not be reached if semaphore logic is correct
                System.err.println("Error: Acquired table semaphore but couldn't find free table.");
                // To prevent deadlock, release the acquired semaphore permit if we failed
                availableTablesSemaphore.release();
                throw new IllegalStateException("Failed to find an available table after acquiring semaphore.");
            }
        } finally {
            mutex.release(); // Ensure mutex is released
        }
        return acquiredTable;
    }


    /**
     * Releases a specific table, making it available again.
     * This is the consumeAt equivalent mentioned in the requirements[cite: 84, 85, 86].
     * @param tableId The ID of the table to release.
     * @return true if the table was found and released, false otherwise.
     */
    public boolean releaseTable(int tableId) throws InterruptedException {
        Table tableToRelease = null;
        mutex.acquire(); // Lock to safely find and update the table state
        try {
            for(Table table : tables) {
                if(table.getTableId() == tableId) {
                    if (table.isOccupied()) { // Check if it was actually occupied
                        table.free(); // Mark as free
                        tableToRelease = table;
                    } else {
                        // Log warning: Trying to release a table that wasn't occupied
                        System.err.println("Warning: Attempted to release Table " + tableId + " which was not occupied.");
                    }
                    break;
                }
            }
        } finally {
            mutex.release(); // Ensure mutex is released
        }

        if (tableToRelease != null) {
            availableTablesSemaphore.release(); // Signal that a table is now available
            return true;
        } else {
            System.err.println("Error: Attempted to release non-existent Table ID " + tableId);
            return false; // Table ID not found
        }
    }

    /**
     * Gets the total number of tables managed by this buffer.
     * @return The number of tables.
     */
    public int getTotalTables() {
        return tables.size();
    }
}