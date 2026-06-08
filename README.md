# Wacom Linux Bridge

Este utilitário permite a comunicação bidirecional de navegadores web com a tableta de captura de assinaturas **Wacom STU-500b** em sistemas operativos Linux. Atua como um servidor local na porta `9001` de forma a emular o comportamento oficial do SDK web nativo da Wacom (SigCaptX) existente no Windows.

Esta ferramenta foi pensada em exclusivo como "ponte" para integrar com o plugin *AlbaranesFirmaWacom* para o software ERP **FacturaScripts**, mas pode ser adaptada para qualquer outro fim de captura num ambiente Web / Local.

## Pré-requisitos
Antes de instalar, certifique-se de que o seu sistema dispõe do seguinte software:
- **Sistema Operativo:** Distribuições baseadas em Linux.
- **Software Necessário:**
  - `python3` (já vem pré-instalado na esmagadora maioria das distribuições Linux)
  - `java` (JRE/JDK versão 8 ou superior) para executar o componente de captura do ecrã físico.
- **Acesso USB (udev):** Deve garantir que o utilizador corrente tem permissões adequadas de leitura/escrita no barramento USB para interagir com a Wacom sem necessitar de permissões constantes de `root`.

## Instalação Rápida (Recomendada)

Criámos um script de instalação interativo que automatiza todo o processo e aloja o utilitário nos diretórios padronizados do sistema.

1. Clone ou descarregue este repositório para o seu computador.
2. Abra um terminal e navegue até a pasta descarregada:
   ```bash
   cd wacom-linux-bridge
   ```
3. Atribua as permissões de execução (se não estiverem ativas) e inicie o instalador:
   ```bash
   chmod +x install.sh
   sudo ./install.sh
   ```

### O que o instalador faz?
* **Alocação Padrão:** Copia de forma segura a aplicação para a diretoria global `/opt/wacom-linux-bridge`.
* **Comando Global:** Regista um atalho universal no sistema, permitindo-lhe iniciar o programa mais tarde através do comando `wacom-bridge` a partir de qualquer terminal.
* **Menu de Aplicações:** Adiciona um novo atalho ao menu de aplicações geral do seu sistema (a ponte corre invisivelmente no fundo).
* **Arranque Automático:** O script pergunta-lhe de forma interativa se pretende adicionar o atalho à lista de arranque de sessão automática do seu utilizador (altamente recomendado).

## Execução Sem Instalar

Se pretender correr a aplicação temporariamente sem alterar o sistema (`/opt`):
1. Navegue para o diretório atual descarregado.
2. Inicie manualmente a ponte executando:
   ```bash
   ./run_bridge.sh
   ```
O utilitário ficará ativo a escutar e a aguardar chamadas na porta `9001`. A janela de assinatura irá abrir-se automaticamente sempre que um pedido de captura web for acionado.

## Arquitetura Resumida
* **WacomCapture.java:** Aplicação em Java acoplada ao SDK Linux Oficial da Wacom `stu-sdk-linux-2.16.1`. Desenha a interface UI local, o fundo LCD da Wacom, os botões, e monitoriza os traços USB até o clique no botão "Aceitar".
* **wacom_bridge.py:** Um micro-servidor WebSockets/HTTP em Python com capacidade CORS. Liga a web application externa chamando o utilitário Java e retornando as coordenadas / imagem renderizada ao final.
