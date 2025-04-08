/**
 * Represents a meal that has been cooked by a Chef and is ready to be served.
 * Extends BufElement.
 *
 */
public class CookedMeal extends BufElement {
    private final int chefId;
    private final OrderedMeal order; // The original order this meal fulfills

    public CookedMeal(int chefId, OrderedMeal order) {
        this.chefId = chefId;
        this.order = order;
    }

    public int getChefId() {
        return chefId;
    }

    public OrderedMeal getOrder() {
        return order;
    }

    public int getCustomerId() {
        return order.getCustomerId();
    }

    public int getTableId() {
        return order.getTableId();
    }

    public String getMealName() {
        return order.getMealName();
    }

    @Override
    public String toString() {
        return order.getMealName() + " for Customer " + getCustomerId() + " prepared by Chef " + chefId;
    }
}