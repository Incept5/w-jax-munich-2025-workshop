
# API Keys Setup Guide

This guide provides detailed instructions for obtaining the required API keys for the Tripper application.

## Required API Keys

You need **three required API keys** to run Tripper:

1. **OpenAI API Key** - For GPT-4.1 models (planner and researcher agents)
2. **Brave Search API Key** - For web search capabilities
3. **Google Maps API Key** - For mapping and location services

**Estimated Cost**: ~$0.10 per travel plan (mostly OpenAI GPT-4.1 usage)

---

## 1. OpenAI API Key (Required)

### Purpose
Powers both the planner agent (GPT-4.1) and researcher agent (GPT-4.1-mini) in Tripper.

### Cost Information
- **GPT-4.1**: ~$2.50 per 1M input tokens, ~$10 per 1M output tokens
- **GPT-4.1-mini**: ~$0.15 per 1M input tokens, ~$0.60 per 1M output tokens
- **Estimated cost per travel plan**: $0.08-0.12 (varies by complexity)
- **Minimum deposit**: $5 (one-time)

### Setup Instructions

#### Step 1: Create OpenAI Account
1. Go to [https://platform.openai.com/signup](https://platform.openai.com/signup)
2. Sign up with email or continue with Google/Microsoft
3. Verify your email address

#### Step 2: Set Up Billing
1. Go to [https://platform.openai.com/account/billing](https://platform.openai.com/account/billing)
2. Click "Add payment method"
3. Enter credit card information
4. Add minimum $5 credit (this will last for 50-60 travel plans)

**Note**: OpenAI requires billing to be set up before you can use the API, even for paid tiers.

#### Step 3: Create API Key
1. Go to [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)
2. Click "Create new secret key"
3. Name it: "Tripper Workshop"
4. Set permissions: "All" (for this workshop)
5. Click "Create secret key"
6. **Copy the key immediately** - you won't see it again!
   - Format: `sk-proj-...` (starts with `sk-proj-` or `sk-`)

#### Step 4: Add to .env File
```bash
OPENAI_API_KEY=sk-proj-your-key-here
```

### Usage Monitoring
- Monitor usage: [https://platform.openai.com/usage](https://platform.openai.com/usage)
- Set usage limits: [https://platform.openai.com/account/limits](https://platform.openai.com/account/limits)

**Recommendation**: Set a monthly budget limit (e.g., $10) to avoid surprises.

---

## 2. Brave Search API Key (Required)

### Purpose
Provides web search capabilities for the researcher agent to find information about destinations, attractions, and travel tips.

### Cost Information
- **Free Tier**: 2,000 queries per month (sufficient for workshop)
- **Pro Tier**: $5/month for 15,000 queries
- **No credit card required** for free tier

### Setup Instructions

#### Step 1: Create Brave Account
1. Go to [https://brave.com/search/api/](https://brave.com/search/api/)
2. Click "Get Started" or "Sign Up"
3. Sign up with email address
4. Verify your email

#### Step 2: Create API Key
1. After login, you'll be on the dashboard
2. Your API key will be displayed immediately
3. Copy the key
   - Format: `BSA...` (starts with `BSA`)

If you need to find it later:
1. Go to [https://api.search.brave.com/app/keys](https://api.search.brave.com/app/keys)
2. View or create new keys

#### Step 3: Add to .env File
```bash
BRAVE_API_KEY=BSAyour-key-here
```

### Usage Monitoring
- View usage: [https://api.search.brave.com/app/dashboard](https://api.search.brave.com/app/dashboard)
- Free tier resets monthly
- Queries are counted per search request

**Note**: The free tier (2,000 queries/month) is more than enough for workshop usage and several dozen travel plans.

---

## 3. Google Maps API Key (Required)

### Purpose
Provides location services, geocoding, place details, and mapping for travel destinations.

### Cost Information
- **Free Tier**: $200 credit per month (very generous)
- **Typical Usage**: A travel plan uses ~$0.01-0.05 in API calls
- **Credit card required** (for verification, but free tier is sufficient)

### Setup Instructions

#### Step 1: Create Google Cloud Account
1. Go to [https://console.cloud.google.com/](https://console.cloud.google.com/)
2. Sign in with your Google account
3. Accept terms of service
4. Set up billing (required but free tier applies)
   - Click "Activate" on the free trial banner
   - Enter credit card information
   - You get $300 credit for 90 days + $200/month ongoing

#### Step 2: Create a Project
1. Click "Select a project" dropdown at the top
2. Click "New Project"
3. Name it: "Tripper Workshop"
4. Click "Create"
5. Wait for project creation (a few seconds)
6. Select the new project

#### Step 3: Enable Required APIs
1. Go to [https://console.cloud.google.com/apis/library](https://console.cloud.google.com/apis/library)
2. Search and enable these APIs (click "Enable" for each):
   - **Maps JavaScript API**
   - **Places API**
   - **Geocoding API**
   - **Maps Static API** (optional, for static map images)

#### Step 4: Create API Key
1. Go to [https://console.cloud.google.com/apis/credentials](https://console.cloud.google.com/apis/credentials)
2. Click "Create Credentials" → "API Key"
3. Copy the API key immediately
   - Format: `AIza...` (starts with `AIza`)

#### Step 5: Restrict API Key (Recommended for Security)
1. Click "Edit API key" (pencil icon)
2. Under "API restrictions":
   - Select "Restrict key"
   - Check only the APIs you enabled above
3. Click "Save"

**Note**: For workshop purposes, you can skip restrictions, but for production, always restrict API keys.

#### Step 6: Add to .env File
```bash
GOOGLE_MAPS_API_KEY=AIzayour-key-here
```

### Usage Monitoring
- View usage: [https://console.cloud.google.com/apis/dashboard](https://console.cloud.google.com/apis/dashboard)
- Set quotas: [https://console.cloud.google.com/apis/api/maps-backend.googleapis.com/quotas](https://console.cloud.google.com/apis/api/maps-backend.googleapis.com/quotas)

**Recommendation**: Set up billing alerts to notify you if you approach the free tier limit (though this is very unlikely with workshop usage).

---

## Optional API Keys

### Anthropic API Key (Optional)
If you want to use Claude instead of GPT-4:

1. Go to [https://console.anthropic.com/](https://console.anthropic.com/)
2. Sign up and verify email
3. Add billing ($5 minimum)
4. Create API key
5. Add to .env: `ANTHROPIC_API_KEY=sk-ant-...`

**Note**: Tripper's pom.xml has Anthropic support commented out by default. Uncomment to enable.

### GitHub Token (Optional)
Only needed if you want to use GitHub MCP tools:

1. Go to [https://github.com/settings/tokens](https://github.com/settings/tokens)
2. Click "Generate new token (classic)"
3. Select scopes: `repo`, `read:org`, `read:user`
4. Generate and copy token
5. Add to .env: `GITHUB_PERSONAL_ACCESS_TOKEN=ghp_...`

---

## Complete .env File Example

After obtaining all required keys, your `.env` file should look like:

```bash
# Required API Keys
OPENAI_API_KEY=sk-proj-abc123...xyz789
BRAVE_API_KEY=BSA1234567890abcdef
GOOGLE_MAPS_API_KEY=AIzaSyABC123...XYZ789

# Optional API Keys
ANTHROPIC_API_KEY=sk-ant-api03-...
GITHUB_PERSONAL_ACCESS_TOKEN=ghp_abc123...
```

**Security Notes**:
- ✅ The `.env` file is in `.gitignore` (never committed)
- ✅ Never share your API keys publicly
- ✅ Rotate keys if accidentally exposed
- ✅ Use environment variables in production

---

## Verifying Your Setup

After adding all keys to `.env`, verify they work:

### Test 1: Environment Variables Loaded
```bash
# From the tripper directory
source .env
echo $OPENAI_API_KEY
# Should print: sk-proj-...
```

### Test 2: Start the Application
```bash
./run.sh
# Watch for errors in console
# Should see: "Started TripperApplication"
```

### Test 3: Create a Test Plan
1. Go to http://localhost:8747/
2. Enter a simple destination (e.g., "Paris")
3. Submit and watch for errors
4. If successful, all keys are working!

---

## Troubleshooting

### OpenAI Issues

**Error**: `Incorrect API key provided`
- Check that key starts with `sk-proj-` or `sk-`
- Verify no extra spaces in .env file
- Regenerate key if necessary

**Error**: `You exceeded your current quota`
- Add billing: [https://platform.openai.com/account/billing](https://platform.openai.com/account/billing)
- Add at least $5 credit

### Brave Search Issues

**Error**: `Invalid API key`
- Check that key starts with `BSA`
- Verify no extra spaces
- Check dashboard for key status

**Error**: `Rate limit exceeded`
- Wait for monthly reset
- Upgrade to Pro tier if needed

### Google Maps Issues

**Error**: `API key not valid`
- Ensure APIs are enabled (step 3 above)
- Check API key in console
- Wait a few minutes after creating (propagation delay)

**Error**: `This API project is not authorized`
- Enable required APIs in console
- Check API restrictions on key
- Ensure billing is set up

### General Issues

**Problem**: Keys not being loaded
```bash
# Solution: Verify .env file location
ls -la .env
# Should be in tripper/ root directory

# Verify .env is loaded by run.sh
cat run.sh | grep "source .env"
```

**Problem**: Keys in wrong format
```bash
# Verify format
cat .env | grep API_KEY
# Should show: VARIABLE_NAME=value (no quotes, no spaces)
```

---

## Cost Management Tips

### Monitor Usage Regularly
- Check OpenAI usage daily during workshop
- Set up billing alerts in Google Cloud
- Track Brave Search quota

### Optimize Costs
- Use GPT-4.1-mini for less critical tasks (already configured)
- Cache results when possible
- Set reasonable limits in application.yml

### Budget Recommendations
For the workshop:
- **OpenAI**: $5-10 (will last for 50-100 plans)
- **Brave**: Free tier sufficient
- **Google Maps**: Free tier sufficient
- **Total**: ~$5-10 for entire workshop

---

## Security Best Practices

### During Development
- ✅ Keep `.env` file local (never commit)
- ✅ Use `.env.example` for templates
- ✅ Rotate keys if accidentally exposed
- ✅ Use separate keys for each project

### For Production
- ✅ Use environment variables (not .env files)
- ✅ Use secret management services (AWS Secrets Manager, etc.)
- ✅ Implement key rotation policies
- ✅ Restrict API keys to specific IPs/domains
- ✅ Monitor usage and set alerts

---

## Next Steps

Once you have all API keys set up:

1. ✅ Verify keys in `.env` file
2. ✅ Start the application: `./run.sh`
3. ✅ Test with a simple travel plan
4. ✅ Proceed to code exploration

**Need Help?** Check the main [README.md](./README.md) for troubleshooting or ask during the workshop.

---

**Last Updated**: November 2024  
**Workshop**: W-JAX Munich 2025
