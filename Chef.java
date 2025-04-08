import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Represents a Chef who consumes orders and produces cooked meals.
 * Implements Runnable, Consumer (for orders), and Producer (for cooked meals).
 * [cite: 14, 15, 79, 80, 81]
 */
public class Chef implements Runnable, Consumer, Producer {
    private final int chefId;
    private final Buffer orderedMealsBuf;  // Consume orders from here [cite: 80]
    private final Buffer cookedMealsBuf;   // Produce cooked meals here [cite: 81]
    private final Map<String, Integer> prepTimesMinutes; // Meal -> Prep time map [cite: 70]
    private final SimulationClock clock;
    private final AtomicLong totalPrepTime; // Accumulator for stats
    private final AtomicInteger totalMealsPrepared; // Counter for stats


    public Chef(int chefId, Buffer orderedMealsBuf, Buffer cookedMealsBuf, Map<String, Integer> prepTimesMinutes, SimulationClock clock, AtomicLong totalPrepTime, AtomicInteger totalMealsPrepared) {
        this.chefId = chefId;
        this.orderedMealsBuf = orderedMealsBuf;
        this.cookedMealsBuf = cookedMealsBuf;
        this.prepTimesMinutes = prepTimesMinutes;
        this.clock = clock;
        this.totalPrepTime = totalPrepTime;
        this.totalMealsPrepared = totalMealsPrepared;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // 1. Get an Order to Prepare (Consumer role) [cite: 14, 80]
                // System.out.println(clock.getFormattedTime() + " Chef " + chefId + " waiting for order."); // Debug
                OrderedMeal orderToPrepare = (OrderedMeal) consume(); // Consume from orderedMealsBuf
                if (orderToPrepare == null) { // Might happen if consume returns null on interrupt?
                    continue;
                }
                printEvent("starts preparing " + orderToPrepare.getMealName() + " for Customer " + orderToPrepare.getCustomerId() + "."); // [cite: 39, 40]
                long prepStartTime = clock.getTimeMillis();


                // 2. Simulate Preparation Time [cite: 14]
                int prepTimeMinutes = prepTimesMinutes.getOrDefault(orderToPrepare.getMealName(), 5); // Default 5 min if meal not found
                long prepTimeMillis = (long)prepTimeMinutes * 60 * 1000; // Convert to ms
                // --- Simulation Speed Factor ---
                // You might want to divide prepTimeMillis by a speed factor for faster simulation
                // Example: prepTimeMillis /= 10; // 10x faster simulation
                Thread.sleep(prepTimeMillis); // Simulate the cooking time

                long prepEndTime = clock.getTimeMillis();
                long actualPrepDuration = prepEndTime - prepStartTime;
                totalPrepTime.addAndGet(actualPrepDuration); // Add to total prep time stats
                totalMealsPrepared.incrementAndGet(); // Increment meal counter


                // 3. Place Cooked Meal on Bench (Producer role) [cite: 15, 81]
                CookedMeal cookedMeal = new CookedMeal(chefId, orderToPrepare);
                produce(cookedMeal); // Produce to cookedMealsBuf
                printEvent("finishes preparing " + cookedMeal.getMealName() + " for Customer " + cookedMeal.getCustomerId() + "."); // [cite: 41, 42]
                // System.out.println(clock.getFormattedTime() + " Chef " + chefId + " placed " + cookedMeal + " on bench."); // Debug

            }
        } catch (InterruptedException e) {
            // Thread interruption is the signal to stop working (simulation end)
            System.out.println("[" + clock.getFormattedTime() + "] Chef " + chefId + " stopping.");
            Thread.currentThread().interrupt(); // Preserve interrupt status
        } catch (Exception e) {
            System.err.println("Chef " + chefId + " encountered an error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Implements Consumer interface to get an order.
     * @return The OrderedMeal consumed.
     * @throws InterruptedException If the thread is interrupted.
     */
    @Override
    public BufElement consume() throws InterruptedException {
        return orderedMealsBuf.consume(); // Consume from the ordered meals buffer [cite: 80]
    }

    /**
     * Implements Producer interface to place a cooked meal.
     * @param item Must be a CookedMeal.
     * @throws InterruptedException If the thread is interrupted.
     */
    @Override
    public void produce(BufElement item) throws InterruptedException {
        if (!(item instanceof CookedMeal)) {
            throw new IllegalArgumentException("Chef can only produce CookedMeal objects.");
        }
        cookedMealsBuf.produce(item); // Place the cooked meal on the "bench" (buffer) [cite: 81]
    }

    private void printEvent(String message) {
        System.out.println("[" + clock.getFormattedTime() + "] Chef " + chefId + " " + message);
    }
}