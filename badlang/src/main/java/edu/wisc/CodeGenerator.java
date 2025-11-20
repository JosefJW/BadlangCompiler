package edu.wisc;

import java.util.List;

public class CodeGenerator {
	private final SymbolTable globalSymbolTable;

	public CodeGenerator(SymbolTable globalSymbolTable) {
		this.globalSymbolTable = globalSymbolTable;
	}

	public String generate(List<Stmt> AST) {
		StringBuilder sb = new StringBuilder();
		sb.append(".data\n");
		return "";
	}
}
