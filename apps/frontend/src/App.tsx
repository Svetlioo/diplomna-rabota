import { useEffect, useState } from "react";
import { api } from "./api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

type Account = { iban: string; balance: number; currency: string };
type Transaction = { id: string; toIban: string; amount: number; currency: string };

export default function App() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [account, setAccount] = useState<Account>();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [amount, setAmount] = useState("");
  const [toIban, setToIban] = useState("");
  const [txAmount, setTxAmount] = useState("");
  const [error, setError] = useState("");
  const [ready, setReady] = useState(false);

  async function refresh() {
    setAccount(await api("/accounts"));
    setTransactions((await api("/transactions")).content ?? []);
  }

  // On load, the httpOnly cookie (if present) authenticates us, so a browser
  // refresh restores the session instead of bouncing back to the login form.
  useEffect(() => {
    refresh()
      .catch(() => undefined)
      .finally(() => setReady(true));
  }, []);

  async function run(fn: () => Promise<void>) {
    setError("");
    try {
      await fn();
    } catch (e) {
      setError(String(e));
    }
  }

  const auth = (path: string) =>
    run(async () => {
      await api(path, { email, password });
      setPassword("");
      await refresh();
    });

  const move = (path: string) =>
    run(async () => {
      await api(path, { amount: Number(amount) });
      setAmount("");
      await refresh();
    });

  const transfer = () =>
    run(async () => {
      await api("/transactions", { toIban, amount: Number(txAmount) });
      setToIban("");
      setTxAmount("");
      await refresh();
    });

  const logout = () =>
    run(async () => {
      await api("/auth/logout", {});
      setAccount(undefined);
      setTransactions([]);
    });

  if (!ready) {
    return null;
  }

  if (!account) {
    const isLogin = mode === "login";
    return (
      <main className="mx-auto flex min-h-svh max-w-sm flex-col justify-center gap-4 p-4">
        <Card>
          <CardHeader>
            <CardTitle>{isLogin ? "Login" : "Register"}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div className="grid gap-2">
              <Label htmlFor="email">Email</Label>
              <Input id="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="password">Password</Label>
              <Input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
            </div>
            <Button onClick={() => auth(`/auth/${mode}`)}>{isLogin ? "Login" : "Register"}</Button>
            <Button
              variant="link"
              className="h-auto p-0 text-muted-foreground"
              onClick={() => {
                setMode(isLogin ? "register" : "login");
                setError("");
              }}
            >
              {isLogin ? "Need an account? Register" : "Have an account? Login"}
            </Button>
            {error && <p className="text-sm text-destructive">{error}</p>}
          </CardContent>
        </Card>
      </main>
    );
  }

  return (
    <main className="mx-auto flex min-h-svh max-w-md flex-col gap-4 p-4">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>{account.iban}</CardTitle>
            <Button variant="ghost" size="sm" onClick={logout}>Logout</Button>
          </div>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <p className="text-3xl font-semibold">{account.balance} {account.currency}</p>
          <div className="grid gap-2">
            <Label htmlFor="amount">Amount</Label>
            <Input id="amount" type="number" value={amount} onChange={(e) => setAmount(e.target.value)} />
            <div className="flex gap-2">
              <Button className="flex-1" onClick={() => move("/accounts/deposit")}>Deposit</Button>
              <Button className="flex-1" variant="outline" onClick={() => move("/accounts/withdraw")}>Withdraw</Button>
            </div>
          </div>
          <div className="grid gap-2">
            <Label htmlFor="iban">Transfer</Label>
            <Input id="iban" placeholder="recipient IBAN" value={toIban} onChange={(e) => setToIban(e.target.value)} />
            <Input type="number" placeholder="amount" value={txAmount} onChange={(e) => setTxAmount(e.target.value)} />
            <Button onClick={transfer}>Send</Button>
          </div>
          {error && <p className="text-sm text-destructive">{error}</p>}
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Transactions</CardTitle>
            <Button variant="outline" size="sm" onClick={() => run(refresh)}>Refresh</Button>
          </div>
        </CardHeader>
        <CardContent>
          <ul className="text-sm">
            {transactions.map((t) => {
              const incoming = t.toIban === account.iban;
              return (
                <li key={t.id} className="flex justify-between border-b py-1">
                  <span>{incoming ? "Received" : `Sent to ${t.toIban}`}</span>
                  <span className={incoming ? "text-green-600" : "text-destructive"}>
                    {incoming ? "+" : "-"}{t.amount} {t.currency}
                  </span>
                </li>
              );
            })}
          </ul>
        </CardContent>
      </Card>
    </main>
  );
}
