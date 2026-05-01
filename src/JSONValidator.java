import java.util.*;

public class JSONValidator {
    public static void main(String[] args) {
        System.out.println("=== JSON Validator (Context‑Free Grammar) ===\n");
        runTestSuite();
    }

    public static boolean validate(String jsonInput) {
        JSONParser parser = new JSONParser();
        return parser.parse(jsonInput);
    }

    public static void runTestSuite() {
        TestCase[] testCases = getTestCases();
        System.out.printf("%-3s %-50s %s\n", "ID", "Input JSON", "Result");
        System.out.println("=".repeat(80));

        int valid = 0, invalid = 0;
        for (TestCase tc : testCases) {
            boolean isValid = validate(tc.input);
            if (isValid) valid++; else invalid++;
            String display = tc.input.length() > 48 ? tc.input.substring(0, 45) + "..." : tc.input;
            System.out.printf("%-3d %-50s %s\n", tc.id, display, isValid ? "✓ VALID" : "✗ INVALID");
        }
        System.out.println("=".repeat(80));
        System.out.printf("Total: %d | Valid: %d | Invalid: %d\n", testCases.length, valid, invalid);
    }

    private static TestCase[] getTestCases() {
        return new TestCase[]{
                // VALID
                new TestCase(1, "null"),
                new TestCase(2, "true"),
                new TestCase(3, "\"hello\""),
                new TestCase(4, "123"),
                new TestCase(5, "-45.67"),
                new TestCase(6, "{}"),
                new TestCase(7, "[]"),
                new TestCase(8, "{\"key\":\"value\"}"),
                new TestCase(9, "[1,2,3]"),
                new TestCase(10, "{ \"name\" : \"John\" }"),
                new TestCase(11, "[ true , false , null ]"),
                new TestCase(12, "{\"person\":{\"name\":\"Alice\",\"age\":30}}"),
                new TestCase(13, "[[1,2],[3,4]]"),
                new TestCase(14, "[1, \"two\", true, null]"),
                new TestCase(15, "\"\\\" \\\\ \\n \\u0041\""),
                // INVALID
                new TestCase(16, "{key:\"value\"}"),
                new TestCase(17, "[1,2,3,]"),
                new TestCase(18, "{\"a\":1"),
                new TestCase(19, "[1,2,3"),
                new TestCase(20, "{}}"),
                new TestCase(21, "[]]"),
                new TestCase(22, "01"),
                new TestCase(23, "1."),
                new TestCase(24, ".5"),
                new TestCase(25, "True"),
                new TestCase(26, "NULL"),
                new TestCase(27, "\"hello\nworld\""),
                new TestCase(28, "1 2"),
                new TestCase(29, "\"a\" \"b\""),
                new TestCase(30, "")
        };
    }

    static class TestCase {
        int id; String input;
        TestCase(int id, String input) { this.id = id; this.input = input; }
    }
}