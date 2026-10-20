# App Design & Quality Rules for AI Assistants
Before any code change: read README and current architecture; identify user goals, problems, features to preserve and regression risks. Critique proposed features instead of automatically agreeing; prefer usable, simple designs to extra controls. Compare relevant real-world UI patterns (Mobbin if accessible), apply official Android Material 3 and adaptive design guidance, and optionally build editable Figma designs if connected. Never claim external services are connected without confirmation.
## Interface requirements
Use clear visual hierarchy, consistent typography, spacing and contrast; accessible labels and roughly 48dp touch targets on Android. Account for status/navigation bar insets, keyboard/IME, scrolling, landscape/portrait, tablets and font scaling. Keep primary actions discoverable. Show honest loading, empty, error, offline, permission-denied and retry states. Do not autoplay or change behaviors unexpectedly.
## Function and safety requirements
Preserve all existing features, state, storage, preferences and app identity unless explicitly requested. Maintain correct Android update signing/versioning and provide migrations for breaking changes. Never commit secrets or private user data. Confirm destructive actions. Prefer small targeted changes over rewrites.
## Verification
For every UI or feature update, audit navigation, system bars, keyboard, rotation, resize, accessibility, permissions, cancellation, offline failure, persistence, and relevant regressions. Run available builds/tests; report actual results and anything untested. Never say "fully working" without evidence. Provide changed files and reasons.
## Design references
- Google Material 3: https://m3.material.io/
- Android app quality: https://developer.android.com/docs/quality-guidelines/core-app-quality
- Android accessibility: https://developer.android.com/guide/topics/ui/accessibility
- NN/g usability heuristics: https://www.nngroup.com/articles/ten-usability-heuristics/
- Mobbin (optional, examples not guaranteed best practices): https://mobbin.com/
- Figma (optional visual design): https://www.figma.com/
- Context7 (optional code documentation): https://context7.com/
See docs/APP_DESIGN_QUALITY.md.

## NON-NEGOTIABLE APK UPGRADE SIGNING RULE
For every distributed Android APK, follow docs/ANDROID_SIGNING.md. Preserve applicationId and existing user data, strictly increment versionCode, reuse the app's persistent PRIVATE signing key from GitHub Actions Secrets, verify its pinned SHA-256 certificate fingerprint, and fail publication on absent or mismatched signing settings. NEVER publish APKs signed by a disposable runner-generated debug keystore as upgradeable builds. Do not commit/expose a signing key/password. Do not claim seamless upgrades without testing installed-old to installed-new; an APK public certificate cannot recover the private key.
