from flask import Flask, request, jsonify, send_file
from urllib.parse import urlparse
from pathlib import Path
import os
import tempfile
import requests

app = Flask(__name__)
MAX_SIZE = 100 * 1024 * 1024
ALLOWED = {".mp4", ".webm", ".mp3", ".m4a"}

@app.get("/")
def home():
    return jsonify({"name": "SAGE Backend", "status": "online"})

@app.get("/health")
def health():
    return jsonify({"status": "ok"})

def valid_url(url):
    p = urlparse(url)
    return p.scheme == "https" and p.hostname is not None

@app.post("/analyze")
def analyze():
    data = request.get_json(silent=True) or {}
    url = data.get("url", "").strip()
    if not valid_url(url):
        return jsonify({"error": "Valid HTTPS URL required"}), 400

    host = (urlparse(url).hostname or "").lower()
    if host == "youtube.com" or host.endswith(".youtube.com") or host == "youtu.be":
        return jsonify({"status": "recognized", "platform": "YouTube",
                        "message": "Use YouTube's official download options or an authorized source."})
    if host == "instagram.com" or host.endswith(".instagram.com"):
        return jsonify({"status": "recognized", "platform": "Instagram",
                        "message": "Use Instagram's official download options or an authorized source."})
    return jsonify({"status": "valid", "platform": "direct-media",
                    "message": "Direct media URL accepted for checking."})

@app.post("/download")
def download():
    data = request.get_json(silent=True) or {}
    url = data.get("url", "").strip()
    if not valid_url(url):
        return jsonify({"error": "HTTPS URL required"}), 400

    host = (urlparse(url).hostname or "").lower()
    if host == "youtube.com" or host.endswith(".youtube.com") or host == "youtu.be" or host == "instagram.com" or host.endswith(".instagram.com"):
        return jsonify({"error": "For YouTube/Instagram, use official download options or an authorized export/source."}), 403

    ext = Path(urlparse(url).path).suffix.lower()
    if ext not in ALLOWED:
        return jsonify({"error": "Supported direct formats: MP4, WEBM, MP3, M4A"}), 400

    try:
        with requests.get(url, stream=True, timeout=(10, 30)) as r:
            r.raise_for_status()
            size = int(r.headers.get("content-length", "0"))
            if size > MAX_SIZE:
                return jsonify({"error": "File exceeds 100 MB limit"}), 413
            f = tempfile.NamedTemporaryFile(delete=False, suffix=ext)
            total = 0
            for chunk in r.iter_content(64 * 1024):
                if not chunk:
                    continue
                total += len(chunk)
                if total > MAX_SIZE:
                    f.close()
                    os.unlink(f.name)
                    return jsonify({"error": "File exceeds 100 MB limit"}), 413
                f.write(chunk)
            f.close()
            return send_file(f.name, as_attachment=True, download_name="SAGE" + ext)
    except Exception:
        return jsonify({"error": "Download failed; check the URL and permissions."}), 400

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=10000)
