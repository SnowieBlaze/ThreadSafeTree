import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ThreadSafeTreeTest {

    // Test that tries to see if the tree is thread-safe or not.
    // Of course, just because it passed doesn't mean it's necessarily thread-safe.
    // It's just an extra layer of safety.
    @Test
    void testConcurrentPuts() throws InterruptedException {
        ThreadSafeTree tree = new ThreadSafeTree();
        int numThreads = 16;
        int putsPerThread = 500;
        int totalPuts = numThreads * putsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        // For each thread, do a number of puts.
        // Also decrease the countdown for the doneLatch.
        for (int i = 0; i < numThreads; i++) {
            int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < putsPerThread; j++) {
                        String key = "thread: " + threadId + " key: " + j;
                        tree.put(key.getBytes(), "test".getBytes());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Now verify, does the tree contain all the keys?
        // If not, then the tree is not CERTAINLY not thread-safe.
        int foundKeys = 0;
        for (int i = 0; i < numThreads; i++) {
            for (int j = 0; j < putsPerThread; j++) {
                String key = "thread: " + i + " key: " + j;
                if (tree.get(key.getBytes()) != null) {
                    foundKeys++;
                }
            }
        }

        assertEquals(totalPuts, foundKeys);
    }

    // Just some basic tests to see if get and put are acting as expected.
    @Test
    void testGetAndPut(){
        ThreadSafeTree tree = new ThreadSafeTree();
        assertNull(tree.get("random".getBytes()));

        tree.put("test".getBytes(), "first".getBytes());
        assertEquals("first", new String(tree.get("test".getBytes())));

        tree.put("test2".getBytes(), "second".getBytes());
        assertEquals("second", new String(tree.get("test2".getBytes())));

        tree.put("test".getBytes(), "changed!".getBytes());
        assertEquals("changed!", new String(tree.get("test".getBytes())));

        assertThrows(NullPointerException.class, () -> {
            tree.put(null, "test".getBytes());
        });

        assertThrows(NullPointerException.class, () -> {
            tree.put("test".getBytes(), null);
        });

    }
}