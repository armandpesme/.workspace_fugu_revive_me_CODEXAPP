---
name: workspace-agent-skill-maintenance
description: Use when maintaining this workspace's local agent and skill mirrors across .agents, .codex, and .github, including AGENTS.md pointers, Codex TOML agents, GitHub-style agent markdown, skill SKILL.md files, openai.yaml metadata, and local validation.
---

# Workspace Agent Skill Maintenance

## Workflow

1. Treat `.agents/skills` as the active source and `.codex/skills` as its compatibility mirror.
2. Keep `.github/skills` empty unless the workspace convention changes.
3. When adding an agent, create the Codex TOML plus `.agents/agents/*.agent.md` and `.github/agents/*.agent.md` mirrors.
4. Update `AGENTS.md` and `.codex/config.toml` so new local skills are discoverable.
5. Keep bridge files concise: `.agents/AGENTS.md`, `.github/AGENTS.md`, and `.agents/PLAN.md` should point to root canonical files.
6. Validate new skills with the local `quick_validate.py` script when available.

## Guardrails

- Do not duplicate long instructions across root and mirrors.
- Do not introduce GitHub automation that conflicts with current human instructions.
- Prefer short, role-specific agents over broad agents that duplicate `maestro`.
