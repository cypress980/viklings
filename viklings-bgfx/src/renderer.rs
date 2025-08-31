use bgfx_rs::static_lib::*;
use bytemuck::{Pod, Zeroable};
use log::info;
use winit::{
    raw_window_handle::{HasDisplayHandle, HasWindowHandle},
    window::Window,
};

#[derive(Clone, Copy, Debug)]
struct BgfxError(pub &'static str);

impl std::fmt::Display for BgfxError {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "bgfx error: {}", self.0)
    }
}

impl std::error::Error for BgfxError {}

#[repr(C)]
#[derive(Clone, Copy, Debug, Pod, Zeroable)]
struct Vertex {
    x: f32,
    y: f32,
    z: f32,
    r: u32, // ABGR color format
}

pub struct Renderer {
    // For prototype phase, focus on bgfx initialization
    // Shader compilation will be documented as next phase requirement
}

impl Renderer {
    pub fn new(window: &Window) -> Result<Self, Box<dyn std::error::Error>> {
        info!("Initializing bgfx renderer");

        // Get window and display handles
        let window_handle = window.window_handle()?.as_raw();
        let display_handle = window.display_handle()?.as_raw();

        // Initialize bgfx
        let mut init = Init::new();
        init.platform_data.nwh = get_window_ptr(&window_handle);
        init.platform_data.ndt = get_display_ptr(&display_handle);
        
        // Set resolution
        let size = window.inner_size();
        init.resolution.width = size.width;
        init.resolution.height = size.height;
        init.resolution.reset = ResetFlags::VSYNC.bits();

        if !init(&init) {
            return Err(Box::new(BgfxError("Failed to initialize bgfx")));
        }

        info!("bgfx initialized successfully");
        info!("Renderer: {}", get_renderer_name(get_renderer_type()));

        // Set debug options
        set_debug(DebugFlags::TEXT.bits());

        info!("Renderer initialization complete - focus on bgfx context validation");
        info!("Note: Triangle rendering requires bgfx shader compilation tools");

        Ok(Renderer {})
    }


    pub fn resize(&mut self, width: u32, height: u32) {
        info!("Resizing renderer to {}x{}", width, height);
        reset(width, height, ResetFlags::VSYNC.bits(), TextureFormat::Count);
    }

    pub fn render(&mut self, _delta_time: f32) {
        // Set view 0 clear state - this validates bgfx is working
        set_view_clear(
            0,
            ClearFlags::COLOR.bits() | ClearFlags::DEPTH.bits(),
            0x6495edff, // Cornflower blue (classic DirectX clear color)
            1.0,
            0,
        );

        // For prototype: just clear the screen to validate bgfx initialization
        // Triangle rendering would require compiled shaders
        
        // Touch view to ensure it gets processed
        touch(0);
        
        // Advance to next frame
        frame(false);
    }
}

impl Drop for Renderer {
    fn drop(&mut self) {
        info!("Cleaning up renderer resources");
        shutdown();
        info!("bgfx shutdown complete");
    }
}

// Platform-specific window handle extraction
fn get_window_ptr(handle: &winit::raw_window_handle::RawWindowHandle) -> *mut std::ffi::c_void {
    use winit::raw_window_handle::RawWindowHandle;
    match handle {
        #[cfg(target_os = "macos")]
        RawWindowHandle::AppKit(handle) => handle.ns_window.as_ptr(),
        #[cfg(target_os = "windows")]
        RawWindowHandle::Win32(handle) => handle.hwnd.as_ptr(),
        #[cfg(target_os = "linux")]
        RawWindowHandle::Xlib(handle) => handle.window as *mut std::ffi::c_void,
        _ => std::ptr::null_mut(),
    }
}

fn get_display_ptr(handle: &winit::raw_window_handle::RawDisplayHandle) -> *mut std::ffi::c_void {
    use winit::raw_window_handle::RawDisplayHandle;
    match handle {
        #[cfg(target_os = "linux")]
        RawDisplayHandle::Xlib(handle) => handle.display.unwrap().as_ptr(),
        _ => std::ptr::null_mut(),
    }
}