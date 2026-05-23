# Мастър документация на проекта

> Защита на софтуерната верига за доставка на контейнеризирани разпределени
> приложения чрез DevSecOps практики в Kubernetes среда — ТУ София, Киберсигурност.

Този документ описва **всичко** в проекта: целта, архитектурата, потоците и
всеки смислен файл от трите репозитория. Целта е един човек (ти, на защитата) да
може да отвори този файл и да обясни всяка част от системата.

Boilerplate файлове (wrapper-и, lock файлове, `.gitignore`, IDE настройки) са
обобщени групово, а не разписани един по един, защото не носят логика.

---

## 1. Какво представлява проектът

Демонстрира се **сигурна верига за доставка на софтуер** (software supply chain)
за контейнеризирано банково приложение в Kubernetes. Бизнес приложението е
нарочно просто — фокусът е как кодът стига **сигурно** от commit до работещ pod:
сканиране, подписване, SBOM, provenance, политики при допускане (admission) и
контролиран GitOps деплой.

**Резултатът се валидира спрямо SLSA Level 2** и индустриални регулации (DORA,
NIS2, EU CRA, PCI DSS).

### Трите репозитория и ролите им

| Репо | Отговорност | Брой файлове |
|---|---|---|
| **`diplomna-rabota`** | Сорс кодът на сервизите + Dockerfile-и + GitHub Actions CI/CD | ~60 |
| **`diplomna-rabota-infra`** | Terraform — Azure foundation + AKS клъстер + контролери | ~48 |
| **`diplomna-rabota-gitops`** | Desired state на клъстера (Helm charts, per-env values, ArgoCD apps, Kyverno) | ~30 |

Разделянето на три репота е умишлено и отговаря на индустриалния модел:
- **App репо** — разработчиците пишат код; CI произвежда подписан образ.
- **Infra репо** — платформеният екип владее облака (рядко се пипа).
- **GitOps репо** — desired state; единственото нещо, което ArgoCD чете и налага в
  клъстера. Промяна тук = промяна в клъстера (нищо не се деплойва с `kubectl apply`
  на ръка).

### Двата сервиза

- **bank-service** (Spring Boot 4 / Java 25) — всичко свързано с пари: регистрация
  и вход (JWT), една сметка на потребител (IBAN), преводи по IBAN, ledger
  (история), замразяване на сметка. Една PostgreSQL база.
- **fraud-detection** (Python 3.14 / FastAPI) — без състояние, без база. Един
  endpoint `POST /evaluate`, който получава превода в тялото на заявката и връща
  присъда `{suspicious, reason}` по просто правило (сума ≥ праг).

---

## 2. Архитектура и потоци

### 2.1 Изпълнителен поток (как работи приложението)

```
                   ┌────────────────────────────────────────┐
   клиент (Bruno)  │              bank-service               │
   ──── JWT ───────▶  /auth  /accounts  /transactions        │
                   │     │                                    │
                   │     │ преди превод: screening            │
                   │     ▼                                    │
                   │  PaymentService ──HTTP──▶ fraud-detection│
                   │     │  (suspicious? → freeze + 403)      │   POST /evaluate
                   │     ▼                                    │   (без база)
                   │  TransactionService (@Transactional)     │
                   │     debit + credit + ledger в 1 транзакция│
                   │     ▼                                    │
                   │  PostgreSQL (users, accounts, transactions)
                   └────────────────────────────────────────┘
```

Ключово: **screening-ът е ПРЕДИ превода**. Ако fraud-detection върне „suspicious",
сметката се замразява и преводът се отказва с 403 — парите никога не мърдат. Ако
fraud-detection е недостъпен → **fail-open** (преводът минава), за да не спре цялата
система заради паднал помощен сервиз.

### 2.2 Поток на доставката (supply chain — същината на дипломата)

```
git push (PR → main)
   │
   ├─ repo-security.yml ── Gitleaks + Semgrep + Trivy ── SARIF → Code Scanning
   │                        (блокира merge при НОВ проблем)
   │
   └─ след merge в main:
        bank-service-ci.yml / fraud-detection-ci.yml
          build-test → image (build + push в ghcr.io)
                          │
                          ├─ cosign sign      (keyless подпис)
                          ├─ syft SBOM        (CycloneDX, attest + artifact)
                          ├─ SLSA provenance  (slsa-github-generator)
                          │
                          └─ deploy-dev: отваря+авто-merge-ва PR в gitops
                                          (обновява environments/dev/values-*.yaml)
                                              │
                                              ▼
                                          ArgoCD синква dev
                                              │
                                              ▼
                                          Kyverno admission:
                                          проверява Cosign подпис → допуска pod
```

