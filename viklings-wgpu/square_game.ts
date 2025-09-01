// square_game.ts - Complete square game implementation using actual engine APIs
// DEV-007: Complete Square Game Implementation

// Game state
let gameWon = false;
let player;
let goal;
let uiElements = {};
let flashTimer = 0;
let playerIsFlashing = false;
let goalRandomizationEventId = null;

// Initialize game
function initGame() {
    // Initialize game engine with 60 FPS target
    Deno.core.ops.op_init_engine(60.0);
    // Create goal entity FIRST so player renders on top
    goal = Deno.core.ops.op_create_entity();
    Deno.core.ops.op_add_position(goal, 350, 250);
    Deno.core.ops.op_add_render(goal, {
        width: 140,
        height: 140,
        color: [0.0, 1.0, 0.0], // Green
        shape: "square" // Note: outline shape not implemented yet, using square
    });
    Deno.core.ops.op_add_hitbox(goal, {
        width: 20,
        height: 20,
        offset_x: 0,
        offset_y: 0,
        layer: 0
    });
    
    // Create player entity SECOND so it renders on top
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
    
    // Show initial UI
    showInitialUI();
    
    // Schedule goal randomization every 2 seconds
    goalRandomizationEventId = Deno.core.ops.op_schedule_repeating_event_now(2000, "randomizeGoalPosition");
    
    // Schedule FPS logging every 10 seconds
    Deno.core.ops.op_schedule_repeating_event_now(10000, "logPerformanceStats");
    
    // Game initialized silently
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
    
    uiElements.objective = Deno.core.ops.op_show_text("Get your square entirely inside the green goal to win!", 400, 85, {
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
    // Player won
    
    // Cancel goal randomization
    if (goalRandomizationEventId !== null) {
        Deno.core.ops.op_cancel_event(goalRandomizationEventId);
        goalRandomizationEventId = null;
    }
    
    // Remove controllable component to stop player movement
    Deno.core.ops.op_remove_controllable(player);
    
    // Start flashing effect
    playerIsFlashing = true;
    flashTimer = 0;
    
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
    // Resetting game
    
    // Reset game state
    gameWon = false;
    playerIsFlashing = false;
    flashTimer = 0;
    
    // Reset player position and color
    Deno.core.ops.op_set_entity_position(player, 50, 50);
    Deno.core.ops.op_set_entity_color(player, 0.0, 0.0, 1.0); // Blue
    
    // Re-add controllable component
    Deno.core.ops.op_add_controllable(player, {
        movement_speed: 120.0,
        input_type: "arrow_keys",
        bounds: "screen"
    });
    
    // Clear all UI and show initial UI
    Deno.core.ops.op_clear_ui();
    showInitialUI();
    
    // Reschedule goal randomization
    goalRandomizationEventId = Deno.core.ops.op_schedule_repeating_event_now(2000, "randomizeGoalPosition");
    
    // Game reset complete
}

// Game loop function (called by engine each frame)
function gameUpdate() {
    // Call test callback if it exists (for automated testing)
    if (typeof globalThis.gameplayTest === 'function') {
        globalThis.gameplayTest();
    }
    
    if (!gameWon) {
        checkWinCondition();
    } else if (playerIsFlashing) {
        // Handle flashing effect
        flashTimer++;
        
        // Flash every 5 frames (about 12 times per second at 60 FPS) - faster and more visible
        if (flashTimer % 5 === 0) {
            const flashState = Math.floor(flashTimer / 5) % 2;
            if (flashState === 0) {
                // Blue
                Deno.core.ops.op_set_entity_color(player, 0.0, 0.0, 1.0);
            } else {
                // White  
                Deno.core.ops.op_set_entity_color(player, 1.0, 1.0, 1.0);
            }
        }
    }
}

// Goal randomization function called by game clock
function randomizeGoalPosition() {
    // Only randomize if game hasn't been won yet
    if (!gameWon) {
        // Window dimensions: 800x600
        // Goal size: 140x140
        // Keep goal fully within window bounds (70px margin on each side)
        const minX = 70;
        const maxX = 800 - 70; // 730
        const minY = 70;  
        const maxY = 600 - 70; // 530
        
        const newX = minX + Math.random() * (maxX - minX);
        const newY = minY + Math.random() * (maxY - minY);
        
        Deno.core.ops.op_set_entity_position(goal, newX, newY);
    }
}

// Performance stats logging function
function logPerformanceStats() {
    const avgFps = Deno.core.ops.op_get_fps();
    const instantFps = Deno.core.ops.op_get_instant_fps();
    const totalFrames = Deno.core.ops.op_get_total_frames();
    const uptime = Deno.core.ops.op_get_uptime();
    const targetFps = Deno.core.ops.op_get_target_fps();
    
    console.log(`PERF: Target: ${targetFps.toFixed(0)} FPS, Avg: ${avgFps.toFixed(1)}, Instant: ${instantFps.toFixed(1)}, Frames: ${totalFrames}, Uptime: ${uptime.toFixed(1)}s`);
    
    // Demonstrate dynamic frame rate changes
    if (uptime > 15 && uptime < 16) {
        console.log("Changing to 120 FPS for performance test...");
        Deno.core.ops.op_set_target_fps(120.0);
    } else if (uptime > 25 && uptime < 26) {
        console.log("Changing to 30 FPS for battery saving mode...");
        Deno.core.ops.op_set_target_fps(30.0);
    }
}

// Start the game
initGame();