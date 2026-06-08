#!/bin/bash
# Script para compilar o utilitário Java de captura da Wacom
set -e

BRIDGE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDK_JAR="$BRIDGE_DIR/stu-sdk-linux-2.16.1/sdk/Linux/any/wgssSTU.jar"

echo "Compilando WacomCapture.java..."
javac -cp "$SDK_JAR:." "$BRIDGE_DIR/WacomCapture.java"

echo "Compilado com sucesso!"
