package generics;

import java.util.*;
import java.util.function.Predicate;

// Generic Pair class
class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() { return key; }
    public V getValue() { return value; }
    public void setKey(K key) { this.key = key; }
    public void setValue(V value) { this.value = value; }

    @Override
    public String toString() {
        return "Pair{" + key + ", " + value + "}";
    }
}

// Generic Binary Tree implementation
class BinaryTree<T extends Comparable<T>> {
    private class Node {
        T data;
        Node left, right;

        Node(T data) {
            this.data = data;
            left = right = null;
        }
    }

    private Node root;

    public void insert(T data) {
        root = insertRec(root, data);
    }

    private Node insertRec(Node root, T data) {
        if (root == null) {
            return new Node(data);
        }

        if (data.compareTo(root.data) < 0) {
            root.left = insertRec(root.left, data);
        } else if (data.compareTo(root.data) > 0) {
            root.right = insertRec(root.right, data);
        }

        return root;
    }

    public List<T> inorderTraversal() {
        List<T> result = new ArrayList<>();
        inorderRec(root, result);
        return result;
    }

    private void inorderRec(Node root, List<T> result) {
        if (root != null) {
            inorderRec(root.left, result);
            result.add(root.data);
            inorderRec(root.right, result);
        }
    }
}

// Generic Stack implementation with bounded type parameter
class Stack<T extends Comparable<T>> {
    private List<T> elements;
    private int size;

    public Stack() {
        elements = new ArrayList<>();
        size = 0;
    }

    public void push(T element) {
        elements.add(element);
        size++;
    }

    public T pop() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        T element = elements.remove(--size);
        return element;
    }

    public T peek() {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        return elements.get(size - 1);
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public T max() {
        return elements.stream()
            .max(Comparable::compareTo)
            .orElseThrow(EmptyStackException::new);
    }
}

// Generic Queue implementation with wildcard types
class Queue<T> {
    private List<T> elements;

    public Queue() {
        elements = new LinkedList<>();
    }

    public void enqueue(T element) {
        elements.add(element);
    }

    public T dequeue() {
        if (elements.isEmpty()) {
            throw new NoSuchElementException();
        }
        return elements.remove(0);
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    // Wildcard with upper bound
    public void addAll(Collection<? extends T> collection) {
        elements.addAll(collection);
    }

    // Wildcard with lower bound
    public void copyTo(Collection<? super T> collection) {
        collection.addAll(elements);
    }
}

// Generic utility methods
class GenericUtils {
    // Generic method with type parameter
    public static <T> void swap(T[] array, int i, int j) {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    // Generic method with bounded type parameter
    public static <T extends Comparable<T>> T findMax(List<T> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("List is empty");
        }

        T max = list.get(0);
        for (T element : list) {
            if (element.compareTo(max) > 0) {
                max = element;
            }
        }
        return max;
    }

    // Generic method with wildcard
    public static void printList(List<?> list) {
        for (Object elem : list) {
            System.out.print(elem + " ");
        }
        System.out.println();
    }

    // Generic method with multiple bounds
    public static <T extends Number & Comparable<T>> double sum(List<T> list) {
        double sum = 0.0;
        for (T elem : list) {
            sum += elem.doubleValue();
        }
        return sum;
    }

    // Generic method with Predicate
    public static <T> List<T> filter(List<T> list, Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T elem : list) {
            if (predicate.test(elem)) {
                result.add(elem);
            }
        }
        return result;
    }
}

public class GenericDataStructures {
    public static void main(String[] args) {
        // Demonstrate Pair usage
        Pair<String, Integer> pair = new Pair<>("Age", 25);
        System.out.println("Pair: " + pair);

        // Demonstrate BinaryTree usage
        BinaryTree<Integer> tree = new BinaryTree<>();
        tree.insert(5);
        tree.insert(3);
        tree.insert(7);
        tree.insert(1);
        System.out.println("Binary Tree Inorder: " + tree.inorderTraversal());

        // Demonstrate Stack usage
        Stack<Integer> stack = new Stack<>();
        stack.push(1);
        stack.push(3);
        stack.push(2);
        System.out.println("Stack max: " + stack.max());
        System.out.println("Stack pop: " + stack.pop());

        // Demonstrate Queue usage
        Queue<String> queue = new Queue<>();
        queue.enqueue("First");
        queue.enqueue("Second");
        System.out.println("Queue dequeue: " + queue.dequeue());

        // Demonstrate generic utility methods
        Integer[] numbers = {1, 2, 3, 4, 5};
        GenericUtils.swap(numbers, 0, 4);
        System.out.println("After swap: " + Arrays.toString(numbers));

        List<Integer> numberList = Arrays.asList(3, 1, 4, 1, 5);
        System.out.println("Max number: " + GenericUtils.findMax(numberList));

        List<Double> doubles = Arrays.asList(1.1, 2.2, 3.3);
        System.out.println("Sum of doubles: " + GenericUtils.sum(doubles));

        // Demonstrate generic filtering
        List<Integer> filtered = GenericUtils.filter(numberList, n -> n > 3);
        System.out.println("Filtered numbers > 3: " + filtered);

        // Demonstrate wildcard usage
        List<Integer> intList = Arrays.asList(1, 2, 3);
        List<Double> doubleList = Arrays.asList(1.1, 2.2, 3.3);
        System.out.print("Integer list: ");
        GenericUtils.printList(intList);
        System.out.print("Double list: ");
        GenericUtils.printList(doubleList);

        // Demonstrate bounded wildcard
        Queue<Number> numberQueue = new Queue<>();
        numberQueue.addAll(intList);    // Integer extends Number
        numberQueue.addAll(doubleList); // Double extends Number
    }
}