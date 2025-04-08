/**
 * Custom Semaphore class using basic Java synchronization.
 * As required by the project description, this does not use java.util.concurrent.Semaphore.
 */
public class Semaphore {
    private int permits;

    /**
     * Creates a Semaphore with the given number of permits.
     * @param permits The initial number of permits available. Must be non-negative.
     */
    public Semaphore(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("Permits must be non-negative");
        }
        this.permits = permits;
    }

    /**
     * Acquires a permit from this semaphore, blocking until one is available.
     * Also known as P() or wait().
     * @throws InterruptedException if the current thread is interrupted while waiting.
     */
    public synchronized void acquire() throws InterruptedException {
        while (permits == 0) {
            wait(); // Wait until a permit is released
        }
        permits--;
    }

    /**
     * Releases a permit, returning it to the semaphore.
     * Also known as V() or signal().
     * Increases the number of available permits by one. If any threads are waiting
     * to acquire a permit, one is woken up.
     */
    public synchronized void release() {
        permits++;
        notify(); // Notify a waiting thread that a permit is available
    }

    /**
     * Returns the current number of permits available in this semaphore.
     * @return the number of permits available.
     */
    public synchronized int availablePermits() {
        return permits;
    }
}