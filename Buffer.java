import java.util.Vector;

/**
 * Bounded buffer implementation using custom Semaphores for synchronization.
 * Used for ordered meals and cooked meals.
 * [cite: 74, 76]
 */
public class Buffer implements Producer, Consumer {
    private final Vector<BufElement> buffer;
    private final int capacity;
    private final Semaphore mutex;  // For mutual exclusion accessing the buffer
    private final Semaphore spaces; // Counts empty spaces in the buffer
    private final Semaphore items;  // Counts available items in the buffer

    /**
     * Creates a Buffer with a specified capacity.
     * @param capacity The maximum number of items the buffer can hold. [cite: 74]
     */
    public Buffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Buffer capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new Vector<>(capacity);
        this.mutex = new Semaphore(1);        // Initialized to 1 (binary semaphore)
        this.spaces = new Semaphore(capacity); // Initialized to buffer capacity
        this.items = new Semaphore(0);         // Initialized to 0 (no items initially)
    }

    /**
     * Adds an item to the buffer (Producer role).
     * Blocks if the buffer is full.
     * @param item The BufElement to add.
     * @throws InterruptedException If the thread is interrupted.
     */
    @Override
    public void produce(BufElement item) throws InterruptedException {
        spaces.acquire(); // Wait for an empty space
        mutex.acquire();  // Enter critical section

        // Add the item to the buffer
        buffer.add(item);
        // System.out.println(Thread.currentThread().getName() + " produced " + item + ". Buffer size: " + buffer.size()); // Debug

        mutex.release();  // Exit critical section
        items.release();  // Signal that an item is available
    }

    /**
     * Removes an item from the buffer (Consumer role).
     * Blocks if the buffer is empty. Follows FIFO order.
     * @return The consumed BufElement.
     * @throws InterruptedException If the thread is interrupted.
     */
    @Override
    public BufElement consume() throws InterruptedException {
        items.acquire();  // Wait for an available item
        mutex.acquire();  // Enter critical section

        // Remove the item from the buffer (FIFO)
        BufElement item = buffer.remove(0);
        // System.out.println(Thread.currentThread().getName() + " consumed " + item + ". Buffer size: " + buffer.size()); // Debug


        mutex.release();  // Exit critical section
        spaces.release(); // Signal that a space is available

        return item;
    }

    /**
     * Returns the current number of items in the buffer.
     * @return The number of items.
     */
    public int getCurrentSize() {
        // Acquire mutex to safely read size, though Vector.size() is often thread-safe, explicit locking is safer conceptually here.
        int size = -1;
        try {
            mutex.acquire();
            size = buffer.size();
            mutex.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve interrupt status
            // Handle exception appropriately, maybe return -1 or rethrow
            System.err.println("Interrupted while getting buffer size.");
        }
        return size;
    }

    /**
     * Returns the capacity of the buffer.
     * @return The capacity.
     */
    public int getCapacity() {
        return capacity;
    }
}