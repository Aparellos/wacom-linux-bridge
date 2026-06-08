#!/bin/bash
# Script para iniciar a ponte Wacom local no Linux
set -e

BRIDGE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDK_LIB_DIR="$BRIDGE_DIR/stu-sdk-linux-2.16.1/sdk/Linux/x86_64"

# Certifica de exportar a biblioteca nativa do SDK para a JVM
export LD_LIBRARY_PATH="$SDK_LIB_DIR:$LD_LIBRARY_PATH"

echo "Iniciando Ponte Wacom Linux..."
python3 "$BRIDGE_DIR/wacom_bridge.py"
