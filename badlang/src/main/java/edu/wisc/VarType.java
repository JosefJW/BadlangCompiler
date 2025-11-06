package edu.wisc;

public enum VarType {
    INT("int"),
    BOOL("bool"),
    ERR("ERROR");

    private final String text;

    VarType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}


