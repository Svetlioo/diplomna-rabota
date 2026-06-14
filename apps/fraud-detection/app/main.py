"""Rule-based fraud screening service for the diploma banking demo."""

import os

from fastapi import FastAPI
from pydantic import BaseModel

# Transfers at or above this amount are flagged. Rule-based: no model, no state, no DB.
AMOUNT_THRESHOLD = float(os.getenv("FRAUD_AMOUNT_THRESHOLD", "10000"))

app = FastAPI(title="fraud-detection", version="0.0.3")


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
