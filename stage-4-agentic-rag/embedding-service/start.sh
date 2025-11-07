#!/bin/bash
# Start the local embedding service using conda

cd "$(dirname "$0")"

echo "🚀 Starting Local Embedding Service"
echo

# Clear any interfering environment variables FIRST
unset PYTHONPATH
unset VIRTUAL_ENV
unset CONDA_PREFIX

# Check if conda is installed
if ! command -v conda &> /dev/null; then
    echo "❌ Error: conda is not installed or not in PATH"
    echo "Please install Miniconda or Anaconda first:"
    echo "  https://docs.conda.io/en/latest/miniconda.html"
    exit 1
fi

# Initialize conda for bash
eval "$(conda shell.bash hook)"

# Check if conda environment exists
if ! conda env list | grep -q "^embedding-service "; then
    echo "📦 Creating conda environment (Python 3.11)..."
    conda env create -f environment.yml
    
    if [ $? -ne 0 ]; then
        echo "❌ Failed to create conda environment"
        exit 1
    fi
fi

# Verify environment setup
echo "✅ Verifying conda environment..."
PYTHON_VERSION=$(conda run -n embedding-service python -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}')")
echo "   Python: $PYTHON_VERSION"

# Verify einops is accessible
if ! conda run -n embedding-service python -c "import einops" 2>/dev/null; then
    echo "❌ Error: einops not found in conda environment"
    echo "   Attempting to reinstall dependencies..."
    conda run -n embedding-service pip install einops>=0.8.1
fi

echo "✅ Environment ready"
echo "🔄 Starting server on http://localhost:8001"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Use conda run instead of activation for reliability
conda run -n embedding-service python server.py
