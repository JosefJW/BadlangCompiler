# Notes and Additions

## Architecture
MIPS

## Build Instructions

## Testing

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
