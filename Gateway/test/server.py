from numpy import *
from bluetooth import *
import scipy
import scipy.signal as sig
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

server_sock = BluetoothSocket( RFCOMM )
server_sock.bind(("", PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]
uuid = "3f5d6336-2199-4945-8ac1-883d955d9f84"
advertise_service( server_sock, "SampleServer"
    , service_id = uuid
    , service_classes = [ uuid, SERIAL_PORT_CLASS ]
    , profiles = [ SERIAL_PORT_PROFILE ])
    
print "Waiting for connection on RFCOMM channel {0}".format(port)
state = 0
c = 0
try:
    client_sock, client_info = server_sock.accept()
    
    while True:
        if STATE_SEND_HANDSHAKE == state:
            client_sock.send("GT")
            state = state + 1
        elif STATE_WAIT_HANDSHAKE == state:
            data = client_sock.recv(1024)
            if data.strip().upper() == "TG":
                state = state + 1
            else:
                break
        elif STATE_STREAM:
            try:
                client_sock.send("{0:02x}".format(c))
                c = c + 1
                if c > 255:
                    c = 0
            except:
                raise
except:
    raise

print "Shutting down..."
server_sock.close()
client_sock.close()
   