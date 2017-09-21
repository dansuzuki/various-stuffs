from flask import Flask


# from https://blog.miguelgrinberg.com/post/running-your-flask-application-over-https

app = Flask(__name__)

@app.route("/")
def hello():
    return "Hello World!"

if __name__ == "__main__":
    app.run(ssl_context=('cert.pem', 'key.pem'))

