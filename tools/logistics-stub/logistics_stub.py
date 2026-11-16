import json
import os
from http.server import BaseHTTPRequestHandler, HTTPServer

EXPECTED_TOKEN = os.environ.get("STUB_TOKEN", "testToken")
PORT = int(os.environ.get("STUB_PORT", "9090"))


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", 0))).decode()
        auth = self.headers.get("Authorization")
        print(f"POST {self.path} | Authorization: {auth} | body: {body}", flush=True)

        if self.path != "/api/tarifas":
            self.reply(404, {"error": "not found"})
        elif auth != f"Bearer {EXPECTED_TOKEN}":
            self.reply(401, {"error": "invalid token"})
        else:
            self.reply(200, {"coste": 7.95, "moneda": "EUR", "plazoDias": 2})

    def reply(self, status, payload):
        data = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


if __name__ == "__main__":
    print(f"Logistics stub on http://localhost:{PORT} (expects token '{EXPECTED_TOKEN}')", flush=True)
    HTTPServer(("localhost", PORT), Handler).serve_forever()
