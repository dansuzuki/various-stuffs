import requests
import ssl
from ssl import SSLContext

context = SSLContext(ssl.PROTOCOL_SSLv23)
context.load_cert_chain('./cert.pem')

resp = requests.get('https://127.0.0.1:5000/', context=context)

print resp.content
