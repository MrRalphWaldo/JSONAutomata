import java.util.ArrayList;
import java.util.List;

/**
 * TokenStream represents a sequence of tokens produced by the lexer.
 * It provides a cursor for the parser to inspect and advance through tokens.
 */
public class TokenStream {
    private List<Token> tokens;
    private int position;

    public TokenStream() {
        this.tokens = new ArrayList<>();
        this.position = 0;
    }

    public void addToken(Token token) {
        tokens.add(token);
    }

    public Token current() {
        if (position < tokens.size()) {
            return tokens.get(position);
        }
        return Token.EOF;
    }

    public Token peek(int offset) {
        int index = position + offset;
        if (index < tokens.size()) {
            return tokens.get(index);
        }
        return Token.EOF;
    }

    public void advance() {
        if (position < tokens.size()) {
            position++;
        }
    }

    public boolean isAtEnd() {
        return position >= tokens.size() || tokens.get(position) == Token.EOF;
    }

    public int getPosition() {
        return position;
    }

    public void reset() {
        position = 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(tokens.get(i));
        }
        return sb.toString();
    }
}

