# macOS Firewall Setup for Workshop Database Access

## Overview

This guide explains how to configure the macOS firewall to allow workshop participants to connect to the PostgreSQL database running on your host machine.

**Host IP**: `172.20.15.241`  
**Port**: `5432` (PostgreSQL)  
**Duration**: Workshop hours only

---

## Quick Start (For Workshop Day)

### 1. Open Port 5432

```bash
# Navigate to the stage-4-agentic-rag directory
cd stage-4-agentic-rag

# Run the firewall setup script
./firewall-setup.sh open
```

### 2. Verify Port is Open

```bash
# Check if port is listening
./firewall-setup.sh verify
```

### 3. After Workshop: Close Port

```bash
# Close port 5432
./firewall-setup.sh close
```

---

## Detailed Setup Instructions

### Option 1: Using macOS System Preferences (Recommended for Workshop)

The simplest approach for a workshop environment:

1. **Disable the macOS Application Firewall** (temporarily during workshop):
   ```bash
   # Check current firewall status
   sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate
   
   # Disable firewall (requires admin password)
   sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off
   ```

2. **Verify Docker Port Binding**:
   ```bash
   # Confirm PostgreSQL is listening on all interfaces
   docker ps | grep postgres
   # Should show: 0.0.0.0:5432->5432/tcp
   
   # Verify with netstat
   netstat -an | grep 5432
   # Should show: tcp4  0  0  *.5432  *.*  LISTEN
   ```

3. **Test Remote Connection**:
   ```bash
   # From your machine (should work)
   psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag
   
   # Have a participant test from their machine
   # They should use your IP: 172.20.15.241
   ```

4. **After Workshop - Re-enable Firewall**:
   ```bash
   sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate on
   ```

### Option 2: Using Packet Filter (pf) - More Granular Control

If you prefer to keep the firewall enabled and only open port 5432:

1. **Create PF Rule File**:
   ```bash
   # Create a rule to allow port 5432
   sudo tee /etc/pf.anchors/workshop << 'EOF'
   # Allow incoming connections on port 5432 (PostgreSQL)
   pass in proto tcp from any to any port 5432
   EOF
   ```

2. **Load the Rule**:
   ```bash
   # Add anchor to main pf.conf (if not already present)
   if ! grep -q "anchor \"workshop\"" /etc/pf.conf; then
     echo 'anchor "workshop"' | sudo tee -a /etc/pf.conf
     echo 'load anchor "workshop" from "/etc/pf.anchors/workshop"' | sudo tee -a /etc/pf.conf
   fi
   
   # Enable pf and load rules
   sudo pfctl -e  # Enable pf (may already be enabled)
   sudo pfctl -f /etc/pf.conf  # Reload configuration
   ```

3. **Verify PF Rules**:
   ```bash
   # Check if pf is enabled
   sudo pfctl -s info | grep Status
   
   # View current rules
   sudo pfctl -s rules
   ```

4. **After Workshop - Remove Rule**:
   ```bash
   # Remove the workshop anchor
   sudo sed -i '' '/anchor "workshop"/d' /etc/pf.conf
   sudo sed -i '' '/load anchor "workshop"/d' /etc/pf.conf
   sudo rm /etc/pf.anchors/workshop
   
   # Reload pf configuration
   sudo pfctl -f /etc/pf.conf
   ```

---

## Verification Steps

### 1. Check Docker Container Status

```bash
# Verify container is running and port is bound
docker ps -a | grep pgvector

# Should see something like:
# pgvector-db ... Up ... 0.0.0.0:5432->5432/tcp
```

### 2. Check Port Binding on Host

```bash
# Check if port 5432 is listening
sudo lsof -i :5432

# Or use netstat
netstat -an | grep 5432

# Should show:
# tcp4  0  0  *.5432  *.*  LISTEN
```

### 3. Test Local Connection

```bash
# Test from localhost
psql -h localhost -p 5432 -U workshop -d workshop_rag

# Test from host IP
psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag

# Password: workshop123
```

### 4. Test Remote Connection (From Another Machine)

```bash
# From a participant's machine
psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag

# If psql is not installed, use telnet to test port
telnet 172.20.15.241 5432

# Or use nc (netcat)
nc -zv 172.20.15.241 5432
```

---

## Troubleshooting

### Port Still Blocked

1. **Check if Little Snitch or other security software is running**:
   - Some security applications may block connections even with firewall disabled
   - Temporarily disable them for testing

2. **Verify Docker networking**:
   ```bash
   # Restart Docker if needed
   # Docker Desktop: Click whale icon → Restart
   
   # Or restart container
   docker-compose -f stage-4-agentic-rag/docker-compose.yml restart db
   ```