Промоцията към `test` и `prod` е **ръчна** (Promote workflow в gitops, човек
одобрява PR — separation of duties). `prod` винаги взима test-валидирания образ.

### 2.3 Покрити атаки (Глава 5 от дипломата)

| № | Атака | Защита | Къде |
|---|---|---|---|
| 1 | Hardcoded secret в commit | Gitleaks | `repo-security.yml` |
| 2 | SQL injection в код | Semgrep SAST | `repo-security.yml` |
| 3 | Уязвима зависимост (Log4Shell-like) | Trivy SCA | `repo-security.yml` |
| 4 | Уязвим базов образ | Trivy | `repo-security.yml` |
| 5 | Неподписан/подменен образ | Kyverno + Cosign verify | gitops `policies/verify-images.yaml` |

(Атака 6 — runtime аномалия с Falco — беше отпаднала.)

---

## 3. Репозитори `diplomna-rabota` (приложение + CI)

```
diplomna-rabota/
├── apps/
│   ├── bank-service/        Spring Boot / Java 25
│   └── fraud-detection/     Python / FastAPI
├── .github/workflows/       3 pipeline-а
├── docker-compose.yml       локална среда
├── .env.example             шаблон за локални тайни
├── .pre-commit-config.yaml  gitleaks pre-commit hook
└── README.md
```

### 3.1 `apps/bank-service/` — конфигурация и build

**`pom.xml`** — Maven дефиниция. Наследява `spring-boot-starter-parent` **4.0.6**,
Java **25**. Зависимости: actuator (health probes), data-jpa (ORM), security +
oauth2-resource-server (JWT валидация), flyway (миграции), validation (Bean
Validation), webmvc (REST), postgresql драйвер, lombok. Тестовите starter-и са със
`scope=test`. Maven компилаторът е конфигуриран с Lombok annotation processor.

**`Dockerfile`** — еднослоен образ. База `eclipse-temurin:25-jre-alpine`
(официален, пинат, **никога `latest`**). Създава non-root потребител `app`, копира
готовия JAR, върви като `app`, отваря порт 8080. `ENTRYPOINT java -jar`.

**`src/main/resources/application.yaml`** — Spring конфигурация:
- `spring.config.import` зарежда `.env` от няколко нива нагоре (`.`, `..`, `../..`)
  с префикс `optional:` — така `.env` се намира независимо дали стартираш от
  директорията на сервиза, от `apps/` или от root (терминал или IDE). В CI/k8s
  файлът липсва → променливите идват от средата (secrets).
- `datasource` чете `DB_URL/USERNAME/PASSWORD`.
- `jpa.hibernate.ddl-auto: validate` — Hibernate **не** пипа схемата; Flyway я
  владее. `validate` гарантира, че entity-тата съвпадат с мигрираната схема (fail-fast).
- `open-in-view: false` — Hibernate сесията е ограничена до `@Transactional`
  методите → принуждава явно DTO мапване, освобождава DB connection-и по-рано.
- `flyway.clean-disabled` default `true` (clean е забранен); локално се пуска на
  `false` през `.env`.
- `security.jwt.secret/ttl` — HS256 таен ключ и срок на токена (default 12h).
- `fraud.url` — адрес на fraud-detection (локално localhost:8000; в k8s — service DNS).
- `server.port` default 8080.

**`src/main/resources/db/migration/V1__init.sql`** — единствена Flyway миграция
(disposable dev база → няма смисъл от инкрементални V2/V3). Три таблици:
- `users` (id UUID, email UNIQUE, password_hash, created_at).
- `accounts` (id, owner_id UNIQUE FK→users, iban UNIQUE, balance, currency, frozen,
  version за optimistic lock, времеви печати). CHECK constraint-и: `balance >= 0`
  и валута да е ISO-4217 формат (`^[A-Z]{3}$`).
- `transactions` (id, owner_id, to_iban, amount, currency, created_at; CHECK
  `amount > 0`). Индекс по `owner_id` за бързо извличане на история.

### 3.2 `apps/bank-service/` — Java код

Пакет `bg.tu_sofia.diploma.bank`. Слоеста архитектура: `config/` `domain/`
`service/` `exception/` `web/` (+ `web/dto/`).

**`BankServiceApplication.java`** — входна точка. `@SpringBootApplication` +
`@EnableJpaAuditing` (за автоматичните `@CreatedDate/@LastModifiedDate` полета).

#### `config/`

