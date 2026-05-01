import java.util.*;

public class JSONValidator {
    public static void main(String[] args) {
        System.out.println("=== JSON Validator (Pushdown Automaton) ===\n");
        runTestSuite();
    }

    /**
     * Validate a single JSON string.
     * @param verbose if true, prints token stream and PDA trace
     */
    public static ValidationResult validate(String jsonInput, boolean verbose) {
        try {
            Lexer lexer = new Lexer(jsonInput);
            TokenStream tokenStream = lexer.tokenize();

            if (verbose) {
                System.out.println("    Tokens: " + tokenStream);
            }

            JSONParser parser = new JSONParser(false);
            boolean isValid = parser.parse(jsonInput);
            List<Instruction> instructions = parser.getInstructionTrace();

            // Determine final state from last instruction (if any)
            String finalState = getFinalState(instructions);

            // Acceptance requires: parser says valid AND at EOF
            // We also need to check EOF because parser returns false if extra tokens
            // But parse() already does that, so isValid already reflects that.
            // However we also need to handle the case where parser returned true but no SCAN(EOF) recorded?
            // parse() records SCAN(EOF) only when result true, so that's fine.
            // We'll just use isValid.
            boolean accepted = isValid;

            return new ValidationResult(isValid, accepted, finalState, instructions);
        } catch (JsonLexerException e) {
            return new ValidationResult(false, false, "lexer_error", new ArrayList<>());
        } catch (Exception e) {
            return new ValidationResult(false, false, "exception", new ArrayList<>());
        }
    }

    private static String getFinalState(List<Instruction> instructions) {
        if (instructions.isEmpty()) return "none";
        return instructions.get(instructions.size() - 1).getNextState();
    }

    public static void runTestSuite() {
        TestCase[] testCases = getTestCases();

        System.out.printf("%-3s %-50s %s\n", "ID", "Input JSON", "Result");
        System.out.println("=".repeat(80));

        int validCount = 0, invalidCount = 0;

        for (TestCase tc : testCases) {
            ValidationResult result = validate(tc.input, true);
            if (result.isValid) validCount++;
            else invalidCount++;

            String inputDisplay = tc.input.length() > 48 ? tc.input.substring(0, 45) + "..." : tc.input;
            String resultStatus = result.isValid ? "✓ VALID" : "✗ INVALID";
            System.out.printf("%-3d %-50s %s\n", tc.id, inputDisplay, resultStatus);

            // Show PDA trace (if any)
            if (!result.instructions.isEmpty()) {
                System.out.println("    PDA Trace:");
                for (Instruction instr : result.instructions) {
                    System.out.printf("      %s\n", instr);
                }
            } else if (!result.isValid) {
                System.out.println("    PDA Trace: (no instructions – error before parsing)");
            }

            // Show final state and acceptance decision
            System.out.printf("    Final state: %s\n", result.finalState);
            System.out.printf("    Accept? %s\n", result.accepted ? "Yes" : "No");
            System.out.println();
        }

        System.out.println("=".repeat(80));
        System.out.printf("Total Tests: %d\n", testCases.length);
        System.out.printf("Valid JSON: %d\n", validCount);
        System.out.printf("Invalid JSON: %d\n\n", invalidCount);
    }

