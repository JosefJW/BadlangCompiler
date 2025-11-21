package edu.wisc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

/**
 * Used to assign an identifier type to identifiers
 */
enum IdentifierType { VARIABLE, FUNCTION }

/**
 * Stores all information needed for name and type checking for an identifier stored in the environment.
 */
class Identifier {
    private final VarType type;
    private final IdentifierType iType;
    private final List<Stmt.Parameter> parameters;
    private Boolean initialized;
    private String uniqueLabel;

    /**
     * Initialize a new Identifier
     * 
     * @param type        The type associated with the identifier
     * @param iType       The kind of identifier it is (variable or function)
     * @param parameters  Any parameters associated with a function identifier
     * @param initialized Whether the identifier has been initialized or not
     */
    public Identifier(VarType type, IdentifierType iType, List<Stmt.Parameter> parameters, Boolean initialized) {
        this.type = type;
        this.iType = iType;
        this.parameters = parameters;
        this.initialized = initialized;
    }

    public VarType getType() { return type; }
    public IdentifierType getIType() { return iType; }
    public List<Stmt.Parameter> getParameters() { return parameters; }
    public Boolean isInitialized() { return initialized; }
    public void initialize() { initialized = true; }

    public String getUniqueLabel() { return uniqueLabel; }
    public void setUniqueLabel(String newLabel) { uniqueLabel = newLabel; }
}

/**
 * Holds all identifiers and information needed to name and type check those identifiers.
 */
public class Environment {
    private final Environment parent; // The parent environment for this environment
    private final VarType returnType; // The return type for this scope
    private final Map<String, Identifier> identifiers = new HashMap<>(); // All identifiers defined in the scope

    /**
     * Initializes a new environment
     * 
     * @param parent     The parent environment for this environment
     * @param returnType The return type of this scope
     */
    public Environment(Environment parent, VarType returnType) {
        this.parent = parent;
        this.returnType = returnType;
    }

    /**
     * Get all identifier names in the current and parent scopes
     * 
     * @return A set of all identifier names
     */
    public Set<String> getAllIdentifiers() { 
        Set<String> idents = new HashSet<String>();

        idents.addAll(identifiers.keySet());

        if (parent != null) idents.addAll(parent.getAllIdentifiers());

        return idents;
    }
    
    /**
     * Get all identifier names in the current and parent scopes that are associated with variables (not functions)
     * 
     * @return A set of all variable identifier names
     */
    public Set<String> getAllVariables() {
        Set<String> vars = new HashSet<String>();

        for (String key : identifiers.keySet()) {
            if (identifiers.get(key).getIType() == IdentifierType.VARIABLE) {
                vars.add(key);
            }
        }

        if (parent != null) vars.addAll(parent.getAllVariables());

        return vars;
    }

    /**
     * Get all identifier names in the current and parent scopes that are associated with functions (not variables)
     * 
     * @return A set of all function identifier names
     */
    public Set<String> getAllFunctions() {
        Set<String> funcs = new HashSet<String>();

        for (String key : identifiers.keySet()) {
            if (identifiers.get(key).getIType() == IdentifierType.FUNCTION) {
                funcs.add(key);
            }
        }

        if (parent != null) funcs.addAll(parent.getAllFunctions());

        return funcs;
    }

    /**
     * Get this environment's parent
     * 
     * @return The parent environment
     */
    public Environment getParent() { return parent; }

    public VarType getReturnType() { 
        if (returnType != null) return returnType;
        else {
            if (parent == null) return null;
            else return parent.getReturnType();
        } 
    }

    /**
     * Check if an identifier is declared in this specific scope
     * 
     * @param identifier The identifier to check
     * @return True if the identifier is declared in this specific scope; false otherwise
     */
    public Boolean isDeclaredInScope(String identifier) { 
        return identifiers.containsKey(identifier);
    }

    /**
     * Check if an identifier is declared
     * 
     * @param identifier The identifier to check
     * @return True if the identifier is declared; false otherwise
     */
    public Boolean isDeclared(String identifier) {
        if (isDeclaredInScope(identifier)) return true;
        else if (parent == null) return false;
        return parent.isDeclared(identifier);
    }

    /**
     * Check if an identifier has been initialized
     * 
     * @param identifier The identifier to check
     * @return True if the identifier is initialized; false otherwise
     */
    public Boolean isInitialized(String identifier) { 
        if (isDeclaredInScope(identifier)) return identifiers.get(identifier).isInitialized();
        else if (parent == null) return false;
        else return parent.isInitialized(identifier);
    }

    /**
     * Check if an identifier corresponds to a function
     * 
     * @param identifier The identifier to check
     * @return True if the identifier corresponds to a function; false otherwise
     */
    public Boolean isFunction(String identifier) {
        if (isDeclaredInScope(identifier)) return identifiers.get(identifier).getIType() == IdentifierType.FUNCTION;
        else if (parent == null) return false;
        else return parent.isFunction(identifier);
    }

    /**
     * Get the type associated with an identifier
     * 
     * @param identifier The identifier to get the type of
     * @return The type associated with the identifier
     */
    public VarType getType(String identifier) {
        if (isDeclaredInScope(identifier)) return identifiers.get(identifier).getType();
        else if (parent == null) return VarType.ERR;
        else return parent.getType(identifier);
    }

    /**
     * Declare an identifier in this environment
     * 
     * @param identifier  The name of the identifier to declare
     * @param type        The type associated with the identifier
     * @param iType       The type of identifier it is (i.e. variable or function)
     * @param parameters  Any parameters associated with the identifier
     * @param initialized Whether the identifier is initialized or not
     */
    public void declare(String identifier, VarType type, IdentifierType iType, List<Stmt.Parameter> parameters, Boolean initialized) {
        identifiers.put(identifier, new Identifier(type, iType, parameters, initialized));
    }

    /**
     * Get any parameters associated with an identifier
     * 
     * @param identifier The identifier to get the parameters of
     * @return A list of the identifier's parameters
     */
    public List<Stmt.Parameter> getParameters(String identifier) {
        if (isDeclaredInScope(identifier)) return identifiers.get(identifier).getParameters();
        else return parent.getParameters(identifier);
    }

    /**
     * Initialize a declared identifier in the environment
     * 
     * @param identifier The identifier to initialize
     */
    public void initialize(String identifier) {
        if (isDeclaredInScope(identifier)) identifiers.get(identifier).initialize();
        else parent.initialize(identifier);
    }

    public String getUniqueLabel(String identifier) {
        if (isDeclaredInScope(identifier)) return identifiers.get(identifier).getUniqueLabel();
        else return parent.getUniqueLabel(identifier);
    }

    public void setUniqueLabel(String identifier, String label) {
        if (isDeclaredInScope(identifier)) identifiers.get(identifier).setUniqueLabel(label);
        else parent.setUniqueLabel(identifier, label);
    }
}
