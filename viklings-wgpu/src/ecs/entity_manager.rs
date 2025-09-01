use std::collections::HashMap;
use super::components::*;

pub type EntityId = u32;

// Entity Manager - core of our ECS
pub struct EntityManager {
    next_entity_id: EntityId,
    entities: Vec<EntityId>,
    positions: HashMap<EntityId, Position>,
    renders: HashMap<EntityId, Render>,
    controllables: HashMap<EntityId, Controllable>,
    hitboxes: HashMap<EntityId, Hitbox>,
}

impl EntityManager {
    pub fn new() -> Self {
        Self {
            next_entity_id: 0,
            entities: Vec::new(),
            positions: HashMap::new(),
            renders: HashMap::new(),
            controllables: HashMap::new(),
            hitboxes: HashMap::new(),
        }
    }

    pub fn create_entity(&mut self) -> EntityId {
        let id = self.next_entity_id;
        self.next_entity_id += 1;
        self.entities.push(id);
        id
    }

    pub fn add_position(&mut self, entity: EntityId, position: Position) {
        self.positions.insert(entity, position);
    }

    pub fn add_render(&mut self, entity: EntityId, render: Render) {
        self.renders.insert(entity, render);
    }

    pub fn add_controllable(&mut self, entity: EntityId, controllable: Controllable) {
        self.controllables.insert(entity, controllable);
    }

    pub fn remove_controllable(&mut self, entity: EntityId) {
        self.controllables.remove(&entity);
    }

    pub fn add_hitbox(&mut self, entity: EntityId, hitbox: Hitbox) {
        self.hitboxes.insert(entity, hitbox);
    }

    pub fn get_position_mut(&mut self, entity: EntityId) -> Option<&mut Position> {
        self.positions.get_mut(&entity)
    }

    pub fn get_entities_with_render(&self) -> Vec<(EntityId, &Position, &Render)> {
        let mut entities: Vec<_> = self.renders.keys()
            .filter_map(|&entity_id| {
                self.positions.get(&entity_id).map(|pos| (entity_id, pos, &self.renders[&entity_id]))
            })
            .collect();
        
        // Sort by entity ID to ensure consistent render order (higher IDs render on top)
        entities.sort_by_key(|(entity_id, _, _)| *entity_id);
        entities
    }

    pub fn get_controllable_entities(&self) -> Vec<(EntityId, &Position, &Controllable)> {
        self.controllables.keys()
            .filter_map(|&entity_id| {
                self.positions.get(&entity_id).map(|pos| (entity_id, pos, &self.controllables[&entity_id]))
            })
            .collect()
    }

    pub fn get_entities_with_hitbox(&self) -> Vec<(EntityId, &Position, &Hitbox)> {
        self.hitboxes.keys()
            .filter_map(|&entity_id| {
                self.positions.get(&entity_id).map(|pos| (entity_id, pos, &self.hitboxes[&entity_id]))
            })
            .collect()
    }
    
    pub fn set_entity_color(&mut self, entity: EntityId, color: [f32; 3]) -> bool {
        if let Some(render) = self.renders.get_mut(&entity) {
            render.color = color;
            true
        } else {
            false
        }
    }

    // Getter for controllables (needed by movement system)
    pub fn controllables(&self) -> &HashMap<EntityId, Controllable> {
        &self.controllables
    }
}