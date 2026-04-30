/**
 * Token types for the JSON lexer (DFA).
 * Each token represents a terminal symbol in the JSON grammar.
 */
public enum Token {
    // Structural tokens
    LBRACE,        // {
    RBRACE,        // }
    LBRACKET,      // [
    RBRACKET,      // ]
    COLON,         // :
    COMMA,         // ,

    // Literal tokens
    STRING,        // "..."
    NUMBER,        // 123, -45.67, 1.2e3, etc.
    TRUE,          // true
    FALSE,         // false
    NULL,          // null

    // Special tokens
    EOF,           // End of file
    WHITESPACE     // Ignored during parsing
}

