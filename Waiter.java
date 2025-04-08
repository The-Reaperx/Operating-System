import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Represents a Waiter who consumes cooked meals and serves them to customers.
 * Also responsible for clearing the table after the customer leaves.
 * Implements Runnable and Consumer (for cooked meals).
 * [cite: 16, 82, 83, 84]
 */
public class Waiter implements Runnable, Consumer {
    private final int waiterId;
    private final Buffer cookedMealsBuf; // Consume cooked meals from here [cite: 83]
    private final TableBuffer tableBuffer;   // Interact with tables (release) [cite: 84]
    private final SimulationClock clock;
    private final Random random = new Random(); // For simulating eating time [cite: 25]
    private final AtomicInteger customersServedCounter; // To track total served customers


    public Waiter(int waiterId, Buffer cookedMealsBuf, TableBuffer tableBuffer, SimulationClock clock, AtomicInteger customersServedCounter) {
        this.waiterId = waiterId;
        this.cookedMealsBuf = cookedMealsBuf;
        this.tableBuffer = tableBuffer;
        this.clock = clock;
        this.customersServedCounter = customersServedCounter;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // 1. Get a Cooked Meal to Serve (Consumer role) [cite: 16, 83]
                // System.out.println(clock.getFormattedTime() + " Waiter " + waiterId + " waiting for cooked meal."); // Debug
                CookedMeal mealToServe = (CookedMeal) consume(); // Consume from cookedMealsBuf
                if (mealToServe == null) {
                    continue;
                }
                // System.out.println(clock.getFormattedTime() + " Waiter " + waiterId + " picked up " + mealToServe); // Debug

                // 2. Serve the Meal to the Customer at their Table [cite: 16]
                // Simulate time to walk to the table (optional, could add a small sleep)
                printEvent("serves " + mealToServe.getMealName() + " to Customer " + mealToServe.getCustomerId() + " at Table " + mealToServe.getTableId() + "."); // [cite: 43, 44]


                // 3. Simulate Customer Eating Time [cite: 13, 25]
                // Add a random delay to simulate eating
                // Let's assume eating takes between 5 and 15 simulation minutes
                int eatingTimeMinutes = 5 + random.nextInt(11); // Random number between 5 and 15
                long eatingTimeMillis = (long)eatingTimeMinutes * 60 * 1000;
                // --- Simulation Speed Factor ---
                // Divide by speed factor if needed: eatingTimeMillis /= 10;
                Thread.sleep(eatingTimeMillis);


                // 4. Customer Finishes and Leaves; Clear the Table [cite: 13 (leave), 16 (clear), 84]
                printEvent("sees Customer " + mealToServe.getCustomerId() + " finishes eating and leaves the restaurant."); // [cite: 45, 46]

                // Release the table using TableBuffer's specific release method [cite: 84, 86]
                tableBuffer.releaseTable(mealToServe.getTableId());
                printEvent("clears Table " + mealToServe.getTableId() + ". Table is now available."); // [cite: 47]

                // Increment served customer count AFTER they have left
                customersServedCounter.incrementAndGet();

            }
        } catch (InterruptedException e) {
            // Thread interruption signals simulation end
            System.out.println("[" + clock.getFormattedTime() + "] Waiter " + waiterId + " stopping.");
            Thread.currentThread().interrupt(); // Preserve interrupt status
        } catch (Exception e) {
            System.err.println("Waiter " + waiterId + " encountered an error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Implements Consumer interface to get a cooked meal.
     * @return The CookedMeal consumed.
     * @throws InterruptedException If the thread is interrupted.
     */
    @Override
    public BufElement consume() throws InterruptedException {
        return cookedMealsBuf.consume(); // Consume from the cooked meals buffer [cite: 83]
    }

    private void printEvent(String message) {
        System.out.println("[" + clock.getFormattedTime() + "] Waiter " + waiterId + " " + message);
    }
}