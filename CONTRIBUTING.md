# Contributing to Canvas Launcher

Thank you for your interest in improving Canvas Launcher.

We welcome bug fixes, UX polish, tests, documentation improvements, and new features aligned with the project vision: an infinite, fast, and intuitive Android home screen.

## Ground Rules
- Be respectful and constructive.
- Keep pull requests focused and reviewable.
- Add or update tests when changing behavior.
- Explain the "why" in PR descriptions, not only the "what".

## Before You Start
1. Read the [Code of Conduct](CODE_OF_CONDUCT.md).
2. Check open issues and existing PRs to avoid duplicate work.
3. If the change is large, open an issue first to align on approach.

## Local Setup
1. Fork and clone the repository.
2. Open it in Android Studio.
3. Ensure you have Android SDK 26+ and JDK 21.
4. Build the project:

```bash
# macOS / Linux
./gradlew :app:assembleDebug

# Windows PowerShell
.\gradlew.bat :app:assembleDebug
```

## Running Tests
```bash
# macOS / Linux
./gradlew :domain:test :core:model:test :core:performance:test :core:settings:test :feature:canvas:test :feature:launcher:test :app:testDebugUnitTest

# Windows PowerShell
.\gradlew.bat :domain:test :core:model:test :core:performance:test :core:settings:test :feature:canvas:test :feature:launcher:test :app:testDebugUnitTest
```

## Branch and Commit Style
- Branch naming suggestion: `feat/<short-name>`, `fix/<short-name>`, `docs/<short-name>`.
- Commit messages should be clear and imperative.
- Conventional Commits are recommended (for example `feat:`, `fix:`, `docs:`, `test:`), but not required.

## Pull Request Checklist
- [ ] I linked the issue (if applicable).
- [ ] I tested my changes locally.
- [ ] I added or updated tests for behavior changes.
- [ ] I updated docs/README when needed.
- [ ] I kept the PR scope focused.

## Areas Where Contributions Are Especially Valuable
- Canvas gesture tuning and interaction smoothness.
- Accessibility improvements in Compose UI.
- Additional layout strategies and layout controls.
- Performance profiling and benchmark automation.
- Localization and copy quality improvements.
- Test coverage for edge cases in edit and layout flows.

## Security
For security-sensitive reports, do not open a public issue.

Please follow the process in [SECURITY.md](SECURITY.md).
