import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeTree {

    private static final boolean RED = true;
    private static final boolean BLACK = false;

    private final ReentrantReadWriteLock lock;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private Node root;

    private class Node {
        byte[] key;
        byte[] value;
        Node left;
        Node right;
        Node parent;
        boolean color;

        Node(byte[] key, byte[] value, Node parent, boolean color) {
            this.key = key;
            this.value = value;
            this.parent = parent;
            this.color = color;
        }
    }

    /**
     * Default constructor. Creates a new tree and its own internal lock.
     */
    public ThreadSafeTree() {
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    /**
     * Constructor for when an external lock is provided.
     * This allows for coordinating operations on this tree with other data structures.
     * @param lock The external lock to use.
     */
    public ThreadSafeTree(ReentrantReadWriteLock lock) {
        if (lock == null) {
            throw new IllegalArgumentException("Provide a non-null lock for the tree.");
        }
        this.lock = lock;
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    /**
     * Retrieves the value associated with the given key.
     * @param key The key to search for.
     * @return The value associated with the key, or null if the key is not found.
     */
    public byte[] get(byte[] key) {
        if (key == null) return null;

        readLock.lock();
        try {
            Node helper = root;
            while (helper != null) {
                int compare = Arrays.compare(key, helper.key);
                if (compare < 0) {
                    helper = helper.left;
                } else if (compare > 0) {
                    helper = helper.right;
                } else {
                    return helper.value;
                }
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Inserts or updates a key-value pair in the tree.
     * @param key   The key to insert or update.
     * @param value The value to associate with the key.
     */
    public void put(byte[] key, byte[] value) {
        if (key == null || value == null) {
            throw new NullPointerException("Null values or keys not allowed in the tree.");
        }

        writeLock.lock();
        try {
            if (root == null) {
                root = new Node(key, value, null, BLACK);
                return;
            }

            Node helper = root;
            Node parent = null;
            int compare = 0;

            while (helper != null) {
                parent = helper;
                compare = Arrays.compare(key, helper.key);
                if (compare < 0) {
                    helper = helper.left;
                } else if (compare > 0) {
                    helper = helper.right;
                } else {
                    helper.value = value;
                    return;
                }
            }

            Node newNode = new Node(key, value, parent, RED);
            if (compare < 0) {
                parent.left = newNode;
            } else {
                parent.right = newNode;
            }

            fixTree(newNode);

        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Fixes the tree after an insertion or update.
     * It does so by rotating the tree and making color changes to restore RB properties.
     * @param currentNode The node to start at.
     */
    private void fixTree(Node currentNode) {
        while (currentNode != root && isRed(parentOf(currentNode))) {
            if (parentOf(currentNode) == parentOf(parentOf(currentNode)).left) {
                Node uncleNode = parentOf(parentOf(currentNode)).right;
                if (isRed(uncleNode)) {
                    setColor(parentOf(currentNode), BLACK);
                    setColor(uncleNode, BLACK);
                    setColor(parentOf(parentOf(currentNode)), RED);
                    currentNode = parentOf(parentOf(currentNode));
                } else {
                    if (currentNode == parentOf(currentNode).right) {
                        currentNode = parentOf(currentNode);
                        rotateLeft(currentNode);
                    }
                    setColor(parentOf(currentNode), BLACK);
                    setColor(parentOf(parentOf(currentNode)), RED);
                    rotateRight(parentOf(parentOf(currentNode)));
                }
            } else {
                Node uncleNode = parentOf(parentOf(currentNode)).left;
                if (isRed(uncleNode)) {
                    setColor(parentOf(currentNode), BLACK);
                    setColor(uncleNode, BLACK);
                    setColor(parentOf(parentOf(currentNode)), RED);
                    currentNode = parentOf(parentOf(currentNode));
                } else {
                    if (currentNode == parentOf(currentNode).left) {
                        currentNode = parentOf(currentNode);
                        rotateRight(currentNode);
                    }
                    setColor(parentOf(currentNode), BLACK);
                    setColor(parentOf(parentOf(currentNode)), RED);
                    rotateLeft(parentOf(parentOf(currentNode)));
                }
            }
        }
        root.color = BLACK;
    }

    /**
     * Rotates the tree left.
     * @param pivotNode The node to rotate.
     */
    private void rotateLeft(Node pivotNode) {
        if (pivotNode != null) {
            Node rightChild = pivotNode.right;
            pivotNode.right = rightChild.left;
            if (rightChild.left != null) {
                rightChild.left.parent = pivotNode;
            }
            rightChild.parent = pivotNode.parent;
            if (pivotNode.parent == null) {
                root = rightChild;
            } else if (pivotNode == pivotNode.parent.left) {
                pivotNode.parent.left = rightChild;
            } else {
                pivotNode.parent.right = rightChild;
            }
            rightChild.left = pivotNode;
            pivotNode.parent = rightChild;
        }
    }

    /**
     * Rotates the tree right.
     * @param pivotNode The node to rotate.
     */
    private void rotateRight(Node pivotNode) {
        if (pivotNode != null) {
            Node leftChild = pivotNode.left;
            pivotNode.left = leftChild.right;
            if (leftChild.right != null) {
                leftChild.right.parent = pivotNode;
            }
            leftChild.parent = pivotNode.parent;
            if (pivotNode.parent == null) {
                root = leftChild;
            } else if (pivotNode == pivotNode.parent.right) {
                pivotNode.parent.right = leftChild;
            } else {
                pivotNode.parent.left = leftChild;
            }
            leftChild.right = pivotNode;
            pivotNode.parent = leftChild;
        }
    }

    /**
     * Returns the parent of the given node.
     * @param node The node to get the parent of.
     * @return The parent of the given node, or null if the node is the root.
     */
    private Node parentOf(Node node) {
        return (node == null ? null : node.parent);
    }

    /**
     * Returns whether the given node is red.
     * @param node The node to check.
     * @return True if the node is red, false otherwise.
     */
    private boolean isRed(Node node) {
        return (node != null && node.color == RED);
    }

    /**
     * Sets the color of the given node.
     * @param node The node to set the color of.
     * @param color The color to set.
     */
    private void setColor(Node node, boolean color) {
        if (node != null) {
            node.color = color;
        }
    }
}