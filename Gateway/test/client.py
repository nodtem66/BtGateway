from numpy import *
from struct import *
import scipy
import scipy.signal as sig
import bluetooth, sys, time
rr = [1.0, 1.0, 0.5, 1.0, 1.0]
fs = 8000.0
pqrst = sig.wavelets.daub(10)
ecg = scipy.concatenate([sig.resample(pqrst, int(r*fs)) for r in rr])
mmin = min(ecg)
mmax = max(ecg)
ecg = array((ecg-mmin)/(mmax-mmin)*255, dtype=int8).tolist()


STATE_SEND_HANDSHAKE = 0
STATE_WAIT_HANDSHAKE = 1
STATE_STREAM = 2

service_name = 'BtGateway'


port = 0
host = ""
name = ""
service = None
sock = None
service_matches = []

def c(CONST_K=509, delay=100, max_loop=-1):
  global port
  global host
  global name
  global service
  global sock
  global service_matches
  print 'CONST_K: {2} delay: {0} max_loop: {1}'.format(delay, max_loop, CONST_K)
  
  service_matches = bluetooth.find_service(name=service_name)
  if len(service_matches) == 0:
    print "Couldn't find the ", service_name," service"
    return
  service = service_matches[0]
  port = service["port"]
  name = service["name"]
  host = service["host"]

  print "connecting to \"{0}\" on {1}".format(name, host)

  sock = bluetooth.BluetoothSocket( bluetooth.RFCOMM )
  sock.connect((host,port))
  
  start_i = 0
  stop = 0
  while True:
    d = ecg[start_i:start_i+CONST_K]
    l = CONST_K - len(d) 
    if l > 0:
      d.extend(ecg[0:l])
      start_i = l
    else:
      start_i = start_i + CONST_K
    #print hexify(packet(data=d))
    sock.send(packet(data=d))
    time.sleep(delay/1000.0)
    stop = stop + 1
    if max_loop == -1:
      continue
    if stop >= max_loop:
      break
  sock.close()
def packet(address=0x00AABBCC, ch=0x01, opt=0x00, data=[]):
  if type(address) != int:
    print 'error address'
    return
  if type(ch) != int or ch > 0xFF:
    print 'error ch'
    return
  if type(opt) != int or opt > 0xFF:
    print 'error opt'
    return
  if type(data) != list or len(data) > 509:
    print 'error data'
    return
  
  for i,v in enumerate(data):  
    data[i] = (v & 0xFF);
  data_len = len(data)
  data.insert(0,opt)
  data.insert(0,ch)
  data.insert(0,address)
  return pack('>IBB'+'H'* data_len, *data)
  #print hexify(pack('>IBB'+'H'* data_len, *data))
  
def hexify(s):
  return ":".join("{:02X}".format(ord(c)) for c in s)