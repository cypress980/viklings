use viklings_wgpu::application::App;
use log::info;

// Move collision tests to separate module
#[cfg(test)]
mod collision_tests {
    use viklings_wgpu::physics::{CollisionSystem, CollisionEvent};
    use viklings_wgpu::ecs::{EntityManager, Position, Hitbox};

    #[test]
    fn test_aabb_no_overlap() {
        let collision_system = CollisionSystem::new();
        
        // Two rectangles that don't overlap
        let pos1 = Position { x: 0.0, y: 0.0 };
        let hitbox1 = Hitbox { size: [10.0, 10.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 20.0, y: 20.0 };
        let hitbox2 = Hitbox { size: [10.0, 10.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), false);
    }

    #[test]
    fn test_aabb_complete_overlap() {
        let collision_system = CollisionSystem::new();
        
        // Two identical rectangles that completely overlap
        let pos1 = Position { x: 10.0, y: 10.0 };
        let hitbox1 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 10.0, y: 10.0 };
        let hitbox2 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_aabb_partial_overlap() {
        let collision_system = CollisionSystem::new();
        
        // Two rectangles that partially overlap
        let pos1 = Position { x: 0.0, y: 0.0 };
        let hitbox1 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 10.0, y: 10.0 };
        let hitbox2 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_aabb_edge_touching() {
        let collision_system = CollisionSystem::new();
        
        // Two rectangles that touch at the edge (should be considered collision)
        let pos1 = Position { x: 0.0, y: 0.0 };
        let hitbox1 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 20.0, y: 0.0 }; // Right edge of first touches left edge of second
        let hitbox2 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_aabb_just_overlapping() {
        let collision_system = CollisionSystem::new();
        
        // Two rectangles that barely overlap
        let pos1 = Position { x: 0.0, y: 0.0 };
        let hitbox1 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 19.0, y: 0.0 }; // Just overlapping by 1 unit
        let hitbox2 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_aabb_with_offsets() {
        let collision_system = CollisionSystem::new();
        
        // Test collision with offset hitboxes
        let pos1 = Position { x: 0.0, y: 0.0 };
        let hitbox1 = Hitbox { size: [10.0, 10.0], offset: [5.0, 5.0], layer: 0, active: true };
        
        let pos2 = Position { x: 10.0, y: 10.0 };
        let hitbox2 = Hitbox { size: [10.0, 10.0], offset: [-5.0, -5.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_aabb_different_sizes() {
        let collision_system = CollisionSystem::new();
        
        // Small rectangle inside large rectangle
        let pos1 = Position { x: 0.0, y: 0.0 };
        let hitbox1 = Hitbox { size: [50.0, 50.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 5.0, y: 5.0 };
        let hitbox2 = Hitbox { size: [10.0, 10.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_aabb_bounds_calculation() {
        let collision_system = CollisionSystem::new();
        
        let pos = Position { x: 100.0, y: 200.0 };
        let hitbox = Hitbox { size: [40.0, 60.0], offset: [10.0, -5.0], layer: 0, active: true };
        
        let bounds = collision_system.get_aabb_bounds(&pos, &hitbox);
        
        // Center should be position + offset = (110, 195)
        // Half-sizes: width=20, height=30
        assert_eq!(bounds.left, 90.0);   // 110 - 20
        assert_eq!(bounds.right, 130.0); // 110 + 20
        assert_eq!(bounds.top, 165.0);   // 195 - 30
        assert_eq!(bounds.bottom, 225.0); // 195 + 30
    }

    #[test]
    fn test_collision_system_integration() {
        let mut entity_manager = EntityManager::new();
        let mut collision_system = CollisionSystem::new();
        
        // Create two overlapping entities
        let entity1 = entity_manager.create_entity();
        let entity2 = entity_manager.create_entity();
        
        entity_manager.add_position(entity1, Position { x: 0.0, y: 0.0 });
        entity_manager.add_hitbox(entity1, Hitbox { 
            size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true 
        });
        
        entity_manager.add_position(entity2, Position { x: 10.0, y: 10.0 });
        entity_manager.add_hitbox(entity2, Hitbox { 
            size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true 
        });
        
        // Run collision detection
        collision_system.update(&entity_manager);
        
        // Should detect one collision
        let events = collision_system.get_events();
        assert_eq!(events.len(), 1);
        
        // Check that the collision involves both entities (order may vary)
        let collision_entities = (events[0].entity_a, events[0].entity_b);
        assert!(
            (collision_entities.0 == entity1 && collision_entities.1 == entity2) ||
            (collision_entities.0 == entity2 && collision_entities.1 == entity1),
            "Expected collision between entities {} and {}, but got collision between {} and {}",
            entity1, entity2, collision_entities.0, collision_entities.1
        );
    }

    #[test]
    fn test_inactive_hitbox_no_collision() {
        let mut entity_manager = EntityManager::new();
        let mut collision_system = CollisionSystem::new();
        
        // Create two overlapping entities, but make one inactive
        let entity1 = entity_manager.create_entity();
        let entity2 = entity_manager.create_entity();
        
        entity_manager.add_position(entity1, Position { x: 0.0, y: 0.0 });
        entity_manager.add_hitbox(entity1, Hitbox { 
            size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true 
        });
        
        entity_manager.add_position(entity2, Position { x: 10.0, y: 10.0 });
        entity_manager.add_hitbox(entity2, Hitbox { 
            size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: false 
        });
        
        // Run collision detection
        collision_system.update(&entity_manager);
        
        // Should detect no collisions because entity2 hitbox is inactive
        let events = collision_system.get_events();
        assert_eq!(events.len(), 0);
    }

    #[test]
    fn test_negative_coordinates() {
        let collision_system = CollisionSystem::new();
        
        // Test collision detection with negative coordinates
        let pos1 = Position { x: -10.0, y: -10.0 };
        let hitbox1 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: -5.0, y: -5.0 };
        let hitbox2 = Hitbox { size: [20.0, 20.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
    }

    #[test]
    fn test_zero_size_hitbox() {
        let collision_system = CollisionSystem::new();
        
        // Zero-size hitbox at same position should collide with regular hitbox
        let pos1 = Position { x: 5.0, y: 5.0 }; // Inside the second hitbox
        let hitbox1 = Hitbox { size: [0.0, 0.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        let pos2 = Position { x: 0.0, y: 0.0 };
        let hitbox2 = Hitbox { size: [10.0, 10.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos1, &hitbox1, &pos2, &hitbox2), true);
        
        // Zero-size hitbox outside should not collide
        let pos3 = Position { x: 20.0, y: 20.0 }; // Outside the second hitbox
        let hitbox3 = Hitbox { size: [0.0, 0.0], offset: [0.0, 0.0], layer: 0, active: true };
        
        assert_eq!(collision_system.check_collision(&pos3, &hitbox3, &pos2, &hitbox2), false);
    }
}

fn main() {
    env_logger::init();
    
    info!("Viklings wgpu Triangle Prototype - ARCH-002 (Modular)");
    info!("Initializing modular rendering system...");

    App::run();
}