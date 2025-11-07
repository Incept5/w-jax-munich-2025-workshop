#!/bin/bash

# Firewall Setup Script for Workshop Database Access
# Manages macOS firewall configuration for PostgreSQL port 5432

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PORT=5432
HOST_IP="172.20.15.241"
DB_USER="workshop"
DB_NAME="workshop_rag"

# Print colored message
print_msg() {
    local color=$1
    shift
    echo -e "${color}$@${NC}"
}

# Print section header
print_header() {
    echo ""
    print_msg "$BLUE" "═══════════════════════════════════════════════════════════════"
    print_msg "$BLUE" "  $1"
    print_msg "$BLUE" "═══════════════════════════════════════════════════════════════"
    echo ""
}

# Check if running on macOS
check_macos() {
    if [[ "$OSTYPE" != "darwin"* ]]; then
        print_msg "$RED" "Error: This script is designed for macOS only"
        exit 1
    fi
}

# Check if Docker container is running
check_docker() {
    if ! docker ps | grep -q "pgvector-db"; then
        print_msg "$YELLOW" "Warning: PostgreSQL container (pgvector-db) is not running"
        print_msg "$YELLOW" "Start it with: docker-compose up -d"
        return 1
    fi
    return 0
}

# Open firewall (Option 1: Disable application firewall)
open_firewall_simple() {
    print_header "Opening Firewall (Simple Method)"
    
    # Check current status
    local status=$(sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate 2>/dev/null | grep -o "enabled\|disabled")
    
    if [[ "$status" == "disabled" ]]; then
        print_msg "$GREEN" "✓ Firewall is already disabled"
    else
        print_msg "$YELLOW" "Disabling macOS application firewall..."
        print_msg "$YELLOW" "(You will be prompted for your admin password)"
        sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off
        print_msg "$GREEN" "✓ Firewall disabled successfully"
    fi
    
    print_msg "$BLUE" "\nNote: The firewall will remain disabled until you run:"
    print_msg "$BLUE" "  $0 close"
}

# Close firewall (Re-enable application firewall)
close_firewall_simple() {
    print_header "Closing Firewall (Re-enabling)"
    
    print_msg "$YELLOW" "Re-enabling macOS application firewall..."
    print_msg "$YELLOW" "(You will be prompted for your admin password)"
    sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate on
    print_msg "$GREEN" "✓ Firewall re-enabled successfully"
}

# Verify setup
verify_setup() {
    print_header "Verifying Workshop Database Setup"
    
    local all_ok=true
    
    # Check 1: Docker container running
    echo "1. Checking Docker container..."
    if docker ps | grep -q "pgvector-db"; then
        print_msg "$GREEN" "   ✓ PostgreSQL container is running"
    else
        print_msg "$RED" "   ✗ PostgreSQL container is NOT running"
        print_msg "$YELLOW" "   → Run: docker-compose up -d"
        all_ok=false
    fi
    
    # Check 2: Port binding
    echo "2. Checking port binding..."
    if docker ps | grep "pgvector-db" | grep -q "0.0.0.0:5432"; then
        print_msg "$GREEN" "   ✓ Port 5432 is bound to all interfaces"
    else
        print_msg "$RED" "   ✗ Port 5432 is NOT properly bound"
        print_msg "$YELLOW" "   → Check docker-compose.yml ports configuration"
        all_ok=false
    fi
    
    # Check 3: Port listening
    echo "3. Checking if port is listening..."
    if netstat -an | grep -q "\.5432.*LISTEN"; then
        print_msg "$GREEN" "   ✓ Port 5432 is listening"
    else
        print_msg "$RED" "   ✗ Port 5432 is NOT listening"
        all_ok=false
    fi
    
    # Check 4: Firewall status
    echo "4. Checking firewall status..."
    local fw_status=$(sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate 2>/dev/null | grep -o "enabled\|disabled")
    if [[ "$fw_status" == "disabled" ]]; then
        print_msg "$GREEN" "   ✓ Firewall is disabled (port is accessible)"
    else
        print_msg "$YELLOW" "   ⚠ Firewall is enabled (may block connections)"
        print_msg "$YELLOW" "   → Run: $0 open"
    fi
    
    # Check 5: Local connection test
    echo "5. Testing local connection..."
    if PGPASSWORD=workshop123 psql -h localhost -p 5432 -U workshop -d workshop_rag -c "SELECT 1" &>/dev/null; then
        print_msg "$GREEN" "   ✓ Local connection successful"
    else
        print_msg "$RED" "   ✗ Local connection failed"
        print_msg "$YELLOW" "   → Check credentials: workshop/workshop123"
        all_ok=false
    fi
    
    # Check 6: Host IP connection test
    echo "6. Testing connection via host IP..."
    if PGPASSWORD=workshop123 psql -h "$HOST_IP" -p 5432 -U workshop -d workshop_rag -c "SELECT 1" &>/dev/null; then
        print_msg "$GREEN" "   ✓ Connection via $HOST_IP successful"
    else
        print_msg "$YELLOW" "   ⚠ Connection via $HOST_IP failed"
        print_msg "$YELLOW" "   → This might be a network configuration issue"
    fi
    
    # Summary
    echo ""
    if $all_ok; then
        print_header "Setup Status: READY ✓"
        print_msg "$GREEN" "Your database is ready for workshop participants!"
        echo ""
        print_msg "$BLUE" "Participants should connect with:"
        print_msg "$BLUE" "  Host: $HOST_IP"
        print_msg "$BLUE" "  Port: 5432"
        print_msg "$BLUE" "  User: $DB_USER"
        print_msg "$BLUE" "  Password: workshop123"
        print_msg "$BLUE" "  Database: $DB_NAME"
    else
        print_header "Setup Status: NEEDS ATTENTION ⚠"
        print_msg "$YELLOW" "Please address the issues above before the workshop"
    fi
    echo ""
}

