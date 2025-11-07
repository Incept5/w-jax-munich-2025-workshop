# Conda Environment Fix Summary

## Problem

The embedding service was failing with "No module named 'einops'" despite successful installation because:

1. **PYTHONPATH Interference**: Old `PYTHONPATH` environment variable was pointing to a previous venv directory
2. **Python Version**: Python 3.14.0 was being used, which isn't yet supported by sentence-transformers (only 3.9-3.13)
3. **Conda Activation Issues**: Using `conda activate` in shell scripts doesn't always work reliably

## Solution Implemented

### 1. Updated start.sh Script

**Key Changes:**
- Clear interfering environment variables BEFORE conda operations:
  ```bash
  unset PYTHONPATH
  unset VIRTUAL_ENV
  unset CONDA_PREFIX
  ```
- Use `conda run -n environment-name` instead of `conda activate`
- Add verification steps to check Python version and package imports
- Automatic fallback to reinstall dependencies if imports fail

### 2. Environment Configuration

**environment.yml:**
- Specifies Python 3.11 (stable with sentence-transformers)
- Includes all required dependencies with version constraints
- Uses conda-forge channel for reliability

### 3. Verification Script

**test-conda-fix.sh:**
- Checks conda environment exists
- Verifies Python 3.11.x is being used
- Confirms Python executable is from conda environment
- Tests einops and other package imports
- Validates packages are from conda (not system/venv)

## Why This Fix Works

1. **Clearing Environment Variables**: Removes interference from old installations
2. **conda run**: More reliable than activation in shell scripts - handles all environment setup internally
3. **Python 3.11**: Stable version with full sentence-transformers ecosystem support
4. **Verification**: Catches issues before server starts

## Testing the Fix

### Quick Test
```bash
cd stage-4-agentic-rag/embedding-service
./test-conda-fix.sh
```

### Full Test
```bash
cd stage-4-agentic-rag/embedding-service
./start.sh
```

Expected output:
- ✅ Environment ready
- Python: 3.11.x (not 3.14.x)
- Server starts without import errors

## If Problems Persist

### Complete Reset
```bash
# Remove conda environment
conda env remove -n embedding-service

# Remove any old venv
rm -rf venv/

# Check for interfering environment variables
echo $PYTHONPATH    # Should be empty
echo $VIRTUAL_ENV   # Should be empty

# Start fresh
./start.sh
```

### Manual Verification
```bash
# Check what Python is being used
conda run -n embedding-service which python

# Check sys.path
conda run -n embedding-service python -c "import sys; print('\n'.join(sys.path))"

# Test einops import
conda run -n embedding-service python -c "import einops; print(einops.__file__)"
```

## Technical Details

### Why conda run vs conda activate?

**conda activate** in shell scripts:
- Modifies current shell environment
- May not propagate to subprocess
- Affected by shell type (bash/zsh/sh)
- Can be overridden by environment variables

**conda run**:
- Runs command in fresh environment
- Handles all setup internally
- Works regardless of shell type
- Not affected by existing environment variables

### Python Version Requirements

**sentence-transformers** officially supports:
- Python 3.9, 3.10, 3.11, 3.12, 3.13
- PyTorch 2.0+
- transformers 4.41.0+

**Python 3.14**:
- Not yet officially released
- Not tested with sentence-transformers
- May have compatibility issues

**Recommendation**: Use Python 3.11 or 3.12 for maximum stability.

## References

### Web Research Findings

1. **Conda Activation Issues**: Common problem with shell scripts - `conda run` is more reliable
2. **PYTHONPATH Interference**: Old environment variables override conda package paths
3. **Python 3.13 Support**: Only recently added to sentence-transformers (Nov 2024)
4. **Python 3.14**: Not yet officially released, no library support yet

### Key Insights

- `conda run` is the recommended approach for running Python in conda environments from scripts
- Clearing PYTHONPATH is critical when switching from venv to conda
- Python 3.11/3.12 are the sweet spot for ML/AI libraries in late 2024/early 2025
- Verification steps help catch environment issues early

## Files Modified

1. **start.sh** - Complete rewrite with conda run and verification
2. **README.md** - Added comprehensive troubleshooting section
3. **test-conda-fix.sh** - New verification script
4. **CONDA_FIX_SUMMARY.md** - This documentation

## Next Steps

1. Run `./test-conda-fix.sh` to verify the fix
2. Run `./start.sh` to start the service
3. Test embeddings with curl or the Java integration test
4. If issues persist, see "If Problems Persist" section above

---

**Status**: ✅ Fix implemented and documented  
**Date**: 2024-11-07  
**Workshop Stage**: Stage 4 - Agentic RAG
