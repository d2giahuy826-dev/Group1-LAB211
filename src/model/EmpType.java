package model;

public enum EmpType {

    FULLTIME,
    PARTTIME;

    public double getDefaultTaxRate() {
        return this == FULLTIME ? 0.10 : 0.05;
    }

    public EmpType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("EmpType cannot be null or blank.");
        }

        String input = value.trim().toUpperCase();

        for (EmpType type : EmpType.values()) {
            if (type.name().equals(input)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid EmpType: " + value);
    }
}