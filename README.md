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

- **bank-service** — Spring Boot / Java 25, една PostgreSQL база
- **fraud-detection** — Python / FastAPI, без състояние

И двата се build-ват от пинати официални базови образи, въртят като non-root с
read-only root filesystem, и се деплойват през собствени Helm charts.

---

## CI/CD поток

Два service workflow-а (`bank-service-ci.yml`, `fraud-detection-ci.yml`) с еднаква
структура, различен build stack (Maven / pip). Отделен `repo-security.yml` пуска
repo-wide скенерите.

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
- **Admission** — Kyverno проверява Cosign подписа (workflow самоличност + issuer + Rekor)
  преди да допусне образ в клъстера; неподписан/подменен образ се отказва.

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
- Secret **`GITOPS_TOKEN`** (fine-grained PAT) за dev авто-деплоя.
- В gitops репото: разреши на GitHub Actions да създава PR-и (за Promote workflow-а).

---

## Лиценз

[Apache License 2.0](LICENSE)
