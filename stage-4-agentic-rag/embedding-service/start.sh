#!/bin/bash
# Start the local embedding service using conda

cd "$(dirname "$0")"

echo "🚀 Starting Local Embedding Service"
echo

# Clear any interfering environment variables FIRST
unset PYTHONPATH
unset VIRTUAL_ENV
unset CONDA_PREFIX

# Determine conda base path
if [ -d "/opt/miniconda3" ]; then
    CONDA_BASE="/opt/miniconda3"
elif [ -d "$HOME/miniconda3" ]; then
    CONDA_BASE="$HOME/miniconda3"
elif [ -d "$HOME/anaconda3" ]; then
    CONDA_BASE="$HOME/anaconda3"
else
    echo "❌ Error: Could not find conda installation"
    echo "Please install Miniconda or Anaconda first:"
    echo "  https://docs.conda.io/en/latest/miniconda.html"
    exit 1
fi

# Set environment paths
ENV_PATH="$CONDA_BASE/envs/embedding-service"
PYTHON_BIN="$ENV_PATH/bin/python"

# Check if conda environment exists
if [ ! -d "$ENV_PATH" ]; then
    echo "📦 Creating conda environment (Python 3.11)..."
    
    # Check if conda command is available
    if ! command -v conda &> /dev/null; then
        echo "❌ Error: conda command not found in PATH"
        echo "Please add conda to your PATH or run: source $CONDA_BASE/etc/profile.d/conda.sh"
        exit 1
    fi
    
    conda env create -f environment.yml
    
    if [ $? -ne 0 ]; then
        echo "❌ Failed to create conda environment"
        exit 1
    fi
fi

# Verify Python executable exists
if [ ! -f "$PYTHON_BIN" ]; then
    echo "❌ Error: Python not found at $PYTHON_BIN"
    echo "Environment may be corrupted. Try: conda env remove -n embedding-service"
    exit 1
fi

# Verify environment setup
echo "✅ Verifying conda environment..."
PYTHON_VERSION=$($PYTHON_BIN -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}')")
echo "   Python: $PYTHON_VERSION (at $PYTHON_BIN)"

# Verify einops is accessible
if ! $PYTHON_BIN -c "import einops" 2>/dev/null; then
    echo "❌ Error: einops not found in conda environment"
    echo "   Attempting to reinstall dependencies..."
    $ENV_PATH/bin/pip install einops>=0.8.1
fi

echo "✅ Environment ready"
echo "🔄 Starting server on http://localhost:8001"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Use direct Python path for reliability
$PYTHON_BIN server.py
