import http.server
import socketserver
import subprocess
import os
import base64
import json
from urllib.parse import urlparse

PORT = 9001
BRIDGE_DIR = os.path.dirname(os.path.realpath(__file__))

class WacomBridgeHandler(http.server.BaseHTTPRequestHandler):
    def end_headers(self):
        # Enviar headers CORS essenciais
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        super().end_headers()

    def do_OPTIONS(self):
        # Responder preflight requests do navegador
        self.send_response(200)
        self.end_headers()

    def do_GET(self):
        parsed_path = urlparse(self.path)
        
        if parsed_path.path == '/status':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            response = {"status": "running", "device": "Wacom STU-500"}
            self.wfile.write(json.dumps(response).encode('utf-8'))
            return

        elif parsed_path.path == '/capturar':
            from urllib.parse import parse_qs
            qs = parse_qs(parsed_path.query)
            nombre = qs.get("nombre", [""])[0]
            motivo = qs.get("motivo", [""])[0]

            # 1. Limpar imagem antiga se existir
            image_path = '/tmp/wacom_sig.png'
            if os.path.exists(image_path):
                try:
                    os.remove(image_path)
                except Exception as e:
                    print(f"Erro ao remover imagem antiga: {e}")

            # 2. Executar utilitário Java para capturar assinatura
            print(f"Iniciando captura com o tablet Wacom STU para {nombre}...")
            try:
                # O comando assume que o WacomCapture.class está no BRIDGE_DIR
                # E o jar da Wacom está no classpath
                sdk_base = os.path.join(BRIDGE_DIR, "stu-sdk-linux-2.16.1", "sdk", "Linux")
                classpath = f"{os.path.join(sdk_base, 'any', 'wgssSTU.jar')}:{BRIDGE_DIR}"
                cmd = [
                    "java",
                    "-cp", classpath,
                    f"-Djava.library.path={os.path.join(sdk_base, 'x86_64')}",
                    "WacomCapture",
                    nombre,
                    motivo,
                    image_path
                ]
                
                # Executa de forma síncrona, bloqueando até a dialog fechar
                result = subprocess.run(cmd, cwd=BRIDGE_DIR, capture_output=True, text=True)
                print("Processo Java encerrado com código:", result.returncode)
                if result.stderr:
                    print("Java Stderr:", result.stderr)
                if result.stdout:
                    print("Java Stdout:", result.stdout)

            except Exception as e:
                self.send_response(500)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                response = {"status": "error", "message": f"Falha ao iniciar subprocesso Java: {str(e)}"}
                self.wfile.write(json.dumps(response).encode('utf-8'))
                return

            # 3. Verificar se a imagem sig.png foi criada
            if os.path.exists(image_path):
                try:
                    with open(image_path, "rb") as image_file:
                        encoded_string = base64.b64encode(image_file.read()).decode('utf-8')
                    
                    # Remover o arquivo temporário
                    os.remove(image_path)
                    
                    self.send_response(200)
                    self.send_header('Content-Type', 'application/json')
                    self.end_headers()
                    response = {
                        "status": "success",
                        "image": f"data:image/png;base64,{encoded_string}"
                    }
                    self.wfile.write(json.dumps(response).encode('utf-8'))
                    print("Assinatura capturada e enviada ao navegador.")
                except Exception as e:
                    self.send_response(500)
                    self.send_header('Content-Type', 'application/json')
                    self.end_headers()
                    response = {"status": "error", "message": f"Erro ao codificar imagem: {str(e)}"}
                    self.wfile.write(json.dumps(response).encode('utf-8'))
            else:
                # Se o processo acabou mas a imagem não foi gerada, foi cancelamento
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                response = {"status": "cancelled"}
                self.wfile.write(json.dumps(response).encode('utf-8'))
                print("Captura cancelada pelo utilizador.")
                
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == '__main__':
    # Configurar socket reusável
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), WacomBridgeHandler) as httpd:
        print(f"Ponte de Assinatura Wacom Linux iniciada na porta {PORT}")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nEncerrando ponte de assinatura...")
