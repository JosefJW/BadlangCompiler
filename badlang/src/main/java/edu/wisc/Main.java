package edu.wisc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main entry point for the badlang code generator.
 * 
 * This program generates machine code (MIPS or x86 assembly) from badlang programs.
 * 
 * Usage: java edu.wisc.Main <source-file> [output-file]
 * 
 * If output-file is not specified, outputs to stdout.
 */
public class Main {
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java edu.wisc.Main <source-file> [output-file]");
            System.exit(1);
        }
        
        String inputFile = args[0];
        String outputFile = args.length > 1 ? args[1] : null;
        
        try {
            // Read the source file into a string
            String code = "";
            try {
                code = new String(Files.readAllBytes(Paths.get(inputFile)));
            }
            catch (Exception e) {
                System.err.println("Error reading file: " + e.getMessage());
                return;
            }

            // Make a list of lines of the source file
            ArrayList<String> lineList = new ArrayList<String>();
            StringBuilder curLine = new StringBuilder();
            for (int i = 0; i < code.length(); i++) {
                if (i == code.length() - 1) {
                    curLine.append(code.charAt(i));
                    lineList.add(curLine.toString());
                }
                else if (code.charAt(i) == '\n' || i == code.length() - 1) {
                    lineList.add(curLine.toString());
                    curLine = new StringBuilder();
                }
                else {
                    curLine.append(code.charAt(i));
                }
            }

            // Create a Lexer and tokenize the source
            Lexer l = new Lexer(code);
            List<Token> tokens;
            try {
                tokens = l.lex();
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }

            // Create a Parser and parse the tokens
            Parser p = new Parser(tokens, lineList);
            List<Stmt> program;
            try {
                program = p.parseProgram();
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }

            // Create a FunctionCollector and check and collect functions
            FunctionCollector fc = new FunctionCollector(lineList);
            try {
                for (Stmt stmt : program) {
                    stmt.accept(fc);
                }
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }

            // Check for main function
            if (!fc.getGlobalEnvironment().isDeclared("main") || !fc.getGlobalEnvironment().isFunction("main")) {
                System.out.println("No main function found; program must have a main function as the entry point.");
                return;
            }

            // Create a NameChecker and check identifiers
            NameChecker nc = new NameChecker(fc.getGlobalEnvironment(), lineList);
            for (Stmt stmt : program) {
                stmt.accept(nc);
            }

            // Create a TypeChecker and check types
            TypeChecker tc = new TypeChecker(fc.getGlobalEnvironment(), lineList);
            for (Stmt stmt : program) {
                stmt.accept(tc);
            }

            // Error reporting
            if (nc.errorCount + fc.errorCount + tc.errorCount > 0) {
                // Print out error report
                System.out.println();
                System.out.println((nc.errorCount + fc.errorCount + tc.errorCount) + " errors");
                System.out.println();

                // Print errors in order from the three lists of errors
                int fcErrorIndex = 0;
                int ncErrorIndex = 0;
                int tcErrorIndex = 0;
                while(fcErrorIndex < fc.errors.size() && ncErrorIndex < nc.errors.size() && tcErrorIndex < tc.errors.size()) {
                    Error fcError = fc.errors.get(fcErrorIndex);
                    Error ncError = nc.errors.get(ncErrorIndex);
                    Error tcError = tc.errors.get(tcErrorIndex);
                    int fcErrorLine = fcError.getStartLine();
                    int ncErrorLine = ncError.getStartLine();
                    int tcErrorLine = tcError.getStartLine();
                    if (fcErrorLine <= ncErrorLine && fcErrorLine <= tcErrorLine) {
                        System.out.println(fcError.getMessage());
                        fcErrorIndex++;
                    }
                    else if (ncErrorLine <= fcErrorLine && ncErrorLine <= tcErrorLine) {
                        System.out.println(ncError.getMessage());
                        ncErrorIndex++;
                    }
                    else {
                        System.out.println(tcError.getMessage());
                        tcErrorIndex++;
                    }
                }

                // If there are no more tc errors, print the errors in order from the remaining lists of errors
                while (fcErrorIndex < fc.errors.size() && ncErrorIndex < nc.errors.size()) {
                    Error fcError = fc.errors.get(fcErrorIndex);
                    Error ncError = nc.errors.get(ncErrorIndex);
                    int fcErrorLine = fcError.getStartLine();
                    int ncErrorLine = ncError.getStartLine();
                    if (fcErrorLine <= ncErrorLine) {
                        System.out.println(fcError.getMessage());
                        fcErrorIndex++;
                    }
                    else {
                        System.out.println(ncError.getMessage());
                        ncErrorIndex++;
                    }
                }

                // If there are no more fc errors, print the errors in order from the remaining lists of errors
                while (tcErrorIndex < tc.errors.size() && ncErrorIndex < nc.errors.size()) {
                    Error tcError = tc.errors.get(tcErrorIndex);
                    Error ncError = nc.errors.get(ncErrorIndex);
                    int tcErrorLine = tcError.getStartLine();
                    int ncErrorLine = ncError.getStartLine();
                    if (tcErrorLine <= ncErrorLine) {
                        System.out.println(tcError.getMessage());
                        tcErrorIndex++;
                    }
                    else {
                        System.out.println(ncError.getMessage());
                        ncErrorIndex++;
                    }
                }

                // If there are no more nc errors, print the errors in order from the remaining lists of errors
                while (fcErrorIndex < fc.errors.size() && tcErrorIndex < tc.errors.size()) {
                    Error fcError = fc.errors.get(fcErrorIndex);
                    Error tcError = tc.errors.get(tcErrorIndex);
                    int fcErrorLine = fcError.getStartLine();
                    int tcErrorLine = tcError.getStartLine();
                    if (fcErrorLine <= tcErrorLine) {
                        System.out.println(fcError.getMessage());
                        fcErrorIndex++;
                    }
                    else {
                        System.out.println(tcError.getMessage());
                        tcErrorIndex++;
                    }
                }


                // Print any errors in order that are still present in the last remaining lists of errors
                while(fcErrorIndex < fc.errors.size()) {
                    System.out.println(fc.errors.get(fcErrorIndex++).getMessage());
                }
                while (ncErrorIndex < nc.errors.size()) {
                    System.out.println(nc.errors.get(ncErrorIndex++).getMessage());
                }
                while (tcErrorIndex < tc.errors.size()) {
                    System.out.println(tc.errors.get(tcErrorIndex++).getMessage());
                }

                System.out.println((nc.errorCount + fc.errorCount + tc.errorCount) + " errors");
                System.out.println();
                return; // Do not continue compiling if there were errors
            }
            

            

            SymbolTableConstructor STConstructor = new SymbolTableConstructor();
            for (Stmt stmt : program) {
                stmt.accept(STConstructor);
            }


            // Generate code from the AST
            CodeGenerator generator = new CodeGenerator();
            String assemblyCode = generator.generate(program);
            
            // Write the assembly code to output file or stdout
            if (outputFile != null) {
                Files.writeString(Paths.get(outputFile), assemblyCode);
                System.out.println("Code generated successfully: " + outputFile);
            } else {
                System.out.println(assemblyCode);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
       
}
