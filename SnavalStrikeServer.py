import socket
import threading

class ClientHandler(threading.Thread):
    def __init__(self, client_socket, addr):
        super().__init__()
        self.client_socket = client_socket
        self.addr = addr
        self.running = True

    def run(self):
        print(f"[+] New connection: {self.addr}")
        try:
            while self.running:
                message = self.client_socket.recv(1024).decode('utf-8')
                if not message:
                    break
                print(f"Message from {self.addr}: {message}")
                method = message.split(" ")[0]
                if method == "HEARTBEAT":
                    self.client_socket.send("HEARTBEAT ok".encode('utf-8'))
                elif method == "LOGIN":
                    username = message.split(" ")[1]
                    self.client_socket.send(f"LOGIN ok {username}".encode('utf-8'))
                #self.broadcast(message)
                #self.client_socket.send(message.encode('utf-8'))
        except ConnectionResetError:
            print(f"[-] Connection with {self.addr} broken")
        finally:
            self.client_socket.close()
            print(f"[-] Connection with {self.addr} closed")

    #def broadcast(self, message):
    #    for client in clients:
    #        if client != self.client_socket:
    #            try:
    #                client.send(message.encode('utf-8'))
    #            except:
    #                continue

def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(('0.0.0.0', 32951))
    server.listen(5)
    print("[*] SnavalStrikeServer is running on port 32951")
    try:
        while True:
            client_socket, addr = server.accept()
            clients.append(client_socket)
            handler = ClientHandler(client_socket, addr)
            handler.start()
    except KeyboardInterrupt:
        print("[!] Stop SnavalStrikeServer")
    finally:
        server.close()

clients = []

if __name__ == "__main__":
    main()
