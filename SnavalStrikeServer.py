import socket
import threading
import sqlite3
from datetime import datetime

class ClientHandler(threading.Thread):
    def __init__(self, client_socket, addr):
        super().__init__()
        self.client_socket = client_socket
        self.addr = addr
        self.running = True

    def run(self):
        print(f"[{datetime.now()}] [+] NEW connection: {self.addr}")
        try:
            while self.running:
                message = self.client_socket.recv(1024).decode('utf-8')
                if not message:
                    break
                print(f"[{datetime.now()}] [m] MESSAGE from {self.addr}: {message}", end="")
                method = message.split(" ")[0]
                ####################
                #     HEARTBEAT    #
                ####################
                if method == "HEARTBEAT":
                    self.client_socket.send("HEARTBEAT".encode('utf-8'))
                ####################
                #     ADD_USER     #
                ####################
                elif method == "ADD_USER":
                    username = message.split(" ")[1]
                    add_user_id = Database.addUser(username)
                    if add_user_id == 0:
                        self.client_socket.send(f"ADD_USER alreadyinuse {username} END\n".encode('utf-8'))
                    elif add_user_id == -1:
                        self.client_socket.send(f"ADD_USER error {username} END\n".encode('utf-8'))
                    else:
                        self.client_socket.send(f"ADD_USER ok {username} END\n".encode('utf-8'))
                        for client in clients:
                            if client.addr == self.addr:
                                client.id = add_user_id
                                client.username = username
                                client.elo = 1500
                                break
                        for client in clients:
                            if client.addr == self.addr:
                                continue
                            client.socket.send(f"ADD_PLAYER {add_user_id} {username} 1500 1 END\n".encode('utf-8'))
                ####################
                #    DELETE_USER   #
                ####################
                elif method == "DELETE_USER":
                    username = message.split(" ")[1]
                    delete_user_status = Database.deleteUser(username)
                    if delete_user_status == 1:
                        self.client_socket.send(f"DELETE_USER ok {username} END\n".encode('utf-8'))
                    elif delete_user_status == 0:
                        self.client_socket.send(f"DELETE_USER dontfind {username} END\n".encode('utf-8'))
                    else:
                        self.client_socket.send(f"DELETE_USER error {username} END\n".encode('utf-8'))
                    for client in clients:
                        if client.addr == self.addr:
                            client.id = 0
                            client.username = ""
                            client.elo = 1500
                            break
                    for client in clients:
                        if client.addr == self.addr:
                            continue
                        client.socket.send(f"DELETE_PLAYER {username} END\n".encode('utf-8'))
                ####################
                #       LOGIN      #
                ####################
                elif method == "LOGIN":
                    username = message.split(" ")[1]
                    check_user_id, check_user_username, check_user_elo = Database.checkUser(username)
                    if check_user_id == 0:
                        self.client_socket.send(f"LOGIN doesnotexist {check_user_username} {check_user_elo} END\n".encode('utf-8'))
                    elif check_user_id == -1:
                        self.client_socket.send(f"LOGIN error {check_user_username} {check_user_elo} END\n".encode('utf-8'))
                    else:
                        self.client_socket.send(f"LOGIN ok {check_user_username} {check_user_elo} END\n".encode('utf-8'))
                        for client in clients:
                            if client.addr == self.addr:
                                client.id = check_user_id
                                client.username = check_user_username
                                client.elo = check_user_elo
                                break
                        for client in clients:
                            if client.addr == self.addr:
                                continue
                            client.socket.send(f"UPDATE_PLAYER {check_user_username} {check_user_elo} 1 END\n".encode('utf-8'))
                ####################
                #    GET_PLAYERS   #
                ####################
                elif method == "GET_PLAYERS":
                    get_players = Database.getPlayers()
                    for player in get_players:
                        id = player[0]
                        username = player[1]
                        elo = player[2]
                        isOnline = 0
                        itsMe = False
                        for client in clients:
                            if client.username == username and client.elo == elo:
                                if client.addr == self.addr:
                                    itsMe = True
                                    break
                                if client.id != 0:
                                    isOnline = 1
                        if itsMe == False:
                            self.client_socket.send(f"ADD_PLAYER {id} {username} {elo} {isOnline} END\n".encode('utf-8'))
                #self.broadcast(message)
                #self.client_socket.send(message.encode('utf-8'))
        except ConnectionResetError:
            print(f"[{datetime.now()}] [-] BROKEN connection with {self.addr}")
        finally:
            self.client_socket.close()
            remove_client_username = ""
            remove_client_elo = 1500
            for client in clients:
                if client.addr == self.addr:
                    remove_client_username = client.username
                    remove_client_elo = client.elo
                    clients.remove(client)
                    break
            for client in clients:
                if client.addr == self.addr:
                    continue
                client.socket.send(f"UPDATE_PLAYER {remove_client_username} {remove_client_elo} 0 END\n".encode('utf-8'))
            print(f"[{datetime.now()}] [-] CLOSED connection with {self.addr}")

    #def broadcast(self, message):
    #    for client in clients:
    #        if client != self.client_socket:
    #            try:
    #                client.send(message.encode('utf-8'))
    #            except:
    #                continue

