# ZhiXing - Campus Navigation

Java course design project for campus intelligent route planning.

## 1. Development Environment

- JDK: `21` (installed), code is compatible with Java 8 syntax
- Build tool:
  - Preferred: Maven (`pom.xml` is ready)
  - Fallback: PowerShell scripts (`scripts/*.ps1`) using `javac` directly

## 2. Project Structure

```text
.
├─ docs/
├─ scripts/
├─ src/
│  ├─ main/java/com/zhixing/navigation/
│  │  ├─ app/
│  │  └─ domain/
│  │     ├─ graph/
│  │     └─ model/
│  └─ test/java/com/zhixing/navigation/
└─ pom.xml
```

## 3. Local Commands (No Maven Required)

```powershell
.\scripts\build.ps1
.\scripts\run.ps1
.\scripts\run-gui.ps1
.\scripts\qa.ps1
```

## 4. Data Persistence

- Data directory: `data/`
- Core files: `vertex.json`, `edge.json`, `user.json`
- `user.json` stores hashed passwords (`sha256$...`), not plaintext.
- If files are missing/corrupted, the system auto-falls back to default data on startup.
- Optional env var: `ZHIXING_DATA_DIR` (use isolated data dir for tests)

## 5. Maven Commands (When Maven Is Installed)

```powershell
mvn clean test
mvn -q exec:java -Dexec.mainClass=com.zhixing.navigation.app.Main
```
