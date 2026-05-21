# Secure Software Supply Chain for Containerized Distributed Applications

A DevSecOps reference implementation that secures the software supply chain of a
containerized, distributed banking application running on Kubernetes — built as a
bachelor's thesis at the Technical University of Sofia (Cybersecurity).

The focus of this repository is **not** the business logic of the services, but the
**secure delivery pipeline** around them: automated security scanning, artifact
signing, SBOM generation, build provenance, and controlled GitOps deployment —
validated against **SLSA Level 2** and industry regulations (DORA, NIS2, EU CRA, PCI DSS).

---

## What this demonstrates

| Supply-chain risk | Control |
|---|---|
| Hardcoded secret in a commit | Gitleaks (CI) + pre-commit hook + GitHub Push Protection |
| Insecure code (e.g. SQL injection) | Semgrep SAST |
| Vulnerable dependency | Trivy SCA |
| Vulnerable base image | Trivy image scan |
| Unsigned / tampered image | Cosign keyless signing + Kyverno admission verification |
| Untracked build origin | Syft SBOM + SLSA provenance attestation |
| Uncontrolled deployment | GitOps (ArgoCD) — automated to dev, PR-gated to test/prod |

---

## Repositories

The system is split across three repositories:

| Repository | Responsibility |
|---|---|
| **`diplomna-rabota`** (this) | Application source, Dockerfiles, GitHub Actions CI/CD |
| **`diplomna-rabota-infra`** | Terraform — Azure foundation + AKS cluster |
| **`diplomna-rabota-gitops`** | Desired cluster state (Helm charts, per-env values, ArgoCD apps, Kyverno policies) |

---

## Services

Two independently built and deployed services (business logic intentionally out of scope here):

- **bank-service** — Spring Boot / Java 25, one PostgreSQL database
- **fraud-detection** — Python / FastAPI, stateless

Both are containerized from pinned official base images, run as non-root with a
read-only root filesystem, and are deployed via custom Helm charts.

---

## CI/CD pipeline

Each service has its own workflow (`bank-service-ci.yml`, `fraud-detection-ci.yml`)
with an identical structure but a different build stack (Maven vs pip). A separate
`repo-security.yml` runs repository-wide scanners.

```
 PR / push to main
        │
        ▼
   ┌─────────┐
   │ changes │  detect which service / workflow changed (paths-filter)
   └────┬────┘
        ▼
  ┌────────────┐
  │ build-test │   compile + tests
  └─────┬──────┘
        ▼
  ┌───────────────────────────────────────────────────────────┐
  │ image                                                       │
  │  build → Trivy image scan → push (ghcr.io)                  │
  │  → Cosign sign (keyless) → Syft SBOM → Cosign attest        │
  └───────────────┬───────────────────────────┬───────────────┘
                  ▼                           ▼
        ┌──────────────────┐        ┌────────────────────┐
        │ deploy-dev (CD)  │        │ provenance (SLSA)  │
        │ bump dev image   │        │ build attestation  │
        │ via gitops PR    │        └────────────────────┘
        └──────────────────┘
```

| Job | What it does |
|---|---|
| `changes` | Detects whether the service or its workflow changed |
| `build-test` | Compiles and runs tests (Maven `verify` / `pip install` + smoke) |
| `image` | Build → Trivy image scan → push to GHCR → Cosign keyless sign → Syft CycloneDX SBOM → Cosign attest |
| `deploy-dev` | Continuous delivery to dev (see below) |
| `provenance` | SLSA build provenance via `slsa-github-generator` (SLSA Level 2 on GitHub-hosted runners) |

**Least privilege:** the workflow grants only `contents: read` globally; each job
elevates only the permissions it needs (`packages` / `id-token` / `security-events`
on the image and provenance jobs alone).

---

## Security scanning & diff-aware blocking

| Tool | Scope | Where |
|---|---|---|
| **Gitleaks** | Secrets across full git history | repo-wide, every change |
| **Semgrep** | SAST — `p/java`, `p/python`, `p/security-audit`, `p/owasp-top-ten`, `p/cwe-top-25` | repo-wide, every change |
| **Trivy (SCA)** | Dependency / IaC vulnerabilities | repo-wide, every change |
| **Trivy (image)** | OS packages + layers of the built image | per-service, on image build |

The repo-wide scanners (`repo-security.yml`) run on **every** pull request, so the
diff-aware gate always has results to compare — service-specific jobs are skipped when
their service is untouched without blocking the merge.

### How the merge decision is made

