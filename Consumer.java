/**
 * Interface for Consumer roles in the Producer-Consumer pattern.
 * [cite: 72, 73]
 */
public interface Consumer {
    /**
     * Consumes an item from the buffer.
     * @return The consumed BufElement.
     * @throws InterruptedException If the thread is interrupted.
     */
    BufElement consume() throws InterruptedException;
}