**`SecurityConfig.java`** — сърцето на автентикацията:
- Прави `SecretKeySpec` от `JWT_SECRET` за HmacSHA256.
- `SecurityFilterChain`: **stateless** (без сесии), CSRF изключен (няма cookie за
  защита при Bearer токен). Публични пътища: `/auth/**` и `/actuator/health/**`;
  всичко друго изисква валиден JWT (`oauth2ResourceServer.jwt`).
- Bean-ове: `BCryptPasswordEncoder` (за паролите), `NimbusJwtEncoder` (издава
  токени), `NimbusJwtDecoder` с HS256 (валидира токени). Един и същ ключ подписва
  и проверява.

**`FlywayDevConfig.java`** — **само за локална разработка**. Условен bean
(`@ConditionalOnProperty app.db.clean-on-start=true`), който при всяко стартиране
прави `flyway.clean()` + `flyway.migrate()` → пресъздава схемата. Държи базата
„за изхвърляне", докато схемата още се мени. Никога не се пуска в k8s (трие данни).

**`RestClientConfig.java`** — създава `RestClient` bean (`fraudRestClient`) с
base URL `fraud.url`. Това е модерният синхронен HTTP клиент на Spring (за blocking
MVC), с който bank-service вика fraud-detection.

#### `domain/` — entity-та и репозитории

**`User.java`** — entity за потребител. Lombok `@Getter`, защитен no-args
конструктор (изисква се от JPA, но скрит от кода). Статичен фабричен метод
`create(email, passwordHash)` генерира UUID. Няма setter-и → неизменност отвън.

**`UserRepository.java`** — Spring Data JPA. `findByEmail`, `existsByEmail`.

**`Account.java`** — entity за сметка. Полета: id, ownerId (UNIQUE),
iban (UNIQUE), balance, currency, frozen, `@Version` (optimistic lock), времеви
печати (`@CreatedDate/@LastModifiedDate`). Поведенчески методи (богат domain модел,
не anemic):
- `open(ownerId, iban)` — фабрика; стартова наличност 0, валута EUR.
- `freeze()` — замразява.
- `deposit(amount)` / `withdraw(amount)` — withdraw хвърля `IllegalStateException`
  при недостатъчна наличност (превежда се в HTTP грешка по-нагоре).

**`AccountRepository.java`** — `existsByIban`, `findByOwnerId`, `findByIban`, и
ключовото `findByIdForUpdate` с `@Lock(PESSIMISTIC_WRITE)` → генерира
`SELECT ... FOR UPDATE`, заключва реда докато транзакцията не приключи. Това е
основата на безопасните конкурентни преводи.

