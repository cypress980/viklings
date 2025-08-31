// AUTOMATED collision detection test (no keyboard required)
console.log("=== AUTOMATED COLLISION DETECTION TEST ===");

// Create blue square (will move programmatically toward green)
const blueSquare = Deno.core.ops.op_create_entity();
Deno.core.ops.op_add_position(blueSquare, 100, 300);  // Start left
Deno.core.ops.op_add_render(blueSquare, {
    width: 40,
    height: 40,
    color: [0.0, 0.0, 1.0], // Blue
    shape: "square"
});
Deno.core.ops.op_add_hitbox(blueSquare, {
    width: 20,
    height: 20,
    offset_x: 0,
    offset_y: 0
});

// Create green square (static target)
const greenSquare = Deno.core.ops.op_create_entity();
Deno.core.ops.op_add_position(greenSquare, 400, 300);  // Target position
Deno.core.ops.op_add_render(greenSquare, {
    width: 40,
    height: 40,
    color: [0.0, 1.0, 0.0], // Green
    shape: "square"
});
Deno.core.ops.op_add_hitbox(greenSquare, {
    width: 20,
    height: 20,
    offset_x: 0,
    offset_y: 0
});

console.log(`🤖 Blue square (entity ${blueSquare}) starts at (100, 300)`);
console.log(`🎯 Green square (entity ${greenSquare}) target at (400, 300)`);
console.log("🚀 AUTOMATED TEST: Blue square will move toward green automatically");

// Test state
let frameCount = 0;
let isColliding = false;
let testPhase = "approaching";
let hasCollisionOccurred = false;

function checkCollisions() {
    frameCount++;
    
    // Move blue square toward green every few frames
    if (frameCount % 2 === 0 && testPhase === "approaching") {
        const currentX = 100 + (frameCount / 2) * 8; // Move 8 pixels every 2 frames
        
        if (currentX < 420) {
            Deno.core.ops.op_set_entity_position(blueSquare, currentX, 300);
            
            if (frameCount % 20 === 0) {
                console.log(`🤖 Blue square moving: x=${currentX.toFixed(0)} (target: 400)`);
            }
        } else {
            testPhase = "separating";
            console.log("🔄 Starting separation phase");
        }
    }
    
    // Separation phase
    if (frameCount % 2 === 0 && testPhase === "separating") {
        const separateX = 420 + ((frameCount - 100) / 2) * 6; // Move away
        if (separateX < 600) {
            Deno.core.ops.op_set_entity_position(blueSquare, separateX, 300);
            
            if (frameCount % 20 === 0) {
                console.log(`🤖 Blue square separating: x=${separateX.toFixed(0)}`);
            }
        } else {
            testPhase = "complete";
            console.log("✅ AUTOMATED TEST COMPLETED");
            if (hasCollisionOccurred) {
                console.log("🎉 SUCCESS: Collision detection working correctly!");
            } else {
                console.log("⚠️  WARNING: No collision detected during test");
            }
        }
    }
    
    // Check for collision events
    const events = Deno.core.ops.op_poll_collision_events();
    
    let collisionDetected = false;
    for (const event of events) {
        if ((event.entity_a === blueSquare && event.entity_b === greenSquare) ||
            (event.entity_a === greenSquare && event.entity_b === blueSquare)) {
            collisionDetected = true;
            break;
        }
    }
    
    // Handle collision state changes
    if (collisionDetected && !isColliding) {
        console.log("🔥🔥🔥 COLLISION DETECTED! 🔥🔥🔥");
        console.log("➡️  Changing both squares to WHITE");
        
        Deno.core.ops.op_set_entity_color(blueSquare, 1.0, 1.0, 1.0);
        Deno.core.ops.op_set_entity_color(greenSquare, 1.0, 1.0, 1.0);
        isColliding = true;
        hasCollisionOccurred = true;
        
    } else if (!collisionDetected && isColliding) {
        console.log("🔚 COLLISION ENDED");
        console.log("➡️  Returning squares to original colors");
        
        Deno.core.ops.op_set_entity_color(blueSquare, 0.0, 0.0, 1.0); // Blue
        Deno.core.ops.op_set_entity_color(greenSquare, 0.0, 1.0, 0.0); // Green
        isColliding = false;
    }
}

// Make available globally
globalThis.checkCollisions = checkCollisions;

console.log("🎯 Automated collision test ready - starting movement simulation...");