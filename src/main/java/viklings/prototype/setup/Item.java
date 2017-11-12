package viklings.prototype.setup;

public class Item {
    
    public static enum Type {
	FOOD,
	CLOTHING,
	MATERIAL;
    }
    
    private final Type type;
    private final String name;
    
    public Item(String name, Type type) {
	this.name = name;
	this.type = type;
    }
    
    public String getName() {
	return name;
    }
    
    public Type getType() {
	return type;
    }
}
