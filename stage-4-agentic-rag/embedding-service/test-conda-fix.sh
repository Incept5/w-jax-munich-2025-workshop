#!/bin/bash
# Test script to verify conda environment fix

cd "$(dirname "$0")"

echo "🧪 Testing Conda Environment Fix"
echo "=================================="
echo

# Clear environment variables
unset PYTHONPATH
unset VIRTUAL_ENV
unset CONDA_PREFIX

# Initialize conda
eval "$(conda shell.bash hook)"

echo "1️⃣ Checking conda environment exists..."
if conda env list | grep -q "^embedding-service "; then
    echo "✅ Environment exists"
else
    echo "❌ Environment not found - run start.sh first to create it"
    exit 1
fi

echo
echo "2️⃣ Checking Python version..."
PYTHON_VERSION=$(conda run -n embedding-service python -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}')")
echo "   Python: $PYTHON_VERSION"
if [[ "$PYTHON_VERSION" == 3.11.* ]]; then
    echo "✅ Correct Python version (3.11.x)"
else
    echo "⚠️  Warning: Expected 3.11.x but got $PYTHON_VERSION"
fi

echo
echo "3️⃣ Checking Python executable location..."
PYTHON_PATH=$(conda run -n embedding-service python -c "import sys; print(sys.executable)")
echo "   Path: $PYTHON_PATH"
if [[ "$PYTHON_PATH" == *"embedding-service"* ]]; then
    echo "✅ Using conda environment Python"
else
    echo "❌ Not using conda environment Python!"
fi

echo
echo "4️⃣ Checking sys.path..."
echo "   First 3 paths in sys.path:"
conda run -n embedding-service python -c "import sys; [print(f'   - {p}') for p in sys.path[:3]]"

echo
echo "5️⃣ Testing einops import..."
if conda run -n embedding-service python -c "import einops" 2>/dev/null; then
    EINOPS_PATH=$(conda run -n embedding-service python -c "import einops; print(einops.__file__)")
    echo "✅ einops imported successfully"
    echo "   Location: $EINOPS_PATH"
    
    if [[ "$EINOPS_PATH" == *"embedding-service"* ]]; then
        echo "✅ einops from conda environment"
    else
        echo "⚠️  Warning: einops not from conda environment"
    fi
else
    echo "❌ Failed to import einops"
    echo "   Running: pip list | grep einops"
    conda run -n embedding-service pip list | grep einops
    exit 1
fi

echo
echo "6️⃣ Testing sentence-transformers import..."
if conda run -n embedding-service python -c "import sentence_transformers" 2>/dev/null; then
    echo "✅ sentence-transformers imported successfully"
else
    echo "❌ Failed to import sentence-transformers"
    exit 1
fi

echo
echo "7️⃣ Testing fastapi import..."
if conda run -n embedding-service python -c "import fastapi" 2>/dev/null; then
    echo "✅ fastapi imported successfully"
else
    echo "❌ Failed to import fastapi"
    exit 1
fi

echo
echo "=================================="
echo "✅ All tests passed!"
echo
echo "The conda environment is correctly configured."
echo "You can now run: ./start.sh"
