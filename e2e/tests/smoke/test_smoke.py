from helpers.assertions import assert_success


def test_health_check(client):
    resp = client.health()
    assert resp.status_code == 200
    assert resp.json()["status"] == "UP"


def test_list_categories(client):
    resp = client.list_categories()
    body = assert_success(resp)
    assert "data" in body


def test_list_models(client):
    resp = client.list_models()
    body = assert_success(resp)
    assert "data" in body


def test_list_tags(client):
    resp = client.list_tags()
    body = assert_success(resp)
    assert "data" in body
