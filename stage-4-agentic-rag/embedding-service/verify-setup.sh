
#!/bin/bash
# Verify the conda environment setup is correct

cd "$(dirname "$0")"

echo "🔍 Verifying Embedding Service Setup"
echo "======================================"
echo

# Determine conda base path
if [ -d "/opt/miniconda3" ]; then
    CONDA_BASE="/opt/miniconda3"
elif [ -d "$HOME/miniconda3" ]; then
    CONDA_BASE="$HOME/miniconda3"
elif [ -d "$HOME/anaconda3" ]; then
    CONDA_BASE="$HOME/anaconda3"
else
    echo "❌ Conda not found"
    exit 1
fi

ENV_PATH="$CONDA_BASE/envs/embedding-service"
PYTHON_BIN="$ENV_PATH/bin/python"

# Check environment exists
if [ ! -d "$ENV_PATH" ]; then
    echo "❌ Environment 'embedding-service' does not exist"
    echo "   Run: conda env create -f environment.yml"
    exit 1
fi

echo "✅ Conda environment exists at: $ENV_PATH"

# Check Python version
if [ ! -f "$PYTHON_BIN" ]; then
    echo "❌ Python executable not found at: $PYTHON_BIN"
    exit 1
fi

PYTHON_VERSION=$($PYTHON_BIN --version 2>&1)
echo "✅ Python: $PYTHON_VERSION"

# Check it's the right version
PYTHON_MAJOR=$($PYTHON_BIN -c "import sys; print(sys.version_info.major)")
PYTHON_MINOR=$($PYTHON_BIN -c "import sys; print(sys.version_info.minor)")

if [ "$PYTHON_MAJOR" != "3" ] || [ "$PYTHON_MINOR" != "11" ]; then
    echo "⚠️  Warning: Expected Python 3.11, got $PYTHON_VERSION"
fi

# Check key packages
echo
echo "📦 Checking required packages:"

check_package() {
    local package=$1
    if $PYTHON_BIN -c "import $package" 2>/dev/null; then
        local version=$($PYTHON_BIN -c "import $package; print($package.__version__)" 2>/dev/null || echo "unknown")
        echo "   ✅ $package ($version)"
        return 0
    else
        echo "   ❌ $package (not found)"
        return 1
    fi
}

ALL_OK=true
check_package "einops" || ALL_OK=false
check_package "fastapi" || ALL_OK=false
check_package "uvicorn" || ALL_OK=false
check_package "sentence_transformers" || ALL_OK=false
check_package "torch" || ALL_OK=false

echo
if [ "$ALL_OK" = true ]; then
    echo "✅ All checks passed! Environment is ready."
    echo
    echo "To start the server, run:"
    echo "  ./start.sh"
    exit 0
else
    echo "❌ Some packages are missing. Try reinstalling:"
    echo "  conda env remove -n embedding-service"
    echo "  conda env create -f environment.yml"
    exit 1
fi
