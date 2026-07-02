import psycopg2
import psycopg2.extras


class PgHelper:
    def __init__(self, host="localhost", port=5432, dbname="modellite",
                 user="modellite", password="changeme"):
        self.conn_info = dict(host=host, port=str(port), dbname=dbname,
                              user=user, password=password)

    def _connect(self):
        return psycopg2.connect(**self.conn_info)

    def fetch_task(self, task_id):
        with self._connect() as conn, conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute("SELECT * FROM upload_task WHERE id = %s", (str(task_id),))
            return cur.fetchone()

    def fetch_version(self, version_id):
        with self._connect() as conn, conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute("SELECT * FROM model_version WHERE id = %s", (str(version_id),))
            return cur.fetchone()

    def cleanup_by_name_prefix(self, name_prefix="e2e-"):
        with self._connect() as conn, conn.cursor() as cur:
            cur.execute("SELECT id FROM model WHERE name LIKE %s", (f"{name_prefix}%",))
            model_ids = [r[0] for r in cur.fetchall()]
            if model_ids:
                cur.execute("DELETE FROM upload_task WHERE model_id = ANY(%s)", (model_ids,))
                cur.execute("DELETE FROM model_version WHERE model_id = ANY(%s)", (model_ids,))
                cur.execute("DELETE FROM model WHERE id = ANY(%s)", (model_ids,))
            cur.execute("DELETE FROM category WHERE name LIKE %s", (f"{name_prefix}%",))
            conn.commit()
