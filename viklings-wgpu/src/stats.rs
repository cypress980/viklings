use std::time::{Duration, Instant};
use std::collections::VecDeque;

/// Game engine performance statistics
#[derive(Debug, Clone)]
pub struct EngineStats {
    pub fps: f32,
    pub avg_frame_time: f32,
    pub min_frame_time: f32,
    pub max_frame_time: f32,
    pub total_frames: u64,
    pub start_time: Instant,
}

pub struct StatsCollector {
    frame_times: VecDeque<Duration>,
    last_frame_time: Instant,
    total_frames: u64,
    start_time: Instant,
    max_samples: usize,
}

impl StatsCollector {
    pub fn new() -> Self {
        let now = Instant::now();
        Self {
            frame_times: VecDeque::with_capacity(120), // Track last 2 seconds at 60fps
            last_frame_time: now,
            total_frames: 0,
            start_time: now,
            max_samples: 120,
        }
    }

    /// Call this at the end of each frame
    pub fn record_frame(&mut self) {
        let now = Instant::now();
        let frame_time = now.duration_since(self.last_frame_time);
        
        self.frame_times.push_back(frame_time);
        if self.frame_times.len() > self.max_samples {
            self.frame_times.pop_front();
        }
        
        self.last_frame_time = now;
        self.total_frames += 1;
    }

    /// Get current performance statistics
    pub fn get_stats(&self) -> EngineStats {
        if self.frame_times.is_empty() {
            return EngineStats {
                fps: 0.0,
                avg_frame_time: 0.0,
                min_frame_time: 0.0,
                max_frame_time: 0.0,
                total_frames: self.total_frames,
                start_time: self.start_time,
            };
        }

        let total_time: Duration = self.frame_times.iter().sum();
        let avg_frame_time = total_time.as_secs_f32() / self.frame_times.len() as f32;
        let fps = if avg_frame_time > 0.0 { 1.0 / avg_frame_time } else { 0.0 };
        
        let min_frame_time = self.frame_times.iter().min().unwrap().as_secs_f32();
        let max_frame_time = self.frame_times.iter().max().unwrap().as_secs_f32();

        EngineStats {
            fps,
            avg_frame_time,
            min_frame_time,
            max_frame_time,
            total_frames: self.total_frames,
            start_time: self.start_time,
        }
    }

    /// Get instantaneous FPS (based on last frame only)
    pub fn get_instant_fps(&self) -> f32 {
        if let Some(last_frame_time) = self.frame_times.back() {
            let frame_time_secs = last_frame_time.as_secs_f32();
            if frame_time_secs > 0.0 {
                1.0 / frame_time_secs
            } else {
                0.0
            }
        } else {
            0.0
        }
    }

    /// Get average FPS over the sample window
    pub fn get_avg_fps(&self) -> f32 {
        self.get_stats().fps
    }

    /// Get uptime in seconds
    pub fn get_uptime(&self) -> f32 {
        self.start_time.elapsed().as_secs_f32()
    }
}