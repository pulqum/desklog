import socket
import threading
import re
from urllib.parse import unquote
from datetime import datetime
import os

class HTTPSocketServer:
    def __init__(self, host='localhost', port=8080):
        self.host = host
        self.port = port
        self.server_socket = None
        self.running = False
        self.bin_dir = 'bin'
        
        # bin 폴더 생성
        if not os.path.exists(self.bin_dir):
            os.makedirs(self.bin_dir)

    def save_request_to_bin(self, request_data, client_address):
        """요청을 .bin 파일로 저장"""
        try:
            # 타임스탬프 생성 (슬라이드 형식: 2024-10-16-01-12-45)
            timestamp = datetime.now().strftime("%Y-%m-%d-%H-%M-%S")
            filename = f"{timestamp}.bin"
            filepath = os.path.join(self.bin_dir, filename)
            
            # .bin 파일로 저장
            with open(filepath, 'wb') as f:
                f.write(request_data)
            
            print(f"📁 Request saved to: {filepath}")
            return filepath
        except Exception as e:
            print(f"❌ Error saving request to bin: {e}")
            return None

    def start(self):
        """서버 시작"""
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(5)
        self.running = True

        print(f"Socket Server started on {self.host}:{self.port}")

        try:
            while self.running:
                client_socket, client_address = self.server_socket.accept()
                print(f"Connection from {client_address}")

                # 각 클라이언트 요청을 별도 스레드로 처리
                client_thread = threading.Thread(
                    target=self.handle_client,
                    args=(client_socket, client_address)
                )
                client_thread.start()

        except KeyboardInterrupt:
            print("Server shutting down...")
        finally:
            self.stop()

    def stop(self):
        """서버 중지"""
        self.running = False
        if self.server_socket:
            self.server_socket.close()

    def parse_http_request(self, request_data):
        """HTTP 요청 파싱"""
        try:
            # 요청 데이터를 문자열로 변환
            request_str = request_data.decode('utf-8', errors='ignore')
            lines = request_str.split('\n')

            if not lines:
                return None

            # 첫 번째 줄 파싱 (GET /api_root/Post/ HTTP/1.1)
            request_line = lines[0].strip()
            method, path, version = request_line.split(' ', 2)

            # 헤더 파싱
            headers = {}
            body_start = 0

            for i, line in enumerate(lines[1:], 1):
                line = line.strip()
                if line == '':  # 빈 줄 = 헤더 끝, 바디 시작
                    body_start = i + 1
                    break

                if ':' in line:
                    key, value = line.split(':', 1)
                    headers[key.strip()] = value.strip()

            # 바디 추출
            body = '\n'.join(lines[body_start:]) if body_start > 0 else ''

            return {
                'method': method,
                'path': path,
                'version': version,
                'headers': headers,
                'body': body,
                'raw_request': request_str
            }

        except Exception as e:
            print(f"Error parsing request: {e}")
            return None

    def validate_request(self, parsed_request):
        """요청 검증"""
        if not parsed_request:
            return False, "Invalid request format"

        # 1. HTTP 메서드 검증
        if parsed_request['method'] != 'GET':
            return False, f"Unsupported method: {parsed_request['method']}"

        # 2. 경로 검증
        if not parsed_request['path'].startswith('/api_root/Post/'):
            return False, f"Invalid path: {parsed_request['path']}"

        # 3. HTTP 버전 검증
        if parsed_request['version'] != 'HTTP/1.1':
            return False, f"Unsupported HTTP version: {parsed_request['version']}"

        # 4. 필수 헤더 검증
        required_headers = ['Authorization', 'Host', 'User-Agent']
        for header in required_headers:
            if header not in parsed_request['headers']:
                return False, f"Missing required header: {header}"

        # 5. Authorization 토큰 검증
        auth_header = parsed_request['headers'].get('Authorization', '')
        if not auth_header.startswith('Token '):
            return False, "Invalid authorization format"

        token = auth_header[6:]  # 'Token ' 제거
        # 실제 앱에서 사용하는 토큰으로 변경
        expected_token = '696e79eba229f1fab1b970f01b24f14c1903a28f'

        if token != expected_token:
            return False, f"Invalid token (expected: {expected_token}, got: {token})"

        # 6. Host 검증
        host = parsed_request['headers'].get('Host', '')
        if host != '10.0.2.2:8000':
            return False, f"Invalid host: {host}"

        # 7. User-Agent 검증 (Android 앱인지 확인)
        user_agent = parsed_request['headers'].get('User-Agent', '')
        if not user_agent.startswith('Dalvik/'):
            return False, f"Invalid User-Agent: {user_agent}"

        return True, "Request validated successfully"

    def generate_response(self, is_valid, message, parsed_request=None):
        """HTTP 응답 생성"""
        if is_valid:
            # 성공 응답
            status_line = "HTTP/1.1 200 OK"
            body = f"""<!DOCTYPE html>
<html>
<head><title>Request Validated</title></head>
<body>
    <h1>✅ Request Validation Successful</h1>
    <p><strong>Message:</strong> {message}</p>
    <h2>Request Details:</h2>
    <ul>
        <li><strong>Method:</strong> {parsed_request['method']}</li>
        <li><strong>Path:</strong> {parsed_request['path']}</li>
        <li><strong>Token:</strong> {parsed_request['headers'].get('Authorization', '')[6:]}</li>
        <li><strong>User-Agent:</strong> {parsed_request['headers'].get('User-Agent', '')}</li>
    </ul>
</body>
</html>"""
        else:
            # 실패 응답
            status_line = "HTTP/1.1 400 Bad Request"
            body = f"""<!DOCTYPE html>
<html>
<head><title>Request Validation Failed</title></head>
<body>
    <h1>❌ Request Validation Failed</h1>
    <p><strong>Error:</strong> {message}</p>
</body>
</html>"""

        response = f"""{status_line}
Content-Type: text/html; charset=utf-8
Content-Length: {len(body.encode('utf-8'))}
Connection: close

{body}"""

        return response

    def handle_client(self, client_socket, client_address):
        """클라이언트 요청 처리"""
        try:
            # 요청 데이터 수신
            request_data = b''
            while True:
                chunk = client_socket.recv(1024)
                if not chunk:
                    break
                request_data += chunk

                # HTTP 요청 끝 감지 (\r\n\r\n)
                if b'\r\n\r\n' in request_data:
                    break

            if request_data:
                print(f"\n=== New Request from {client_address} ===")

                # 요청을 .bin 파일로 저장 (슬라이드처럼)
                bin_file = self.save_request_to_bin(request_data, client_address)

                # HTTP 요청 파싱
                parsed_request = self.parse_http_request(request_data)

                if parsed_request:
                    print("Parsed Request:")
                    print(f"  Method: {parsed_request['method']}")
                    print(f"  Path: {parsed_request['path']}")
                    print(f"  Version: {parsed_request['version']}")
                    print("  Headers:")
                    for key, value in parsed_request['headers'].items():
                        print(f"    {key}: {value}")

                    # 요청 검증
                    is_valid, message = self.validate_request(parsed_request)

                    print(f"\nValidation Result: {'✅ PASS' if is_valid else '❌ FAIL'}")
                    print(f"Message: {message}")

                    # 응답 생성 및 전송
                    response = self.generate_response(is_valid, message, parsed_request if is_valid else None)
                    client_socket.sendall(response.encode('utf-8'))

                else:
                    print("Failed to parse request")
                    error_response = self.generate_response(False, "Failed to parse HTTP request")
                    client_socket.sendall(error_response.encode('utf-8'))

        except Exception as e:
            print(f"Error handling client {client_address}: {e}")
        finally:
            client_socket.close()

def main():
    # 서버 설정 (Android 에뮬레이터에서 접근 가능하도록)
    server = HTTPSocketServer(host='0.0.0.0', port=8000)

    print("=== HTTP Socket Server for Android Request Validation ===")
    print("Server will validate requests like:")
    print("GET /api_root/Post/ HTTP/1.1")
    print("Authorization: Token bf46b8f9337d1d27b4ef2511514c798be1a954b8")
    print("Host: 10.0.2.2:8000")
    print("User-Agent: Dalvik/...")
    print("\n📁 All requests will be saved to 'bin/' folder as .bin files")
    print("   (just like in the slide example)")
    print("\nStarting server...")

    try:
        server.start()
    except KeyboardInterrupt:
        print("\nServer stopped by user")
    except Exception as e:
        print(f"Server error: {e}")

if __name__ == "__main__":
    main()