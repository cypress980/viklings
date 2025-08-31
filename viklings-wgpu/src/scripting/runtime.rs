use deno_core::{JsRuntime, RuntimeOptions, extension};
use super::operations::*;

// Extension containing our ops
extension!(
    triangle_ops,
    ops = [
        op_move_triangle_to,
        op_init_ecs,
        op_create_entity,
        op_add_position,
        op_add_render,
        op_add_controllable,
        op_add_hitbox,
        op_poll_collision_events,
        op_set_entity_color,
        op_set_entity_position
    ]
);

pub struct ScriptingEngine {
    pub runtime: JsRuntime,
}

impl ScriptingEngine {
    pub fn new() -> Self {
        let mut runtime = JsRuntime::new(RuntimeOptions {
            extensions: vec![triangle_ops::init_ops()],
            ..Default::default()
        });

        // Initialize ECS
        let test_script = r#"
            console.log("TypeScript runtime initialized!");
            
            // Initialize ECS
            const ecsReady = Deno.core.ops.op_init_ecs();
            console.log("ECS initialized:", ecsReady);
        "#;

        match runtime.execute_script("<init>", test_script) {
            Ok(_) => println!("TypeScript ECS initialization successful"),
            Err(e) => eprintln!("TypeScript execution error: {}", e),
        }

        Self { runtime }
    }

    pub fn execute_script(&mut self, name: &'static str, source: String) -> Result<(), Box<dyn std::error::Error>> {
        self.runtime.execute_script(name, source)?;
        Ok(())
    }

    pub fn load_collision_test(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        let collision_script = std::fs::read_to_string("collision_demo.ts")
            .expect("Failed to read collision_demo.ts");
        
        self.execute_script("<collision_test>", collision_script)?;
        Ok(())
    }

    pub fn call_collision_check(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        self.execute_script("<collision_check>", "checkCollisions();".to_string())?;
        Ok(())
    }
}