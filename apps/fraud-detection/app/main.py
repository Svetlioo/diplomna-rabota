"""Rule-based fraud screening for the diploma banking demo."""

import os

from fastapi import FastAPI
from pydantic import BaseModel

# A transfer at or above this amount is flagged as suspicious. Rule-based only —
# no model, no state, no database; the verdict is computed purely from the
# request the bank-service sends.
AMOUNT_THRESHOLD = float(os.getenv("FRAUD_AMOUNT_THRESHOLD", "10000"))

app = FastAPI(title="fraud-detection", version="0.0.1")


class EvaluateRequest(BaseModel):
    ownerId: str
    toIban: str
    amount: float


class Verdict(BaseModel):
    suspicious: bool
    reason: str | None = None


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/evaluate", response_model=Verdict)
def evaluate(request: EvaluateRequest) -> Verdict:
    if request.amount >= AMOUNT_THRESHOLD:
        return Verdict(suspicious=True, reason=f"amount {request.amount} >= threshold {AMOUNT_THRESHOLD}")
    return Verdict(suspicious=False)
