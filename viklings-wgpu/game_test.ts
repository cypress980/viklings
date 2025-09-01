// game_test.ts - Automated gameplay demonstration
// This will demonstrate the complete square game working by simulating player movement

console.log("=== SQUARE GAME AUTOMATED GAMEPLAY TEST ===");

// Game test variables (using globals set by square_game.ts)
let testStep = 0;
let testGameWon = false;
let frameCount = 0;

// Test sequence: move player from (50,50) toward goal at (350,250)
const testSequence = [
    { x: 50, y: 50, description: "Starting position" },
    { x: 100, y: 75, description: "Moving right and slightly down" },
    { x: 150, y: 100, description: "Continuing toward goal" },
    { x: 200, y: 150, description: "Getting closer to goal" },
    { x: 250, y: 200, description: "Almost at goal" },
    { x: 300, y: 225, description: "Very close to goal" },
    { x: 340, y: 245, description: "Adjacent to goal - should detect collision!" },
    { x: 350, y: 250, description: "Exactly at goal center - WIN!" }
];

// Make this a global function so it can be called from square_game.ts
globalThis.gameplayTest = function() {
    frameCount++;
    
    // Debug output every 60 frames to show we're being called
    if (frameCount % 60 === 0) {
        console.log(`📊 Test frame ${frameCount} - gameplayTest() called`);
    }
    
    // Only move every 60 frames (about 1 second at 60 FPS)
    if (frameCount % 60 !== 0) {
        return;
    }
    
    if (testStep >= testSequence.length) {
        console.log("✅ Gameplay test complete!");
        return;
    }
    
    const currentTest = testSequence[testStep];
    
    console.log(`Step ${testStep + 1}: ${currentTest.description}`);
    console.log(`  Moving player to (${currentTest.x}, ${currentTest.y})`);
    
    // Move the player to the test position
    const moveSuccess = Deno.core.ops.op_set_entity_position(player, currentTest.x, currentTest.y);
    
    if (moveSuccess) {
        console.log(`  ✅ Player moved successfully`);
        
        // Check for collisions
        const collisionEvents = Deno.core.ops.op_poll_collision_events();
        
        if (collisionEvents.length > 0) {
            console.log(`  🎯 COLLISION DETECTED! Events: ${collisionEvents.length}`);
            
            for (const event of collisionEvents) {
                if ((event.entity_a === player && event.entity_b === goal) ||
                    (event.entity_a === goal && event.entity_b === player)) {
                    
                    console.log("  🏆 PLAYER REACHED GOAL! WIN CONDITION TRIGGERED!");
                    testGameWon = true;
                    
                    // Show win UI (this would trigger the win screen)
                    console.log("  📺 Win UI would be displayed now");
                    console.log("  🎮 Player can press R to reset and play again");
                    
                    // Take a screenshot to show the win state
                    console.log("  📸 Taking screenshot to show the win...");
                    return;
                }
            }
        } else {
            console.log(`  ⏳ No collision yet, continuing...`);
        }
    } else {
        console.log(`  ❌ Failed to move player`);
    }
    
    testStep++;
};

// Call our test function from within the original gameUpdate
// This will be called by the existing gameUpdate() function through a global callback

console.log("🎮 Automated gameplay test will begin after game initialization...");
console.log("🎯 Test will move blue square from start to goal position");
console.log("🏆 Should trigger win condition when player reaches goal!");