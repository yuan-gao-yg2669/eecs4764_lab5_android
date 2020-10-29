'''
Connect with the board socket server. host is your IP. please send request to your IP;
In the request, please set HTTP request body have one line like'Command: <YOUR_COMMAND>, replace <YOUR_COMMAND> with the translated speech';

sample request:
GET /favicon.ico HTTP/1.1\r\n'
Host: 192.168.1.107\r\n'
Command: turn on

The board will send back a html to respond about the change of oled
'''

from machine import Pin, I2C, RTC
import socket, network, ssd1306

# user specified parameter
SSID = ''                # replace with your SSID
password = ''       # replace with your password
cmd_on = b'turn on'          # replace with the speech used to turn on the watch 
cmd_off = b'turn off'        # replace with the speech used to turn of the watch

# set oled parameter
oled_width = 128
oled_height = 32
# I2C initialization
i2c = I2C(-1, scl=Pin(5), sda=Pin(4))
oled = ssd1306.SSD1306_I2C(oled_width, oled_height, i2c)
# initialize clock time
rtc = RTC()
# (y, m, d, w, h, m, s, ms)
rtc.datetime((2020, 10, 25, 7, 22, 00, 00, 0))

# set the html format
html = """<!DOCTYPE html>
<html>
    <head> <title>ESP8266 Smart Watch</title> </head> 
    <body> <h1>ESP8266 Smart Watch</h1>
            <h2>%s</h2>
    </body>
</html>
"""

# change the time display format
def TimeForm(datetime):
    hour = datetime[4]
    hour = "%02d" % hour
    hour = str(hour)
    minute = datetime[5]
    minute = "%02d" % minute
    minute = str(minute)
    second = datetime[6]
    second = "%02d" % second
    second = str(second)
    return hour+':'+minute+':'+second

# display and update time
def Update(oled, x, y):
    oled.fill(0)
    oled.text(TimeForm(rtc.datetime()), x, y)
    oled.show()

# create wifi connection
def Connect(SSID, password):
    sta_if = network.WLAN(network.STA_IF)
    if not sta_if.isconnected():
        print('connecting to network...')
        sta_if.active(True)
        sta_if.connect(SSID, password)
        while not sta_if.isconnected():
            pass
    print('network config:', sta_if.ifconfig())
    addr = addr = sta_if.ifconfig()[0]
    return addr

def CheckCommand(command):
    global cmd_on, cmd_off
    if cmd_on in command:
        Update(oled, 0, 0)
        text = 'Smart Watch was truned on'
    elif cmd_off in command:
        oled.fill(0)
        oled.show()
        text = 'Smart Watch was truned off'
    else:
        oled.fill(0)
        oled.text(command, 0, 0)
        oled.show()
        text = 'Displaying " %s "' %str(command, 'utf-8')
    return text

# main
# create socket server
host_addr = Connect(SSID, password)
s=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
s.bind((host_addr,80))
s.listen(1)
print('listening on', host_addr)
command = b'turn on'

while True:
    CheckCommand(command)
    while True:
        try:
            s.settimeout(0.01)
            conn, addr = s.accept()
        except OSError:
            break
        print('client connected from', addr)
        try:
            conn_file = conn.makefile('rwb', 0)
            while True:
                line = conn_file.readline()
                print(line)
                if b'Command: ' in line:
                    command = line[9:-2]
                if not line or line == b'\r\n':
                    break
            print('-------------------------   Command: %s  -------------------------' % command)
            text = CheckCommand(command)
            response = html % text
            conn.send('HTTP/1.0 200 OK\r\nContent-type: text/html\r\n\r\n')
            conn.send(response)
            conn.close()
        except OSError:
            conn.send('HTTP/1.0 200 OK\r\nContent-type: text/html\r\nReceived: Fail\r\n\r\n')
            break