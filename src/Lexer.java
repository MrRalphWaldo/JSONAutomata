/**
 * Lexer (Deterministic Finite Automaton) for JSON.
 * Converts a JSON string into a stream of tokens.
 */
public class Lexer {
    private String input;
    private int position;
    private int line;
    private int column;

    public Lexer(String input) {
        this.input = input;
        this.position = 0;
        this.line = 1;
        this.column = 1;

    }

    public TokenStream tokenize() {
        TokenStream stream = new TokenStream();
        Token token;
        do {
            token = nextToken();
            if (token != Token.WHITESPACE && token != Token.EOF) {
                stream.addToken(token);
            }
        } while (token != Token.EOF);
        stream.addToken(Token.EOF);
        return stream;
    }

    private Token nextToken() {
        skipWhitespace();

        if (position >= input.length()) {
            return Token.EOF;
        }

        char ch = input.charAt(position);

        // Structural characters
        switch (ch) {
            case '{':
                advance();
                return Token.LBRACE;
            case '}':
                advance();
                return Token.RBRACE;
            case '[':
                advance();
                return Token.LBRACKET;
            case ']':
                advance();
                return Token.RBRACKET;
            case ':':
                advance();
                return Token.COLON;
            case ',':
                advance();
                return Token.COMMA;
            case '"':
                return scanString();
        }

        // Numbers: starts with digit or minus
        if (Character.isDigit(ch) || ch == '-') {
            return scanNumber();
        }

        // Keywords: true, false, null
        if (Character.isLetter(ch)) {
            return scanKeyword();
        }

        // Invalid character
        throw new JsonLexerException("Unexpected character: " + ch + " at line " + line + " column " + column);
    }

    private Token scanString() {
        // Consume opening quote
        advance();

        StringBuilder sb = new StringBuilder();
        while (position < input.length()) {
            char ch = input.charAt(position);

            if (ch == '"') {
                // Consume closing quote
                advance();
                // Token value: sb.toString()
                return Token.STRING;
            } else if (ch == '\\') {
                // Escape sequence
                advance();
                if (position >= input.length()) {
                    throw new JsonLexerException("Unterminated string escape at line " + line);
                }
                char escaped = input.charAt(position);
                switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                    case 'b':
                    case 'f':
                    case 'n':
                    case 'r':
                    case 't':
                        sb.append('\\').append(escaped);
                        advance();
                        break;
                    case 'u':
                        // Unicode escape: backslash u followed by 4 hex digits
                        sb.append('\\').append('u');
                        advance();
                        for (int i = 0; i < 4; i++) {
                            if (position >= input.length()) {
                                throw new JsonLexerException("Invalid \\u escape at line " + line);
                            }
                            char hex = input.charAt(position);
                            if (!isHexDigit(hex)) {
                                throw new JsonLexerException("Invalid hex digit in \\u escape: " + hex);
                            }
                            sb.append(hex);
                            advance();
                        }
                        break;
                    default:
                        throw new JsonLexerException("Invalid escape sequence: \\" + escaped);
                }
            } else if (ch < ' ') {
                // Control characters not allowed in strings
                throw new JsonLexerException("Unescaped control character in string at line " + line);
            } else {
                sb.append(ch);
                advance();
            }
        }

        throw new JsonLexerException("Unterminated string at line " + line);
    }

    private Token scanNumber() {
        StringBuilder sb = new StringBuilder();

        // Optional minus
        if (position < input.length() && input.charAt(position) == '-') {
            sb.append(input.charAt(position));
            advance();
        }

        // Integer part
        if (position >= input.length() || !Character.isDigit(input.charAt(position))) {
            throw new JsonLexerException("Invalid number at line " + line);
        }

        if (input.charAt(position) == '0') {
            // Just a zero
            sb.append('0');
            advance();
        } else {
            // Digit sequence
            while (position < input.length() && Character.isDigit(input.charAt(position))) {
                sb.append(input.charAt(position));
                advance();
            }
        }

        // Fractional part
        if (position < input.length() && input.charAt(position) == '.') {
            sb.append('.');
            advance();
            if (position >= input.length() || !Character.isDigit(input.charAt(position))) {
                throw new JsonLexerException("Invalid number: missing digits after decimal point at line " + line);
            }
            while (position < input.length() && Character.isDigit(input.charAt(position))) {
                sb.append(input.charAt(position));
                advance();
            }
        }

        // Exponent part
        if (position < input.length() && (input.charAt(position) == 'e' || input.charAt(position) == 'E')) {
            sb.append(input.charAt(position));
            advance();
            if (position < input.length() && (input.charAt(position) == '+' || input.charAt(position) == '-')) {
                sb.append(input.charAt(position));
                advance();
            }
            if (position >= input.length() || !Character.isDigit(input.charAt(position))) {
                throw new JsonLexerException("Invalid number: missing digits in exponent at line " + line);
            }
            while (position < input.length() && Character.isDigit(input.charAt(position))) {
                sb.append(input.charAt(position));
                advance();
            }
        }

        return Token.NUMBER;
    }

    private Token scanKeyword() {
        int start = position;
        while (position < input.length() && Character.isLetter(input.charAt(position))) {
            advance();
        }
        String keyword = input.substring(start, position);

        switch (keyword) {
            case "true":
                return Token.TRUE;
            case "false":
                return Token.FALSE;
            case "null":
                return Token.NULL;
            default:
                throw new JsonLexerException("Invalid keyword: " + keyword + " at line " + line);
        }
    }

    private void skipWhitespace() {
        while (position < input.length()) {
            char ch = input.charAt(position);
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                advance();
            } else {
                break;
            }
        }
    }

    private void advance() {
        if (position < input.length()) {
            if (input.charAt(position) == '\n') {

                column = 1;
            } else {
                column++;
            }
            position++;
        }
    }

    private boolean isHexDigit(char ch) {
        return Character.isDigit(ch) || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
    }
}

class JsonLexerException extends RuntimeException {
    public JsonLexerException(String message) {
        super(message);
    }
}

