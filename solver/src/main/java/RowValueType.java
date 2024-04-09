public enum RowValueType {
    INT, REAL, STRING;

    @Override
    public String toString() {
        switch (this) {
            case INT:
                return "Int";
            case REAL:
                return "Real";
            case STRING:
                return "String";
        }
        return "";
    }
}
