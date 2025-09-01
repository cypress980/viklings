use std::time::{Duration, Instant};
use std::thread;

/// Frame rate limiter that ensures consistent frame pacing
pub struct FrameLimiter {
    target_fps: f32,
    frame_duration: Duration,
}

impl FrameLimiter {
    pub fn new(target_fps: f32) -> Self {
        Self {
            target_fps,
            frame_duration: Duration::from_secs_f32(1.0 / target_fps),
        }
    }

    /// Sleep until it's time for the next frame
    pub fn sync(&self, last_frame_time: Instant) {
        let elapsed = last_frame_time.elapsed();
        if elapsed < self.frame_duration {
            let sleep_duration = self.frame_duration - elapsed;
            thread::sleep(sleep_duration);
        }
    }

    /// Get the target frame duration
    pub fn get_frame_duration(&self) -> Duration {
        self.frame_duration
    }

    /// Get the target FPS
    pub fn get_target_fps(&self) -> f32 {
        self.target_fps
    }

    /// Calculate actual FPS from elapsed time
    pub fn calculate_fps(elapsed_time: f32) -> f32 {
        if elapsed_time > 0.0 {
            1.0 / elapsed_time
        } else {
            0.0
        }
    }
}