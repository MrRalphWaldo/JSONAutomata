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

    // Fixed semantic PDA states
    private static final String Q_START = "q_start";
    private static final String Q_VALUE_ENTRY = "q_value_entry";
    private static final String Q_VALUE_DECISION = "q_value_decision";
    private static final String Q_OBJECT_OPEN = "q_object_open";
    private static final String Q_OBJECT_MEMBERS = "q_object_members";
    private static final String Q_OBJECT_CLOSE = "q_object_close";
    private static final String Q_PAIR_KEY = "q_pair_key";
    private static final String Q_PAIR_COLON = "q_pair_colon";
    private static final String Q_PAIR_VALUE = "q_pair_value";
    private static final String Q_ARRAY_OPEN = "q_array_open";
    private static final String Q_ARRAY_ELEMENTS = "q_array_elements";
    private static final String Q_ARRAY_CLOSE = "q_array_close";
    private static final String Q_ACCEPT = "q_accept";
    private static final String Q_REJECT = "q_reject";

    private TokenStream tokens;
    private List<Instruction> instructions;  // Trace of executed instructions
    private String currentState;
    private boolean verbose;

    public JSONParser(boolean verbose) {
        this.verbose = verbose;
        this.instructions = new ArrayList<>();
        this.currentState = Q_START;
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
            this.currentState = Q_START;

            // Enter value parsing and execute PDA
            recordInstruction(Instruction.Operation.WRITE, "VALUE", Q_VALUE_ENTRY);
            boolean result = parseValue(Q_ACCEPT);

            // After parsing the value, we should be at EOF
            if (result && tokens.current() != Token.EOF) {
                return false;
            }

            // Record final ACCEPT instruction
            if (result) {
                recordInstruction(Instruction.Operation.SCAN, "EOF", Q_ACCEPT);
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
     * Uses currentState and advances to nextState.
     */
    private void recordInstruction(Instruction.Operation operation, String arg, String nextState) {
        instructions.add(new Instruction(currentState, operation, arg, nextState));
        currentState = nextState;
    }

    /**
     * Tries to match a specific token and advance.
     */
    private boolean match(Token expected, String nextState) {
        if (tokens.current() == expected) {
            recordInstruction(Instruction.Operation.SCAN, expected.toString(), nextState);
            tokens.advance();
            return true;
        }
        return false;
    }

    /**
     * Expects a token and throws error if not found.
     */
    private boolean consume(Token expected, String message, String nextState) {
        if (tokens.current() == expected) {
            recordInstruction(Instruction.Operation.SCAN, expected.toString(), nextState);
            tokens.advance();
            return true;
        }
        throw new JsonParseException(message + " (got " + tokens.current() + ")");
    }

    // ========== Grammar productions ==========

    private boolean parseValue(String nextStateAfterValue) {
        // Enter value decision
        recordInstruction(Instruction.Operation.READ, "VALUE", Q_VALUE_DECISION);

        switch (tokens.current()) {
            case LBRACE:
                recordInstruction(Instruction.Operation.WRITE, "OBJECT", Q_OBJECT_OPEN);
                return parseObject(nextStateAfterValue);
            case LBRACKET:
                recordInstruction(Instruction.Operation.WRITE, "ARRAY", Q_ARRAY_OPEN);
                return parseArray(nextStateAfterValue);
            case STRING:
                recordInstruction(Instruction.Operation.WRITE, "STRING", nextStateAfterValue);
                tokens.advance();
                return true;
            case NUMBER:
                recordInstruction(Instruction.Operation.WRITE, "NUMBER", nextStateAfterValue);
                tokens.advance();
                return true;
            case TRUE:
                recordInstruction(Instruction.Operation.WRITE, "TRUE", nextStateAfterValue);
                tokens.advance();
                return true;
            case FALSE:
                recordInstruction(Instruction.Operation.WRITE, "FALSE", nextStateAfterValue);
                tokens.advance();
                return true;
            case NULL:
                recordInstruction(Instruction.Operation.WRITE, "NULL", nextStateAfterValue);
                tokens.advance();
                return true;
            default:
                return false;
        }
    }

    private boolean parseObject(String nextStateAfterObject) {
        recordInstruction(Instruction.Operation.WRITE, "LBRACE", Q_OBJECT_OPEN);
        if (!consume(Token.LBRACE, "Expected '{'", Q_OBJECT_MEMBERS)) {
            return false;
        }

        if (tokens.current() == Token.RBRACE) {
            // Empty object: {}
            recordInstruction(Instruction.Operation.WRITE, "RBRACE", Q_OBJECT_CLOSE);
            tokens.advance();
            recordInstruction(Instruction.Operation.READ, "OBJECT", nextStateAfterObject);
            return true;
        }

        // Non-empty object: { members }
        if (!parseMembers(Q_OBJECT_CLOSE)) {
            return false;
        }

        recordInstruction(Instruction.Operation.WRITE, "RBRACE", Q_OBJECT_CLOSE);
        if (!consume(Token.RBRACE, "Expected '}'", Q_OBJECT_CLOSE)) {
            return false;
        }

        recordInstruction(Instruction.Operation.READ, "OBJECT", nextStateAfterObject);
        return true;
    }

    private boolean parseMembers(String nextStateAfterMembers) {
        recordInstruction(Instruction.Operation.WRITE, "MEMBERS", Q_PAIR_KEY);
        if (!parsePair(Q_OBJECT_MEMBERS)) {
            return false;
        }

        while (tokens.current() == Token.COMMA) {
            recordInstruction(Instruction.Operation.SCAN, "COMMA", Q_PAIR_KEY);
            tokens.advance();
            if (!parsePair(Q_OBJECT_MEMBERS)) {
                return false;
            }
        }

        recordInstruction(Instruction.Operation.READ, "MEMBERS", nextStateAfterMembers);
        return true;
    }

    private boolean parsePair(String nextStateAfterPair) {
        recordInstruction(Instruction.Operation.WRITE, "PAIR", Q_PAIR_KEY);
        if (tokens.current() != Token.STRING) {
            return false;
        }
        recordInstruction(Instruction.Operation.SCAN, "STRING", Q_PAIR_COLON);
        tokens.advance();

        if (!consume(Token.COLON, "Expected ':'", Q_VALUE_ENTRY)) {
            return false;
        }

        if (!parseValue(Q_PAIR_VALUE)) {
            return false;
        }

        recordInstruction(Instruction.Operation.READ, "PAIR", nextStateAfterPair);
        return true;
    }

    private boolean parseArray(String nextStateAfterArray) {
        recordInstruction(Instruction.Operation.WRITE, "LBRACKET", Q_ARRAY_OPEN);
        if (!consume(Token.LBRACKET, "Expected '['", Q_ARRAY_ELEMENTS)) {
            return false;
        }

        if (tokens.current() == Token.RBRACKET) {
            // Empty array: []
            recordInstruction(Instruction.Operation.WRITE, "RBRACKET", Q_ARRAY_CLOSE);
            tokens.advance();
            recordInstruction(Instruction.Operation.READ, "ARRAY", nextStateAfterArray);
            return true;
        }

        // Non-empty array: [ elements ]
        if (!parseElements(Q_ARRAY_CLOSE)) {
            return false;
        }

        recordInstruction(Instruction.Operation.WRITE, "RBRACKET", Q_ARRAY_CLOSE);
        if (!consume(Token.RBRACKET, "Expected ']'", Q_ARRAY_CLOSE)) {
            return false;
        }

        recordInstruction(Instruction.Operation.READ, "ARRAY", nextStateAfterArray);
        return true;
    }

    private boolean parseElements(String nextStateAfterElements) {
        recordInstruction(Instruction.Operation.WRITE, "ELEMENTS", Q_VALUE_ENTRY);
        if (!parseValue(Q_ARRAY_ELEMENTS)) {
            return false;
        }

        while (tokens.current() == Token.COMMA) {
            recordInstruction(Instruction.Operation.SCAN, "COMMA", Q_VALUE_ENTRY);
            tokens.advance();
            if (!parseValue(Q_ARRAY_ELEMENTS)) {
                return false;
            }
        }

        recordInstruction(Instruction.Operation.READ, "ELEMENTS", nextStateAfterElements);
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
