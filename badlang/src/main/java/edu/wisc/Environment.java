package edu.wisc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

enum IdentifierType { VARIABLE, FUNCTION }

class Identifier {
    private final VarType type;
    private final IdentifierType iType;
    private final List<Stmt.Parameter> parameters;
    private Boolean initialized;

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
}

public class Environment {
    private final Environment parent;
    private final VarType returnType;
    private final Map<String, Identifier> identifiers = new HashMap<>();

    public Environment(Environment parent, VarType returnType) {
        this.parent = parent;
        this.returnType = returnType;
    }

    public Set<String> getAllIdentifiers() { return identifiers.keySet(); }
    
    public Set<String> getAllVariables() {
        Set<String> vars = new HashSet<String>();

        for (String key : identifiers.keySet()) {
            if (identifiers.get(key).getIType() == IdentifierType.VARIABLE) {
                vars.add(key);
            }
        }

        return vars;
    }

    public Set<String> getAllFunctions() {
        Set<String> funcs = new HashSet<String>();

        for (String key : identifiers.keySet()) {
            if (identifiers.get(key).getIType() == IdentifierType.FUNCTION) {
                funcs.add(key);
            }
        }

        return funcs;
    }

    public Environment getParent() { return parent; }

    public VarType getReturnType() { 
        if (returnType != null) return returnType;
        else {
            if (parent == null) return null;
            else return parent.getReturnType();
        } 
    }

    public Boolean isDeclaredInScope(String identifier) { 
        return identifiers.containsKey(identifier);
    }

    public Boolean isDeclared(String identifier) {
        if (isDeclaredInScope(identifier)) return true;
        else if (parent == null) return false;
        return parent.isDeclared(identifier);
    }

    public Boolean isInitialized(String identifier) { 
        if (isDeclaredInScope(identifier)) return identifiers.get(identifier).isInitialized();
        else return parent.isInitialized(identifier);
    }

    public Boolean isFunction(String identifier) {
        if (isDeclaredInScope(identifier)) return identifiers.get(identifier).getIType() == IdentifierType.FUNCTION;
        else return parent.isFunction(identifier);
    }

    public VarType getType(String identifier) {
        if (isDeclaredInScope(identifier)) return identifiers.get(identifier).getType();
        else return parent.getType(identifier);
    }

    public void declare(String identifier, VarType type, IdentifierType iType, List<Stmt.Parameter> parameters, Boolean initialized) {
        identifiers.put(identifier, new Identifier(type, iType, parameters, initialized));
    }

    public List<Stmt.Parameter> getParameters(String identifier) {
        if (isDeclaredInScope(identifier)) return identifiers.get(identifier).getParameters();
        else return parent.getParameters(identifier);
    }

    public void initialize(String identifier) {
        if (isDeclaredInScope(identifier)) identifiers.get(identifier).initialize();
        else parent.initialize(identifier);
    }
}
