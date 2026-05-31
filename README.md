# Защита на софтуерната верига за доставка

DevSecOps реализация, която защитава софтуерната верига за доставка на
контейнеризирано банково приложение в Kubernetes. Бакалавърска дипломна работа,
ТУ София, специалност Киберсигурност.

Това хранилище съдържа изходния код на сервизите, техните Dockerfile-и и CI/CD
процеса в GitHub Actions. Акцентът е върху сигурната доставка (сканиране,
подписване, SBOM, provenance, контролиран GitOps деплой), а не върху бизнес
логиката на приложението. Целевото ниво на интегритет на веригата е SLSA Level 2.

## Трите хранилища

| Хранилище | Отговорност |
|---|---|
| `diplomna-rabota` (това) | Изходен код на сервизите, Dockerfile-и, GitHub Actions CI/CD |
| `diplomna-rabota-infra` | Terraform за Azure foundation и AKS клъстера |
| `diplomna-rabota-gitops` | Желаното състояние на клъстера: Helm charts, per-env values, ArgoCD приложения, Kyverno политики |

## Сервизи

- `bank-service` (Spring Boot, Java 25, една PostgreSQL база). JWT автентикация:
  httpOnly cookie за браузъра и bearer header за API клиенти.
- `fraud-detection` (Python, FastAPI). Без състояние, без база. Правило-базирана
  проверка на превод преди изпълнението му.
- `frontend` (React 19, Vite, TypeScript, Tailwind v4, shadcn). Сервира се от
  nginx като non-root и reverse-proxy-ва `/api` към bank-service.

Трите образа се изграждат от пинати официални базови образи, въртят като non-root
с read-only root filesystem и се деплойват през собствени Helm charts.

## CI/CD поток

Три сервизни workflow-а (`bank-service-ci.yml`, `fraud-detection-ci.yml`,
`frontend-ci.yml`) с еднаква структура и различен build stack (Maven, pip, Node).
Отделен `repo-security.yml` пуска скенерите върху цялото хранилище.

**При Pull Request към `main`:**
- `changes` (paths-filter) открива кои сервизи са засегнати.
- За всеки засегнат сервиз: `build-test` (компилация и тестове) и `image` (build
  само за валидация, че Dockerfile-ът работи). Образът не се публикува на PR.
- `repo-security` винаги пуска Gitleaks, Semgrep и Trivy върху цялото хранилище и
  качва резултатите в GitHub Code Scanning.
- Резултатите от скановете решават дали merge е разрешен.

**При push към `main` (след merge):**
- За засегнатия сервиз: `image` изгражда образа, публикува го в `ghcr.io`,
  подписва го с Cosign, генерира SBOM (Syft) и SLSA provenance.
- `deploy-dev` обновява dev средата в gitops хранилището.

## Сигурностни сканове

| Скенер | Какво хваща |
|---|---|
| Gitleaks | Тайни (hardcoded secrets) |
| Semgrep | Опасен код (SAST), например SQL injection |
| Trivy | Уязвими зависимости (SCA) и проблеми в Dockerfile / IaC |

И трите се пускат върху цялото хранилище на всеки PR и качват резултати в Code
Scanning. Блокирането е diff-aware: преценява се спрямо вече записаното на `main`,
така че стара уязвимост в съседен код не блокира несвързан PR.

Блокира при нов проблем, въведен в самия PR: нов secret, нов опасен код, нова
уязвима зависимост (High/Critical с налична поправка) или провален build. Стар
проблем, който вече е на `main`, се вижда в Security tab, но не блокира.

Тайните се хващат на три нива: локално преди commit (`pre-commit` + gitleaks),
при push (GitHub Push Protection отказва самия `git push`) и в CI (Gitleaks job
блокира merge и записва находката в Security tab). Активиране на локалната
проверка веднъж след клониране: `pre-commit install`.

## Supply chain сигурност

- Подписване: Cosign keyless (Sigstore, OIDC към Fulcio към Rekor). Подписът е по
  digest.
- SBOM: Syft в CycloneDX формат, закачен към образа като Cosign attestation и
  качен като artifact.
- Provenance: `slsa-github-generator`, което постига SLSA Level 2.
- Admission: Kyverno налага две политики на образите в `dev`, `test` и `prod`:
  - `verify-image-signatures` изисква валиден Cosign подпис (workflow самоличност,
    issuer и Rekor), плюс SLSA provenance attestation (подписана от
    `slsa-github-generator`), плюс CycloneDX SBOM attestation (подписана от CI).
    Липсва ли подпис или attestation, образът се отказва.
  - `restrict-image-registries` допуска само образи от `ghcr.io/svetlioo/*`.
    verify-images проверява само съвпадащите образи, затова рестрикцията на
    registry е отделен контрол.

## Деплой (GitOps)

Клъстерът (AKS) има среди `dev`, `test` и `prod`, реконсилирани от ArgoCD от
gitops хранилището.

- Деплойва се само засегнатият сервиз. Промяна в два сервиза изисква два отделни
  PR-а, по един за всеки.
- Деплойте са сериализирани (обща `concurrency` група), за да чете всеки
  най-новия `main` без конфликти.
- `dev` е автоматично: CI отваря и авто-merge-ва PR в gitops с новия образ.
- `test` и `prod` са ръчни: Promote workflow в gitops отваря PR, който човек
  одобрява (separation of duties). prod винаги взима test-валидирания образ.

Всяка среда пинва едновременно tag (четимост) и digest (неизменност).

## Настройка на хранилището (еднократно)

- Branch ruleset на `main`: изисквай PR, status checks и code scanning results
  (Trivy и Semgrep, праг High or higher), блокирай force push.
- Secret scanning и Push protection (безплатно за публични хранилища).
- Secret `GITOPS_TOKEN` (fine-grained PAT, Contents и Pull requests write върху
  gitops) в двете хранилища: в `diplomna-rabota` за dev авто-деплоя и в
  `diplomna-rabota-gitops` за Promote workflow-а. PR, отворен с `GITHUB_TOKEN`, не
  тригерира checks, затова промоцията ползва PAT.
- В gitops хранилището разреши на GitHub Actions да създава PR-и.

## Лиценз

[Apache License 2.0](LICENSE)
