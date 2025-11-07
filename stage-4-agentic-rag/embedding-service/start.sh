#!/bin/bash
# Start the local embedding service

cd "$(dirname "$0")"

echo "🚀 Starting Local Embedding Service"
echo

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "📦 Creating virtual environment..."
    python3 -m venv venv
    
    echo "📥 Installing dependencies..."
    source venv/bin/activate
    pip install --upgrade pip
    pip install -r requirements.txt
else
    source venv/bin/activate
fi

echo "✅ Environment ready"
echo "🔄 Starting server on http://localhost:8001"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

python server.py
