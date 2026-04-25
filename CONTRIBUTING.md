# Contributing to Open Jarvis

## Ways to Contribute

- Report bugs via GitHub Issues
- Submit skill files (JSON) for common tasks
- Add AI app profiles to AIAppMeta.kt
- Add known package classifications to AppAnalyzer
- Translate README to other languages
- Test on different Android versions and devices
- Submit provider implementations for new LLM APIs

## Adding a Skill

Create a JSON file following the schema in docs/SKILLS.md
Test it works on at least 2 devices
Submit as a PR to skills/ folder

## Adding a Provider

Implement LLMProvider interface
See docs/PROVIDERS.md for full guide
Include a test for the provider

## Code Style

- Kotlin official style guide
- No !! operator — use safe calls and elvis
- All coroutines in appropriate scope (never GlobalScope)
- All strings in strings.xml
- Add proguard rules for new classes

## Pull Request Process

1. Fork the repo
2. Create feature branch
3. Make changes with tests
4. Update docs if needed
5. Submit PR with description
6. Wait for review

## Commit Messages

Use conventional commits:
- feat: new feature
- fix: bug fix
- docs: documentation
- refactor: code refactor
- test: adding tests

## Code of Conduct

Be respectful. We're building tools for users, not creating content.

## Getting Help

- Discord: (link in README)
- GitHub Discussions
- IRC: #openjarvis on LIBERA