# Test connection from remote machine (simulation)
test_connection() {
    print_header "Testing Database Connection"
    
    print_msg "$BLUE" "Testing connection to $HOST_IP:$PORT..."
    
    # Test 1: netcat test
    echo "1. Testing port connectivity..."
    if nc -z -w 2 "$HOST_IP" "$PORT" 2>/dev/null; then
        print_msg "$GREEN" "   ✓ Port $PORT is accessible"
    else
        print_msg "$RED" "   ✗ Port $PORT is not accessible"
        print_msg "$YELLOW" "   → Make sure firewall is open: $0 open"
        return 1
    fi
    
    # Test 2: PostgreSQL connection
    echo "2. Testing PostgreSQL connection..."
    if PGPASSWORD=workshop123 psql -h "$HOST_IP" -p "$PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT version();" &>/dev/null; then
        print_msg "$GREEN" "   ✓ PostgreSQL connection successful"
        
        # Get database info
        local version=$(PGPASSWORD=workshop123 psql -h "$HOST_IP" -p "$PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT version();" 2>/dev/null | head -n1 | xargs)
        local doc_count=$(PGPASSWORD=workshop123 psql -h "$HOST_IP" -p "$PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM documents;" 2>/dev/null | xargs)
        
        echo ""
        print_msg "$BLUE" "Database Information:"
        print_msg "$BLUE" "  Version: $version"
        print_msg "$BLUE" "  Documents: $doc_count"
    else
        print_msg "$RED" "   ✗ PostgreSQL connection failed"
        print_msg "$YELLOW" "   → Check credentials and database status"
        return 1
    fi
    
    echo ""
    print_msg "$GREEN" "Connection test successful! Participants should be able to connect."
}

# Show status
show_status() {
    print_header "Current Status"
    
    # Docker
    echo "Docker Container:"
    if docker ps | grep -q "pgvector-db"; then
        print_msg "$GREEN" "  ✓ Running"
        docker ps | grep "pgvector-db" | awk '{print "    Container: " $1 "\n    Status: " $5 " " $6 " " $7 "\n    Ports: " $NF}'
    else
        print_msg "$RED" "  ✗ Not running"
    fi
    
    echo ""
    
    # Firewall
    echo "Firewall:"
    local fw_status=$(sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate 2>/dev/null | grep -o "enabled\|disabled")
    if [[ "$fw_status" == "disabled" ]]; then
        print_msg "$GREEN" "  ✓ Disabled (port is open)"
    else
        print_msg "$YELLOW" "  ⚠ Enabled (may block connections)"
    fi
    
    echo ""
    
    # Port
    echo "Port $PORT:"
    if netstat -an | grep -q "\.5432.*LISTEN"; then
        print_msg "$GREEN" "  ✓ Listening"
    else
        print_msg "$RED" "  ✗ Not listening"
    fi
    
    echo ""
    
    # Network
    echo "Host IP Addresses:"
    ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print "  " $2}'
    
    echo ""
}

# Print usage
usage() {
    print_header "Workshop Database Firewall Setup"
    
    cat << EOF
Usage: $0 <command>

Commands:
  open        Open firewall for workshop (disables macOS firewall)
  close       Close firewall after workshop (re-enables macOS firewall)
  verify      Verify complete setup (Docker, port, firewall, connection)
  test        Test database connection
  status      Show current status
  help        Show this help message

Examples:
  $0 open          # Before workshop - open firewall
  $0 verify        # Verify everything is working
  $0 test          # Test connection from your machine
  $0 close         # After workshop - close firewall

Configuration:
  Host IP:    $HOST_IP
  Port:       $PORT
  Database:   $DB_NAME
  User:       $DB_USER
  Password:   workshop123

For detailed instructions, see FIREWALL_SETUP.md
EOF
    echo ""
}

# Main script
main() {
    check_macos
    
    case "${1:-help}" in
        open)
            open_firewall_simple
            echo ""
            print_msg "$YELLOW" "Don't forget to close the firewall after the workshop:"
            print_msg "$YELLOW" "  $0 close"
            ;;
        close)
            close_firewall_simple
            ;;
        verify)
            verify_setup
            ;;
        test)
            test_connection
            ;;
        status)
            show_status
            ;;
        help|--help|-h)
            usage
            ;;
        *)
            print_msg "$RED" "Unknown command: $1"
            echo ""
            usage
            exit 1
            ;;
    esac
}

main "$@"
