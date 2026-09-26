# Origine des skills installés

Skills copiés depuis des dépôts open source (format Agent Skills, agentskills.io), pour usage dans ERP_360.

## Android / Kotlin / Compose (lot initial)

| Dossier | Source | Auteur |
|---|---|---|
| compose-state-and-effects, compose-performance, compose-component-design, compose-animations | github.com/chrisbanes/skills | Chris Banes |
| kotlin-concurrency-and-flow, kotlin-api-design, kotlin-control-flow, gradle-run | github.com/chrisbanes/skills | Chris Banes |
| compose-ui-testing-patterns | github.com/chrisbanes/skills | Chris Banes |
| android-architecture, compose-ui, android-viewmodel, android-data-layer | github.com/gecko23/android-agent-skills | gecko23 |
| android-testing, kotlin-concurrency-expert, compose-performance-audit | github.com/gecko23/android-agent-skills | gecko23 |
| compose-navigation, android-accessibility, android-coroutines, gradle-build-performance | github.com/gecko23/android-agent-skills | gecko23 |
| claude-android-ninja | github.com/Drjacky/claude-android-ninja | Drjacky |
| skill-creator | github.com/anthropics/skills | Anthropic |

## Design / UI-UX (lot « mieux travailler les interfaces »)

| Dossier | Source | Auteur |
|---|---|---|
| frontend-design, theme-factory, brand-guidelines, canvas-design, docx, pdf, pptx, xlsx | github.com/anthropics/skills | Anthropic |
| audit-ai-design-slop, design-first-ui-prompting, no-ai-design-slop | github.com/MengTo/Skills (catégorie `ui/`) | MengTo |
| accessibility-testing, logo-design, user-flow-mapping, wireframing | github.com/seb1n/awesome-ai-agent-skills (catégorie `design-and-ui-ux/`) | community |
| ui-ux-pro-max, design-system, design, ui-styling, brand, banner-design, slides | github.com/nextlevelbuilder/ui-ux-pro-max-skill | nextlevelbuilder |
| design-consultation, design-review, design-shotgun, devex-review | github.com/garrytan/gstack (skills design standalone) | Garry Tan |
| taste-skill, minimalist-skill, brutalist-skill, soft-skill, redesign-skill, output-skill, stitch-skill, image-to-code-skill, brandkit | github.com/Leonxlnx/taste-skill | Leonxlnx |
| api-and-interface-design, browser-testing-with-devtools, ci-cd-and-automation, code-review-and-quality, code-simplification, constraint-driven-development, context-engineering, debugging-and-error-recovery, deprecation-and-migration, documentation-and-adrs, doubt-driven-development, frontend-ui-engineering, git-workflow-and-versioning, idea-refine, incremental-implementation, interview-me, observability-and-instrumentation, performance-optimization, planning-and-task-breakdown, security-and-hardening, shipping-and-launch, source-driven-development, spec-driven-development, test-driven-development, using-agent-skills | github.com/addyosmani/agent-skills | Addy Osmani |
| brainstorming, diagnosing-superpowers, dispatching-parallel-agents, executing-plans, finishing-a-development-branch, receiving-code-review, requesting-code-review, subagent-driven-development, systematic-debugging, superpowers-test-driven-development, using-git-worktrees, using-superpowers, verification-before-completion, writing-plans, writing-skills | github.com/obra/superpowers | Jesse Vincent |
| design-references/ (liste curatée de 28+ DESIGN.md, 9 familles esthétiques, aperçus) + gstack-design-doctrine.md | github.com/rohitg00/awesome-claude-design + github.com/garrytan/gstack (DESIGN.md) | rohitg00 / Garry Tan |

Notes :
- `frontend-design` de seb1n non repris : doublon de `frontend-design` officiel (Anthropic), déjà plus complet.
- `test-driven-development` de superpowers renommé `superpowers-test-driven-development` pour éviter le doublon avec Addy Osmani.
- La skill `figma` citée dans l'article n'existe plus dans `anthropics/skills` (repo mis à jour) ; les équivalents installés sont `image-to-code-skill` (Leonxlnx), `stitch-skill` et `canvas-design` (Anthropic).
- MengTo/Skills : seuls les 3 skills de la catégorie `ui/` sont portables ici ; les ~120 autres sont spécifiques web (GSAP, Three.js, WebGL, Tailwind) — source : github.com/MengTo/Skills.
- gstack : le skill racine est un routeur qui exige l'outillage complet (setup, bin/, lib/) — seule la famille design a été extraite. Setup complet : github.com/garrytan/gstack.
- Répertoires annuaires consultés pour la curation : github.com/VoltAgent/awesome-agent-skills (1000+ skills, index), github.com/ComposioHQ/awesome-claude-skills (liste curatée Creative & Media), github.com/anthropics/skills (spec officielle des skills dans `spec/`).

Utilisation : dans une session, demandez « applique le skill <nom> » à l'agent.
