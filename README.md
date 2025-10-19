# ThreadSafeTree
- This is a sample implementation of a thread-safe version of a sorted in-memory tree.
- Internally, it is very simple: it uses the Red-Black tree structure, but adds locks for reading and getting.
- The way it works is that reading is locked when writing is happening, and vice versa. Also writing locks writing, but reading does not lock reading.
- So it can be concurrently read without issues, and it's mostly writing that locks it.
