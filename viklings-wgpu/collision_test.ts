// collision_test.ts - Two-square collision detection demo

console.log("Creating collision test entities...");

// Create first square (controllable, blue initially)
const square1 = Deno.core.ops.op_create_entity();
Deno.core.ops.op_add_position(square1, 100, 200);  // Start far left - no collision
Deno.core.ops.op_add_render(square1, {
    width: 40,
    height: 40,
    color: [0.0, 0.0, 1.0], // Blue
    shape: "square"
});
Deno.core.ops.op_add_hitbox(square1, {
    width: 20,    // Smaller hitbox than visual
    height: 20,
    offset_x: 0,   // Centered
    offset_y: 0
});
Deno.core.ops.op_add_controllable(square1, {
    movement_speed: 150.0,
    input_type: "arrow_keys",
    bounds: "screen"
});

// Create second square (static, green initially)  
const square2 = Deno.core.ops.op_create_entity();
Deno.core.ops.op_add_position(square2, 500, 300);  // Start far right - no collision
Deno.core.ops.op_add_render(square2, {
    width: 40,
    height: 40,
    color: [0.0, 1.0, 0.0], // Green
    shape: "square"
});
Deno.core.ops.op_add_hitbox(square2, {
    width: 20,
    height: 20,
    offset_x: 0,
    offset_y: 0
});

console.log("Created squares " + square1 + " and " + square2);
console.log("COLLISION TEST: Use arrow keys to move blue square (left) into green square (right)");
console.log("Expected: Both squares should turn WHITE when hitboxes collide");
console.log("Initial state: Blue and green squares should be visible and separated");

// Track collision state to avoid spam
let isColliding = false;
const originalColors = {
    square1: [0.0, 0.0, 1.0], // Blue
    square2: [0.0, 1.0, 0.0]  // Green
};

// Automated movement test - move blue square towards green after 5 seconds
let testStartTime = Date.now();
let autoMoveStarted = false;

// Collision detection loop (called each frame by engine)
function checkCollisions() {
    const events = Deno.core.ops.op_poll_collision_events();
    
    // Check if we have a collision between our two squares
    let collisionDetected = false;
    for (const event of events) {
        if ((event.entity_a === square1 && event.entity_b === square2) ||
            (event.entity_a === square2 && event.entity_b === square1)) {
            collisionDetected = true;
            break;
        }
    }
    
    // Handle collision state changes
    if (collisionDetected && !isColliding) {
        console.log("Collision detected! Squares turning white...");
        
        // Turn both squares white when they collide
        Deno.core.ops.op_set_entity_color(square1, 1.0, 1.0, 1.0); // White
        Deno.core.ops.op_set_entity_color(square2, 1.0, 1.0, 1.0); // White
        isColliding = true;
    } else if (!collisionDetected && isColliding) {
        console.log("Collision ended! Squares returning to original colors...");
        
        // Return to original colors when collision ends
        Deno.core.ops.op_set_entity_color(square1, originalColors.square1[0], originalColors.square1[1], originalColors.square1[2]); // Blue
        Deno.core.ops.op_set_entity_color(square2, originalColors.square2[0], originalColors.square2[1], originalColors.square2[2]); // Green
        isColliding = false;
    }
}

// Make checkCollisions globally available
globalThis.checkCollisions = checkCollisions;