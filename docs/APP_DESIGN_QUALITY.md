# Design & Quality Review Checklist
This checklist guides future AI-assisted edits. It does not redesign the current app or connect paid services.
## Before coding
1. Define the primary user task, existing pain points and measurable success criteria.
2. Audit the current screens and user flows. Identify existing behavior that cannot regress.
3. Review comparable UI patterns, official Material 3 guidance and accessibility standards; consider at least two approaches and explain trade-offs.
4. Choose the least complex accessible solution and list files/flows affected.
## Acceptance checks
- Clear primary action, logical navigation/back, consistent visual hierarchy.
- Readable text, sufficient contrast, accessible labels and touch targets around 48dp.
- Correct safe-area/navigation/status-bar insets, keyboard movement, rotation, tablets and large-text handling.
- Meaningful progress, empty, permission denied, offline, error, retry and cancellation.
- Settings, data, navigation and playback/session states persist as intended after background, reboot/update and rotation.
- No regressions, secrets exposure or app identity/signature changes.
- Run build, tests and lint where available; verify on device/emulator for screen-size assertions. Report any untested cases.
## Review log
|Area|Finding|Severity|Proposed fix|Evidence|Verified?|
|---|---|---|---|---|---|
|Visual hierarchy||||||
|Navigation and insets||||||
|Keyboard and rotation||||||
|Accessibility||||||
|Failures and permissions||||||
|Persistence and updates||||||
|Privacy and security||||||
## References
Android Material Design: https://m3.material.io/
Android quality: https://developer.android.com/docs/quality-guidelines/core-app-quality
Accessibility: https://developer.android.com/guide/topics/ui/accessibility
Nielsen Norman Group: https://www.nngroup.com/articles/ten-usability-heuristics/
Mobbin: https://mobbin.com/
Figma: https://www.figma.com/
Context7: https://context7.com/
Optional services are not connected merely by listing a URL.