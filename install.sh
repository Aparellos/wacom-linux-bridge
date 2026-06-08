#!/bin/bash

# Ensure the script is run as root
if [ "$EUID" -ne 0 ]; then
  echo "Por favor, execute este script com sudo ou como root."
  exit 1
fi

echo "A instalar Wacom Linux Bridge..."

INSTALL_DIR="/opt/wacom-linux-bridge"
BIN_LINK="/usr/local/bin/wacom-bridge"
DESKTOP_ENTRY_GLOBAL="/usr/share/applications/wacom-linux-bridge.desktop"

# Create installation directory
echo "-> A copiar ficheiros para $INSTALL_DIR"
mkdir -p "$INSTALL_DIR"
cp -r ./* "$INSTALL_DIR/"
cp .gitignore "$INSTALL_DIR/" 2>/dev/null

# Set executable permissions
chmod +x "$INSTALL_DIR/run_bridge.sh"
chmod +x "$INSTALL_DIR/build.sh"

# Create a global command
echo "-> A criar atalho global em $BIN_LINK"
ln -sf "$INSTALL_DIR/run_bridge.sh" "$BIN_LINK"

# Create a Desktop Entry (Shortcut)
echo "-> A criar atalho no menu de aplicações"
cat <<EOF > "$DESKTOP_ENTRY_GLOBAL"
[Desktop Entry]
Name=Wacom Linux Bridge
Comment=Ponte local para tableta Wacom STU
Exec=$INSTALL_DIR/run_bridge.sh
Icon=$INSTALL_DIR/icon.png
Terminal=false
Type=Application
Categories=Utility;
EOF

chmod 644 "$DESKTOP_ENTRY_GLOBAL"

echo ""
echo "Instalação concluída com sucesso!"
echo "Pode iniciar a ponte a qualquer momento executando 'wacom-bridge' no terminal ou procurando no menu de aplicações."
echo ""

# Handle Autostart
if [ -n "$SUDO_USER" ]; then
    REAL_USER="$SUDO_USER"
    USER_HOME=$(getent passwd "$SUDO_USER" | cut -d: -f6)
else
    REAL_USER="$USER"
    USER_HOME="$HOME"
fi

read -p "Deseja que a Wacom Linux Bridge arranque automaticamente ao iniciar sessão para o utilizador $REAL_USER? [Y/n] " prompt
if [[ $prompt == "y" || $prompt == "Y" || $prompt == "yes" || $prompt == "Yes" || $prompt == "" ]]; then
    AUTOSTART_DIR="$USER_HOME/.config/autostart"
    echo "-> A configurar arranque automático em $AUTOSTART_DIR"
    
    mkdir -p "$AUTOSTART_DIR"
    cp "$DESKTOP_ENTRY_GLOBAL" "$AUTOSTART_DIR/wacom-linux-bridge.desktop"
    
    # Ensure correct ownership
    chown -R "$REAL_USER:$REAL_USER" "$AUTOSTART_DIR"
    
    echo "Arranque automático ativado!"
else
    echo "Arranque automático ignorado."
fi

echo ""
echo "Tudo pronto!"
