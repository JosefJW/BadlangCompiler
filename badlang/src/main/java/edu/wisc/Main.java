package edu.wisc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
            // TODO: Parse the input file to create an AST
            // This assumes you've integrated your parser from P3
            
            // TODO: Perform semantic analysis (name analysis and type checking from P4)
            
            // TODO: Generate code from the AST
            // CodeGenerator generator = new CodeGenerator();
            // String assemblyCode = generator.generate(ast);
            
            // TODO: Write the assembly code to output file or stdout
            // if (outputFile != null) {
            //     Files.writeString(Paths.get(outputFile), assemblyCode);
            //     System.out.println("Code generated successfully: " + outputFile);
            // } else {
            //     System.out.println(assemblyCode);
            // }
            
            System.out.println("Code generation not yet implemented!");
            System.out.println("Your task: Implement a code generator that produces MIPS or x86 assembly code.");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
       
}
