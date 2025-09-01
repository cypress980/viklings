// square_game.js - Complete square game implementation using actual engine APIs
// DEV-007: Complete Square Game Implementation

// Game state
let gameWon = false;
let player = 0;
let goal = 0;
let uiElements = {};

// Initialize game
function initGame() {
    console.log("Initializing Square Game...");
    
    // Create player entity - blue square (30x30) with smaller center hitbox (10x10)
    player = Deno.core.ops.op_create_entity();
    Deno.core.ops.op_add_position(player, 50, 50);
    Deno.core.ops.op_add_render(player, {
        width: 30,
        height: 30,
        color: [0.0, 0.0, 1.0], // Blue
        shape: "square"
    });
    Deno.core.ops.op_add_hitbox(player, {
        width: 10,
        height: 10,
        offset_x: 0,
        offset_y: 0,
        layer: 0
    });
    Deno.core.ops.op_add_controllable(player, {
        movement_speed: 120.0,
        input_type: "arrow_keys",
        bounds: "screen"
    });
    
    // Create goal entity - green square (40x40) with center hitbox (20x20)
    goal = Deno.core.ops.op_create_entity();
    Deno.core.ops.op_add_position(goal, 350, 250);
    Deno.core.ops.op_add_render(goal, {
        width: 40,
        height: 40,
        color: [0.0, 1.0, 0.0], // Green
        shape: "square"
    });
    Deno.core.ops.op_add_hitbox(goal, {
        width: 20,
        height: 20,
        offset_x: 0,
        offset_y: 0,
        layer: 0
    });
    
    // Show initial UI
    showInitialUI();
    
    console.log("Game initialized: Player=" + player + ", Goal=" + goal);
}

function showInitialUI() {
    uiElements.title = Deno.core.ops.op_show_text("Simple Square Game", 400, 30, {
        fontSize: 20,
        color: [1.0, 1.0, 1.0],
        alignment: "center"
    });
    
    uiElements.instructions = Deno.core.ops.op_show_text("Use arrow keys to move the blue square", 400, 60, {
        fontSize: 14,
        color: [0.8, 0.8, 0.8],
        alignment: "center"
    });
    
    uiElements.objective = Deno.core.ops.op_show_text("Get your center into the green goal to win!", 400, 85, {
        fontSize: 14,
        color: [0.8, 0.8, 0.8],
        alignment: "center"
    });
    
    uiElements.reset = Deno.core.ops.op_show_text("Press R to restart", 10, 570, {
        fontSize: 12,
        color: [0.6, 0.6, 0.6],
        alignment: "left"
    });
}

function checkWinCondition() {
    const collisionEvents = Deno.core.ops.op_poll_collision_events();
    
    for (const event of collisionEvents) {
        if ((event.entity_a === player && event.entity_b === goal) ||
            (event.entity_a === goal && event.entity_b === player)) {
            
            if (!gameWon) {
                gameWon = true;
                handleWin();
            }
        }
    }
}

function handleWin() {
    console.log("Player wins! Goal reached.");
    
    // Clear instruction UI
    Deno.core.ops.op_hide_text(uiElements.instructions);
    Deno.core.ops.op_hide_text(uiElements.objective);
    
    // Show win message
    uiElements.winTitle = Deno.core.ops.op_show_text("YOU WIN!", 400, 200, {
        fontSize: 32,
        color: [1.0, 1.0, 0.0], // Yellow
        alignment: "center"
    });
    
    uiElements.winMessage = Deno.core.ops.op_show_text("Great job! You reached the goal.", 400, 240, {
        fontSize: 16,
        color: [1.0, 1.0, 1.0],
        alignment: "center"
    });
    
    uiElements.restartPrompt = Deno.core.ops.op_show_text("Press R to play again", 400, 270, {
        fontSize: 14,
        color: [0.8, 0.8, 0.8],
        alignment: "center"
    });
}

function resetGame() {
    console.log("Resetting game...");
    
    // Reset game state
    gameWon = false;
    
    // Reset player position
    Deno.core.ops.op_set_entity_position(player, 50, 50);
    
    // Clear all UI and show initial UI
    Deno.core.ops.op_clear_ui();
    showInitialUI();
    
    console.log("Game reset complete");
}

// Game loop function (called by engine each frame)
function gameUpdate() {
    if (!gameWon) {
        checkWinCondition();
    }
}

// Start the game
initGame();