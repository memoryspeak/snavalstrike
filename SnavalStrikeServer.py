import socket
import threading
from threading import Lock
import sqlite3
from datetime import datetime

class ClientHandler(threading.Thread): # наследуемся от threading.THREAD
    def __init__(self, socket, addr):
        super().__init__()
        self.socket = socket
        self.addr = addr
        self.running = True

    def run(self):
        print(f"[{datetime.now()}] [+] NEW connection: {self.addr}")
        try:
            while self.running:
                # читаем сообщение
                message = self.socket.recv(1024).decode('utf-8')
                # если сообщения в сокете нет, занаво начинаем цикл чтения
                if not message:
                    break
                
                # сообщения приходят с символом '\n' на конце, поэтому end=""
                print(f"[{datetime.now()}] [m] MESSAGE from {self.addr}: {message}", end="")
                
                # ВАЖНО
                # API взаимодействия между клиентом и сервером строится следующим образом:
                # текст пересылаемых сообщений примерно такого вида:
                # METHOD параметр1 параметр2 параметр3 END\\n
                # поэтому парсинг очень простой - сплит по пробелам
                # дальше проверяем МЕТОД - на основании этого метода
                # пишем логику обработки остальных параметров
                method = message.split(" ")[0]
                
                ####################
                #     HEARTBEAT    #
                ####################
                # метод биения сердца, просто чтобы не закрывалось соединение
                # тип сообщения HEARTBEAT END\n
                if method == "HEARTBEAT":
                    self.sendMessage(self.socket, "HEARTBEAT")
                
                ####################
                #     ADD_USER     #
                ####################
                # метод РЕГИСТРАЦИИ нового юзера
                # тип сообщения ADD_USER username END\n
                elif method == "ADD_USER":
                    # парсим по пробелу, находим параметр имени
                    username = message.split(" ")[1]
                    
                    # добавляем пользователя в базу данных
                    # возвращается id добавленного пользователя
                    # или 0, если пользователь уже существует
                    # или -1, если ошибка добавления пользователя
                    add_user_id = Database.addUser(username)
                    
                    # в любом случае нашему пользователю отправляется сообщение вида
                    # ADD_USER {status} {username} END\n
                    # где status может быть alreadyinuse, error, ok
                    if add_user_id == 0:
                        self.sendMessage(self.socket, f"ADD_USER alreadyinuse {username} END\n")
                    elif add_user_id == -1:
                        self.sendMessage(self.socket, f"ADD_USER error {username} END\n")
                    else:
                        self.sendMessage(self.socket, f"ADD_USER ok {username} END\n")
                        # если пользователь успешно добавлен в базу данных
                        # находим его в списке подключенных клиентов
                        # и изменяем поля данных
                        for client in clients:
                            if client.addr == self.addr:
                                client.setDatabaseId(add_user_id)
                                client.setUsername(username)
                                client.setElo(1500)
                                client.setIsOnline()
                                break
                        
                        # производим рассылку сообщения типа
                        # ADD_PLAYER {add_user_id} {username} {elo} {isOnline} {isBusy} END\n
                        # всем клиентам (кроме себя)
                        # где поле add_user_id используется на клиенте
                        # для уникального идентификатора в списке RecyclerView
                        # elo устанавливается 1500
                        # параметры isOnline (клиент онлайн), isBusy (клиент занят в игре)
                        # принимают значения 1 или 0
                        with clients_lock:
                            clients_copy = clients.copy()
                        for client in clients_copy:
                            if client.addr != self.addr:
                                self.sendMessage(client.socket, f"ADD_PLAYER {add_user_id} {username} 1500 1 0 END\n")
                
                ####################
                #    DELETE_USER   #
                ####################
                # метод удаления юзера
                # тип сообщения DELETE_USER username END\n
                elif method == "DELETE_USER":
                    # парсим по пробелу, находим параметр имени
                    username = message.split(" ")[1]
                    
                    # удаляем пользователя из базы данных
                    # возвращается 1, если пользователь удален успешно
                    # или 0, если пользователь не найден
                    # или -1, если ошибка удаления пользователя
                    delete_user_status = Database.deleteUser(username)
                    
                    # в любом случае нашему пользователю отправляется сообщение вида
                    # DELETE_USER {status} {username} END\n
                    # где status может быть dontfind, error, ok
                    if delete_user_status == 1:
                        self.sendMessage(self.socket, f"DELETE_USER ok {username} END\n")
                    elif delete_user_status == 0:
                        self.sendMessage(self.socket, f"DELETE_USER dontfind {username} END\n")
                    else:
                        self.sendMessage(self.socket, f"DELETE_USER error {username} END\n")
                    
                    # в любом случае находим клиент в списке подключенных клиентов
                    # и изменяем поля данных на дефолтные
                    for client in clients:
                        if client.addr == self.addr:
                            client.setDatabaseId(0)
                            client.setUsername("")
                            client.setElo(1500)
                            client.setIsOnline()
                            break
                    
                    # производим рассылку сообщения типа
                    # DELETE_PLAYER {username} END\n
                    # всем клиентам (кроме себя)
                    with clients_lock:
                        clients_copy = clients.copy()
                    for client in clients_copy:
                        if client.addr != self.addr:
                            self.sendMessage(client.socket, f"DELETE_PLAYER {username} END\n")
                ####################
                #       LOGIN      #
                ####################
                # метод логирования юзера
                # тип сообщения LOGIN username END\n
                elif method == "LOGIN":
                    # парсим по пробелу, находим параметр имени
                    username = message.split(" ")[1]
                    
                    # проверяем есть ли пользователь в базе данных
                    # возвращается список [check_id, check_username, check_elo]
                    # если пользователь найден, то check_id будет id пользователя в базе данных
                    # если пользователя нет с таким именем, то check_id будет по дефолту (ноль, 0)
                    # если случилась ошибка, то check_id будет -1
                    check_user_id, check_user_username, check_user_elo = Database.checkUser(username)
                    
                    # в любом случае нашему пользователю отправляется сообщение вида
                    # LOGIN {status} {username} {elo} END\n
                    # где status может быть doesnotexist, error, ok
                    if check_user_id == 0:
                        self.sendMessage(self.socket, f"LOGIN doesnotexist {check_user_username} {check_user_elo} END\n")
                    elif check_user_id == -1:
                        self.sendMessage(self.socket, f"LOGIN error {check_user_username} {check_user_elo} END\n")
                    else:
                        self.sendMessage(self.socket, f"LOGIN ok {check_user_username} {check_user_elo} END\n")
                        # если пользователь успешно найден в базе данных
                        # находим его в списке подключенных клиентов
                        # и изменяем поля данных
                        for client in clients:
                            if client.addr == self.addr:
                                client.setDatabaseId(check_user_id)
                                client.setUsername(check_user_username)
                                client.setElo(check_user_elo)
                                client.setIsOnline()
                                break
                        
                        # производим рассылку сообщения типа
                        # UPDATE_PLAYER {username} {elo} {isOnline} {isBusy} END\n
                        # всем клиентам (кроме себя)
                        # где параметры ОНЛАЙН и ЗАНЯТ (в игре) априори 1 и 0
                        with clients_lock:
                            clients_copy = clients.copy()
                        for client in clients_copy:
                            if client.addr != self.addr:
                                self.sendMessage(client.socket, f"UPDATE_PLAYER {check_user_username} {check_user_elo} 1 0 END\n")
                ####################
                #    GET_PLAYERS   #
                ####################
                # метод возвращает всех игроков
                # тип сообщения GET_PLAYERS END\n
                elif method == "GET_PLAYERS":
                    # получаем список списков игроков из базы данных
                    # [
                    #     [id, username, elo],
                    #     [id, username, elo],
                    #     [id, username, elo]...
                    # ]
                    get_players = Database.getPlayers()
                    
                    # для каждого игрока из полученного списка
                    for player in get_players: # [id, username, elo]
                        # получаем остальные параметры
                        socket, addr, database_id, username, elo, isOnline, isBusy = getClientBy("username", player[1])
                        
                        # для каждого пользователся из базы данных
                        # проверяем, что его параметры валидны (что все не равны None)
                        # проверяем, что текущий пользователь из списка
                        # это НЕ НАШ текущий клиент
                        if all(x is not None for x in [socket, addr, database_id, username, elo, isOnline, isBusy]):
                            if addr != self.addr:
                                # и отправляем сообщение типа
                                # ADD_PLAYER {database_id} {username} {elo} {1 if isOnline else 0} {1 if isBusy else 0} END\n
                                # в основном здесь самые главные параметры isOnline, isBusy
                                # именно их мы искали в списке пользователей
                                self.sendMessage(self.socket, f"ADD_PLAYER {database_id} {username} {elo} {1 if isOnline else 0} {1 if isBusy else 0} END\n")
                        else:
                            # или сообщение о добавлении неактивного,
                            # но присутствующего в базе данных пользователя,
                            # то есть, такого, параметры которого все None
                            # то есть берем те параметры, которые пришли из базы данных
                            # а isOnline, isBusy ставим в нули
                            self.sendMessage(self.socket, f"ADD_PLAYER {player[0]} {player[1]} {player[2]} 0 0 END\n")
                ####################
                #   REQUEST_GAME   #
                ####################
                # запрос на игру
                # тип сообщения REQUEST_GAME {username} END\n
                # username - это с кем хочет играть инициатор, то есть это целевой
                elif method == "REQUEST_GAME":
                    # парсим имя целевого пользователя
                    request_username = message.split(" ")[1]
                    
                    # параметры инициатора и целевого пользователя
                    # инициатор
                    socketA = None
                    addrA = None
                    database_idA = None
                    usernameA = None
                    eloA = None
                    isOnlineA = None
                    isBusyA = None
                    # целевой
                    socketB = None
                    addrB = None
                    database_idB = None
                    usernameB = None
                    eloB = None
                    isOnlineB = None
                    isBusyB = None
                    
                    # устанавливаем статус ЗАНЯТО в оба клиента
                    # заполняем параметры клиентов, когда их находим
                    for client in clients:
                        if client.addr == self.addr:
                            with clients_lock:
                                client.isBusy = True
                            socketA, addrA, database_idA, usernameA, eloA, isOnlineA, isBusyA = getClientBy("addr", self.addr)
                        if client.username == request_username:
                            with clients_lock:
                                client.isBusy = True
                            socketB, addrB, database_idB, usernameB, eloB, isOnlineB, isBusyB = getClientBy("username", request_username)
                    
                    # отправляем всем клиентам (кроме инициатора и целевого)
                    # обновленные статусы ЗАНЯТО (инициатора и целевого)
                    with clients_lock:
                        clients_copy = clients.copy()
                    for client in clients_copy:
                        if all(x is not None for x in [socketA, addrA, database_idA, usernameA, eloA, isOnlineA, isBusyA]):
                            if addrA != client.addr:
                                self.sendMessage(client.socket, f"UPDATE_PLAYER {usernameA} {eloA} {1 if isOnlineA else 0} {1 if isBusyA else 0} END\n")
                        if all(x is not None for x in [socketB, addrB, database_idB, usernameB, eloB, isOnlineB, isBusyB]):
                            if addrB != client.addr:
                                self.sendMessage(client.socket, f"UPDATE_PLAYER {usernameB} {eloB} {1 if isOnlineB else 0} {1 if isBusyB else 0} END\n")
                    
                    # проверяем список games
                    # содержит ли он игру, в которой участвует
                    # один из игроков (инициатор или целевой)
                    with games_lock:
                        games_copy = games.copy()
                    usernamesInGames = False
                    for game in games_copy:
                        if game.isUsernameInGame(usernameA) or game.isUsernameInGame(usernameB):
                            usernamesInGames = True
                            break
                    # если нет такой игры
                    # отправляем сообщения для инициатора и целевого
                    # эти сообщения позволят отобразить AlertDialog в интерфейсе клиентов
                    if usernamesInGames == False:
                        if all(x is not None for x in [socketA, addrA, database_idA, usernameA, eloA, isOnlineA, isBusyA]) and all(x is not None for x in [socketB, addrB, database_idB, usernameB, eloB, isOnlineB, isBusyB]):
                            games.append(Game(usernameA, usernameB))
                            self.sendMessage(socketA, f"REQUEST_GAME {usernameA} {eloA} {usernameB} {eloB} END\n")
                            self.sendMessage(socketB, f"REQUEST_GAME {usernameA} {eloA} {usernameB} {eloB} END\n")
                ####################
                #   RESPONSE_GAME  #
                ####################
                # ответ на запрос
                # тип сообщения RESPONSE_GAME {username} {status} END\n
                # username - это кому предназначен ответ, напарник
                # status может быть ok или bad
                elif method == "RESPONSE_GAME":
                    # парсим имя напарника, статус
                    response_username = message.split(" ")[1]
                    status = message.split(" ")[2]
                    
                    # параметры ответчика и напарника
                    # ответчик
                    socketA = None
                    addrA = None
                    database_idA = None
                    usernameA = None
                    eloA = None
                    isOnlineA = None
                    isBusyA = None
                    # напарник
                    socketB = None
                    addrB = None
                    database_idB = None
                    usernameB = None
                    eloB = None
                    isOnlineB = None
                    isBusyB = None
                    
                    # устанавливаем статусы в оба клиента на основании присланного status
                    # заполняем параметры клиентов, когда их находим
                    for client in clients:
                        if client.addr == self.addr:
                            with clients_lock:
                                if status == "bad":
                                    client.isBusy = False
                                else:
                                    client.isBusy = True
                            socketA, addrA, database_idA, usernameA, eloA, isOnlineA, isBusyA = getClientBy("addr", self.addr)
                        if client.username == response_username:
                            with clients_lock:
                                if status == "bad":
                                    client.isBusy = False
                                else:
                                    client.isBusy = True
                            socketB, addrB, database_idB, usernameB, eloB, isOnlineB, isBusyB = getClientBy("username", response_username)
                    
                    # отправляем всем клиентам (кроме ответчика и напарника)
                    # обновленные статусы ответчика и напарника
                    with clients_lock:
                        clients_copy = clients.copy()
                    for client in clients_copy:
                        if all(x is not None for x in [socketA, addrA, database_idA, usernameA, eloA, isOnlineA, isBusyA]):
                            if addrA != client.addr:
                                self.sendMessage(client.socket, f"UPDATE_PLAYER {usernameA} {eloA} {1 if isOnlineA else 0} {1 if isBusyA else 0} END\n")
                        if all(x is not None for x in [socketB, addrB, database_idB, usernameB, eloB, isOnlineB, isBusyB]):
                            if addrB != client.addr:
                                self.sendMessage(client.socket, f"UPDATE_PLAYER {usernameB} {eloB} {1 if isOnlineB else 0} {1 if isBusyB else 0} END\n")
                    
                    # проверяем список games
                    # содержит ли он игру, в которой участвует
                    # один из игроков (ответчик или напарник)
                    # сохраним игру в find_game
                    with games_lock:
                        games_copy = games.copy()
                    find_game = None
                    for game in games_copy:
                        if game.isUsernameInGame(usernameA) and game.isUsernameInGame(usernameB):
                            find_game = game
                            break
                    # если есть такая игра
                    # отправляем сообщения для ответчика и напарника
                    # эти сообщения позволят убрать AlertDialog в интерфейсе клиентов
                    # и запустить игру, если status == 'ok'
                    # также удаляем игру, если status == 'bad'
                    if find_game is not None:
                        if status == "ok":
                            # обходим список игр
                            for game in games:
                                # если находим нашу игру
                                if game == find_game:
                                    # запускаем её
                                    with games_lock:
                                        game.running = True
                                    break
                            if all(x is not None for x in [socketA, addrA, database_idA, usernameA, eloA, isOnlineA, isBusyA]) and all(x is not None for x in [socketB, addrB, database_idB, usernameB, eloB, isOnlineB, isBusyB]):
                                self.sendMessage(socketA, f"RESPONSE_GAME {usernameA} {eloA} {usernameB} {eloB} ok END\n")
                                self.sendMessage(socketB, f"RESPONSE_GAME {usernameA} {eloA} {usernameB} {eloB} ok END\n")
                        else:
                            # обходим список игр
                            for game in games:
                                # если находим нашу игру
                                if game == find_game:
                                    # удаляем её
                                    with games_lock:
                                        games.remove(game)
                                    break
                            if all(x is not None for x in [socketA, addrA, database_idA, usernameA, eloA, isOnlineA, isBusyA]) and all(x is not None for x in [socketB, addrB, database_idB, usernameB, eloB, isOnlineB, isBusyB]):
                                self.sendMessage(socketA, f"RESPONSE_GAME {usernameA} {eloA} {usernameB} {eloB} bad END\n")
                                self.sendMessage(socketB, f"RESPONSE_GAME {usernameA} {eloA} {usernameB} {eloB} bad END\n")
        except ConnectionResetError: # ошибка обрыва соединения с клиентом
            print(f"[{datetime.now()}] [-] BROKEN connection with {self.addr}")
        finally:
            # данная ветка срабатывает
            # при отключении клиента от сокета
            
            # закрываем соединение с отключившимся клиентом на стороне сервера
            self.socket.close()
            
            # remove_client -(удаляемый)
            # - это наш отключившийся клиент,
            # который нужно удалить
            # rival_client - (соперник)
            # - это наш ВОЗМОЖНЫЙ соперник,
            # с которым ВОЗМОЖНО была игра у отключившегося клиента
            remove_client_username = None
            remove_client_elo = None
            rival_client_username = None
            rival_client_elo = None
            rival_client_isOnline = None
            rival_client_isBusy = None
            
            # находим среди клиентов remove клиента
            # и бережно сохраняем его параметры
            # затем безжалостно удаляем его из списка
            for client in clients:
                if client.addr == self.addr:
                    with clients_lock:
                        remove_client_username = client.username
                        remove_client_elo = client.elo
                        clients.remove(client)
                    break
                    
            # обходим список игр для определения того факта,
            # существует ли игра, в которой участвует remove клиент
            with games_lock:
                games_copy = games.copy()
            for game in games_copy:
                # нашли такую игру
                if game.isUsernameInGame(remove_client_username):
                    # игра содержит удаляемого игрока в качестве игрока А
                    if game.usernameA == remove_client_username:
                        # значит соперник - это игрок B
                        rival_client_username = game.usernameB
                    # игра содержит удаляемого игрока в качестве игрока B
                    elif game.usernameB == remove_client_username:
                        # значит соперник - это игрок А
                        rival_client_username = game.usernameA
                    # после бережного сохранения имени соперника
                    # безжалостно удаляем игру из списка
                    with games_lock:
                        games.remove(game)
                break
            
            # если соперник существует,
            if rival_client_username is not None:
                # обходим список клиентов,
                for client in clients:
                    # находим нашего соперника и удаляем у него статус ЗАНЯТО
                    if client.username == rival_client_username:
                        with clients_lock:
                            client.isBusy = False
                        break
                # затем находим остальные (кроме имени) параметры нашего соперника
                _, _, _, rival_client_username, rival_client_elo, rival_client_isOnline, rival_client_isBusy = getClientBy("username", rival_client_username)
            
            # всем клиентам,
            with clients_lock:
                clients_copy = clients.copy()
            for client in clients_copy:
                # кроме удаляемого клиента
                if client.addr != self.addr:
                    if remove_client_username is not None and remove_client_elo is not None:
                        # отсылаем сообщение о том, что удаляемый клиент оффлайн
                        self.sendMessage(client.socket, f"UPDATE_PLAYER {remove_client_username} {remove_client_elo} 0 0 END\n")
                    if rival_client_username is not None and rival_client_elo is not None:
                        # и отсылаем сообщение о том, что наш соперник теперь в статусе СВОБОДЕН
                        self.sendMessage(client.socket, f"UPDATE_PLAYER {rival_client_username} {rival_client_elo} {1 if rival_client_isOnline else 0} {1 if rival_client_isBusy else 0} END\n")
            print(f"[{datetime.now()}] [-] CLOSED connection with {self.addr}")

    # функция отправки сообщения для класса ClientHandler
    def sendMessage(self, socket, message):
        try:
            # если все четко - отправляем сообщение в целевой сокет
            socket.send(message.encode('utf-8'))
        except Exception as e:
            # если ошибка - печатаем ошибку и ничего не отправляем
            print(f"[{datetime.now()}] [!] ERROR send message `{message}`: {e}")

