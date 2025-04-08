/**
 * Represents a meal ordered by a customer. Extends BufElement.
 * Contains details about the order.
 *
 */
public class OrderedMeal extends BufElement {
    private final int customerId;
    private final String mealName;
    private final int tableId; // Table where the customer is seated

    public OrderedMeal(int customerId, String mealName, int tableId) {
        this.customerId = customerId;
        this.mealName = mealName;
        this.tableId = tableId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getMealName() {
        return mealName;
    }

    public int getTableId() {
        return tableId;
    }

    @Override
    public String toString() {
        return mealName + " for Customer " + customerId + " at Table " + tableId;
    }
}