#!/bin/bash
#
# jarvis.sh - CLI bridge to Open Jarvis Android agent
# Usage: jarvis [command] [--help]
#

set -e

SCRIPT_NAME="$(basename "$0")"
SOCKET_FILE="jarvis.port"
DEFAULT_PORT=0
TIMEOUT=30

# Find socket port
find_port() {
    local port_file="${ANDROID_DATA}/../files/${SOCKET_FILE}"
    if [ -f "$port_file" ]; then
        cat "$port_file"
    else
        local files_dir="/data/data/com.openjarvis/files"
        if [ -f "${files_dir}/${SOCKET_FILE}" ]; then
            cat "${files_dir}/${SOCKET_FILE}"
        else
            echo "$DEFAULT_PORT"
        fi
    fi
}

# Auto-detect tool
get_tool() {
    if command -v socat &>/dev/null; then
        echo "socat"
    elif command -v nc &>/dev/null; then
        echo "nc"
    elif command -v busybox &>/dev/null && busybox nc &>/dev/null; then
        echo "nc"
    else
        echo "none"
    fi
}

# Send command via socat
send_socat() {
    local port="$1"
    local cmd="$2"
    echo "{\"cmd\":\"$cmd\",\"requestId\":\"$$\"}" | socat - TCP:127.0.0.1:"$port",timeout=1
}

# Send command via nc
send_nc() {
    local port="$1"
    local cmd="$2"
    printf '{"cmd":"%s","requestId":"%s"}\n' "$cmd" "$$" | nc -w 1 127.0.0.1 "$port"
}

# Main send function
send_cmd() {
    local port="$1"
    local cmd="$2"
    local tool="$3"
    
    case "$tool" in
        socat)
            send_socat "$port" "$cmd"
            ;;
        nc)
            send_nc "$port" "$cmd"
            ;;
        *)
            echo "Error: No network tool (socat/nc) available" >&2
            exit 1
            ;;
    esac
}

# Parse response
parse_response() {
    local response="$1"
    local is_tty="$2"
    
    # Check for errors
    if echo "$response" | grep -q '"status":"error"'; then
        echo "Error: $(echo "$response" | grep -o '"result":"[^"]*"' | cut -d'"' -f4)" >&2
        return 1
    fi
    
    # Extract result
    local result
    result=$(echo "$response" | grep -o '"result":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    # Format output
    if [ "$is_tty" = "true" ]; then
        echo -e "$result"
    else
        echo "$result"
    fi
}

# Stream mode for progress
stream_cmd() {
    local port="$1"
    local cmd="$2"
    local tool="$3"
    
    while read -r line; do
        if [ -z "$line" ]; then
            continue
        fi
        
        local response
        case "$tool" in
            socat)
                response=$(send_socat "$port" "$line")
                ;;
            nc)
                response=$(send_nc "$port" "$line")
                ;;
        esac
        
        if echo "$response" | grep -q '"status":"done"'; then
            parse_response "$response" "$(is_tty)"
            return 0
        elif echo "$response" | grep -q '"status":"error"'; then
            parse_response "$response" "$(is_tty)" || true
            return 1
        fi
    done
}

is_tty() {
    [ -t 1 ]
}

show_help() {
    cat <<EOF
Open Jarvis CLI Bridge

Usage:
  $SCRIPT_NAME <command>          Execute a command
  $SCRIPT_NAME status             Show service status
  $SCRIPT_NAME history             Show recent task history
  $SCRIPT_NAME providers           Show LLM provider stats
  $SCRIPT_NAME memory              Show memory context
  $SCRIPT_NAME --help              Show this help

  echo "<command>" | $SCRIPT_NAME  Pipe mode (reads from stdin)

Examples:
  $SCRIPT_NAME "open whatsapp"
  $SCRIPT_NAME status
  echo "open chrome" | $SCRIPT_NAME

Exit codes:
  0 = success
  1 = error (service not running, command failed)

EOF
}

main() {
    local cmd=""
    local tool
    
    # Handle --help
    if [ "$1" = "--help" ]; then
        show_help
        exit 0
    fi
    
    tool=$(get_tool)
    if [ "$tool" = "none" ]; then
        echo "Error: Need socat or nc to connect to jarvis" >&2
        echo "Install with: pkg install socat" >&2
        exit 1
    fi
    
    local port
    port=$(find_port)
    
    if [ "$port" = "0" ] || [ -z "$port" ]; then
        echo "Error: Open Jarvis service not running" >&2
        echo "Start the app first" >&2
        exit 1
    fi
    
    if [ -n "$1" ]; then
        cmd="$1"
    elif [ -t 0 ]; then
        echo "Error: No command provided and no stdin" >&2
        show_help
        exit 1
    else
        stream_cmd "$port" "" "$tool"
        exit $?
    fi
    
    local response
    response=$(send_cmd "$port" "$cmd" "$tool")
    
    if parse_response "$response" "$(is_tty)"; then
        exit 0
    else
        exit 1
    fi
}

main "$@"