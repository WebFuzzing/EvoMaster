import java.util.Objects;

public class IntSolvedValue extends SolvedValue {
    private final int value;

    public IntSolvedValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntSolvedValue that = (IntSolvedValue) o;
        return getValue() == that.getValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }

    @Override
    public String toString() {
        return "IntSolvedValue{" +
                "value=" + value +
                '}';
    }
}
