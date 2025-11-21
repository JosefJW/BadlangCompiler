# Notes and Additions

## Architecture
MIPS

## Build Instructions
To compile the project, run:
```bash
javac -d badlang/target/classes badlang/src/main/java/edu/wisc/*.java
```
To run the compiled code, run:
```bash
java -cp badlang/target/classes edu.wisc.Main <Target Badlang File>
```

## Compiling
The compiler takes many steps to compile a program. These will all be described here.

This compiler follows this pipeline:
Lexing --> Parsing --> Function Collecting --> Name Analysis --> Type Analysis --> Variable Renaming --> Symbol Table Construction --> Code Generation

These are all described in depth below.

### Lexing
The first step is lexing. The compiler will pass over a text file containing Badlang code and separate it into valid tokens. If an unrecognized character appears, the compiler will halt and inform the user without generating any code.

### Parsing
After lexing, the tokens are parsed into expressions and statements. This is the step where the AST is formed. If an unrecognized string of tokens is found, the compiler will halt and inform the user without generating any code.

### Function Collecting
After parsing, the compiler goes through the top level of the AST, collecting function signatures and storing them in an environment. This is done to hoist the functions, so that they can be called from anywhere in the code. If a function is declared twice, the compiler will store this information as an Error, but it will continue with Name Analysis and Type Analysis before halting.

### Name Analysis
After collecting the function signatures, the compiler will then perform name analysis. Name analysis ensures that variables are declared in an appropriate scope when the variable is used. It also looks for name collisions between variables and functions. Any issues discovered here will be stored as an Error, but the compiler will continue with Type Analysis before halting.

Please note: Although function signatures are collected first, when name collisions (such as a variable and function sharing the same name), the compiler will adhere to declaration order for reporting the error.
That is, for the following code:
```{badlang}
int x = 5;

fun int x() { return 4; }
```
will result in the Error:
```
Name Error
~~~~~~~~~~~~~~~~~~~
3 | fun int x() {
  | ^^^^^^^^^
  | Identifier x was previously used to define a variable; variables and functions cannot share names.
```
Although the function collecting step happens before any variables are saved, the Name Error will still be reported for the function 'x()', not the variable 'x'.

### Type Analysis
After performing name analysis, the compiler will perform type analysis. Type analysis ensures that all literals, variables, and function calls are used in ways that are consistent with their type. Any issues discovered here will be stored as an error. After type analysis, if there were any errors found during Function Collecting, Name Analysis, or Type Analysis, they will be reported in an Error Report and the compiler will halt.

### Variable Renaming
If there were no errors found with any of the above, we know that the Badlang code is semantically correct. With that, we can start the process of compiling the code to MIPS. Our first step in this part of the process is giving all the variables and functions unique identifiers. We do this so that we can use an unscoped symbol table when performing the actual code generation. Here is an example:
```
fun int main() {
   int x = 5;
   if (x == 5) {
      print x;
      int x = 7;
      print x;
   }
   print x;
}
```
Due to scoping rules, this program prints "575":
- The first x refers to the outer variable.
- The second x refers to the inner (shadowing) variable.
- The last x again refers to the outer variable.
However, since our symbol table is unscoped, we cannot reference all of these as "x" because the outer and inner x will then map to the same thing. So, variable renaming may remake this function as:
```
fun int main() {
   int x_0 = 5;
   if (x_0 == 5) {
      print x_0;
      int x_1 = 7;
      print x_1;
   }
   print x_0;
}
```
The variable renamer walks the AST with full knowledge of scope, generating and assigning unique identifiers for all bindings. Each usage of an identifier is then rewritten to use the unique name associated with the identifier at that scope. After renaming, all identifiers in the program are globally unique, which greatly simplifies what our symbol table will need to track.

### Symbol Table Construction
With each identifier having a completely unique binding, we can begin constructing the symbol table. 

For variables, the symbol table stores:
- Name
- Type
- Offset
- Initial Value (if global variable)

For functions, the symbol table stores:
- Name
- Return Type
- An inner symbol table with the function's Local Variables

With this information, we will know how to refer to variables during code generation, and we will know how much space a function needs in its stack frame to store its local variables.

For example, a Badlang file containing the following:
```
int global = 0;
fun int add(int a, int b, int c, int d) {
   int addition1 = a + b;
   int addition2 = c + d;
   int addition3 = addition1 + addition2;
   return addition;
}
```
Will have this corresponding section in the symbol table:
```
global_0 : int (VARIABLE), offset=0, initial=0
add_1 : int (FUNCTION)
  LocalVars:
    addition1_6 : int (VARIABLE), offset=0
    addition2_7 : int (VARIABLE), offset=4
    a_2 : int (PARAMETER), offset=0
    b_3 : int (PARAMETER), offset=4
    c_4 : int (PARAMETER), offset=8
    d_5 : int (PARAMETER), offset=12
    addition3_8 : int (VARIABLE), offset=8
```
Notice that each identifier now has a "_[#]" appended to it. Remember that this is done by the Variable Renamer to ensure unique identifiers.
Additionally, notice that the local variables are separate from the global variables and that the local variable offsets are separate from the parameter offsets.

### Code Generation
Finally, we turn the AST into MIPS assembly code. 

#### Globals
Global variables are emitted into the MIPS .data section. Each global is assigned a label using its unique identifier, and its initial value is emitted using .word.

#### Functions
Every function is emitted into the .text section under a label assigned by its unique identifier. Each function follows a uniform calling convention:
1. Prologue
- Allocates a stack frame large enough to hold all local variables.
- Saves the return address ($ra) and frame pointer ($fp).
2. Body
- The actual statements within the function.
3. Epilogue
- Restores the return address ($ra) and frame pointer ($fp).
- Deallocates the stack frame.
- Jumps to $ra.