**`Transaction.java`** — ledger запис за **успешен** превод (провалите стават HTTP
грешки → няма ред, затова няма поле „status"). Записва се в същата транзакция като
движението на парите → не може да съществува без парите наистина да са мръднали.
Полета само за четене (`updatable = false`). Фабрика `record(...)`.

**`TransactionRepository.java`** — `findByOwnerId` (странициран `Page`),
`findByIdAndOwnerId` (история, ограничена до собственика → не виждаш чужди преводи).

#### `service/` — бизнес логика

**`AccountService.java`** — операции по сметки. Важни моменти:
- `openForOwner` — отваря единствената сметка при регистрация; един-към-един
  гарантиран от UNIQUE на `owner_id`.
- `deposit/withdraw` — заключват собствената сметка (`lockOwnAccount`), withdraw
  отказва при замразена сметка или недостатъчна наличност.
- `transfer(ownerId, toIban, amount)` — **най-важният метод**. Източникът винаги е
  собствената сметка (от JWT) → не можеш да дебитираш чужда. Заключва **двата** реда
  с `SELECT ... FOR UPDATE` в **ред по id** (детерминистичен ред → няма deadlock при
  две едновременни срещуположни транзакции). Проверки: не към себе си, не замразена,
  еднаква валута, достатъчна наличност. Дебит + кредит в една транзакция → или и
  двете, или нищо.
- `freeze` — замразява при flagged превод.
- `uniqueIban` — генерира IBAN, докато не уцели свободен.

**`AuthService.java`** — регистрация и вход.
- `register` — в **една транзакция** създава потребител (bcrypt парола) И отваря
  сметката му → регистриран потребител винаги има точно една сметка, няма отделна
  стъпка „създай сметка". Хвърля 409 при зает email.
- `login` — намира по email, сверява паролата (bcrypt `matches`), иначе 401.
- `issueToken` — съставя JWT (subject = userId, claim email, issuedAt, expiresAt по
  TTL), подписва с HS256.

**`PaymentService.java`** — оркестрация на use-case „плащане": **първо** screening,
**после** изпълнение. Умишлено **не е** `@Transactional` — fraud повикването трябва
да е извън движението на парите. `pay()` = `fraudScreeningService.check(...)` →
`transactionService.transfer(...)`.

**`FraudScreeningService.java`** — викане на fraud-detection преди превода.
`check()` пита `isSuspicious()`; ако да → замразява сметката, логва, хвърля
`SuspiciousTransferException` (→ 403, парите не мърдат). `isSuspicious()` прави
HTTP POST `/evaluate` с тялото на превода; при **всяка** грешка/недостъпност →
връща `false` (**fail-open**) и логва. Вътрешни record-и `EvaluateRequest` и
`Verdict`.

**`TransactionService.java`** — изпълнява и записва превода в **една**
`@Transactional`: движение на парите (`accountService.transfer`) + ledger insert
(`Transaction.record`) или и двете commit-ват, или и двете rollback-ват. Само
успешни преводи се записват. Плюс четене: `getOwnTransaction`, `getOwnTransactions`
(странициран).

**`IbanGenerator.java`** — генерира валиден български IBAN с коректни ISO 13616
контролни цифри (MOD-97). Фиксиран демо банков код „DIPL" + 14 случайни цифри
(`SecureRandom`). Резултатът е коректен 22-знаков IBAN.

#### `exception/` — доменни изключения

Девет малки изключения, всяко мапнато към HTTP статус в `GlobalExceptionHandler`:
`AccountNotFoundException` (404, със статични `forOwner()/forIban()`),
`AccountFrozenException` (403), `SuspiciousTransferException` (403),
`InsufficientFundsException` (422), `SameAccountTransferException` (422),
`CurrencyMismatchException` (422), `EmailAlreadyExistsException` (409),
`InvalidCredentialsException` (401), `TransactionNotFoundException` (404).

#### `web/` — REST слой

**`AuthController.java`** — `POST /auth/register` (201 + Bearer токен),
`POST /auth/login` (токен). `@Valid` валидира телата.

**`AccountController.java`** — `GET /accounts` (своята сметка), `POST /accounts/deposit`,
`POST /accounts/withdraw`. Собственикът винаги идва от JWT (`@AuthenticationPrincipal
Jwt`, subject = userId) — няма `/me` път, защото потребителят е винаги от токена.

**`TransactionController.java`** — `POST /transactions` (създава превод през
`PaymentService.pay` → screening + изпълнение, 201), `GET /transactions/{id}`
(само свой), `GET /transactions` (странициран списък, default 20, по createdAt desc).

**`GlobalExceptionHandler.java`** — `@RestControllerAdvice`, който превръща всяко
доменно изключение в чист JSON `ErrorResponse` с правилен HTTP статус и кодов
идентификатор (напр. `INSUFFICIENT_FUNDS`). Хваща и `MethodArgumentNotValidException`
(Bean Validation) → 400 с обединени съобщения.

**`web/dto/`** — DTO-та като Java records: `RegisterRequest`, `LoginRequest`,
`TokenResponse` (с фабрика `bearer(...)`), `AccountResponse` (`from(account)`),
`AmountRequest`, `CreateTransferRequest` (toIban + amount), `TransactionResponse`
(`from(tx)`), `ErrorResponse` (`of(code, message)`). DTO-тата изолират JSON
контракта от entity-тата (не leak-ват вътрешни полета).

### 3.3 `apps/fraud-detection/` — Python сервиз

**`app/main.py`** — целият сервиз в един файл. FastAPI app. `AMOUNT_THRESHOLD`
от env (default 10000). Pydantic модели `EvaluateRequest` (ownerId, toIban, amount)
и `Verdict` (suspicious, reason). Два endpoint-а: `GET /health` (за k8s probes) и
`POST /evaluate` — ако `amount >= threshold` → suspicious с обяснение, иначе не.
**Без база, без състояние** — присъдата се смята само от тялото на заявката.

**`Dockerfile`** — база `python:3.14-alpine` (официален, пинат). non-root, инсталира
зависимостите от `requirements.txt`, стартира uvicorn на порт 8000.

**`requirements.txt`** — `fastapi==0.136.1`, `uvicorn==0.47.0` (без `[standard]` —
по-малко зависимости, без alpine/musl рискове).

### 3.4 `.github/workflows/` — CI/CD

**`repo-security.yml`** — repo-wide скенери при **всеки** PR и push към main. Три
независими job-а, всеки качва SARIF в GitHub Code Scanning:
- `gitleaks` — `gitleaks-action@v2.3.9`, пълна git история (`fetch-depth: 0`) →
  тайни в commit-и (Атака 1). Hard-fail-ва check-а при secret И качва SARIF
  (`category: gitleaks`, `if: always()`) → секретът се записва и в Security tab.
- `semgrep` — в контейнер `semgrep/semgrep:1.163.0`, набори правила `p/java`,
  `p/python`, `p/security-audit`, `p/owasp-top-ten`, `p/cwe-top-25` → опасен код,
  напр. SQLi (Атака 2). `category: semgrep`.
- `trivy` — `trivy-action@v0.36.0`, `scan-type: fs` върху целия repo,
  `severity: CRITICAL,HIGH`, `ignore-unfixed: true` → уязвими зависимости + IaC
  (Атаки 3, 4). `category: trivy`.

Блокирането е **diff-aware**: GitHub Code Scanning + branch ruleset „Require code
scanning results" блокира merge само при **нов** проблем в PR-а; стар проблем на
main се показва, но не блокира.

**`bank-service-ci.yml`** — per-service pipeline. Тригери: PR и push към main.
Job-ове:
- `changes` — `dorny/paths-filter` открива дали `apps/bank-service/**` или самият
  workflow са пипнати → `bank` output. Останалите job-ове се пускат само ако да
  (или винаги при push към main).
- `build-test` — JDK 25, `./mvnw -B verify` (компилация + тестове).
- `image` — пакетира JAR, извлича версия от pom + run_number + кратък SHA → таг.
  Buildx build. **На PR само build** (валидира Dockerfile, не публикува). **На push
  към main**: login в ghcr.io → push → **Cosign keyless sign** (`v3.0.6`, форсиран
  легаси tag-based формат с `--new-bundle-format=false --use-signing-config=false`,
  защото ghcr не поддържа OCI 1.1 Referrers) → **Syft SBOM** (CycloneDX) качен и
  като artifact, и attest-нат към образа → изход: image/digest/tag.
- `provenance` — извиква reusable `slsa-github-generator/.../generator_container_slsa3.yml@v2.1.0`
  → генерира и подписва **SLSA provenance** (Level 2). Подписва се от собствената
  keyless самоличност на генератора (затова Kyverno provenance политика, ако имаше,
  би сочила генератора, не този workflow).
- `deploy-dev` — `needs: [changes, image, provenance]` (чака provenance да успее →
  не деплойва образ без provenance). Само при push към main. Обща `concurrency`
  група `deploy-dev` (сериализира деплоите → без merge конфликти в gitops). Клонира
  gitops репото с `GITOPS_TOKEN`, обновява `environments/dev/values-bank.yaml` (tag
  + digest през `yq`), отваря PR и го **авто-merge-ва** (`--squash --admin`) → audit
  trail.

**`fraud-detection-ci.yml`** — почти идентичен на bank-service-ci.yml, но build
stack-ът е pip вместо Maven (setup-python вместо setup-java), образ
`ghcr.io/svetlioo/fraud-detection`, paths-filter сочи `apps/fraud-detection/**`.
Същите sign/SBOM/SLSA/deploy-dev стъпки.

### 3.5 Root файлове

**`docker-compose.yml`** — локална среда. `bank-db` (postgres:17.2-alpine, порт от
`.env`, healthcheck с `pg_isready`, persistent volume) и `fraud-detection` (build от
`./apps/fraud-detection`, порт 8000). bank-service се пуска от IDE/терминал, не в
compose.

**`.env.example`** — шаблон за локални тайни (копира се в `.env`, който е gitignored).
DB креденшъли, `JWT_SECRET` (≥32 байта, `openssl rand -hex 32`), `FRAUD_URL`,
`FRAUD_AMOUNT_THRESHOLD`, и локалните `APP_DB_CLEAN_ON_START=true` +
`FLYWAY_CLEAN_DISABLED=false` (никога в k8s).

**`.pre-commit-config.yaml`** — pre-commit hook с gitleaks v8.30.1 → хваща тайни
**преди** commit (първа линия на защита, преди CI). Активиране на клонинг:
`pre-commit install`.

**`README.md`** — публичен README (на български), описва CI/CD потока, кога
скановете блокират/пускат (с таблица на доказаните сценарии) и supply chain
сигурността.

**Boilerplate (групово):** `apps/bank-service/mvnw`, `mvnw.cmd`, `.mvn/` (Maven
wrapper), `.dockerignore`, `.gitignore`, `.gitattributes`, `LICENSE` (Apache 2.0).

---

## 4. Репозитори `diplomna-rabota-infra` (Terraform → Azure)

```
diplomna-rabota-infra/
├── shared/      foundation: resource group + storage за Terraform state
├── aks/         Kubernetes клъстер
├── data/        PostgreSQL Flexible Server + бази + k8s secrets
├── argocd/      ArgoCD контролер (Helm release)
├── kyverno/     Kyverno контролер (Helm release)
└── scripts/     старт/стоп на клъстера (пестене на пари)
```

Всеки модул е самостоятелен Terraform root с еднаква структура от файлове:
`main.tf` (ресурси), `variables.tf` (входове), `outputs.tf` (изходи), `versions.tf`
(версии на provider-и), `backend.tf` (отдалечен state в Azure storage),
`.terraform.lock.hcl` (заключени версии), `README.md`. Модулите, които пипат
клъстера, имат и `providers.tf` + `data.tf` (четат AKS state, за да конфигурират
kubernetes/helm provider-ите).

**Защо отделни модули, а не един:** изолация на state и на „blast radius". Можеш да
правиш `terraform apply` на `data/` без да пипаш `aks/`. `shared` се прави първи
(държи state storage-а), после `aks`, после `data/argocd/kyverno`.

### 4.0 CI сигурност (`repo-security.yml` + pre-commit)

`.github/workflows/repo-security.yml` — на всеки PR/push: `gitleaks` (тайни) +
`trivy` със **`scan-type: config`** (IaC misconfig върху Terraform-а), и двете качват
SARIF → GitHub Code Scanning. Блокиране като в главния repo: required check
`Gitleaks (secrets)` + ruleset „Require code scanning results" (Trivy, High+,
diff-aware). `.pre-commit-config.yaml` (gitleaks v8.30.1) дава локален gate преди
commit (`pre-commit install`).

