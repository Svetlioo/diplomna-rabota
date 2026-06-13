# Защита на софтуерната верига за доставки

DevSecOps реализация, която защитава софтуерната верига за доставки на
контейнеризирано банково приложение в Kubernetes. Бакалавърска дипломна работа,
ТУ София, специалност Киберсигурност.

Това хранилище съдържа изходния код на услугите, техните Dockerfile файлове и
CI/CD процеса в GitHub Actions. Акцентът е върху сигурната доставка чрез
сканиране, подписване, SBOM, provenance и контролирано внедряване през GitOps.

## Трите хранилища

| Хранилище | Отговорност |
|---|---|
| `diplomna-rabota` (това) | Изходен код на услугите, Dockerfile файлове, GitHub Actions CI/CD |
| `diplomna-rabota-infra` | Terraform за Azure (споделена основа, AKS клъстер, база и контролери) |
| `diplomna-rabota-gitops` | Желано състояние на клъстера (Helm charts, values по среда, ArgoCD приложения и Kyverno политики) |

## Структура

```
.
├── apps/
│   ├── bank-service/      Spring Boot (Java 25); акаунти, преводи, JWT
│   ├── fraud-detection/   FastAPI (Python); проверка на преводи по правила
│   └── frontend/          React, Vite, TypeScript; раздаван от nginx
├── .github/workflows/     CI за всяка услуга и repo-security (скенери)
├── docker-compose.yml     Локална база и fraud-detection
└── .env.example           Примерни променливи за локалното пускане
```

## Услуги

- `bank-service` (Spring Boot, Java 25, PostgreSQL). Управлява акаунти, баланси и
  преводи между тях; защитен с JWT автентикация.
- `fraud-detection` (Python, FastAPI). Без състояние и без база. Преди да се
  изпълни превод, го проверява спрямо набор от правила и отбелязва съмнителните.
- `frontend` (React 19, Vite, TypeScript). Статичен build, който nginx раздава в
  контейнера; заявките към `/api` се пренасочват към bank-service.

Трите образа се изграждат от официални базови образи, заключени на конкретна
версия, и се внедряват през собствени Helm charts.

## Локално пускане

Нужни са Docker, JDK 25 и Node 22.

1. Копиране на примерните променливи:
   ```bash
   cp .env.example .env
   ```
   Задължителни променливи в `.env`:
   - `BANK_DB_USER`, `BANK_DB_PASSWORD` — потребител и парола за PostgreSQL
     контейнера (docker compose ги подава на базата).
   - `DB_USERNAME`, `DB_PASSWORD` — същите стойности, с които bank-service се
     свързва към базата.
   - `JWT_SECRET` — стойност от `openssl rand -hex 32`.

   Останалите променливи (`BANK_DB_NAME`, `BANK_DB_PORT`, `DB_URL`, `FRAUD_URL`,
   `FRAUD_AMOUNT_THRESHOLD`) са с готови стойности по подразбиране.

2. Стартиране на базата и fraud-detection:
   ```bash
   docker compose up -d
   ```
   PostgreSQL тръгва на 5432, fraud-detection на 8000 (изгражда се от
   `apps/fraud-detection`).

3. Стартиране на bank-service (чете `.env` автоматично, Flyway създава схемата):
   ```bash
   cd apps/bank-service && ./mvnw spring-boot:run
   ```
   Слуша на 8080.

4. Стартиране на frontend:
   ```bash
   cd apps/frontend && npm install && npm run dev
   ```
   Достъпен на `http://localhost:5173`. Vite dev сървърът пренасочва заявките от
   `/api` към bank-service на порт 8080.

## CI/CD процес

Всяка услуга има собствен CI workflow с еднаква структура и различен build
инструмент (Maven, pip, Node). Отделен `repo-security.yml` пуска скенерите върху
цялото хранилище.

При pull request към `main` се изпълняват само проверките. Услугата се компилира
и тества, образът се изгражда за валидация без публикуване и се пускат скенерите.
Неуспешна проверка блокира сливането.

При сливане в `main` образът на засегнатата услуга се изгражда, публикува в
`ghcr.io`, подписва се с Cosign (keyless), получава CycloneDX SBOM (Syft) и SLSA
provenance (slsa-github-generator), а `deploy-dev` обновява dev средата в gitops
хранилището.

## Сигурностни скенери

| Скенер | Какво спира |
|---|---|
| Gitleaks | Тайни, влезли в кода или в историята |
| Semgrep | Уязвими шаблони в кода (SAST), например SQL инжекция |
| Trivy | Уязвими зависимости (SCA) |

Скенерите качват резултатите си в Code Scanning (SARIF). Блокира се нов проблем,
въведен в самия PR; стар проблем, който вече е на `main`, се вижда в Security
tab, но не блокира несвързани PR-и.

Тайните се хващат на две нива. Локално преди commit с `pre-commit install`
(еднократно след клониране) и в CI, където Gitleaks блокира сливането.

## Supply chain сигурност

Подписването е с Cosign keyless (Sigstore); подписът е по digest и се записва в
Rekor. SBOM се генерира със Syft в CycloneDX формат и се прикача към образа като
подписана attestation. Provenance идва от slsa-github-generator, който издава
подписано удостоверение от кое хранилище, кой commit и кой workflow е изграден
образът. При допускане Kyverno налага две политики в `dev`, `test` и `prod`.
`verify-image-signatures` изисква валиден Cosign подпис, SLSA provenance и
CycloneDX SBOM attestation, а `restrict-image-registries` допуска само образи от
`ghcr.io/svetlioo/*`.

## Внедряване (GitOps)

Средите `dev`, `test` и `prod` се синхронизират от ArgoCD спрямо gitops
хранилището. `dev` се обновява автоматично от CI чрез pull request с новия таг и
digest. Придвижването към `test` и `prod` е ръчно през Promote workflow в gitops
хранилището, който копира таг и digest между средите и отваря pull request за
одобрение. Придвижва се същият подписан образ, без повторно изграждане.

## Настройка на хранилището (еднократно)

- Branch ruleset на `main` изисква pull request и преминали status checks
  (Build & test, Gitleaks (secrets), Semgrep (SAST), Build & publish container
  image), плюс code scanning results (Gitleaks, Semgrep, Trivy с праг High or
  higher), и забранява директен push.
- Secret `GITOPS_TOKEN` (fine-grained PAT с Contents и Pull requests write върху
  gitops хранилището) за автоматичния dev pull request.
- Gitleaks hook за тайни се активира еднократно след клониране с
  `pre-commit install`.

## Лиценз

[Apache License 2.0](LICENSE)
