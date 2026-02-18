# How to Test WebRTC with a Friend

## Option 1: Use ngrok (Easiest)
1. Install ngrok: `brew install ngrok` or download from ngrok.com
2. Expose your backend: `ngrok http 8080`
3. Expose your frontend: `ngrok http 3000`
4. Update frontend URLs to use ngrok URLs
5. Share ngrok URL with friend

## Option 2: Local Network
1. Find your IP: `ifconfig` or `ipconfig`
2. Update backend CORS to allow your IP
3. Share URL with friend on same WiFi

## Option 3: Cloud Deployment
Deploy to Heroku, Vercel, or similar for real testing

## Option 4: Virtual Machine
Use different VMs to simulate different users
