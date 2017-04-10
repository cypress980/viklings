package viklings.prototype;

import engine.GameEngine;
import viklings.prototype.setup.ViklingsSetup;

public class Viklings {
    
    public static void main(String[] args) {
	try {
	    ViklingsSetup setup = new ViklingsSetup();
	    GameEngine gameEngine = setup.getGameEngine();
	    gameEngine.start();
	} catch (Exception excp) {
	    excp.printStackTrace();
	    System.exit(-1);
	}
    }
}