# класс для взаимодействия с базой данных
# содержит только статические методы
# просто для удобства компановки кода
class Database:
    # создаем таблицу пользователей (если она еще не существует)
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

    # добавляем в таблицу пользователя по его имени
    # остальные параметры добавятся по дефолту
    # ВНИМАТЕЛЬНО метод возвращает id последнего добавленного пользователя в базу данных
    # если ошибка, возвращает -1
    # если пользователь уже есть в базе, возвращает 0
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

    # метод проверки, существует ли юзер с указанным именем
    # ВНИМАТЕЛЬНО возвращает список [check_id, check_username, check_elo]
    # если пользователь найден, то check_id будет id пользователя в базе данных
    # если пользователя нет с таким именем, то check_id будет по дефолту (ноль, 0)
    # если случилась ошибка, то check_id будет -1
    @staticmethod
    def checkUser(username):
        check_id = 0
        check_username = ""
        check_elo = 1500
        try:
            with sqlite3.connect(DATABASE_NAME) as conn:
                cursor = conn.cursor()
                # делаем запрос на получение всех пользователей у которых имя равно нашему
                cursor.execute('''
                SELECT * FROM users WHERE username = ?;
                ''', (username,))
                # и берем самый первый результат
                check_result = cursor.fetchone()
                is_check = False
                if check_result:
                    is_check = True
                    # результат будет в виде списка,
                    # где на 0 индексе будет id пользователя
                    # на 1 индексе будет имя пользователя
                    # на 2 индексе будет рейтинг пользователя
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

    # метод удаляет пользователя по его имени
    # ВНИМАТЕЛЬНО возвращает 1 если пользователь удален успешно
    # возвращает 0 если не найдено текущего имени
    # возвращает -1 если ошибка
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

    # метод возвращает список списков всех пользователей в базе данных
    # [
    #     [id, username, elo],
    #     [id, username, elo],
    #     [id, username, elo]...
    # ]
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

