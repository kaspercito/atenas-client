// Worker de Atenas Client: guarda y entrega los cosmeticos elegidos por cada jugador.
const NAME_RE = /^[a-z0-9_]{1,16}$/;
const ID_RE = /^[a-z0-9_]{1,24}$/;
const TTL = 60 * 60 * 24 * 30; // 30 dias; se renueva cada vez que el jugador entra

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const json = (obj, status = 200) =>
      new Response(JSON.stringify(obj), { status, headers: { "content-type": "application/json" } });

    if (request.method === "POST" && url.pathname === "/set") {
      let b;
      try { b = await request.json(); } catch (e) { return json({ error: "json" }, 400); }
      const name = String(b.name || "").toLowerCase();
      const token = String(b.token || "");
      if (!NAME_RE.test(name) || token.length < 16 || token.length > 64) return json({ error: "datos" }, 400);

      const key = "u:" + name;
      const cur = await env.COSMETICS.get(key, "json");
      if (cur && cur.token !== token) return json({ error: "nombre en uso" }, 403);

      const pick = (v) => (typeof v === "string" && ID_RE.test(v) && v !== "none" ? v : null);
      await env.COSMETICS.put(
        key,
        JSON.stringify({ token, cape: pick(b.cape), wings: pick(b.wings), hat: pick(b.hat) }),
        { expirationTtl: TTL }
      );
      return json({ ok: true });
    }

    if (request.method === "GET" && url.pathname === "/get") {
      const names = (url.searchParams.get("names") || "")
        .toLowerCase().split(",").filter((n) => NAME_RE.test(n)).slice(0, 40);
      const out = {};
      await Promise.all(names.map(async (n) => {
        const v = await env.COSMETICS.get("u:" + n, "json");
        if (v) out[n] = { cape: v.cape, wings: v.wings, hat: v.hat };
      }));
      return json(out);
    }

    return json({ name: "atenas-cosmetics", ok: true });
  },
};
