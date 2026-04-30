import java.util.*;

/**
 * Recursive Descent Parser for JSON (implements the PDA via top-down parsing).
 * 
 * Instruction-based PDA model:
 * - SCAN(token): consume token from input
 * - WRITE(symbol): push symbol onto stack
 * - READ(symbol): pop symbol from stack
 * 
 * Grammar (LL(1)):
 * value       → object | array | string | number | TRUE | FALSE | NULL
 * object      → '{' '}' | '{' members '}'
 * members     → pair ( ',' pair )*
 * pair        → string ':' value
 * array       → '[' ']' | '[' elements ']'
 * elements    → value ( ',' value )*
 */
public class JSONParser {
    private TokenStream tokens;
    private List<Instruction> instructions;  // Trace of executed instructions
    private int instructionCounter;
    private int stateCounter;
    private boolean verbose;

    public JSONParser(boolean verbose) {
        this.verbose = verbose;
        this.instructions = new ArrayList<>();
        this.instructionCounter = 0;
        this.stateCounter = 0;
    }

    /**
     * Parse a JSON string and return whether it's valid.
     * Tracks instructions executed as READ/WRITE/SCAN operations.
     */
    public boolean parse(String input) {
        try {
            // Lexical analysis: tokenize the input
            Lexer lexer = new Lexer(input);
            this.tokens = lexer.tokenize();

            if (verbose) {
                System.out.println("Tokens: " + tokens);
            }

            // Syntactic analysis: validate token sequence using PDA
            this.instructions.clear();
            this.instructionCounter = 0;
            this.stateCounter = 0;
            
            boolean result = parseValue();

            // After parsing the value, we should be at EOF
            if (result && tokens.current() != Token.EOF) {
                return false;
            }

            // Record final ACCEPT instruction
            if (result) {
                recordInstruction(Instruction.Operation.SCAN, "EOF", "q_accept");
            }

            return result;
        } catch (JsonLexerException e) {
            if (verbose) {
                System.err.println("Lexer error: " + e.getMessage());
            }
            return false;
        } catch (Exception e) {
            if (verbose) {
                System.err.println("Parser error: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Record an instruction in the PDA trace.
     */
    private void recordInstruction(Instruction.Operation operation, String arg, String nextState) {
        String currentState = "q" + instructionCounter;
        String state = currentState.isEmpty() ? "q0" : currentState;
        instructions.add(new Instruction(state, operation, arg, nextState));
        instructionCounter++;  // Increment for next instruction state
    }

    /**
     * Tries to match a specific token and advance.
     */
    private boolean match(Token expected) {
        if (tokens.current() == expected) {
            // Record SCAN instruction
            recordInstruction(Instruction.Operation.SCAN, expected.toString(), "q" + stateCounter);
            tokens.advance();
            return true;
        }
        return false;
    }

    /**
     * Expects a token and throws error if not found.
     */
    private boolean consume(Token expected, String message) {
        if (tokens.current() == expected) {
            // Record SCAN instruction
            recordInstruction(Instruction.Operation.SCAN, expected.toString(), "q" + stateCounter);
            tokens.advance();
            return true;
        }
        throw new JsonParseException(message + " (got " + tokens.current() + ")");
    }

    // ========== Grammar productions ==========

    private boolean parseValue() {
        // Record READ(VALUE) instruction
        recordInstruction(Instruction.Operation.READ, "VALUE", "q" + stateCounter);
        
        switch (tokens.current()) {
            case LBRACE:
                // Record WRITE instructions for OBJECT production
                recordInstruction(Instruction.Operation.WRITE, "OBJECT", "q" + stateCounter);
                return parseObject();
            case LBRACKET:
                recordInstruction(Instruction.Operation.WRITE, "ARRAY", "q" + stateCounter);
                return parseArray();
            case STRING:
                recordInstruction(Instruction.Operation.WRITE, "STRING", "q" + stateCounter);
                tokens.advance();
                return true;
            case NUMBER:
                recordInstruction(Instruction.Operation.WRITE, "NUMBER", "q" + stateCounter);
                tokens.advance();
                return true;
            case TRUE:
                recordInstruction(Instruction.Operation.WRITE, "TRUE", "q" + stateCounter);
                tokens.advance();
                return true;
            case FALSE:
                recordInstruction(Instruction.Operation.WRITE, "FALSE", "q" + stateCounter);
                tokens.advance();
                return true;
            case NULL:
                recordInstruction(Instruction.Operation.WRITE, "NULL", "q" + stateCounter);
                tokens.advance();
                return true;
            default:
                return false;
        }
    }

    private boolean parseObject() {
        recordInstruction(Instruction.Operation.WRITE, "LBRACE", "q" + stateCounter);
        if (!consume(Token.LBRACE, "Expected '{'")) {
            return false;
        }

        if (tokens.current() == Token.RBRACE) {
            // Empty object: {}
            recordInstruction(Instruction.Operation.WRITE, "RBRACE", "q" + stateCounter);
            tokens.advance();
            recordInstruction(Instruction.Operation.READ, "OBJECT", "q" + stateCounter);
            return true;
        }

        // Non-empty object: { members }
        if (!parseMembers()) {
            return false;
        }

        recordInstruction(Instruction.Operation.WRITE, "RBRACE", "q" + stateCounter);
        if (!consume(Token.RBRACE, "Expected '}'")) {
            return false;
        }

        recordInstruction(Instruction.Operation.READ, "OBJECT", "q" + stateCounter);
        return true;
    }

    private boolean parseMembers() {
        recordInstruction(Instruction.Operation.WRITE, "MEMBERS", "q" + stateCounter);
        if (!parsePair()) {
            return false;
        }

        while (tokens.current() == Token.COMMA) {
            recordInstruction(Instruction.Operation.SCAN, "COMMA", "q" + stateCounter);
            tokens.advance();
            if (!parsePair()) {
                return false;
            }
        }

        recordInstruction(Instruction.Operation.READ, "MEMBERS", "q" + stateCounter);
        return true;
    }

    private boolean parsePair() {
        recordInstruction(Instruction.Operation.WRITE, "PAIR", "q" + stateCounter);
        if (tokens.current() != Token.STRING) {
            return false;
        }
        recordInstruction(Instruction.Operation.SCAN, "STRING", "q" + stateCounter);
        tokens.advance();

        recordInstruction(Instruction.Operation.SCAN, "COLON", "q" + stateCounter);
        if (!consume(Token.COLON, "Expected ':'")) {
            return false;
        }

        if (!parseValue()) {
            return false;
        }

        recordInstruction(Instruction.Operation.READ, "PAIR", "q" + stateCounter);
        return true;
    }

    private boolean parseArray() {
        recordInstruction(Instruction.Operation.WRITE, "LBRACKET", "q" + stateCounter);
        if (!consume(Token.LBRACKET, "Expected '['")) {
            return false;
        }

        if (tokens.current() == Token.RBRACKET) {
            // Empty array: []
            recordInstruction(Instruction.Operation.WRITE, "RBRACKET", "q" + stateCounter);
            tokens.advance();
            recordInstruction(Instruction.Operation.READ, "ARRAY", "q" + stateCounter);
            return true;
        }

        // Non-empty array: [ elements ]
        if (!parseElements()) {
            return false;
        }

        recordInstruction(Instruction.Operation.WRITE, "RBRACKET", "q" + stateCounter);
        if (!consume(Token.RBRACKET, "Expected ']'")) {
            return false;
        }

        recordInstruction(Instruction.Operation.READ, "ARRAY", "q" + stateCounter);
        return true;
    }

    private boolean parseElements() {
        recordInstruction(Instruction.Operation.WRITE, "ELEMENTS", "q" + stateCounter);
        if (!parseValue()) {
            return false;
        }

        while (tokens.current() == Token.COMMA) {
            recordInstruction(Instruction.Operation.SCAN, "COMMA", "q" + stateCounter);
            tokens.advance();
            if (!parseValue()) {
                return false;
            }
        }

        recordInstruction(Instruction.Operation.READ, "ELEMENTS", "q" + stateCounter);
        return true;
    }

    /**
     * Get the instruction trace (PDA program execution).
     */
    public List<Instruction> getInstructionTrace() {
        return instructions;
    }

    /**
     * Get a human-readable derivation sequence from instruction trace.
     */
    public String getDerivationSequence() {
        StringBuilder sb = new StringBuilder();
        for (Instruction instr : instructions) {
            if (instr.getOperation() != Instruction.Operation.WRITE && 
                instr.getOperation() != Instruction.Operation.READ) {
                continue;  // Only show significant operations
            }
            if (sb.length() > 0) sb.append(" → ");
            sb.append(instr.getOperation()).append(" ").append(instr.getArgument());
        }
        return sb.toString();
    }
}

class JsonParseException extends RuntimeException {
    public JsonParseException(String message) {
        super(message);
    }
}
