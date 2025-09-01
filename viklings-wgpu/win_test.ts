// win_test.ts - Direct win test to check flashing
console.log("=== DIRECT WIN TEST - Testing Flashing Effect ===");

// Move player directly to goal position to test win condition
console.log("Moving player directly to goal to test win condition and flashing...");

// Move player to goal position (350, 250)
const moveSuccess = Deno.core.ops.op_set_entity_position(player, 350, 250);

if (moveSuccess) {
    console.log("SUCCESS: Player moved to goal position (350, 250)");
    console.log("EXPECTED: Win condition should trigger now!");
    console.log("EXPECTED: Player should start flashing blue/white and become uncontrollable");
    console.log("Test will exit automatically in 3 seconds...");
} else {
    console.log("ERROR: Failed to move player");
}