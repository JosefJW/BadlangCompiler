#!/bin/bash

# Create output directories if they don't exist
mkdir -p badlang/tests/assembly/simple
mkdir -p badlang/tests/assembly/complex

# Compile all .bl files in a folder
compile_folder() {
    input_folder=$1
    output_folder=$2
    javac -d badlang/target/classes badlang/src/main/java/edu/wisc/*.java
    for file in "$input_folder"/*.bl; do
        [ -e "$file" ] || continue  # skip if no files
        base=$(basename "$file" .bl)
        echo "Compiling $file -> $output_folder/$base.asm"
        java -cp badlang/target/classes edu.wisc.Main "$file" "$output_folder/$base.asm"
    done
}

# Compile simple and complex tests
compile_folder "badlang/tests/simple" "badlang/tests/assembly/simple"
compile_folder "badlang/tests/complex" "badlang/tests/assembly/complex"
