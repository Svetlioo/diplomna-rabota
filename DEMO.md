# Команди за защитата

Бърз справочник с командите, които пускам по време на защитата. Всичко е под
ръка — копи/пейст в терминала.

## 0. Преди защитата — да съм сигурен, че клъстерът е up

```bash
# Стартирай AKS (ако е спрян за credits)
infrastructure/scripts/aks-start.sh

# Локални port-forward-и (frontend на 8080, ArgoCD UI на 8081)
scripts/port-forward.sh
```

---

## 1. Атаки — PR-и в `diplomna-rabota`

Всеки бранч е отворен PR срещу `main`; CI се пуска и блокира merge.

| # | Атака | Бранч / PR | Какво хваща |
|---|---|---|---|
| 1 | Hardcoded secret (commit) | `attack/hardcoded-secret` | Gitleaks |
| 2 | SQL injection (Java) | `attack/sql-injection` | Semgrep SAST |
| 3 | Уязвима Java зависимост (Log4Shell) | `attack/vuln-dependency` | Trivy SCA |
| 4 | Уязвима npm зависимост (lodash 4.17.4) | `attack/frontend-vuln-dependency` | Trivy SCA |

Отваряш PR-а → червени checks → отваряш Security tab → показваш находката.

## 2. Локален gitleaks pre-commit (преди кодът да напусне машината)

```bash
git checkout -b attack/gitleaks-precommit
printf 'AWS_ACCESS_KEY_ID=AKIA<16 random caps/digits>\nAWS_SECRET_ACCESS_KEY=<40 random chars>\n' > leaked.env
git add leaked.env
git commit -m "test: leak"        # pre-commit hook блокира тук
# чистене:
git restore --staged leaked.env && rm leaked.env
git checkout main && git branch -D attack/gitleaks-precommit
```

> Бележка: gitleaks има allowlist за официалните AWS docs example стойности
> (`AKIAIOSFODNN7EXAMPLE` и т.н.) — затова за демото ползваме random pattern.

---

## 3. Kyverno полиси (admission в клъстера)

```bash
# Кои полиси действат
kubectl get clusterpolicies
kubectl get clusterpolicy verify-image-signatures -o yaml
kubectl get clusterpolicy restrict-image-registries -o yaml

# Kyverno контролерите
kubectl -n kyverno get pods

# Audit резултати (какво е admit-нал/отказал)
kubectl get clusterpolicyreports
kubectl get policyreports -A

# Live негативен тест 1 — чужд registry се отказва
kubectl -n dev run rogue-registry --image=docker.io/nginx:latest
# → Error from server: admission webhook "validate.kyverno.svc-fail" denied:
#   restrict-image-registries: image must be from ghcr.io/svetlioo/*

# Live негативен тест 2 — образ от НАШЕТО registry, но НЕподписан
# (push-ваме alpine под наш репо name, БЕЗ cosign sign)
echo $GITHUB_PAT | docker login ghcr.io -u svetlioo --password-stdin
docker pull alpine:3.22
docker tag alpine:3.22 ghcr.io/svetlioo/unsigned-test:demo
docker push ghcr.io/svetlioo/unsigned-test:demo
kubectl -n dev run rogue-unsigned --image=ghcr.io/svetlioo/unsigned-test:demo
# → Error from server: admission webhook "validate.kyverno.svc-fail" denied:
#   verify-image-signatures: .attestors[0]: no signatures found

# Положителен контрол — реален подписан образ минава
IMG=$(yq '.image.repository + ":" + .image.tag + "@" + .image.digest' \
      ../diplomna-rabota-gitops/environments/dev/values-bank.yaml)
kubectl -n dev run bank-canary --image="$IMG"
# → pod/bank-canary created

# Почистване
kubectl -n dev delete pod rogue-registry rogue-unsigned bank-canary --ignore-not-found
```

## 4. Подове и какво има в клъстера

```bash
# Контекст и възли
kubectl config current-context
kubectl get nodes -o wide

# Бегъл преглед на всичко наше
kubectl get pods,svc,ingress -A | grep -E 'dev|test|prod|argocd|kyverno'

# Per-environment
kubectl -n dev   get pods,svc
kubectl -n test  get pods,svc
kubectl -n prod  get pods,svc

# Кой образ върви всеки под (доказва pin-нат digest)
kubectl -n dev get pods \
  -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[*].image}{"\n"}{end}'

# ArgoCD приложения
kubectl -n argocd get applications -o wide

# Helm релийзи
helm list -A
```

## 5. Supply chain доказателства за конкретен образ

```bash
IMAGE=ghcr.io/svetlioo/bank-service:0.1.5

# Дървото — подпис + атестации
cosign tree $IMAGE

# Подпис (Sigstore keyless, Fulcio + Rekor)
cosign verify $IMAGE \
  --certificate-identity-regexp '^https://github\.com/Svetlioo/diplomna-rabota/' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com

# SLSA provenance
cosign verify-attestation $IMAGE \
  --type slsaprovenance \
  --certificate-identity-regexp '^https://github\.com/slsa-framework/slsa-github-generator/' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com

# SBOM (CycloneDX)
cosign download attestation $IMAGE \
  | jq -r '.payload | @base64d | fromjson | .predicate.Data' \
  | jq '.components | length'
```

## 6. След защитата — спри клъстера

```bash
infrastructure/scripts/aks-stop.sh
```
