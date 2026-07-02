def assert_success(resp):
    assert resp.status_code == 200, f"expected 200, got {resp.status_code}: {resp.text}"
    body = resp.json()
    assert body["code"] == 0, f"expected code 0, got {body.get('code')}: {body.get('message')}"
    return body


def assert_error(resp, http_status, error_code=None):
    assert resp.status_code == http_status, f"expected {http_status}, got {resp.status_code}: {resp.text}"
    body = resp.json()
    if error_code is not None:
        expected = int(error_code)
        assert body["code"] == expected, f"expected code {expected}, got {body.get('code')}: {body.get('message')}"
    return body
