# Firewall Setup - Summary for Workshop Instructor

## Quick Actions

### Before Workshop (5 minutes)
```bash
cd stage-4-agentic-rag

# 1. Start database
docker-compose up -d

# 2. Open firewall
./firewall-setup.sh open
# Enter your admin password when prompted

# 3. Verify everything works
./firewall-setup.sh verify

# 4. Test connection
psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag
# Password: workshop123
```

### During Workshop
- Database is accessible at `172.20.15.241:5432`
- Participants use `./run.sh --shared` to connect
- Monitor with: `docker logs -f stage4-pgvector`

### After Workshop (2 minutes)
```bash
# Close firewall
./firewall-setup.sh close

# Optionally stop database
docker-compose down
```

---

## What Was Set Up

### 1. Firewall Management Script
**File**: `firewall-setup.sh`

A comprehensive bash script that:
- ✅ Opens macOS firewall (disables temporarily)
- ✅ Closes macOS firewall (re-enables)
- ✅ Verifies complete setup (Docker, port, connection)
- ✅ Tests database connectivity
- ✅ Shows current status
- ✅ Color-coded output for clarity

**Usage**:
```bash
./firewall-setup.sh open     # Before workshop
./firewall-setup.sh verify   # Check everything
./firewall-setup.sh test     # Test connection
./firewall-setup.sh status   # Show current state
./firewall-setup.sh close    # After workshop
./firewall-setup.sh help     # Show all commands
```

### 2. Documentation Created

**FIREWALL_QUICKSTART.md** (5-minute guide)
- Before/during/after workshop checklists
- Connection details for participants
- Quick troubleshooting
- Essential commands only

**FIREWALL_SETUP.md** (Complete reference)
- Two methods: Simple (disable firewall) vs. Advanced (pf rules)
- Detailed verification steps
- Comprehensive troubleshooting
- Security considerations
- Alternative approaches (SSH tunnels)
- Workshop day checklist

**FIREWALL_SUMMARY.md** (This file)
- Quick reference for instructor
- What was set up
- How it works
- Testing procedures

### 3. README Updates
Added firewall setup section to main README.md:
- Links to firewall documentation
- Quick command reference
- Positioned for easy instructor access

---

## How It Works

### macOS Firewall Architecture

macOS has two firewall mechanisms:

1. **Application Firewall** (Socket Filter Framework)
   - Default macOS firewall
   - Controls application-level network access
   - Managed via System Preferences → Security & Privacy → Firewall

2. **Packet Filter (pf)** (Advanced)
   - BSD-style packet filtering
   - More granular control
   - Configured via `/etc/pf.conf`

### Our Approach: Simple Method

For workshop simplicity, we **temporarily disable the Application Firewall**:

**Why this approach?**
- ✅ Simple: One command
- ✅ Effective: Immediately allows all incoming connections
- ✅ Reversible: Easy to re-enable after workshop
- ✅ Workshop-safe: Trusted local network environment

**Command used:**
```bash
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off
```

**Security note**: This is acceptable for a workshop environment on a trusted network. For production or untrusted networks, use the advanced pf-based method in FIREWALL_SETUP.md.

### Docker Networking

The PostgreSQL container is configured to bind to **all interfaces**:

```yaml
# docker-compose.yml
services:
  db:
    ports:
      - "0.0.0.0:5432:5432"  # ← Binds to all interfaces
```

This means:
- `localhost:5432` → Works from host machine
- `172.20.15.241:5432` → Works from network
- `0.0.0.0:5432` → Listens on all available IPs

**Without firewall open**: Docker binding alone is not enough - macOS blocks incoming connections.

**With firewall open**: Participants on same network can connect.

---

## Verification Checklist

### Pre-Workshop Verification

Run through this checklist to ensure everything is ready:

```bash
# 1. Database running
docker ps | grep pgvector
# ✓ Should show: 0.0.0.0:5432->5432/tcp

# 2. Port listening
netstat -an | grep 5432
# ✓ Should show: tcp4  0  0  *.5432  *.*  LISTEN

# 3. Firewall disabled
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate
# ✓ Should show: Firewall is disabled

# 4. Local connection works
PGPASSWORD=workshop123 psql -h localhost -p 5432 -U workshop -d workshop_rag -c "SELECT 1"
# ✓ Should show: 1

# 5. Host IP connection works
PGPASSWORD=workshop123 psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag -c "SELECT 1"
# ✓ Should show: 1

# 6. Document count correct
PGPASSWORD=workshop123 psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag -c "SELECT COUNT(*) FROM documents"
# ✓ Should show: 487 (or similar)
```

**Or use the automated script:**
```bash
./firewall-setup.sh verify
```

### During Workshop Testing

**Have a participant test connectivity:**
```bash
# From their machine
nc -zv 172.20.15.241 5432
# ✓ Should show: Connection succeeded

psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag
# ✓ Should connect successfully
```

---

## Troubleshooting Guide

### Problem: Participants can't connect

