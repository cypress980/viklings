use std::collections::{HashMap, VecDeque};
use log::{debug, info, error};

/// Terminal command handler function type
pub type CommandHandler = fn(&[String]) -> String;

/// Terminal history entry
#[derive(Debug, Clone)]
pub struct HistoryEntry {
    pub command: String,
    pub output: String,
    pub timestamp: std::time::Instant,
}

/// Game terminal/console system
pub struct Terminal {
    /// Command history (input and output)
    history: VecDeque<HistoryEntry>,
    /// Current input line
    current_input: String,
    /// Cursor position in current input
    cursor_pos: usize,
    /// Whether terminal is visible
    visible: bool,
    /// Maximum history entries to keep
    max_history: usize,
    /// Registered commands
    commands: HashMap<String, CommandHandler>,
    /// Command history for up/down arrow navigation
    command_history: VecDeque<String>,
    /// Current position in command history
    history_pos: Option<usize>,
}

impl Terminal {
    pub fn new() -> Self {
        let mut terminal = Self {
            history: VecDeque::new(),
            current_input: String::new(),
            cursor_pos: 0,
            visible: false,
            max_history: 100,
            commands: HashMap::new(),
            command_history: VecDeque::new(),
            history_pos: None,
        };

        // Register built-in commands
        terminal.register_builtin_commands();
        terminal
    }

    pub fn is_visible(&self) -> bool {
        self.visible
    }

    pub fn toggle(&mut self) {
        self.visible = !self.visible;
        if self.visible {
            debug!("Terminal opened");
        } else {
            debug!("Terminal closed");
        }
    }

    pub fn show(&mut self) {
        self.visible = true;
    }

    pub fn hide(&mut self) {
        self.visible = false;
    }

    /// Register a command handler
    pub fn register_command(&mut self, name: String, handler: CommandHandler) {
        self.commands.insert(name.clone(), handler);
        debug!("Registered terminal command: {}", name);
    }

    /// Execute a command
    pub fn execute_command(&mut self, command_line: String) -> String {
        let command_line = command_line.trim();
        if command_line.is_empty() {
            return String::new();
        }

        // Add to command history
        if self.command_history.is_empty() || self.command_history.back() != Some(&command_line.to_string()) {
            self.command_history.push_back(command_line.to_string());
            if self.command_history.len() > self.max_history {
                self.command_history.pop_front();
            }
        }

        // Parse command and arguments
        let parts: Vec<String> = command_line.split_whitespace()
            .map(|s| s.to_string())
            .collect();
        
        if parts.is_empty() {
            return String::new();
        }

        let command = &parts[0];
        let args = &parts[1..];

        // Execute command
        let output = if let Some(handler) = self.commands.get(command) {
            handler(args)
        } else {
            format!("Unknown command: {}. Type 'help' for available commands.", command)
        };

        // Add to history
        let entry = HistoryEntry {
            command: command_line.to_string(),
            output: output.clone(),
            timestamp: std::time::Instant::now(),
        };

        self.add_history_entry(entry);
        output
    }

    /// Add entry to terminal history
    fn add_history_entry(&mut self, entry: HistoryEntry) {
        self.history.push_back(entry);
        if self.history.len() > self.max_history {
            self.history.pop_front();
        }
    }

    /// Handle character input
    pub fn handle_char_input(&mut self, c: char) {
        if !self.visible {
            return;
        }

        match c {
            '\n' | '\r' => {
                // Execute command
                let command = self.current_input.clone();
                self.current_input.clear();
                self.cursor_pos = 0;
                self.history_pos = None;
                self.execute_command(command);
            }
            '\x08' => {
                // Backspace
                if self.cursor_pos > 0 {
                    self.current_input.remove(self.cursor_pos - 1);
                    self.cursor_pos -= 1;
                }
            }
            c if c.is_ascii_graphic() || c == ' ' => {
                // Regular character
                self.current_input.insert(self.cursor_pos, c);
                self.cursor_pos += 1;
            }
            _ => {
                // Ignore other characters
            }
        }
    }

    /// Handle special key input (arrows, etc.)
    pub fn handle_key_input(&mut self, key: TerminalKey) {
        if !self.visible {
            return;
        }

        match key {
            TerminalKey::ArrowUp => {
                if let Some(pos) = self.history_pos {
                    if pos > 0 {
                        self.history_pos = Some(pos - 1);
                    }
                } else if !self.command_history.is_empty() {
                    self.history_pos = Some(self.command_history.len() - 1);
                }

                if let Some(pos) = self.history_pos {
                    if let Some(cmd) = self.command_history.get(pos) {
                        self.current_input = cmd.clone();
                        self.cursor_pos = self.current_input.len();
                    }
                }
            }
            TerminalKey::ArrowDown => {
                if let Some(pos) = self.history_pos {
                    if pos < self.command_history.len() - 1 {
                        self.history_pos = Some(pos + 1);
                        if let Some(cmd) = self.command_history.get(pos + 1) {
                            self.current_input = cmd.clone();
                            self.cursor_pos = self.current_input.len();
                        }
                    } else {
                        self.history_pos = None;
                        self.current_input.clear();
                        self.cursor_pos = 0;
                    }
                }
            }
            TerminalKey::ArrowLeft => {
                if self.cursor_pos > 0 {
                    self.cursor_pos -= 1;
                }
            }
            TerminalKey::ArrowRight => {
                if self.cursor_pos < self.current_input.len() {
                    self.cursor_pos += 1;
                }
            }
        }
    }

    /// Get current input line
    pub fn get_current_input(&self) -> &str {
        &self.current_input
    }

    /// Get cursor position
    pub fn get_cursor_pos(&self) -> usize {
        self.cursor_pos
    }

    /// Get recent history for display
    pub fn get_display_history(&self, max_lines: usize) -> Vec<String> {
        let mut lines = Vec::new();
        
        // Add history entries
        for entry in self.history.iter().rev().take(max_lines - 1) {
            if !entry.output.is_empty() {
                lines.push(format!("> {}", entry.command));
                lines.push(entry.output.clone());
            }
        }
        
        lines.reverse();
        lines
    }

    /// Register built-in commands
    fn register_builtin_commands(&mut self) {
        // Help command
        self.commands.insert("help".to_string(), |_args| {
            "Available commands:\n\
             - help: Show this help\n\
             - clear: Clear terminal\n\
             - fps [target]: Get or set target FPS\n\
             - stats: Show engine statistics\n\
             - quit: Close terminal".to_string()
        });

        // Clear command
        self.commands.insert("clear".to_string(), |_args| {
            "".to_string() // Special case handled by UI
        });

        // Quit command
        self.commands.insert("quit".to_string(), |_args| {
            "Terminal closed.".to_string()
        });
    }
}

/// Terminal key events
#[derive(Debug, Clone, PartialEq)]
pub enum TerminalKey {
    ArrowUp,
    ArrowDown,
    ArrowLeft,
    ArrowRight,
}