    private static TestCase[] getTestCases() {
        // Keep your existing test cases exactly as before
        return new TestCase[]{
                new TestCase(1, "null"), new TestCase(2, "true"), // ... etc ...
                new TestCase(1, "null"),
                new TestCase(2, "true"),
                new TestCase(3, "false"),
                new TestCase(4, "0"),
                new TestCase(5, "123"),
                new TestCase(6, "-456"),
                new TestCase(7, "123.456"),
                new TestCase(8, "1.23e10"),
                new TestCase(9, "1.23E-10"),
                new TestCase(10, "\"\""),
                new TestCase(11, "\"hello\""),
                new TestCase(12, "\"hello world\""),

                // Objects
                new TestCase(13, "{}"),
                new TestCase(14, "{\"name\":\"John\"}"),
                new TestCase(15, "{\"name\":\"John\",\"age\":30}"),
                new TestCase(16, "{\"name\":\"John\", \"age\": 30, \"city\": \"NYC\"}"),
                new TestCase(17, "{\"person\":{\"name\":\"John\",\"age\":30}}"),
                new TestCase(18, "{\"numbers\":[1,2,3]}"),

                // Arrays
                new TestCase(19, "[]"),
                new TestCase(20, "[1]"),
                new TestCase(21, "[1,2,3]"),
                new TestCase(22, "[true,false,null]"),
                new TestCase(23, "[\"a\",\"b\",\"c\"]"),
                new TestCase(24, "[1,\"two\",true,null]"),
                new TestCase(25, "[[1,2],[3,4]]"),
                new TestCase(26, "[{},{}]"),

                // Complex nested structures
                new TestCase(27, "{\"users\":[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]}"),
                new TestCase(28, "[{\"a\":1},{\"b\":2},{\"c\":3}]"),
                new TestCase(29, "{\"level1\":{\"level2\":{\"level3\":{\"value\":42}}}}"),

                // Whitespace handling
                new TestCase(30, "{ }"),
                new TestCase(31, "[ ]"),
                new TestCase(32, "{ \"key\" : \"value\" }"),
                new TestCase(33, "[ 1 , 2 , 3 ]"),
                new TestCase(34, "\n{\n\"key\"\n:\n\"value\"\n}\n"),

                // String escapes
                new TestCase(35, "\"\\\"\""),
                new TestCase(36, "\"\\\\\""),
                new TestCase(37, "\"\\n\""),
                new TestCase(38, "\"\\t\""),
                new TestCase(39, "\"\\u0041\""),

                // Edge cases
                new TestCase(40, "0.0"),
                new TestCase(41, "-0"),
                new TestCase(42, "1e10"),
                new TestCase(43, "1e+10"),
                new TestCase(44, "1e-10"),

                // Missing structural elements
                new TestCase(45, "{"),
                new TestCase(46, "}"),
                new TestCase(47, "["),
                new TestCase(48, "]"),
                new TestCase(49, "{]"),
                new TestCase(50, "[}"),

                // Trailing commas
                new TestCase(51, "{\"key\":\"value\",}"),
                new TestCase(52, "[1,2,3,]"),
                new TestCase(53, "[,]"),

                // Missing colons/commas
                new TestCase(54, "{\"key\"\"value\"}"),
                new TestCase(55, "{\"key\":\"value\",\"key2\":\"value2\"}}"),
                new TestCase(56, "[1 2 3]"),

                // Bad numbers
                new TestCase(57, "01"),
                new TestCase(58, "1."),
                new TestCase(59, ".5"),
                new TestCase(60, "1e"),
                new TestCase(61, "+1"),

                // Unquoted strings
                new TestCase(62, "{key:\"value\"}"),
                new TestCase(63, "[hello]"),

                // Bad escape sequences
                new TestCase(64, "\"\\x\""),
                new TestCase(65, "\"\\u12\""),

                // Multiple values at top level
                new TestCase(66, "\"hello\" \"world\""),
                new TestCase(67, "1 2"),
                new TestCase(68, "{} []"),

                // Unterminated structures
                new TestCase(69, "{\"key\":\"value\""),
                new TestCase(70, "[1,2,3"),
                new TestCase(71, "\"unterminated"),

                // Empty input
                new TestCase(72, ""),

                // Undefined keywords
                new TestCase(73, "True"),
                new TestCase(74, "False"),
                new TestCase(75, "Null"),
                new TestCase(76, "undefined"),
        };
    }

    static class TestCase { int id; String input; TestCase(int id, String input) { this.id = id; this.input = input; } }

    static class ValidationResult {
        boolean isValid;      // syntactic validity as per JSON grammar
        boolean accepted;     // reached EOF after successful parse
        String finalState;
        List<Instruction> instructions;
        ValidationResult(boolean isValid, boolean accepted, String finalState, List<Instruction> instructions) {
            this.isValid = isValid; this.accepted = accepted; this.finalState = finalState; this.instructions = instructions;
        }
    }
}