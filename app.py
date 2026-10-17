
from flask import Flask, request, jsonify
from urllib.parse import urlparse

app = Flask(__name__)

@app.get("/")
def home():
    return jsonify({
        "name": "SAGE Backend",
        "status": "online",
        "message": "Backend is running!"
    })

@app.get("/health")
def health():
    return jsonify({"status": "ok"})

@app.post("/analyze")
def analyze():
    data = request.get_json(silent=True) or {}
    url = data.get("url", "").strip()

    if not url:
        return jsonify({"error": "URL is required"}), 400

    parsed = urlparse(url)

    if parsed.scheme not in ("http", "https") or not parsed.netloc:
        return jsonify({"error": "Valid HTTP/HTTPS URL required"}), 400

    return jsonify({
        "url": url,
        "status": "valid",
        "message": "URL received. Only use media you own or have permission to download."
    })

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=10000)