Most scanners do **not** fail their own job — they run with `exit-code: 0` and only
upload a **SARIF** report to GitHub Code Scanning. The merge decision is taken centrally
by a branch **ruleset** ("Require code scanning results", threshold *High or higher*),
not by each tool's exit code. This is what makes blocking **diff-aware**.

**How diff-aware works:** every finding is given a stable fingerprint (rule + file +
code context). On a pull request, the findings are matched against the baseline already
recorded on `main`:

- A finding that **matches the baseline** = pre-existing → shown in the Security tab,
  but it does **not** fail the check.
- A finding with **no match** = introduced by this PR → it **fails** the check at or
  above the configured severity → merge is blocked.

### Per-scan outcome

| Scan | Mechanism | New problem in this PR | No problem | Pre-existing (not from this PR) |
|---|---|---|---|---|
| **build-test** | required job | BLOCKS — compile/test failure | passes | n/a |
| **Gitleaks** (secrets) | required job + Push Protection | BLOCKS — push rejected / job red | passes | not re-flagged (already in history — **rotate it**) |
| **Semgrep** (SAST) | Code Scanning, diff-aware | BLOCKS — new High+ alert | passes | visible in Security tab, does **not** block |
| **Trivy SCA** (deps/IaC) | Code Scanning, diff-aware | BLOCKS — new High/Critical (with a fix) | passes | visible, does **not** block |
| **Trivy image** | Code Scanning, diff-aware | BLOCKS — new image CVE | passes | visible, does **not** block |
| **Cosign + Kyverno** | cluster admission (not a PR gate) | unsigned/tampered image rejected at deploy | pod admitted | n/a |

So a PR that introduces nothing vulnerable merges cleanly; a PR that adds a new High+
finding is blocked; and a PR that merely *touches* code near an old, pre-existing finding
is **not** punished for it — the old finding stays visible but never blocks.

> `Gitleaks` and `Cosign + Kyverno` are the two non-SARIF gates: Gitleaks fails its job
> directly (and Push Protection rejects the `git push` itself), while Cosign/Kyverno act
> at Kubernetes admission, not at merge time.

Secret leakage is defended in depth: a local **gitleaks pre-commit hook**
(`.pre-commit-config.yaml`) for prevention, GitHub **Push Protection** at the server,
and the CI gitleaks job as a merge gate. A secret that ever reaches history must be
**rotated** — it stays in git forever.

---

## Supply chain security

- **Signing** — Cosign keyless signing (Sigstore, OIDC → Fulcio → Rekor); images are
  signed by digest, not tag.
- **SBOM** — Syft generates a CycloneDX SBOM, attached to the image as a Cosign
  attestation and uploaded as a workflow artifact for visibility.
- **Provenance** — `slsa-github-generator` produces SLSA build provenance (Level 2).
- **Admission control** — Kyverno verifies the Cosign signature (keyless attestor:
  workflow identity + issuer + Rekor) before any image is admitted to the cluster, and
  pins images to their digest. Unsigned or tampered images are rejected.

---

## Deployment (GitOps)

The cluster runs on Azure Kubernetes Service with `dev` / `test` / `prod` namespaces,
reconciled by **ArgoCD** from the gitops repository.

```
 image built & signed (CI)
        │
        ▼
  dev   ── automatic ──▶ CI opens & auto-merges a gitops PR bumping the dev image
        │                (full audit trail, zero manual steps) → ArgoCD syncs dev
        ▼
  test  ── PR-gated ───▶ "Promote" workflow opens a PR; a human reviews and merges
        ▼
  prod  ── PR-gated ───▶ same, separation of duties (change management)
```

Each environment pins both the image **tag** (readability) and **digest**
(immutability). ArgoCD reconciles the desired state from git.

---

## Repository structure

```
.
├── apps/
│   ├── bank-service/        # Spring Boot / Java 25
│   └── fraud-detection/     # Python / FastAPI
├── .github/workflows/
│   ├── bank-service-ci.yml
│   ├── fraud-detection-ci.yml
│   └── repo-security.yml
├── .pre-commit-config.yaml  # gitleaks pre-commit hook
└── docker-compose.yml       # local development
```

---

## Standards & frameworks

SLSA v1.1 (target Level 2) · NIST SSDF · OWASP Top 10 for Kubernetes · CIS Kubernetes
Benchmark · STRIDE threat modeling · DORA · NIS2 · EU Cyber Resilience Act · PCI DSS v4.0

---

## License

[Apache License 2.0](LICENSE)
