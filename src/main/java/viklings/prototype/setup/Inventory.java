package viklings.prototype.setup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Inventory {

    private final Map<Item, AtomicLong> inventory = new ConcurrentHashMap<>();

    public void add(Item item) {
	AtomicLong count = inventory.computeIfAbsent(item, (i) -> {return new AtomicLong(0);});
	count.incrementAndGet();
    }
    
    /**
     * Decrements the count of the specified item in the inventory.
     * If not present or count is already zero, returns false. If present, returns true.
     * 
     * @param item
     * @return
     */
    public boolean remove(Item item) {
	return decrementIfMoreThanZero(inventory.get(item));
    }
    
    /**
     * 
     * @return true if decremented, else false
     */
    public boolean decrementIfMoreThanZero(AtomicLong count) {
	if (count == null) return false;
	long num = count.get();
	if (num > 0) {
	    while(!count.compareAndSet(num, -1)) {
		num = count.get();
		if (num <= 0) return false;
	    }
	    return true;
	} else {
	    return false;
	}
    }
}