#### Expressions
Expressions are solved with a stack-based approach.
- Literal and variable values are pushed onto the stack.
- Operand expressions pop the values from the stack, perform the operation, and push the result.

## Testing

### QtSpim
All assembly code was tested using QtSpim MIPS simulator.

QtSpim may automatically add an exception handler and other code to the beginning of a file. To avoid this, assembly generated by this compiler should be run without QtSpim’s exception handler enabled. To disable this:
1. In the top-left of QtSpim, open the "Simulator" tab.
2. Open "Settings".
3. In settings, open the "MIPS" tab.
4. Uncheck the box that says "Load Exception Handler".
5. Close out of settings.
6. In the top-left of QtSpim, press the net icon to reinitialize the simulator.
7. Load and run your assembly files.

### Complex Tests
For the complex tests, you will notice that for each .bl file, there is a corresponding .c file. These .c files are virtually identical to the .bl files, except the function definitions and print statements are switched to match with C. Since these are tests for the compiler, logic errors in any of the complex tests are not of particular importance, so these .c files are to ensure that the output from my compiler is consistent with a known working compiler rather than me getting stuck on any small logic errors in the Badlang code.

### Compiling Tests
In the tests folder, there is the compileAll.sh file. This file simply runs my compiler on all of the tests Badlang files. It can be run with:
```
sh badlang/tests/compileAll.sh
```
from outside of the badlang folder.

### Symbol Table
If you would like to view the symbol table for any compiled program, simply change the class variable 'debug' in Main.java from false to true. When true, the symbol table will be printed for each compiled program.

## Rules

### Executable Code
All executable code must reside inside of a function. Only global variable declarations and function declarations may be outside of a function.

### Main Function
A main function must exist as the starting point of the code.

### Global Variables
The initial values for global variables must be known at compile time. That is, a global variable cannot be initialized to a function call.

Setting a global variable to the result of a function call within a function is ok.

## Behaviors

### Print Statements
After years of tumultuous development, compiler scientists at the University of Wisconsin - Madison are happy to announce the next innovation in Badlang printing technology. The printsp and println commands!

```
print [expr];
```
We all know it. We all love it. Does exactly what it says it will do, prints the expression and nothing more.

```
printsp [expr];
```
An exciting new edition to the output apparatus. This lovely new statement will print the expression followed by a space.

```
println [expr];
```
But they didn't stop there. With the println command, you can print an expression followed by a whole linebreak!

### Booleans
For purposes such as printing, booleans will be treated as 1s and 0s, with true equalling 1 and false equalling 0.

This affects how the compiler internally handles booleans at the code generation step and if a boolean is printed, it will print the number equivalent.

Booleans still may not be used as integers in arithmetic.

## New Features

### Modulo
The modulo '%' operator is now a part of Badlang. It can be used as part of expressions and has the same precedence as multiplication and division.
```
10 % 3;
```

## Limitations and Issues



# P5: Badlang Code Generation

## Overview

**Congratulations!** You've made it to the final programming assignment in the compilers course! 

In this assignment, you will implement **machine code generation** for the badlang programming language. This is where your compiler finally produces executable code that can run on real (or simulated) hardware. You will transform your abstract syntax tree (AST) into actual assembly instructions.

This assignment is **open-ended** by design. Code generation is complex and requires you to make many design decisions. You have the freedom to choose your target architecture and implementation strategy.

## Target Architecture: Your Choice!

You have two options for your target architecture:

 **Option 1: MIPS Assembly** (Recommended)
- Covered extensively in lecture
- Simple, regular instruction set
- Easy to test with SPIM simulator


 **Option 2: x86 Assembly**
- More complex but widely used

Choose the architecture you're more comfortable with. MIPS is recommended if you followed along with the lecture examples.

## Requirements

1. **Generate Valid Assembly Code**
   - Your compiler must output valid assembly code for your chosen architecture that:
     - Can be assembled and run (in a simulator or on real hardware)
     - Correctly implements the semantics of the badlang program
     - Produces the expected output

2. **Testing Strategy**
   - Since testing is not simple, you will need to
     - Use a simulator (e.g., SPIM for MIPS)
     - Create test programs and verify their output
     - Document your testing approach

## Deliverables

Submit a zip file of the `P5` folder containing:

1. **Your source code**: All Java files for your code generator
2. **Tests**: At least 10 example programs that demonstrate different language features
3. **README additions**: Add a section to this README documenting:
   - Which architecture you chose (MIPS or x86)
   - How to build and run your compiler
   - How you tested the generated code
   - Known limitations or issues
   - **If you worked with a partner, include both names at the top**

## Grading Rubric

Your assignment will be graded on:

### 1. **Correctness (50%)**
- Does the generated code run correctly?
- Does it produce the expected output?

### 2. **Completeness (25%)**
- Are all language features implemented?
- Literals, variables, expressions
- Control flow (if, while)
- Functions and calling conventions
- Print statements

### 3. **Code Quality (10%)**
- Is your code generator well-organized?
- Is the generated assembly code readable?
- Did you follow good software engineering practices?
- Is your code well-commented?

### 4. **Testing (15%)**
- Did you create comprehensive test programs?
- Did you test all language features?
- Did you document your testing approach?

**Remember**: This assignment is open-ended by design. There's no single "correct" approach. Make reasonable design decisions, document them, and focus on getting working code that passes your tests.

--

## Resources
- MIPS tutorial: https://minnie.tuhs.org/CompArch/Resources/mips_quick_tutorial.html
- code generation with MIPS notes: https://pages.cs.wisc.edu/~aws/courses/cs536/readings/codegen.html
- This is the MIPS simulator: https://spimsimulator.sourceforge.net
