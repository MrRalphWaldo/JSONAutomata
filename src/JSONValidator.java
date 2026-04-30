import java.util.*;

/**
 * JSONValidator - Main entry point for the JSON validation system.
 * Uses a Lexer (DFA) followed by a JSONParser (PDA) to validate JSON input.
 */
public class JSONValidator {
    public static void main(String[] args) {
        System.out.println("=== JSON Validator (Pushdown Automaton) ===\n");

        // Run test suite
        runTestSuite();
    }

    /**
     * Validate a single JSON string and return PDA instruction trace.
     */
    public static ValidationResult validate(String jsonInput) {
        try {
            JSONParser parser = new JSONParser(false);
            boolean isValid = parser.parse(jsonInput);

            // Get the instruction trace showing PDA execution
            List<Instruction> instructions = parser.getInstructionTrace();

            return new ValidationResult(isValid, isValid ? "VALID" : "INVALID", instructions);
        } catch (Exception e) {
            return new ValidationResult(false, "INVALID: " + e.getMessage(), new ArrayList<>());
        }
    }


    /**
     * Run comprehensive test suite.
     * Shows the PDA execution trace for each test case.
     */
    public static void runTestSuite() {
        TestCase[] testCases = getTestCases();

        System.out.printf("%-3s %-50s %s\n", "ID", "Input JSON", "Result");
        System.out.println("=".repeat(80));

        int validCount = 0;
        int invalidCount = 0;

        for (TestCase tc : testCases) {
            ValidationResult result = validate(tc.input);

            if (result.isValid) {
                validCount++;
            } else {
                invalidCount++;
            }

            String resultStatus = result.isValid ? "✓ VALID" : "✗ INVALID";
            String inputDisplay = tc.input.length() > 50 ? tc.input.substring(0, 47) + "..." : tc.input;

            // Show test case header
            System.out.printf("%-3d %-50s %s\n", tc.id, inputDisplay, resultStatus);

            // Show PDA execution trace
            if (!result.instructions.isEmpty()) {
                System.out.println("    PDA Trace:");
                int stepCount = 0;
                for (Instruction instr : result.instructions) {
                    if (stepCount < 5) {  // Show first 5 steps
                        System.out.printf("      %s\n", instr);
                    } else if (stepCount == 5) {
                        int remaining = result.instructions.size() - 5;
                        System.out.printf("      ... and %d more steps\n", remaining);
                        break;
                    }
                    stepCount++;
                }
            }
            System.out.println();
        }

        System.out.println("=".repeat(80));
        System.out.printf("Total Tests: %d\n", testCases.length);
        System.out.printf("Valid JSON: %d\n", validCount);
        System.out.printf("Invalid JSON: %d\n\n", invalidCount);
        
    }

    /**
     * Test cases for JSON validation.
     * NO EXPECTED RESULTS - We test to see what the validator outputs.
     */
    private static TestCase[] getTestCases() {
        return new TestCase[]{
                // Primitives
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

    /**
     * Test case structure.
     * NO EXPECTED RESULT - We discover what the validator outputs.
     */
    static class TestCase {
        int id;
        String input;

        TestCase(int id, String input) {
            this.id = id;
            this.input = input;
        }
    }

    /**
     * Result of validation including PDA instruction trace.
     */
    static class ValidationResult {
        boolean isValid;
        String message;
        List<Instruction> instructions;

        ValidationResult(boolean isValid, String message, List<Instruction> instructions) {
            this.isValid = isValid;
            this.message = message;
            this.instructions = instructions;
        }
    }


}