### 4.1 `shared/` — foundation

**`main.tf`** — `azurerm_resource_group` „rg-diploma-shared"; `azurerm_storage_account`
за Terraform remote state (TLS 1.2 минимум, без публични blob-ове, versioning
включено); `azurerm_storage_container` „tfstate" (private). Това е „bootstrap"
слоят — тук живее state-ът на всички останали модули.

### 4.2 `aks/` — Kubernetes клъстер

**`main.tf`** — `azurerm_resource_group` „rg-diploma-aks"; `azurerm_kubernetes_cluster`
с един system node pool, `SystemAssigned` managed identity, версия и размер на нодовете
от променливи. Control plane-ът е безплатен (Azure for Students). Това е единственият
клъстер; средите са namespaces (dev/test/prod), не отделни клъстери.

### 4.3 `data/` — база данни и тайни

**`main.tf`** — най-важният модул за данните:
- `random_password.admin` — генерира администраторска парола.
- `azurerm_postgresql_flexible_server` — един Postgres 17 сървър (Burstable
  B1ms, най-евтиният), public access (за демо), парола-базирана автентикация.
- `azurerm_postgresql_flexible_server_firewall_rule` „AllowAzureServices" — пуска
  достъп от Azure услуги (0.0.0.0).
- `azurerm_postgresql_flexible_server_database.envs` — `for_each` по
  `var.environments` → по една база на среда (`bank_dev`, `bank_test`, `bank_prod`).
