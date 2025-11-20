package edu.wisc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

enum IdentifierType { VARIABLE, PARAMETER, FUNCTION }

class Identifier {
    // For all identifiers
    private final String name;
    private final VarType type;
    private final IdentifierType iType;

    // For variable identifiers
    private final Integer offset;
    private final Object initial;

    // For function identifiers
    private final SymbolTable localVars;

    /**
     * Initialize a new identifier
     * 
     * @param name          The name of the identifier
     * @param type          The type associated with this identifier
     * @param iType         The type of identifier this is
     * @param offset        The location of the identifier
     * @param initial       (Variable) The initial value of the variable (or null)
     * @param size          (Function) The size a function needs
     * @param params        (Function) The parameters for the function
     * @param localVars     (Function) The symbol table for the local variables in the function
     */
    private Identifier(String name, VarType type, IdentifierType iType, Integer offset, Object initial, SymbolTable localVars) {
        this.name = name;
        this.type = type;
        this.iType = iType;
        this.offset = offset;
        this.initial = initial;
        this.localVars = localVars;
    }

    /**
     * Initialize a new variable identifier
     * 
     * @param name        The name of the identifier
     * @param type        The type associated with the identifier
     * @param offset      The location of the identifier
     * @param initial     The variable's initial value (or null)
     */
    public static Identifier variable(String name, VarType type, Integer offset, Object initial) {
        return new Identifier(name, type, IdentifierType.VARIABLE, offset, initial, null);
    }

    /**
     * Initialize a new parameter identifier
     * 
     * @param name       The name of the identifier
     * @param type       The type associated with the identifier
     * @param offset     The location of the identifier
     */
    public static Identifier parameter(String name, VarType type, Integer offset) {
        return new Identifier(name, type, IdentifierType.PARAMETER, offset, null, null);
    }

    /**
     * Initialize a new function identifier
     * 
     * @param name        The name of the identifier
     * @param type        The type associated with the identifier
     * @param offset      The location of the identifier
     * @param size        The size the function needs
     * @param params      The parameters for the function
     * @param localVars   The symbol table for the local variables in the function
     */
    public static Identifier function(String name, VarType type, SymbolTable localVars) {
        return new Identifier(name, type, IdentifierType.FUNCTION, null, null, localVars);
    }

    // For all identifiers
    public String getName() { return name; }
    public VarType getType() { return type; }
    public IdentifierType getIType() { return iType; }
    public boolean isFunction() { return iType == IdentifierType.FUNCTION; }
    public boolean isVariable() { return iType == IdentifierType.VARIABLE; }

    // For variable identifiers
    public Integer getOffset() { return offset; }
    public Object getInitial() { return initial; }

    // For function identifiers
    public SymbolTable getLocalVars() { return localVars; }
}

public class SymbolTable {
    private final Map<String, Identifier> identifiers = new HashMap<>(); // All identifiers defined in the scope
    int nextVariableOffset = 0;
    int nextParameterOffset = 0;

    public void putVariable(String name, VarType type, Object initial) {
        identifiers.put(name, Identifier.variable(name, type, nextVariableOffset, initial));
        switch(type) {
            case INT: nextVariableOffset += 4; break;
            case BOOL: nextVariableOffset += 4; break;
            default: throw new RuntimeException("Variable type not supported in symbol table.");
        }
    }

    public void putParameter(String name, VarType type) {
        identifiers.put(name, Identifier.parameter(name, type, nextParameterOffset));
        switch(type) {
            case INT: nextParameterOffset += 4; break;
            case BOOL: nextParameterOffset += 4; break;
            default: throw new RuntimeException("Variable type not supported in symbol table.");
        }
    }

    public void putFunction(String name, VarType type, SymbolTable localVars) {
        identifiers.put(name, Identifier.function(name, type, localVars));
    }

    public Boolean contains(String name) {
        return identifiers.containsKey(name);
    }

    public Object getVariableInitialValue(String name) {
        // These errors should never be thrown because we already did name analysis
        if (!identifiers.containsKey(name)) {
            throw new RuntimeException("Error constructing symbol table.");
        }
        if (!identifiers.get(name).isVariable()) {
            throw new RuntimeException("Error constructing symbol table.");
        }

        return identifiers.get(name).getInitial();
    }
}
