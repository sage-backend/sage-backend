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
    parsed = urlparse(url)

    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        return jsonify({"error": "Valid URL required"}), 400

    host = (parsed.hostname or "").lower()
    supported = (
        host == "youtube.com" or host.endswith(".youtube.com")
        or host == "youtu.be"
        or host == "instagram.com" or host.endswith(".instagram.com")
    )

    if supported:
        return jsonify({
            "status": "recognized",
            "platform": "YouTube" if "youtube" in host or host == "youtu.be" else "Instagram",
            "message": "Link recognized. Use the platform's official download option or authorized media source."
        })

    return jsonify({
        "status": "valid",
        "platform": "direct-media-or-other",
        "message": "URL received."
    })

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=10000)