- `kubernetes_secret_v1.bank_db` — `for_each` по средите → k8s Secret
  `bank-service-db` във всеки namespace (dev/test/prod), с
  `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` (sslmode=require). bank-service го чете
  през `envFrom` (виж Helm chart). **Това е мостът инфра → приложение.**

**`data.tf`** — `terraform_remote_state.aks` чете AKS state от shared storage-а, за
да знае към кой клъстер да създаде secret-ите.

**`variables.tf`** — дефинира `environments` map (dev/test/prod → име на база),
postgres версия, admin потребител, регион (`polandcentral` — заради ограничение на
subscription-а), имена.

### 4.4 `argocd/` — GitOps контролер

**`main.tf`** — `kubernetes_namespace_v1` „argocd"; `helm_release` за официалния
`argo-cd` chart (от argoproj.github.io), с настроен reconciliation timeout. След
този apply ArgoCD е инсталиран и готов да чете gitops репото.

### 4.5 `kyverno/` — admission контролер

**`main.tf`** — `kubernetes_namespace_v1` „kyverno"; `helm_release` за официалния
`kyverno` chart. След този apply Kyverno е в клъстера и чака политики (които идват
от gitops, не оттук — виж раздел 5).

### 4.6 `scripts/`

**`aks-start.sh` / `aks-stop.sh`** — стартират/спират AKS клъстера И Postgres
сървъра (за пестене на Azure credits, когато не се работи). stop е `--no-wait`;
start освежава kubeconfig накрая. Имената на ресурсите са hardcode-нати.

---

## 5. Репозитори `diplomna-rabota-gitops` (desired state)