# данный метод ищет по списку игроков
# в соответствии с методом "addr" - по адресу клиента или "username" - по имени клиента
# и возвращает списик вида
# [socket, addr, database_id, username, elo, isOnline, isBusy]
# по дефолту возвращает [None, None, None, None, None, None, None]
def getClientBy(method, param):
    with clients_lock:
        clients_copy = clients.copy()
    if method == "addr":
        for client in clients_copy:
            if client.addr == param:
                return client.getFields()
    elif method == "username":
        for client in clients_copy:
            if client.username == param:
                return client.getFields()
    return [None, None, None, None, None, None, None]

class Client:
    def __init__(self, socket, addr):
        super().__init__()
        with clients_lock:
            # инициализируются сокет и адрес
            self.socket = socket
            self.addr = addr
            # остальные поля по дефолту
            self.database_id = 0
            self.username = ""
            self.elo = 1500
            self.isOnline = False
            self.isBusy = False

    # метод возвращает список значений параметров в виде
    # [socket, addr, database_id, username, elo, isOnline, isBusy]
    # либо по дефолту [None, None, None, None, None, None, None]
    def getFields(self):
        socket = None
        addr = None
        database_id = None
        username = None
        elo = None
        isOnline = None
        isBusy = None
        self.setIsOnline()
        with clients_lock:
            socket = self.socket
            addr = self.addr
            database_id = self.database_id
            username = self.username
            elo = self.elo
            isOnline = self.isOnline
            isBusy = self.isBusy
        return [socket, addr, database_id, username, elo, isOnline, isBusy]

    def setSocket(self, socket):
        with clients_lock:
            self.socket = socket

    def setAddr(self, addr):
        with clients_lock:
            self.addr = addr

    def setDatabaseId(self, database_id):
        with clients_lock:
            self.database_id = database_id

    def setUsername(self, username):
        with clients_lock:
            self.username = username

    def setElo(self, elo):
        with clients_lock:
            self.elo = elo

    # метод устанавливает значение isOnline
    # на основании того,
    # равно ли имя пользователя пустой строке или нет
    def setIsOnline(self):
        with clients_lock:
            self.isOnline = self.username != ""

    def setIsBusy(self, isBusy):
        with clients_lock:
            self.isBusy = isBusy

class Game:
    def __init__(self, usernameA, usernameB):
        super().__init__()
        with games_lock:
            self.usernameA = usernameA
            self.usernameB = usernameB
            self.running = False

    # метод возвращает буль - участвует ли пользователь в игре или нет
    def isUsernameInGame(self, username):
        if username == self.usernameA:
            return True
        if username == self.usernameB:
            return True
        return False

# список всех подлюченных к серверу клиентов типа Client + блокировщик потока
clients = []
clients_lock = Lock()

# список всех игр типа Game + блокировщик потока
games = []
games_lock = Lock()

# константы
PORT = 32951
DATABASE_NAME = "snaval_strike_server_database.db"

if __name__ == "__main__":
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(('0.0.0.0', PORT))
    
    # максимальное количество ожидающих подключений в очереди
    # для простых серверов обычно используют значения от 5 до 10
    # для высоконагруженных систем значение может быть увеличено
    # но это требует настройки операционной системы
    server.listen(8)
    
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