class Database:
    @staticmethod
    def createTableUsers():
        try:
            with sqlite3.connect(DATABASE_NAME) as conn:
                cursor = conn.cursor()
                cursor.execute('''
                CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                elo INTEGER DEFAULT 1500
                );
                ''')
                print(f"[{datetime.now()}] [n] DATABASE create table users")
        except sqlite3.Error as e:
            print(f"[{datetime.now()}] [!] DATABASE create table users error: {e}")

    @staticmethod
    def addUser(username):
        try:
            with sqlite3.connect(DATABASE_NAME) as conn:
                cursor = conn.cursor()
                try:
                    cursor.execute('''
                    INSERT INTO users (username) VALUES (?);
                    ''', (username,))
                    print(f"[{datetime.now()}] [n] ADD username = `{username}`")
                    return cursor.lastrowid
                except sqlite3.IntegrityError:
                    print(f"[{datetime.now()}] [!] ADD username: `{username}` already in use")
                    return 0
        except sqlite3.Error as e:
            print(f"[{datetime.now()}] [!] ADD username: `{username}` error: {e}")
            return -1

    @staticmethod
    def checkUser(username):
        check_id = 0
        check_username = ""
        check_elo = 1500
        try:
            with sqlite3.connect(DATABASE_NAME) as conn:
                cursor = conn.cursor()
                cursor.execute('''
                SELECT * FROM users WHERE username = ?;
                ''', (username,))
                check_result = cursor.fetchone()
                is_check = False
                if check_result:
                    is_check = True
                    check_id = check_result[0]
                    check_username = check_result[1]
                    check_elo = check_result[2]
                if is_check == True:
                    print(f"[{datetime.now()}] [n] LOGIN username = `{username}`")
                else:
                    print(f"[{datetime.now()}] [!] LOGIN username: `{username}` does not exist")
        except sqlite3.Error as e:
            check_id = -1
            print(f"[{datetime.now()}] [!] LOGIN username: `{username}` error: {e}")
        finally:
            return [check_id, check_username, check_elo]

    @staticmethod
    def deleteUser(username):
        try:
            with sqlite3.connect(DATABASE_NAME) as conn:
                cursor = conn.cursor()
                cursor.execute('''
                DELETE FROM users WHERE username=?;
                ''', (username,))
                if (cursor.rowcount > 0):
                    print(f"[{datetime.now()}] [n] DELETE username = `{username}`")
                    return 1
                else:
                    print(f"[{datetime.now()}] [!] DELETE username: `{username}` dont find")
                    return 0
        except sqlite3.Error as e:
            print(f"[{datetime.now()}] [!] DELETE username: `{username}` error: {e}")
            return -1

    @staticmethod
    def getPlayers():
        try:
            with sqlite3.connect(DATABASE_NAME) as conn:
                cursor = conn.cursor()
                cursor.execute('''
                SELECT * FROM users;
                ''')
                return cursor.fetchall()
        except sqlite3.Error as e:
            print(f"[{datetime.now()}] [!] GET PLAYERS error: {e}")
            return []

def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
        s.close()
        return local_ip
    except Exception as e:
        print(f"[{datetime.now()}] [!] get local ip error: {e}")
        return "0.0.0.0"

class Client:
    def __init__(self, socket, addr):
        super().__init__()
        self.socket = socket
        self.addr = addr
        self.id = 0
        self.username = ""
        self.elo = 1500

def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(('0.0.0.0', PORT))
    server.listen(5)
    print("-=== Snaval Strike Server ===-")
    print(f"[{datetime.now()}] [*] ADDRESS {get_local_ip()}:{PORT}")
    Database.createTableUsers()
    try:
        while True:
            client_socket, addr = server.accept()
            client = Client(client_socket, addr)
            clients.append(client)
            handler = ClientHandler(client_socket, addr)
            handler.start()
    except KeyboardInterrupt:
        print(f"[{datetime.now()}] [!] STOP")
        print("-=== Snaval Strike Server ===-")
    finally:
        server.close()

clients = []
PORT = 32951
DATABASE_NAME = "snaval_strike_server_database.db"

if __name__ == "__main__":
    main()
