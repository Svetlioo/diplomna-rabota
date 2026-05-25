const BASE = import.meta.env.VITE_API_URL ?? "/api";

export async function api(path: string, token?: string, body?: unknown) {
  const res = await fetch(BASE + path, {
    method: body ? "POST" : "GET",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) throw new Error((await res.text()) || res.statusText);
  return res.json();
}
