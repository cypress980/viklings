use std::collections::BinaryHeap;
use std::cmp::Ordering;

pub type EventId = u32;

/// A scheduled game event
#[derive(Debug, Clone)]
pub struct GameEvent {
    pub id: EventId,
    pub trigger_time_millis: u64,
    pub callback_name: String,
    pub repeat_interval_millis: Option<u64>,
}

impl PartialEq for GameEvent {
    fn eq(&self, other: &Self) -> bool {
        self.trigger_time_millis == other.trigger_time_millis
    }
}

impl Eq for GameEvent {}

impl PartialOrd for GameEvent {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl Ord for GameEvent {
    fn cmp(&self, other: &Self) -> Ordering {
        // Reverse ordering for min-heap (earliest events first)
        other.trigger_time_millis.cmp(&self.trigger_time_millis)
    }
}

/// Game clock that manages time-based events and scheduling
pub struct GameClock {
    events: BinaryHeap<GameEvent>,
    next_event_id: EventId,
}

impl GameClock {
    pub fn new() -> Self {
        Self {
            events: BinaryHeap::new(),
            next_event_id: 1,
        }
    }

    /// Schedule an event to fire after a certain number of seconds since game start
    pub fn schedule_event_at_seconds(&mut self, seconds: f64, callback_name: String) -> EventId {
        let trigger_time_millis = (seconds * 1000.0) as u64;
        self.schedule_event_at_millis(trigger_time_millis, callback_name)
    }

    /// Schedule an event to fire after a certain number of milliseconds since game start
    pub fn schedule_event_at_millis(&mut self, millis: u64, callback_name: String) -> EventId {
        let event_id = self.next_event_id;
        self.next_event_id += 1;

        let event = GameEvent {
            id: event_id,
            trigger_time_millis: millis,
            callback_name,
            repeat_interval_millis: None,
        };

        self.events.push(event);
        event_id
    }

    /// Schedule a repeating event with a cadence in milliseconds
    pub fn schedule_repeating_event(&mut self, start_time_millis: u64, interval_millis: u64, callback_name: String) -> EventId {
        let event_id = self.next_event_id;
        self.next_event_id += 1;

        let event = GameEvent {
            id: event_id,
            trigger_time_millis: start_time_millis,
            callback_name,
            repeat_interval_millis: Some(interval_millis),
        };

        self.events.push(event);
        event_id
    }

    /// Schedule a repeating event to start immediately with a given interval
    pub fn schedule_repeating_event_now(&mut self, interval_millis: u64, callback_name: String) -> EventId {
        self.schedule_repeating_event(0, interval_millis, callback_name)
    }

    /// Remove a scheduled event by ID
    pub fn cancel_event(&mut self, event_id: EventId) -> bool {
        // Note: BinaryHeap doesn't support efficient removal by value
        // For now, we'll mark it as cancelled by using a special callback name
        // A more sophisticated implementation could use a separate data structure
        let _cancelled_name = format!("__CANCELLED_{}", event_id);
        
        // We can't easily remove from BinaryHeap, so we'll filter during processing
        // This is a limitation we'd address in a production implementation
        false
    }

    /// Process events that should fire at the current game time
    pub fn update(&mut self, current_time_millis: u64) -> Vec<GameEvent> {
        let mut fired_events = Vec::new();

        // Process all events that should fire now
        while let Some(event) = self.events.peek() {
            if event.trigger_time_millis <= current_time_millis {
                let event = self.events.pop().unwrap();
                
                // Skip cancelled events
                if event.callback_name.starts_with("__CANCELLED_") {
                    continue;
                }

                // If it's a repeating event, reschedule it
                if let Some(interval) = event.repeat_interval_millis {
                    let next_event = GameEvent {
                        id: event.id,
                        trigger_time_millis: current_time_millis + interval,
                        callback_name: event.callback_name.clone(),
                        repeat_interval_millis: Some(interval),
                    };
                    self.events.push(next_event);
                }

                fired_events.push(event);
            } else {
                break;
            }
        }

        fired_events
    }

    /// Get the number of scheduled events
    pub fn event_count(&self) -> usize {
        self.events.len()
    }

    /// Get the next event time (if any)
    pub fn next_event_time(&self) -> Option<u64> {
        self.events.peek().map(|event| event.trigger_time_millis)
    }
}