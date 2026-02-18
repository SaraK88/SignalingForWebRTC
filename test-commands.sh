#!/bin/bash

echo "🚀 Agora Signaling Backend Test Commands"
echo "=========================================="

echo ""
echo "1. Test RTM Token (for signaling):"
echo "curl \"http://localhost:8080/rtm-token?uid=testUser123\""
curl "http://localhost:8080/rtm-token?uid=testUser123"
echo ""

echo ""
echo "2. Test Media Token (for video/audio):"
echo "curl \"http://localhost:8080/media-token?channelName=room1&uid=testUser123\""
curl "http://localhost:8080/media-token?channelName=room1&uid=testUser123"
echo ""

echo ""
echo "3. Test Error Handling (empty UID):"
echo "curl \"http://localhost:8080/rtm-token?uid=\""
curl "http://localhost:8080/rtm-token?uid="
echo ""

echo ""
echo "4. Test Error Handling (long UID):"
echo "curl \"http://localhost:8080/rtm-token?uid=$(python -c 'print(\"a\"*65)')\""
curl "http://localhost:8080/rtm-token?uid=$(python -c 'print(\"a\"*65)')"
echo ""

echo ""
echo "✅ All tests completed!"
