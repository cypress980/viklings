// game.ts - Simple Square Game
// Goal: Move the blue player square so its center overlaps the goal's center

import { Engine, Entity, Vec2, Color } from './engine';

// Game state
let gameWon = false;

// Create controllable player entity
const player = Engine.createEntity({
    position: new Vec2(50, 50),
    size: new Vec2(30, 30),
    color: Color.BLUE,
    type: 'solid',
    controllable: {
        movement: 'arrow_keys',  // Engine handles arrow key -> movement
        speed: 120,              // pixels per second
        bounds: 'screen'         // keep within screen bounds
    },
    hitbox: {
        size: new Vec2(10, 10),  // Small center hitbox for precise collision
        offset: new Vec2(0, 0)   // Centered on entity
    }
});

// Create goal entity
const goal = Engine.createEntity({
    position: new Vec2(350, 250),
    size: new Vec2(40, 40),
    color: Color.GREEN,
    type: 'outline',
    static: true,  // doesn't move
    hitbox: {
        size: new Vec2(20, 20),  // Center area of goal for win condition
        offset: new Vec2(0, 0)   // Centered on entity
    }
});

// Register collision event between hitboxes - more precise than full overlap
Engine.onHitboxCollision(player, goal, () => {
    if (!gameWon) {
        gameWon = true;
        Engine.pauseInput(player);  // stop accepting input for player
        Engine.showMessage("You Win! Press R to play again");
        console.log("Victory! Player center reached goal center");
    }
});

// Game reset handler - we handle this specific input
Engine.onKeyPressed('KeyR', () => {
    resetGame();
});

// Reset game state
function resetGame() {
    console.log("Resetting game...");
    
    // Pause engine physics/input during reset
    Engine.pausePhysics();
    
    gameWon = false;
    
    // Reset player position
    Engine.setEntityPosition(player, new Vec2(50, 50));
    
    // Re-enable player input
    Engine.resumeInput(player);
    Engine.hideMessage();
    
    // Resume engine physics/input
    Engine.resumePhysics();
    
    console.log("Game reset complete");
}

// Game initialization
function init() {
    console.log("Simple Square Game");
    console.log("Use arrow keys to move the blue square");
    console.log("Get the center of your square into the goal area to win");
    console.log("Press R to restart anytime");
}

// Start the game
init();

// Export for hot reloading and debugging
export { player, goal, resetGame };