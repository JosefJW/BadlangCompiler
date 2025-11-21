package edu.wisc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Used to track what type of identifier a binding is.
 */
enum STIdentifierType { VARIABLE, PARAMETER, FUNCTION }

/**
 * Stores the metadata associated with an identifier.
 */
class STIdentifier {
    // For all identifiers
    private final String name;
    private final VarType type;
    private final STIdentifierType iType;

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
    private STIdentifier(String name, VarType type, STIdentifierType iType, Integer offset, Object initial, SymbolTable localVars) {
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
    public static STIdentifier variable(String name, VarType type, Integer offset, Object initial) {
        return new STIdentifier(name, type, STIdentifierType.VARIABLE, offset, initial, null);
    }

    /**
     * Initialize a new parameter identifier
     * 
     * @param name       The name of the identifier
     * @param type       The type associated with the identifier
     * @param offset     The location of the identifier
     */
    public static STIdentifier parameter(String name, VarType type, Integer offset) {
        return new STIdentifier(name, type, STIdentifierType.PARAMETER, offset, null, null);
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
    public static STIdentifier function(String name, VarType type, SymbolTable localVars) {
        return new STIdentifier(name, type, STIdentifierType.FUNCTION, null, null, localVars);
    }

    // For all identifiers
    public String getName() { return name; }
    public VarType getType() { return type; }
    public STIdentifierType getIType() { return iType; }
    public boolean isFunction() { return iType == STIdentifierType.FUNCTION; }
    public boolean isVariable() { return iType == STIdentifierType.VARIABLE; }
    public boolean isParameter() { return iType == STIdentifierType.PARAMETER; }

    // For variable identifiers
    public Integer getOffset() { return offset; }
    public Object getInitial() { return initial; }

    // For function identifiers
    public SymbolTable getLocalVars() { return localVars; }
    public Integer getLocalVarsSize() { 
        if (localVars == null) return null;
        return localVars.nextVariableOffset;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" : ").append(type).append(" (").append(iType).append(")");
        
        if (isVariable() || isParameter()) {
            sb.append(", offset=").append(offset);
            if (isVariable() && initial != null) {
                sb.append(", initial=").append(initial);
            }
        }
        
        if (isFunction() && localVars != null) {
            sb.append("\n  LocalVars:\n");
            // Indent each line of the function's symbol table
            String[] lines = localVars.toString().split("\n");
            for (String line : lines) {
                sb.append("    ").append(line).append("\n");
            }
        }
        
        return sb.toString();
    }
}

/**
 * Stores a mapping of identifier strings to their metadata.
 * Assumes code is semantically correct when adding values.
 */
public class SymbolTable {
    private final Map<String, STIdentifier> identifiers = new HashMap<>(); // Stores all currently defined identifiers
    int nextVariableOffset = 0; // Used to track the local variable offset within functions
    int nextParameterOffset = 0; // Used to track the parameter variable offset within functions

    /**
     * Add a variable to the symbol table.
     * 
     * @param name    The variable's identifier
     * @param type    The type of the variable
     * @param initial If applicable, store the initial value of the variable (or null)
     */
    public void putVariable(String name, VarType type, Object initial) {
        identifiers.put(name, STIdentifier.variable(name, type, nextVariableOffset, initial));
        switch(type) {
            case INT: nextVariableOffset += 4; break;
            case BOOL: nextVariableOffset += 4; break;
            default: throw new RuntimeException("Variable type not supported in symbol table.");
        }
    }

    /**
     * Add a parameter to the symbol table.
     * 
     * @param name The parameter's identifier
     * @param type The type of the parameter
     */
    public void putParameter(String name, VarType type) {
        identifiers.put(name, STIdentifier.parameter(name, type, nextParameterOffset));
        switch(type) {
            case INT: nextParameterOffset += 4; break;
            case BOOL: nextParameterOffset += 4; break;
            default: throw new RuntimeException("Variable type not supported in symbol table.");
        }
    }

    /**
     * Add a function to the symbol table
     * 
     * @param name      The function's identifier
     * @param type      The return type of the function
     * @param localVars A symbol table containing the local variables and parameters defined for/in the function
     */
    public void putFunction(String name, VarType type, SymbolTable localVars) {
        identifiers.put(name, STIdentifier.function(name, type, localVars));
    }

    /**
     * Check if an identifier is stored in the symbol table
     * 
     * @param name The identifier to check for
     * @return true if the identifier is stored; false otherwise
     */
    public Boolean contains(String name) {
        return identifiers.containsKey(name);
    }

    /**
     * Get the metadata for an identifier stored in the symbol table
     * 
     * @param name The identifier to get the metadata for
     * @return The metadata for the identifier
     */
    public STIdentifier get(String name) {
        if (!contains(name)) return null;
        return identifiers.get(name);
    }

    /**
     * Returns metadata for all identifiers of type variable in the symbol table.
     * Note: Type variable is distinct from type parameter
     * 
     * @return A set of the metadata for all variables
     */
    public Set<STIdentifier> getVariables() {
        Set<STIdentifier> variables = new HashSet<>();
        for (STIdentifier value : identifiers.values()) {
            if (value.isVariable()) {
                variables.add(value);
            }
        }
        return variables;
    }

    /**
     * Returns metadata for all identifiers of type function in the symbol table.
     * 
     * @return A set of the metadata for all functions
     */
    public Set<STIdentifier> getFunctions() {
        Set<STIdentifier> functions = new HashSet<>();
        for (STIdentifier value : identifiers.values()) {
            if (value.isFunction()) {
                functions.add(value);
            }
        }
        return functions;
    }

    /**
     * Returns metadata for all identifiers of type parameter in the symbol table.
     * 
     * @return A set of the metadata for all parameters
     */
    public Set<STIdentifier> getParameters() {
        Set<STIdentifier> parameters = new HashSet<>();
        for (STIdentifier value : identifiers.values()) {
            if (value.isParameter()) {
                parameters.add(value);
            }
        }
        return parameters;
    }

    /**
     * Get the initial value for a variable stored in the symbol table.
     * 
     * @param name The identifier to get the initial value of
     * @return The initial value of the identifier
     */
    public Object getVariableInitialValue(String name) {
        STIdentifier id = identifiers.get(name);
        if (id == null) {
            throw new IllegalArgumentException("Variable '" + name + "' does not exist in the symbol table.");
        }
        if (!id.isVariable()) {
            throw new IllegalArgumentException("'" + name + "' is not a variable.");
        }
        return id.getInitial();
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (STIdentifier id : identifiers.values()) {
            sb.append(id.toString()).append("\n");
        }
        return sb.toString();
    }
}