**Diagnosis steps:**
```bash
# Step 1: Check firewall is actually disabled
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate
# If "enabled" → Run: ./firewall-setup.sh open

# Step 2: Check database is running
docker ps | grep pgvector
# If not showing → Run: docker-compose up -d

# Step 3: Check port binding
docker ps | grep pgvector | grep "0.0.0.0:5432"
# If not showing → Check docker-compose.yml

# Step 4: Test from your machine
psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag
# If fails → Database configuration issue
# If works → Network or participant machine issue

# Step 5: Check participant's network
# Are they on the same WiFi/network?
# Is their machine's firewall blocking outbound connections?
```

### Problem: "Connection refused"

**Most likely causes:**
1. Firewall still enabled → `./firewall-setup.sh open`
2. Database not running → `docker-compose up -d`
3. Wrong IP address → Verify with `ifconfig | grep inet`

### Problem: "Authentication failed"

**Check credentials:**
- Username: `workshop` (not `postgres`)
- Password: `workshop123` (not `password`)
- Database: `workshop_rag` (not `postgres`)

### Problem: Port already in use

```bash
# Check what's using port 5432
sudo lsof -i :5432

# If it's another PostgreSQL
brew services list | grep postgresql
brew services stop postgresql

# Restart container
docker-compose restart db
```

---

## Security Notes

### During Workshop

**Current setup is intentionally simple:**
- Weak credentials (`workshop/workshop123`)
- Firewall disabled
- Database accessible to entire network

**This is acceptable because:**
- ✅ Workshop environment (controlled setting)
- ✅ Trusted local network
- ✅ Limited duration (few hours)
- ✅ No sensitive data
- ✅ Easy cleanup

### After Workshop

**Important: Close the firewall!**
```bash
./firewall-setup.sh close
```

**Consider:**
- Stopping the database: `docker-compose down`
- Clearing data: `docker-compose down -v`
- Changing credentials if keeping database running

### For Production

**Never use this setup for production!**

Instead:
- Use strong, unique credentials
- Keep firewall enabled with specific pf rules
- Use SSL/TLS for connections
- Implement IP whitelisting
- Use SSH tunnels for remote access
- Enable PostgreSQL authentication logs
- Regular security updates

See FIREWALL_SETUP.md "Alternative: SSH Tunnel" section for secure remote access patterns.

---

## Command Reference

### Firewall Management
```bash
./firewall-setup.sh open      # Open firewall
./firewall-setup.sh close     # Close firewall
./firewall-setup.sh verify    # Full system check
./firewall-setup.sh test      # Test connection
./firewall-setup.sh status    # Show status
```

### Database Management
```bash
docker-compose up -d          # Start database
docker-compose down           # Stop database
docker-compose down -v        # Stop + delete data
docker-compose restart db     # Restart database
docker logs -f stage4-pgvector # View logs
```

### Connection Testing
```bash
# Local test
psql -h localhost -p 5432 -U workshop -d workshop_rag

# Host IP test
psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag

# Port connectivity test
nc -zv 172.20.15.241 5432

# Query database
PGPASSWORD=workshop123 psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag -c "SELECT COUNT(*) FROM documents"
```

### Manual Firewall Commands
```bash
# Check status
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate

# Disable
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off

# Enable
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate on
```

---

## Files Created

```
stage-4-agentic-rag/
├── firewall-setup.sh           # Automated management script ✨
├── FIREWALL_QUICKSTART.md      # 5-minute quick start guide
├── FIREWALL_SETUP.md           # Complete reference documentation
├── FIREWALL_SUMMARY.md         # This file (instructor summary)
└── README.md                   # Updated with firewall section
```

All scripts are executable and tested on macOS.

---

## Next Steps

### Before Workshop
1. ✅ Read this summary
2. ✅ Run `./firewall-setup.sh verify`
3. ✅ Test connection from another device if possible
4. ✅ Write connection details on whiteboard
5. ✅ Have FIREWALL_QUICKSTART.md ready for reference

### During Workshop
1. ✅ Monitor: `docker logs -f stage4-pgvector`
2. ✅ Help participants connect
3. ✅ Have troubleshooting commands ready

### After Workshop
1. ✅ Run `./firewall-setup.sh close`
2. ✅ Optionally: `docker-compose down`
3. ✅ Consider keeping for local development

---

## Questions?

**During setup**: See FIREWALL_SETUP.md for detailed troubleshooting  
**Quick reference**: See FIREWALL_QUICKSTART.md  
**Verification**: Run `./firewall-setup.sh verify`  
**Status check**: Run `./firewall-setup.sh status`

**Emergency**: If something breaks, you can always:
```bash
# Reset everything
./firewall-setup.sh close
docker-compose down -v
docker-compose up -d
./ingest.sh
./firewall-setup.sh open
```

---

**Created**: 2025-11-07  
**Workshop**: W-JAX Munich 2025  
**Stage**: 4 (Agentic RAG)
