use std::time::{Instant, Duration};

pub struct Timer {
    pub start_time: Instant,
    pub frame_count: u64,
    pub screenshot_taken: bool,
}

impl Timer {
    pub fn new() -> Self {
        Self {
            start_time: Instant::now(),
            frame_count: 0,
            screenshot_taken: false,
        }
    }

    pub fn elapsed(&self) -> Duration {
        self.start_time.elapsed()
    }

    pub fn increment_frame(&mut self) {
        self.frame_count += 1;
    }

    pub fn reset(&mut self) {
        self.start_time = Instant::now();
        self.frame_count = 0;
    }

    pub fn fps(&self) -> f64 {
        let elapsed = self.elapsed();
        if elapsed.as_secs_f64() > 0.0 {
            self.frame_count as f64 / elapsed.as_secs_f64()
        } else {
            0.0
        }
    }
}