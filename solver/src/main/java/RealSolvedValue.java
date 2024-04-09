import java.util.Objects;

public class RealSolvedValue extends SolvedValue {
    private final Double value;

    public RealSolvedValue(Double value) {
        this.value = value;
    }

    public Double getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RealSolvedValue that = (RealSolvedValue) o;
        return Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }
}
