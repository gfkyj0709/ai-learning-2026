#!/bin/bash
THRESHOLD=86400

for PID in $(pgrep -f "extensionHost"); do
    ETIMES=$(ps -o etimes= -p $PID 2>/dev/null | tr -d ' ')
    if [ -n "$ETIMES" ] && [ "$ETIMES" -gt "$THRESHOLD" ]; then
        echo "$(date): Killing extensionHost PID $PID (running ${ETIMES}s)"
        kill $PID
    fi
done
