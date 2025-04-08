import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents the process of a customer arriving, getting a table, and placing an order.
 * Implements Runnable for threading and Producer for placing orders into the orderedMealsBuf.
 * [cite: 78]
 */
public class CustKiosk implements Runnable, Producer {
    private final Customer customer;
    private final TableBuffer tableBuffer; // Shared resource for tables [cite: 66]
    private final Buffer orderedMealsBuf;  // Shared buffer for placing orders [cite: 66]
    private final SimulationClock clock;   // Shared simulation clock
    private final AtomicLong totalTableWaitTime; // Accumulator for stats
    private final long arrivalDelayMillis; // Delay before this customer "arrives"


    public CustKiosk(Customer customer, TableBuffer tableBuffer, Buffer orderedMealsBuf, SimulationClock clock, AtomicLong totalTableWaitTime, long startTimeMillis) {
        this.customer = customer;
        this.tableBuffer = tableBuffer;
        this.orderedMealsBuf = orderedMealsBuf;
        this.clock = clock;
        this.totalTableWaitTime = totalTableWaitTime;
        // Calculate delay from simulation start time
        this.arrivalDelayMillis = (long)customer.getArrivalTimeMinutes() * 60 * 1000; // Convert minutes to milliseconds
    }

    @Override
    public void run() {
        try {
            // 1. Simulate Arrival Delay
            Thread.sleep(arrivalDelayMillis); // Wait until the customer's arrival time

            long arrivalTimestamp = clock.getTimeMillis();
            printEvent("arrives.");

            // 2. Acquire a Table (Producer reserves a table resource)
            // System.out.println(clock.getFormattedTime() + " Customer " + customer.getCustomerId() + " waiting for table."); // Debug
            Table assignedTable = tableBuffer.acquireTable(customer.getCustomerId());
            long seatedTimestamp = clock.getTimeMillis();
            long waitTime = seatedTimestamp - arrivalTimestamp;
            totalTableWaitTime.addAndGet(waitTime); // Add to total wait time for stats
            printEvent("is seated at " + assignedTable + " (Waited " + (waitTime / 1000.0) + " sec)."); // [cite: 35, 36]


            // 3. Place Order (Producer adds to orderedMealsBuf) [cite: 13]
            OrderedMeal order = new OrderedMeal(customer.getCustomerId(), customer.getOrderMealName(), assignedTable.getTableId());
            produce(order); // Use the Producer interface method
            printEvent("places an order: " + customer.getOrderMealName() + "."); // [cite: 37, 38]

            // Customer logic after ordering (waiting for food, eating) is implicitly handled
            // by the Waiter needing to serve them at their assigned table before the table is released.
            // The customer thread/kiosk essentially finishes its primary task here.

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Customer " + customer.getCustomerId() + "'s kiosk thread interrupted.");
        }
    }

    /**
     * Implements the Producer interface method to add an order to the buffer.
     * @param item Must be an OrderedMeal.
     * @throws InterruptedException If the thread is interrupted.
     */
    @Override
    public void produce(BufElement item) throws InterruptedException {
        if (!(item instanceof OrderedMeal)) {
            throw new IllegalArgumentException("CustKiosk can only produce OrderedMeal objects.");
        }
        orderedMealsBuf.produce(item); // Add the order to the shared buffer [cite: 78]
        // System.out.println(clock.getFormattedTime() + " Customer " + customer.getCustomerId() + " order placed in buffer."); // Debug
    }

    private void printEvent(String message) {
        System.out.println("[" + clock.getFormattedTime() + "] Customer " + customer.getCustomerId() + " " + message);
    }
}