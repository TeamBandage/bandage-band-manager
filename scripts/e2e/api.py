"""Bandage 개발 서버 E2E용 최소 API 클라이언트.

ponytail: requests 대신 stdlib urllib. 의존성 0개로 어디서든 바로 실행된다.
모든 호출은 CALL_LOG에 남아 장애 지점 보고서의 원천 데이터가 된다.
"""

import json
import os
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request

BASE = os.environ.get("BANDAGE_BASE", "https://bandage.team/api/v1")
CALL_LOG = []
_CTX = ssl.create_default_context()


class ApiError(Exception):
    def __init__(self, status, body, method, path):
        self.status, self.body = status, body
        super().__init__(f"{method} {path} -> {status}: {str(body)[:300]}")


def call(method, path, token=None, body=None, params=None, raw=False, expect=None):
    """API 호출. expect(=상태코드 리스트)를 주면 예외 대신 (status, body)를 반환한다."""
    url = BASE + path
    if params:
        url += "?" + urllib.parse.urlencode({k: v for k, v in params.items() if v is not None})
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    if data:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)

    started = time.time()
    try:
        with urllib.request.urlopen(req, timeout=60, context=_CTX) as r:
            status, text = r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        status, text = e.code, e.read().decode()
    except Exception as e:  # 네트워크 계층 실패도 기록 대상
        CALL_LOG.append(
            {"method": method, "path": path, "status": 0, "ms": int((time.time() - started) * 1000), "error": str(e)}
        )
        raise

    try:
        parsed = json.loads(text) if text else None
    except json.JSONDecodeError:
        parsed = text

    CALL_LOG.append(
        {
            "method": method,
            "path": path,
            "status": status,
            "ms": int((time.time() - started) * 1000),
            "ok": 200 <= status < 300,
        }
    )

    if expect is not None:
        return status, parsed
    if not (200 <= status < 300):
        raise ApiError(status, parsed, method, path)
    if raw:
        return parsed
    return parsed.get("data") if isinstance(parsed, dict) else parsed


def join(email, name, password="12345678"):
    """회원가입. 이미 있으면 조용히 통과(재실행 가능성 확보)."""
    st, body = call("POST", "/members/join", body={"email": email, "name": name, "password": password}, expect=[200, 400, 409])
    return body


def login(email, password="12345678"):
    return call("POST", "/auth/login", body={"email": email, "password": password})["accessToken"]


def upload_image(presign, file_path):
    """presigned URL로 실제 PUT 업로드. 성공 시 objectKey 반환."""
    blob = open(file_path, "rb").read()
    req = urllib.request.Request(presign["uploadUrl"], data=blob, method="PUT")
    req.add_header("Content-Type", presign["contentType"])
    with urllib.request.urlopen(req, timeout=120, context=_CTX) as r:
        if r.status not in (200, 204):
            raise ApiError(r.status, "S3 upload failed", "PUT", "s3")
    return presign["objectKey"]


def pick_id(obj, *names):
    """응답 DTO마다 id 키 이름이 다르다(selectionId/itemId/setlistId...). 순서대로 찾는다."""
    if not isinstance(obj, dict):
        return None
    for n in names:
        if obj.get(n):
            return obj[n]
    for k, v in obj.items():  # 폴백: ...Id 로 끝나는 첫 키
        if k.lower().endswith("id") and isinstance(v, str) and len(v) > 8:
            return v
    return None


def summary():
    total = len(CALL_LOG)
    failed = [c for c in CALL_LOG if not c.get("ok")]
    return {"total": total, "failed": len(failed), "failures": failed}
