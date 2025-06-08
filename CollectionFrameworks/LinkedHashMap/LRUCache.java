import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K,V> extends LinkedHashMap<K,V> {
    private int capacity;
    
    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > capacity;
    }
    
    public static void main(String[] args) {
        LRUCache<String, Integer> studentMap = new LRUCache<>(3);
        studentMap.put("a", 33);
        studentMap.put("b", 332);
        studentMap.put("x", 22);
        studentMap.put("d", 443);
        System.out.println(studentMap.get("x"));
        System.out.println(studentMap);

    }
}