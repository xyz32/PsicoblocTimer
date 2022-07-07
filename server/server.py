#!/usr/bin/env python3

import signal
import sys
import time
import RPi.GPIO as GPIO
import socket
import threading

clients_lock = threading.Lock()

BUTTON_LEFT_GPIO = 15
BUTTON_RIGHT_GPIO = 14

active_clients = set()

def signal_handler(sig, frame):
    print("GPIO cleanup")
    GPIO.cleanup()

    print("TCP clients cleanup")
    for client in active_clients:
        client.shutdown(socket.SHUT_RDWR)
        client.close()

    print("TCP server cleanup")
    server.shutdown(socket.SHUT_RDWR)
    server.close()
    sys.exit(0)

def button_released_callback(channel):
    if channel == BUTTON_LEFT_GPIO:
        print("button left pressed")
        sendToClients("LEFT\n")

    if channel == BUTTON_RIGHT_GPIO:
        print("button right pressed")
        sendToClients("RIGHT\n")

def clientHandler(client_socket):
    data = client_socket.recv(1024).decode().strip()
    print("Received \"" + data + "\" from client")
    if data != "1234":
        client_socket.send("invalid auth!\n".encode())
        client_socket.close()
        print("Connection closed for client: \"" + client_socket.getsockname() + "\"")
        return
    else:
        client_socket.send("SUCCESS\n".encode())
        with clients_lock:
            active_clients.add(client_socket)
            print(active_clients)

    try:
        while True:
            data = client_socket.recv(1024).decode().strip()
            if not data:
                client_socket.shutdown(socket.SHUT_RDWR)
                break
    finally:
        with clients_lock:
            active_clients.remove(client)
            client_socket.close()

def sendToClients(data):
    with clients_lock:
        for client in active_clients:
            client.sendall(data.encode())

if __name__ == '__main__':
    print("Setting up gpio")
    GPIO.setmode(GPIO.BCM)

    GPIO.setup(BUTTON_LEFT_GPIO, GPIO.IN, pull_up_down=GPIO.PUD_UP)
    GPIO.add_event_detect(BUTTON_LEFT_GPIO, GPIO.FALLING,
            callback=button_released_callback, bouncetime=100)

    GPIO.setup(BUTTON_RIGHT_GPIO, GPIO.IN, pull_up_down=GPIO.PUD_UP)
    GPIO.add_event_detect(BUTTON_RIGHT_GPIO, GPIO.FALLING,
            callback=button_released_callback, bouncetime=100)

    signal.signal(signal.SIGINT, signal_handler)

    print("Setting tcp server on port 8123.")
    bind_ip = "" # Replace this with your own IP address
    bind_port = 8123 # Feel free to change this port
    # create and bind a new socket
    global server
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((bind_ip, bind_port))
    server.listen(5)
    print("Server is listening on %s:%d" % (bind_ip, bind_port))

    while True:
        # wait for client to connect
        client, addr = server.accept()
        print("Client connected " + str(addr))
        # create and start a thread to handle the client
        client_handler = threading.Thread(target = clientHandler, args=(client,))
        client_handler.start()

