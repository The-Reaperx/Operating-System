/**
 * Represents a customer with arrival time and order details.
 * [cite: 12, 13, 66]
 */
public class Customer {
    private final int customerId;
    private final int arrivalTimeMinutes; // Arrival time in minutes from simulation start (e.g., 08:00 -> 0)
    private final String orderMealName;

    public Customer(int customerId, int arrivalTimeMinutes, String orderMealName) {
        this.customerId = customerId;
        this.arrivalTimeMinutes = arrivalTimeMinutes;
        this.orderMealName = orderMealName;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getArrivalTimeMinutes() {
        return arrivalTimeMinutes;
    }

    public String getOrderMealName() {
        return orderMealName;
    }

    @Override
    public String toString() {
        return "Customer " + customerId + " (Arrives at " + arrivalTimeMinutes + " min, Orders: " + orderMealName + ")";
    }
}