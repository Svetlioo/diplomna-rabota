import { useState } from "react";
import { api } from "./api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

type Account = { iban: string; balance: number; currency: string };
type Transaction = { id: string; toIban: string; amount: number; currency: string };

export default function App() {
  const [token, setToken] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [account, setAccount] = useState<Account>();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [amount, setAmount] = useState("");
  const [toIban, setToIban] = useState("");
  const [txAmount, setTxAmount] = useState("");
  const [error, setError] = useState("");

  async function run(fn: () => Promise<void>) {
    setError("");
    try {
      await fn();
    } catch (e) {
      setError(String(e));
    }
  }

  async function refresh(t: string) {
    setAccount(await api("/accounts", t));
    setTransactions((await api("/transactions", t)).content ?? []);
  }

  const auth = (path: string) =>
    run(async () => {
      const { token: t } = await api(path, undefined, { email, password });
      setToken(t);
      await refresh(t);
    });

  const move = (path: string) =>
    run(async () => {
      await api(path, token, { amount: Number(amount) });
      setAmount("");
      await refresh(token);
    });

  const transfer = () =>
    run(async () => {
      await api("/transactions", token, { toIban, amount: Number(txAmount) });
      setToIban("");
      setTxAmount("");
      await refresh(token);
    });

  if (!token) {
    return (
      <main className="mx-auto flex min-h-svh max-w-sm flex-col justify-center gap-4 p-4">
        <Card>
          <CardHeader>
            <CardTitle>Bank</CardTitle>
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
            <div className="flex gap-2">
              <Button className="flex-1" onClick={() => auth("/auth/login")}>Login</Button>
              <Button className="flex-1" variant="outline" onClick={() => auth("/auth/register")}>Register</Button>
            </div>
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
          <CardTitle>{account?.iban}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <p className="text-3xl font-semibold">
            {account?.balance} {account?.currency}
          </p>
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
          <CardTitle>Transactions</CardTitle>
        </CardHeader>
        <CardContent>
          <ul className="text-sm">
            {transactions.map((t) => (
              <li key={t.id} className="flex justify-between border-b py-1">
                <span>{t.toIban}</span>
                <span>{t.amount} {t.currency}</span>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </main>
  );
}
