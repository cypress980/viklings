use std::time::Instant;

/// High-precision game timer that tracks frame time and provides delta time
pub struct GameTimer {
    last_loop_time: Instant,
    start_time: Instant,
}

impl GameTimer {
    pub fn new() -> Self {
        let now = Instant::now();
        Self {
            last_loop_time: now,
            start_time: now,
        }
    }

    /// Initialize the timer (sets the initial time reference)
    pub fn init(&mut self) {
        let now = Instant::now();
        self.last_loop_time = now;
        self.start_time = now;
    }

    /// Get the current time as seconds since epoch (high precision)
    pub fn get_time(&self) -> f64 {
        self.last_loop_time.elapsed().as_secs_f64()
    }

    /// Get time since game started in seconds
    pub fn get_game_time(&self) -> f64 {
        self.start_time.elapsed().as_secs_f64()
    }

    /// Get elapsed time since last call and update the loop time
    pub fn get_elapsed_time(&mut self) -> f32 {
        let now = Instant::now();
        let elapsed = now.duration_since(self.last_loop_time).as_secs_f32();
        self.last_loop_time = now;
        elapsed
    }

    /// Get the last loop time instant
    pub fn get_last_loop_time(&self) -> Instant {
        self.last_loop_time
    }

    /// Get time since game started in milliseconds (for scheduling)
    pub fn get_game_time_millis(&self) -> u64 {
        self.start_time.elapsed().as_millis() as u64
    }

    /// Get elapsed time without updating the timer (peek)
    pub fn peek_elapsed_time(&self) -> f32 {
        Instant::now().duration_since(self.last_loop_time).as_secs_f32()
    }
}