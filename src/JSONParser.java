/**
 * Recursive Descent Parser for JSON based on Context‑Free Grammar.
 * No PDA trace – just validates JSON.
 */
public class JSONParser {
    private TokenStream tokens;

    public JSONParser() {}

    public boolean parse(String input) {
        try {
            Lexer lexer = new Lexer(input);
            this.tokens = lexer.tokenize();
            boolean result = parseValue();
            if (result && tokens.current() != Token.EOF) return false;
            return result;
        } catch (Exception e) {
            return false;
        }
    }

    // value → object | array | string | number | true | false | null
    private boolean parseValue() {
        switch (tokens.current()) {
            case LBRACE:  return parseObject();
            case LBRACKET: return parseArray();
            case STRING:   tokens.advance(); return true;
            case NUMBER:   tokens.advance(); return true;
            case TRUE:     tokens.advance(); return true;
            case FALSE:    tokens.advance(); return true;
            case NULL:     tokens.advance(); return true;
            default:       return false;
        }
    }

    // object → '{' members '}'
    private boolean parseObject() {
        if (tokens.current() != Token.LBRACE) return false;
        tokens.advance();
        if (!parseMembers()) return false;
        if (tokens.current() != Token.RBRACE) return false;
        tokens.advance();
        return true;
    }

    // members → pair (',' pair)* | ε
    private boolean parseMembers() {
        if (tokens.current() == Token.RBRACE) return true; // empty
        if (!parsePair()) return false;
        while (tokens.current() == Token.COMMA) {
            tokens.advance();
            if (!parsePair()) return false;
        }
        return true;
    }

    // pair → string ':' value
    private boolean parsePair() {
        if (tokens.current() != Token.STRING) return false;
        tokens.advance();
        if (tokens.current() != Token.COLON) return false;
        tokens.advance();
        return parseValue();
    }

    // array → '[' elements ']'
    private boolean parseArray() {
        if (tokens.current() != Token.LBRACKET) return false;
        tokens.advance();
        if (!parseElements()) return false;
        if (tokens.current() != Token.RBRACKET) return false;
        tokens.advance();
        return true;
    }

    // elements → value (',' value)* | ε
    private boolean parseElements() {
        if (tokens.current() == Token.RBRACKET) return true; // empty
        if (!parseValue()) return false;
        while (tokens.current() == Token.COMMA) {
            tokens.advance();
            if (!parseValue()) return false;
        }
        return true;
    }
}