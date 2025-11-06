
#!/bin/bash

# Build the project if JAR doesn't exist or source files are newer
if [ ! -f target/stage-1-simple-agent.jar ] || [ src/ -nt target/stage-1-simple-agent.jar ]; then
    echo "Building project..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "Build failed!"
        exit 1
    fi
    echo ""
fi

# Run the application with all arguments passed through
java -jar target/stage-1-simple-agent.jar "$@"
