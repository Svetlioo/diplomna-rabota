# Защита на софтуерната верига за доставки

DevSecOps реализация, която защитава софтуерната верига за доставки на
контейнеризирано банково приложение в Kubernetes. Бакалавърска дипломна работа,
ТУ София, специалност Киберсигурност.

Това хранилище съдържа изходния код на услугите, техните Dockerfile-и и CI/CD
процеса в GitHub Actions. Акцентът е върху сигурната доставка: сканиране,
подписване, SBOM, provenance и контролирано внедряване през GitOps.

## Трите хранилища

| Хранилище | Отговорност |
|---|---|
| `diplomna-rabota` (това) | Изходен код на услугите, Dockerfile-и, GitHub Actions CI/CD |
| `diplomna-rabota-infra` | Terraform за Azure: споделена основа, AKS клъстер, база, контролери |
| `diplomna-rabota-gitops` | Желано състояние на клъстера: Helm charts, values по среда, ArgoCD приложения, Kyverno политики |

## Услуги

- `bank-service` (Spring Boot, Java 25, PostgreSQL). JWT автентикация: httpOnly
  cookie за браузъра и bearer header за API клиенти.
- `fraud-detection` (Python, FastAPI). Без състояние и без база. Правило-базирана
  проверка на превод преди изпълнението му.
- `frontend` (React 19, Vite, TypeScript). Статичен build, сервиран в контейнера;
  заявките към `/api` се пренасочват към bank-service.

Трите образа се изграждат от фиксирани официални базови образи, работят като
non-root с read-only root filesystem и се внедряват през собствени Helm charts.

## Локално пускане

Нужни са Docker, JDK 25 и Node 22.

1. Копирай примерните променливи и попълни стойностите:
   ```bash
   cp .env.example .env
   ```
   Задай `BANK_DB_USER`, `BANK_DB_PASSWORD`, `DB_USERNAME`, `DB_PASSWORD` (същите
   като на базата) и `JWT_SECRET` (`openssl rand -hex 32`).

2. Вдигни базата и fraud-detection:
   ```bash
   docker compose up -d
   ```
   PostgreSQL слуша на 5432, fraud-detection на 8000.

3. Пусни bank-service:
   ```bash
   cd apps/bank-service && ./mvnw spring-boot:run
   ```
   Слуша на 8080.

4. Пусни frontend:
   ```bash
   cd apps/frontend && npm install && npm run dev
   ```
   Отвори `http://localhost:5173`; Vite проксира `/api` към bank-service.

## CI/CD поток

Всяка услуга има собствен CI workflow с еднаква структура и различен build
инструмент (Maven, pip, Node). Отделен `repo-security.yml` пуска скенерите върху
цялото хранилище.

При pull request към `main` се изпълняват само проверките: компилация и тестове,
изграждане на образа за валидация (без публикуване) и скенерите. Неуспешна
проверка блокира сливането.

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

Тайните се хващат на три нива: локално преди commit (`pre-commit install`
еднократно след клониране), при push (GitHub Push Protection) и в CI (Gitleaks
блокира сливането).

## Supply chain сигурност

- Подписване: Cosign keyless (Sigstore), подпис по digest, запис в Rekor.
- SBOM: Syft в CycloneDX формат, прикачен към образа като подписана attestation.
- Provenance: slsa-github-generator издава подписано удостоверение от кое
  хранилище, кой commit и кой workflow е изграден образът.
- Допускане: Kyverno налага две политики в `dev`, `test` и `prod`:
  `verify-image-signatures` изисква валиден Cosign подпис, SLSA provenance и
  CycloneDX SBOM attestation; `restrict-image-registries` допуска само образи от
  `ghcr.io/svetlioo/*`.

## Внедряване (GitOps)

Средите `dev`, `test` и `prod` се синхронизират от ArgoCD спрямо gitops
хранилището. `dev` се обновява автоматично от CI чрез pull request с новия таг и
digest. Придвижването към `test` и `prod` е ръчно през Promote workflow в gitops
хранилището, който копира таг и digest между средите и отваря pull request за
одобрение. Придвижва се същият подписан образ, без повторно изграждане.

## Настройка на хранилището (еднократно)

- Branch ruleset на `main`: изисква pull request, status checks и code scanning
  results; забранен force push.
- Secret scanning и Push Protection включени.
- Secret `GITOPS_TOKEN` (fine-grained PAT с Contents и Pull requests write върху
  gitops хранилището) за автоматичния dev pull request.

## Лиценз

[Apache License 2.0](LICENSE)