```
diplomna-rabota-gitops/
├── bootstrap/       root-app (app-of-apps) + AppProject
├── apps/            6 ArgoCD Application (2 сервиза × 3 среди) + kyverno-policies
├── helm-charts/     custom charts: bank-service, fraud-detection
├── environments/    per-env values: dev/test/prod × bank/fraud
├── policies/        Kyverno ClusterPolicy (Cosign verify)
├── .github/workflows/  promote.yml + repo-security.yml
└── .pre-commit-config.yaml  gitleaks pre-commit hook
```

**Моделът „app-of-apps":** ArgoCD-то инсталирано от инфра-репото получава **един**
root Application (ръчно), който сочи `apps/`. Root-ът рекурсивно открива всички
Application-и там и ги създава. Оттам нататък всичко е GitOps — нищо не се прави
с `kubectl` на ръка.

### 5.0 CI сигурност (`repo-security.yml` + pre-commit)

`.github/workflows/repo-security.yml` — на всеки PR/push: `gitleaks` (тайни) +
`trivy` със **`scan-type: config`** (IaC misconfig върху Helm chart-овете и K8s
манифестите), SARIF → Code Scanning, блокиране през ruleset (`Gitleaks (secrets)`
required + „Require code scanning results", Trivy High+). `.pre-commit-config.yaml`
(gitleaks v8.30.1) → локален gate преди commit. Идентично на infra и главния repo.

### 5.1 `bootstrap/`

**`appproject.yaml`** — ArgoCD `AppProject` „diploma": ограничава кои репота
(само gitops) и кои namespaces (dev/test/prod) са позволени; whitelist за
ресурсите. Граница на сигурност — приложенията не могат да деплойват извън
позволеното.

**`root-app.yaml`** — root Application (app-of-apps). Сочи `apps/` с
`directory.recurse: true`, automated sync (prune + selfHeal). Това е единственото
нещо, което се прилага ръчно веднъж; всичко друго follow-ва.

### 5.2 `apps/` — ArgoCD Application манифести

Шест почти идентични файла: `bank-service-{dev,test,prod}.yaml` и
`fraud-detection-{dev,test,prod}.yaml`. Всеки:
- `project: diploma`, сочи съответния Helm chart (`helm-charts/<сервиз>`) с
  per-env values файл (`environments/<env>/values-<сервиз>.yaml`).
- `destination.namespace` = средата (dev/test/prod).
- `syncPolicy.automated` (prune + selfHeal) + `CreateNamespace=true` +
  `ServerSideApply=true`.

И трите среди (включително prod) са с automated sync; разликата е **как стига
образът дотам**: dev автоматично от CI, test/prod през ръчен Promote PR. prod има
`replicaCount: 2` (виж values).

**`kyverno-policies.yaml`** — отделен Application (project `default`, защото слага
cluster-scoped политика в namespace `kyverno`), сочи `policies/`. Така Kyverno
политиките също са под GitOps.

### 5.3 `helm-charts/` — custom charts

Два chart-а с еднаква структура (написани от нулата, не upstream — за да се
демонстрират конфигурациите). Пример `bank-service`:

**`Chart.yaml`** — метаданни (име, version 0.1.0, appVersion 0.0.1).

**`templates/_helpers.tpl`** — шаблонни helper-и: име, labels, selectorLabels, и
ключовият **`image`** helper — рендерира `repo:tag@digest`, когато и двете са
зададени (четимост от тага + неизменност от digest-а). Точно това позволява
digest pinning-ът да съвпада с това, което Kyverno mutate-ва → ArgoCD остава Synced.

**`templates/deployment.yaml`** — Deployment с втвърдена сигурност:
- `automountServiceAccountToken: false` (CIS K8s Benchmark 5.1.6 — pod-ът не получава
  излишен API токен).
- `podSecurityContext`: runAsNonRoot, fixed UID/GID, `seccompProfile: RuntimeDefault`.
- `containerSecurityContext`: `allowPrivilegeEscalation: false`,
  `readOnlyRootFilesystem: true`, `capabilities.drop: [ALL]`. (Pod Security
  Standards „restricted".)
- `envFrom secretRef: bank-service-db` — чете DB креденшълите от secret-а, създаден
  от Terraform `data` модула.
- liveness/readiness probes към actuator health endpoint-и.
- read-only root → `emptyDir` volume за `/tmp`.

**`templates/service.yaml`** — ClusterIP Service (вътрешен), порт от values.

**`values.yaml`** — default-и: образ (`ghcr.io/svetlioo/bank-service`, tag/digest
празни — попълват се per-env), 1 реплика, resources (requests/limits),
securityContext-и, probes, и `env` (Hikari pool size, `FRAUD_URL` сочещ
in-cluster `http://fraud-detection:8000`).

`fraud-detection` chart-ът е аналогичен: порт 8000, health на `/health`, без
`envFrom` (няма база), по-малки resources, env `FRAUD_AMOUNT_THRESHOLD`.

### 5.4 `environments/` — per-env values

Шест малки overlay файла (`dev/test/prod` × `bank/fraud`). Всеки задава само това,
което се различава от chart default-ите — преди всичко **`image.tag` + `image.digest`**
(точният подписан образ за тази среда). dev се обновява автоматично от CI; prod има
и `replicaCount: 2`. Едновременното пиниране на tag (четимост) и digest (неизменност)
е умишлено — digest-ът е това, което Kyverno и cosign проверяват.

### 5.5 `policies/verify-images.yaml` — Kyverno (Атака 5)

`ClusterPolicy` „verify-image-signatures" — **ядрото на supply-chain защитата при
admission**:
- `validationFailureAction: Enforce`, `failurePolicy: Fail` (отказва при съмнение).
- Прилага се за **Pod**-ове в namespaces dev/test/prod.
- `verifyImages` за `ghcr.io/svetlioo/*`: `mutateDigest` + `verifyDigest` +
  `required` (образът трябва да е подписан).
- `attestors.keyless`: подписът трябва да е от самоличност, чийто subject пасва на
  regex `^https://github\.com/Svetlioo/diplomna-rabota/\.github/workflows/[^/]+\.yml@refs/heads/.+$`
  (т.е. **някой workflow от твоето app репо**), issuer `token.actions.githubusercontent.com`,
  проверено в Rekor. → Неподписан образ или подписан от чужда самоличност се отказва.

Допълнителните полета (`admission`, `background`, `emitWarning`,
`skipBackgroundRequests`, `useCache`, `signatureAlgorithm: sha256` и т.н.) са
Kyverno-инжектирани default-и, **изрично декларирани в git**, за да няма diff
между git и live → ArgoCD остава Synced.

> Бележка: политиката е scope-ната до `ghcr.io/svetlioo/*`. Външни образи (напр.
> `nginx`) **не** се блокират — за това би трябвало отделна „Restrict Image
> Registries" allowlist политика (не е добавена). Тази политика пази твоята верига,
> не чужди образи.

### 5.6 `.github/workflows/promote.yml` — промоция test/prod

`workflow_dispatch` (ръчно от Actions). Входове: `service` (bank/fraud/both) и
`path` (dev-to-test / test-to-prod). Копира `image.tag` + `image.digest` от
по-долната среда в по-горната (`yq`) и **отваря PR** — **не** го merge-ва (човек
одобрява → separation of duties). prod винаги взима test-валидирания образ.
(Променливата е `PATH_INPUT`, не `PATH`, защото `PATH` е резервирана.)

---

## 6. Речник на ключовите инструменти

| Инструмент | Роля в проекта |
|---|---|
| **Spring Boot / Java 25** | bank-service |
| **FastAPI / Python 3.14** | fraud-detection |
| **Flyway** | версионирани миграции на схемата |
| **GitHub Actions** | CI/CD |
| **Gitleaks** | scan за тайни (Атака 1) |
| **Semgrep** | SAST — опасен код (Атака 2) |
| **Trivy** | SCA + IaC + образи (Атаки 3, 4) |
| **Cosign (Sigstore)** | keyless подписване + verify (Атака 5) |
| **Syft** | SBOM (CycloneDX) |
| **slsa-github-generator** | SLSA Level 2 provenance |
| **ghcr.io** | container registry |
| **Terraform** | IaC за Azure |
| **AKS** | управляван Kubernetes |
| **Azure PostgreSQL Flexible Server** | бази (по една на среда) |
| **ArgoCD** | GitOps реконсилиация |
| **Helm** | пакетиране на манифести |
| **Kyverno** | admission политики (verify подпис) |

---

## 7. Как да обясниш проекта за 30 секунди

> „Имам банково приложение от два сервиза в Kubernetes, но дипломата не е за
> приложението — за **сигурната верига за доставка** е. Всеки commit минава през
> три скенера (тайни, опасен код, уязвимости), които блокират merge при нов проблем.
> След merge CI build-ва образа, **подписва** го с Cosign, прави **SBOM** и **SLSA
> provenance**, и автоматично деплойва в dev през GitOps (ArgoCD). При допускане в
> клъстера **Kyverno** проверява подписа — неподписан или подменен образ се отказва.
> test и prod се промотират ръчно с човешко одобрение. Целта е **SLSA Level 2**."
