/**
 * Represents a PDA instruction in the form: state | operation(argument, next_state)
 *
 * Operations:
 * - SCAN(token): consume a token from input
 * - WRITE(symbol): push a symbol onto the stack
 * - READ(symbol): pop a symbol from the stack
 */
public class Instruction {
    public enum Operation {
        SCAN,   // Consume input token
        WRITE,  // Push symbol to stack
        READ    // Pop symbol from stack
    }

    private String state;
    private Operation operation;
    private String argument;
    private String nextState;

    public Instruction(String state, Operation operation, String argument, String nextState) {
        this.state = state;
        this.operation = operation;
        this.argument = argument;
        this.nextState = nextState;
    }

    public String getState() {
        return state;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getArgument() {
        return argument;
    }

    public String getNextState() {
        return nextState;
    }

    @Override
    public String toString() {
        return String.format("%s | %s(%s, %s)", state, operation, argument, nextState);
    }
}