3. **Check for other services on port 5432**:
   ```bash
   # See what's using port 5432
   sudo lsof -i :5432
   
   # If another PostgreSQL is running
   brew services list | grep postgresql
   ```

### Connection Refused from Remote Machine

1. **Verify host IP is correct**:
   ```bash
   # Get your current IP
   ifconfig | grep "inet " | grep -v 127.0.0.1
   
   # Or
   ipconfig getifaddr en0  # WiFi
   ipconfig getifaddr en1  # Ethernet
   ```

2. **Check if you're on the same network**:
   - Workshop participants must be on the same local network
   - VPN connections may interfere
   - Guest WiFi networks often isolate devices

3. **Test with telnet/nc first**:
   ```bash
   # From participant machine - test connectivity
   nc -zv 172.20.15.241 5432
   
   # Should show: Connection to 172.20.15.241 port 5432 [tcp/postgresql] succeeded!
   ```

### Database Authentication Issues

1. **Verify credentials**:
   - Username: `workshop`
   - Password: `workshop123`
   - Database: `workshop_rag`

2. **Check PostgreSQL logs**:
   ```bash
   # View database logs
   docker logs pgvector-db
   
   # Follow logs in real-time
   docker logs -f pgvector-db
   ```

---

## Security Considerations

### During Workshop

- ✅ **Simple credentials** (`workshop/workshop123`) - acceptable for workshop
- ✅ **Trusted network** - assumes workshop local network is secure
- ✅ **Limited duration** - port open only during workshop hours
- ⚠️ **Document cleanup** - remind yourself to close port after workshop

### Best Practices

1. **Time-box access**: Close port immediately after workshop
2. **Monitor connections**: Watch Docker logs for unexpected activity
3. **Use strong credentials**: If extending beyond workshop, change password
4. **Consider SSH tunnel**: For production, use SSH tunneling instead

### Alternative: SSH Tunnel (More Secure)

If you're concerned about security, participants can use SSH tunnel instead:

```bash
# On participant machine (requires SSH access to your machine)
ssh -L 5432:localhost:5432 adam@172.20.15.241

# Then connect to localhost
psql -h localhost -p 5432 -U workshop -d workshop_rag
```

---

## Workshop Day Checklist

### Before Workshop (Setup)

- [ ] Start PostgreSQL container: `docker-compose up -d`
- [ ] Verify container is running: `docker ps | grep pgvector`
- [ ] Check port binding: `netstat -an | grep 5432`
- [ ] Open firewall port: `./firewall-setup.sh open`
- [ ] Test local connection: `psql -h 172.20.15.241 ...`
- [ ] Verify host IP: `ifconfig | grep inet`
- [ ] Have credentials ready: `workshop/workshop123`
- [ ] Ingest sample data: `./ingest.sh`

### During Workshop (Monitor)

- [ ] Watch for connection issues: `docker logs -f pgvector-db`
- [ ] Help participants test connectivity: `nc -zv 172.20.15.241 5432`
- [ ] Monitor database size: `docker exec pgvector-db psql -U workshop -d workshop_rag -c "SELECT pg_size_pretty(pg_database_size('workshop_rag'));"`

### After Workshop (Cleanup)

- [ ] Close firewall port: `./firewall-setup.sh close`
- [ ] Optionally stop container: `docker-compose down`
- [ ] Optionally clear data: `docker-compose down -v`
- [ ] Re-enable security software if disabled

---

## Quick Reference Commands

```bash
# Status check
docker ps | grep pgvector
netstat -an | grep 5432

# Open firewall (Option 1 - Disable)
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off

# Open firewall (Option 2 - PF rule)
sudo pfctl -f /etc/pf.conf

# Test connection
psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag

# Close firewall (Option 1 - Re-enable)
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate on

# View logs
docker logs -f pgvector-db
```

---

## Getting Help

If you encounter issues during the workshop:

1. **Check this document first** - most issues are covered above
2. **View database logs**: `docker logs pgvector-db`
3. **Test connectivity**: `nc -zv 172.20.15.241 5432`
4. **Verify container status**: `docker ps -a`
5. **Check Docker networking**: `docker network inspect stage-4-agentic-rag_default`

---

## Additional Resources

- [macOS pfctl documentation](https://www.freebsd.org/cgi/man.cgi?query=pfctl)
- [PostgreSQL connection troubleshooting](https://www.postgresql.org/docs/current/client-authentication.html)
- [Docker networking guide](https://docs.docker.com/network/)
- [Workshop setup guide](./WORKSHOP_SETUP.md)
- [Connection quick reference](./WORKSHOP_CONNECTION.md)
