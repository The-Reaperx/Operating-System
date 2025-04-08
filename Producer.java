/**
 * Interface for Producer roles in the Producer-Consumer pattern.
 * [cite: 71, 72]
 */
public interface Producer {
    /**
     * Produces an item and adds it to the buffer.
     * @param item The BufElement to add.
     * @throws InterruptedException If the thread is interrupted.
     */
    void produce(BufElement item) throws InterruptedException;
}