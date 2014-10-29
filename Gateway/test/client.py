from numpy import *
from struct import *
import scipy
import scipy.signal as sig
import bluetooth, sys, time
import re
import math
#from console import get_terminal_size
#(COLUMNS, LINES) = get_terminal_size()
# Generate ECG from wavelet
'''
rr = [1.0, 1.0, 0.5, 1.0, 1.0]
fs = 8000.0
pqrst = sig.wavelets.daub(10)
ecg = scipy.concatenate([sig.resample(pqrst, int(r*fs)) for r in rr])
mmin = min(ecg)
mmax = max(ecg)
ecg = array((ecg-mmin)/(mmax-mmin)*255, dtype=int8).tolist()
'''

# ECG From MIT Physionet (samples.csv)
f = open('samples.csv')
time_reg = re.compile(r'(\d+):(\d+)\.(\d+)')
time_list = []
data_MLII_list = []
data_V5_list = []
counter = 0
timer1_sec = 0
timer2_sec = 0

# Deny First two lines
f.readline()

# Start parse data
print 'Loading samples.csv ',
for line in f.xreadlines():
  counter += 1
  line = line.strip().split(",")
  '''
  match = time_reg.search(line[0]).groups()
  if len(match) != 3:
    print "Error, ", line
    continue
  sec = int(match[0])*60 + int(match[1]) + int(match[2])/1000.0
  time_list.append(sec)
  '''
  data_MLII_list.append(int(line[1]))
  data_V5_list.append(int(line[2]))
f.close()
print '({:d} records)'.format(len(data_MLII_list)),
ecg = data_MLII_list
print 'OK'

service_name = 'BtGateway'


port = 0
host = ""
name = ""
service = None
sock = None
service_matches = []

def c(CONST_K=60, delay_sec=1.0/6, max_loop=-1, protocol=0xAAAA):
  global port
  global host
  global name
  global service
  global sock
  global service_matches
  print 'CONST_K: {2} delay_sec: {0} max_loop: {1} protocol: {3}'.format(delay_sec, max_loop, CONST_K, protocol)
  
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
    tick()
    if protocol == 0xAAAA:
      d = ecg[start_i:start_i+CONST_K]
      l = CONST_K - len(d) 
      
      if l > 0:
        d.extend(ecg[0:l])
        start_i = l
      else:
        start_i = start_i + CONST_K

      sock.send(packet1(data=d, ch=0x00))

    elif protocol == 0xABCD:
      d = []
      d.append(data_MLII_list[start_i])
      d.append(data_V5_list[start_i])
      d.extend([start_i,(int) (100*math.sin(math.pi*start_i/500.0)+100),0,1,2,3])
      
      start_i += 1
      if start_i >= len(data_V5_list):
        start_i = 0
      if start_i >= len(data_MLII_list):
        start_i = 0
      sock.send(packet2(d))
    
    #print hexify(packet(data=d))
    
    stop = stop + 1
    
    
    tock()
    if (elapse() < delay_sec):
      time.sleep(delay_sec - elapse())
    
    if max_loop == -1:
      continue
      #End condition
    if stop >= max_loop:
      break
  sock.close()

def packet1(address=0x00AABBCC, ch=0x00, opt=0x00, data=[]):
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
    data[i] = (v & 0xFFFF);
  data_len = len(data)
  data.insert(0,opt)
  data.insert(0,ch)
  data.insert(0,address)
  return pack('>IBB'+'H'* data_len, *data)
  #print hexify(pack('>IBB'+'H'* data_len, *data))

def packet2(raw_data=[]):
  data = []
  for i,v in enumerate(raw_data):
    data.append((v & 0xFF0000) >> 16)
    data.append((v & 0x00FF00) >> 8)
    data.append(v & 0x0000FF)
  data.append(0x04)
  data = [0x01, 0x01, 0x00,0x01, 0xAA, 0xBB, 0xCC] + data
  return pack('>' + 'B'*len(data), *data)
  
def hexify(s):
  return ":".join("{:02X}".format(ord(c)) for c in s)
  

def tick():
  global timer1_sec
  timer1_sec = time.time()

def tock(isShow = False):
  global timer2_sec
  timer2_sec = time.time()
  if isShow:
    print 'Elasped Time:', (timer2_sec - timer1_sec)
def elapse():
    return timer2_sec - timer1_sec
def protocol2(m=-1):
  c(protocol=0xABCD, delay_sec = 1/360.0, max_loop = m)
if __name__ == "__main__":
  print "Usage: c(CONST_K=60, delay_sec=1.0/6, max_loop=-1)"
