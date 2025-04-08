/**
 * Represents a table in the restaurant. Extends BufElement for use in TableBuffer.
 * [cite: 17, 66]
 */
public class Table extends BufElement {
    private final int tableId;
    private boolean occupied;
    private int customerId = -1; // ID of the customer occupying the table, -1 if free

    public Table(int tableId) {
        this.tableId = tableId;
        this.occupied = false;
    }

    public int getTableId() {
        return tableId;
    }

    public synchronized boolean isOccupied() {
        return occupied;
    }

    /**
     * Occupies the table for a specific customer.
     * @param customerId The ID of the customer occupying the table.
     * @return true if occupation was successful, false if already occupied.
     */
    public synchronized boolean occupy(int customerId) {
        if (!occupied) {
            this.occupied = true;
            this.customerId = customerId;
            return true;
        }
        return false;
    }

    /**
     * Frees the table.
     */
    public synchronized void free() {
        this.occupied = false;
        this.customerId = -1;
    }

    /**
     * Gets the ID of the customer currently occupying the table.
     * @return Customer ID or -1 if free.
     */
    public synchronized int getOccupyingCustomerId() {
        return customerId;
    }

    @Override
    public String toString() {
        return "Table " + tableId;
    }
}