use std::collections::HashMap;
use serde::{Deserialize, Serialize};
use log::debug;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TextOptions {
    #[serde(rename = "fontSize")]
    pub font_size: Option<f32>,
    pub color: Option<[f32; 3]>,
    pub alignment: Option<String>,
}

#[derive(Debug, Clone)]
pub struct UIElement {
    pub id: u32,
    pub text: String,
    pub x: f32,
    pub y: f32,
    pub font_size: f32,
    pub color: [f32; 3],
    pub alignment: String,
    pub visible: bool,
}

pub struct UISystem {
    elements: HashMap<u32, UIElement>,
    next_id: u32,
}

impl UISystem {
    pub fn new() -> Self {
        Self {
            elements: HashMap::new(),
            next_id: 1,
        }
    }

    pub fn show_text(&mut self, text: String, x: f32, y: f32, options: Option<TextOptions>) -> u32 {
        let id = self.next_id;
        self.next_id += 1;

        let options = options.unwrap_or(TextOptions {
            font_size: Some(16.0),
            color: Some([1.0, 1.0, 1.0]),
            alignment: Some("left".to_string()),
        });

        let element = UIElement {
            id,
            text: text.clone(),
            x,
            y,
            font_size: options.font_size.unwrap_or(16.0),
            color: options.color.unwrap_or([1.0, 1.0, 1.0]),
            alignment: options.alignment.unwrap_or_else(|| "left".to_string()),
            visible: true,
        };

        self.elements.insert(id, element);
        debug!("UI: Created text element {} at ({}, {}): '{}'", id, x, y, text);
        id
    }

    pub fn hide_text(&mut self, element_id: u32) -> bool {
        if let Some(element) = self.elements.get_mut(&element_id) {
            element.visible = false;
            debug!("UI: Hidden text element {}", element_id);
            true
        } else {
            false
        }
    }

    pub fn clear_ui(&mut self) -> bool {
        let count = self.elements.len();
        self.elements.clear();
        self.next_id = 1;
        debug!("UI: Cleared {} text elements", count);
        true
    }

    pub fn get_visible_elements(&self) -> Vec<&UIElement> {
        self.elements.values().filter(|e| e.visible).collect()
    }

    pub fn render(&self) {
        // For now, just log visible elements (later can be integrated with actual text rendering)
        let visible_count = self.elements.values().filter(|e| e.visible).count();
        if visible_count > 0 {
            debug!("UI: Rendering {} visible text elements", visible_count);
            for element in self.elements.values().filter(|e| e.visible) {
                debug!("  Text '{}' at ({}, {}) - size: {}, color: {:?}", 
                        element.text, element.x, element.y, element.font_size, element.color);
            }
        }
    }
}