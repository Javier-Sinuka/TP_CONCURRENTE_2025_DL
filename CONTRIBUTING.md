# Contributing Guide – Petri-Net Simulator

Welcome 👋 and thanks for helping us build the Petri-net CLI!  
Please read this short guide **before** opening a pull-request.

---

## 1 . Branching model – GitFlow

```

main        ← always production-ready
dev         ← integration branch for the upcoming release
feature/*   ← new code (one per ticket or user-story)
hotfix/*    ← urgent patches to main
release/*   ← pre-release hardening branches

````

* **Never** commit directly to `main` or `dev`.  
* **One feature, one branch, one PR**.
* Delete your local & remote feature branch after merge.

### Naming tips

| Purpose      | Branch prefix | Example                      |
|--------------|---------------|------------------------------|
| New feature  | `feature/`    | `feature/incidence-matrix`   |
| Bug fix      | `hotfix/`     | `hotfix/null-pointer-#42`    |
| Release prep | `release/`    | `release/v1.0.0-rc`          |

---

## 2 . Building the project

### Linux / macOS

```bash
git clone https://github.com/Javier-Sinuka/TP_CONCURRENTE_2025_DL.git
cd TP_CONCURRENTE_2025_DL
./mvnw spotless:apply                     # auto-format to Google Java Style
./mvnw clean install                      # compile, test, and package the application
java -jar target/petri-sim-1.0.jar --help # run the application
````

### Windows (Command Prompt or PowerShell)

```powershell
git clone https://github.com/Javier-Sinuka/TP_CONCURRENTE_2025_DL.git
cd TP_CONCURRENTE_2025_DL
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean install
java -jar target/petri-sim-1.0.jar --help
```

*Requires JDK 8 (set `JAVA_HOME`) — no separate Maven install needed.*

---

## 3 . Pull-request checklist ✅

Before opening or merging a PR, make sure:

* [ ] Branch is up-to-date with `dev` (`git pull --rebase origin dev`).
* [ ] `./mvnw spotless:apply && ./mvnw clean install` succeeds without changes.
* [ ] **Javadoc comments for every public class, method, and field**.
* [ ] PR description links the related GitHub Issue (`Fixes #123`).
* [ ] All *new* files include the Apache 2.0 header (see other files).
* [ ] No `target/`, IDE folders, or OS junk included.

### When to open a PR

| Task size        | PR timing                                                                                         |
| ---------------- | ------------------------------------------------------------------------------------------------- |
| ≤ 1 day of work  | Open the PR **same day**, even if draft — early feedback matters.                                 |
| Ongoing > 1 day  | Create a **draft PR** once the branch compiles; mark “Ready for review” when feature is finished. |
| Hotfix to `main` | Branch from `main` → PR back to `main`, then merge down into `dev`.                               |

---

## 4 . Coding standards

1. **Google Java Style** — enforced by Spotless; run `spotless:apply` before every commit.

2. **Javadoc = required**

   ```java
   /**
    * Calculates the enable vector.
    *
    * @param marking current marking vector
    * @return vector of enabled transitions
    */
   public boolean[] calculateEnableVector(int[] marking) { … }
   ```

3. No magic numbers: use `static final` constants.

4. Follow the **Conventional Commits** standard for commit messages:

   ```
   <type>(<scope>): <short summary>

   [optional body]

   [optional footer(s)]
   ```

   Examples:
   ```
   feat(cli): add new command for matrix generation
   fix(parser): handle null pointer exception (#42)
   docs(README): improve documentation for build steps
   ```

   Common types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.  
   Use issue references in the footer (e.g., `Fixes #123`).

---

##  5 . Daily Programming Workflow

Follow this 9-step loop to stay in sync, keep history clean, and minimise merge pain.

| #                                              | Command(s)                                                            | Why                                                          |
| ---------------------------------------------- | --------------------------------------------------------------------- | ------------------------------------------------------------ |
| **1. Sync**                                    | `git switch dev`<br>`git pull --ff-only`                              | Start from the latest integration branch.                    |
| **2. Create / pick issue**                     | Assign yourself on the Project board                                  | One owner avoids overlaps.                                   |
| **3. Branch off**                              | `git switch -c feature/<slug>`                                        | New branch isolated from others.                             |
| **4. Code + build loop**                       | `./mvnw spotless:apply`<br>`./mvnw clean install`                     | Format, compile, repeat—no IDE surprises.                    |
| **5. Commit early & often**                    | `git add <files>`<br>`git commit -m "core: calc enable vector (#45)"` | Small commits help reviews & bisects.                        |
| **6. Push regularly**                          | `git push -u origin feature/<slug>`                                   | Remote backup + enables early PR review.                     |
| **7. Draft PR**                                | GitHub “Create draft pull request”                                    | Reviewer can comment while you finish coding.                |
| **8. Rebase if dev moved**                     | `git fetch origin`<br>`git rebase origin/dev`                         | Keeps history linear; resolve conflicts locally.             |
| **9. Mark PR “Ready” → Review → Squash-merge** | GitHub UI                                                             | Merge into `dev`, then delete branch locally & remotely.     |


---

## 6 . Contact

Questions or blockers?
Open a GitHub Discussion or send a message through the `#prog-concurrente` channel on Discord.

---

Happy coding & may all your transitions fire 🎇
