# Workshop Firewall Setup - Quick Start

## 🚀 Before Workshop (5 Minutes)

### 1. Start Database
```bash
cd stage-4-agentic-rag
docker-compose up -d
```

### 2. Open Firewall (Choose ONE option)

**Option A: Automated Script** (Recommended)
```bash
./firewall-setup.sh open
# Enter your admin password when prompted
```

**Option B: Manual Command**
```bash
# Disable macOS firewall temporarily
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off
```

### 3. Verify Setup
```bash
./firewall-setup.sh verify
```

### 4. Test Connection
```bash
# Should succeed
psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag
# Password: workshop123
```

---

## 📢 Give to Participants

**Connection Details:**
```
Host:     172.20.15.241
Port:     5432
Database: workshop_rag
User:     workshop
Password: workshop123
```

**Quick Test:**
```bash
# Test port connectivity
nc -zv 172.20.15.241 5432

# Connect to database
psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag
```

**Or use shared flag:**
```bash
cd stage-4-agentic-rag
./run.sh --shared
```

---

## 🧹 After Workshop (2 Minutes)

### 1. Close Firewall

**Option A: Automated Script**
```bash
./firewall-setup.sh close
```

**Option B: Manual Command**
```bash
# Re-enable macOS firewall
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate on
```

### 2. Optionally Stop Database
```bash
docker-compose down
# or keep it running for local development
```

---

## 🔧 Troubleshooting

### Participants Can't Connect

1. **Check firewall is open:**
   ```bash
   sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate
   # Should show: disabled
   ```

2. **Check database is running:**
   ```bash
   docker ps | grep pgvector
   # Should show: 0.0.0.0:5432->5432/tcp
   ```

3. **Check port is listening:**
   ```bash
   netstat -an | grep 5432
   # Should show: tcp4  0  0  *.5432  *.*  LISTEN
   ```

4. **Test from your machine:**
   ```bash
   psql -h 172.20.15.241 -p 5432 -U workshop -d workshop_rag
   ```

5. **Have participant test connectivity:**
   ```bash
   # On their machine
   nc -zv 172.20.15.241 5432
   # Should show: succeeded!
   ```

### Common Issues

**"Connection refused"**
- Firewall is still blocking → Run: `./firewall-setup.sh open`
- Database not running → Run: `docker-compose up -d`

**"No route to host"**
- Wrong IP address → Verify: `ifconfig | grep inet`
- Different network → Ensure same WiFi/network

**"Authentication failed"**
- Wrong credentials → Use: `workshop` / `workshop123`
- Wrong database → Use: `workshop_rag`

---

## 📚 Full Documentation

For detailed instructions and advanced options:
- **[FIREWALL_SETUP.md](./FIREWALL_SETUP.md)** - Complete guide
- **[WORKSHOP_SETUP.md](./WORKSHOP_SETUP.md)** - Workshop configuration
- **[WORKSHOP_CONNECTION.md](./WORKSHOP_CONNECTION.md)** - Connection reference

---

## 🎯 Workshop Day Checklist

- [ ] Database running: `docker-compose up -d`
- [ ] Firewall open: `./firewall-setup.sh open`
- [ ] Test works: `psql -h 172.20.15.241 ...`
- [ ] Write connection details on whiteboard
- [ ] After workshop: `./firewall-setup.sh close`
