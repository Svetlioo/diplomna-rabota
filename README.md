# Защита на софтуерната верига за доставка

DevSecOps реализация, която защитава софтуерната верига за доставка на
контейнеризирано банково приложение в Kubernetes — бакалавърска дипломна работа
(ТУ София, Киберсигурност).

Фокусът на това репозитори е **сигурната доставка** на сервизите (сканове,
подписване, SBOM, provenance, контролиран GitOps деплой), а не бизнес логиката им.
Целта е **SLSA Level 2** + индустриални регулации (DORA, NIS2, EU CRA, PCI DSS).

---

## Репозитории

| Репо | Отговорност |
|---|---|
| **`diplomna-rabota`** (това) | Сорс на сервизите, Dockerfile-и, GitHub Actions CI/CD |
| **`diplomna-rabota-infra`** | Terraform — Azure foundation + AKS клъстер |
| **`diplomna-rabota-gitops`** | Desired state на клъстера (Helm charts, per-env values, ArgoCD apps, Kyverno) |

## Сервизи

- **bank-service** — Spring Boot / Java 25, една PostgreSQL база; JWT auth (httpOnly cookie за браузъра + bearer header за API клиенти)
- **fraud-detection** — Python / FastAPI, без състояние
- **frontend** — React 19 / Vite / TypeScript / Tailwind v4 / shadcn; сервира се от nginx (non-root), който reverse-proxy-ва `/api` към bank-service

И трите се build-ват от пинати официални базови образи, въртят като non-root с
read-only root filesystem, и се деплойват през собствени Helm charts.

---

## CI/CD поток

Три service workflow-а (`bank-service-ci.yml`, `fraud-detection-ci.yml`,
`frontend-ci.yml`) с еднаква структура, различен build stack (Maven / pip / Node).
Отделен `repo-security.yml` пуска repo-wide скенерите.

### При Pull Request към `main`
- `changes` (paths-filter) — открива кои сервизи са пипнати.
- За **всеки променен сервиз**: `build-test` (компилация + тестове) и `image`
  (build за валидация, че Dockerfile-ът работи). Образът **не се публикува** на PR.
- `repo-security` (**винаги**, върху целия repo): Gitleaks, Semgrep, Trivy → качват
  SARIF в GitHub Code Scanning.
- Скановете решават дали merge е разрешен (виж по-долу).

### При push към `main` (след merge)
- За променения сервиз: `image` build-ва, **push-ва в ghcr.io**, **подписва** с Cosign,
  прави **SBOM** (Syft) и **SLSA provenance**.
- `deploy-dev` обновява dev средата в gitops (виж „Деплой").

---

## Сигурностни сканове — кога блокира, кога пуска

| Скенер | Какво хваща |
|---|---|
| **Gitleaks** | Тайни (hardcoded secrets) |
| **Semgrep** | Опасен код (SAST) — напр. SQL injection |
| **Trivy** | Уязвими зависимости (SCA) + Dockerfile / IaC проблеми |

И трите се пускат върху **целия repo на всеки PR** и качват резултати в Code Scanning.
Блокирането е **diff-aware**: преценява се спрямо вече записаното на `main`.

### Кога НЕ ти дава да мърджнеш (блокира)
- **Нов** secret в PR-а → Gitleaks (+ Push Protection отказва самия `git push`).
- **Нов** опасен код (SQLi) → Semgrep.
- **Нова** уязвима зависимост (High/Critical с налична поправка) → Trivy.
- Провален build или тест → `build-test`.

### Кога ти дава да мърджнеш
- Няма нов проблем в PR-а.
- Има проблем, но е **стар** (вече на `main`) — показва се в Security tab, но **не блокира**.
  Тоест докосваш код близо до стара уязвимост → не те наказва за нея.

### Тайните се хващат на три нива (defense in depth)
- **Локално, преди commit** — `pre-commit` + gitleaks v8.30.1 (`.pre-commit-config.yaml`).
  Активиране веднъж: `pre-commit install`. Спира тайната, преди да напусне машината.
- **При push** — GitHub Push Protection отказва самия `git push`.
- **В CI** — Gitleaks job-ът блокира merge **и** записва находката в Security tab.

### Доказани сценарии (тествани)
| Промяна в PR | Резултат |
|---|---|
| `log4j-core 2.14.1` (Log4Shell) | Trivy → 2 Critical + 1 High → **червено, блокиран** |
| Конкатениран SQL в `executeQuery` | Semgrep → **червено, блокиран** |
| Hardcoded AWS ключ | Gitleaks → **червено, блокиран** |
| Чист код (PreparedStatement, без тайна, без уязвима зависимост) | **зелено, мърджва се** |

---

## Supply chain сигурност

- **Подписване** — Cosign keyless (Sigstore, OIDC → Fulcio → Rekor); подпис **по digest**.
- **SBOM** — Syft CycloneDX, закачен към образа като Cosign attestation + качен като artifact.
- **Provenance** — `slsa-github-generator` (SLSA Level 2).
- **Admission** — Kyverno налага две политики на образите в `dev/test/prod`:
  - `verify-image-signatures` — валиден Cosign подпис (workflow самоличност + issuer + Rekor)
    **плюс** SLSA provenance атестация (подписана от `slsa-github-generator`) **плюс**
    CycloneDX SBOM атестация (подписана от CI). Липсва ли подпис или атестация → отказан.
  - `restrict-image-registries` — допуска **само** образи от `ghcr.io/svetlioo/*`; всеки образ
    от чуждо registry (напр. `docker.io/...`) се отказва. (verify-images проверява само
    съвпадащите образи, затова рестрикцията на registry е **отделен** контрол.)

---

## Деплой (GitOps)

Клъстерът (AKS) има среди `dev` / `test` / `prod`, реконсилирани от **ArgoCD** от
gitops репозиторито.

- **Деплойва се само променения сервиз.** Промениш bank → само bank се деплойва.
  Промениш и двата сервиза → **два отделни PR-а** (по един за всеки).
- Deploy-ите са **сериализирани** (обща `concurrency` група) — един по един, всеки
  чете най-новия `main`, така че няма конфликти.
- **dev = автоматично** — CI отваря и **авто-merge-ва** PR в gitops с новия образ.
- **test / prod = ръчно** — Promote workflow в gitops отваря PR, който **човек одобрява**
  (separation of duties). prod винаги взима test-валидирания образ.

Всяка среда пинва едновременно **tag** (четимост) и **digest** (неизменност).

---

## Настройка на репото (еднократно)

- **Branch ruleset** на `main`: изисквай PR, изисквай status checks, изисквай
  „code scanning results" (Trivy + Semgrep, праг *High or higher*), блокирай force push.
- **Secret scanning + Push protection** (безплатно за публични repos).
- Secret **`GITOPS_TOKEN`** (fine-grained PAT, Contents + Pull requests = write върху gitops)
  в **двете** репота: в `diplomna-rabota` за dev авто-деплоя, и в `diplomna-rabota-gitops`
  за Promote workflow-а — PR, отворен с `GITHUB_TOKEN`, **не** тригерира checks, затова
  промоцията ползва PAT, за да минават required checks.
- В gitops репото: разреши на GitHub Actions да създава PR-и (за Promote workflow-а).

---

## Лиценз

[Apache License 2.0](LICENSE)
