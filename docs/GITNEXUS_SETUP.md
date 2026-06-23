# GitNexus setup for the Minecraft workspace template

GitNexus is used to help Codex App, Codex CLI and GitHub Copilot understand the codebase with fewer unnecessary file reads.

## Installed mode

This workspace uses:

- **global GitNexus install**: `gitnexus mcp`

Installed version: **1.6.7**

## Re-index the workspace

Run from the workspace root:

```powershell
gitnexus analyze --skip-git --index-only --name minecraft-workspace-template
gitnexus list
gitnexus status
```

Use `--skip-git` because this template can be copied before it becomes a Git repository. Use `--index-only` to prevent GitNexus from injecting generic skills or changing `AGENTS.md` automatically.

Fallback (if global install is unavailable):

```powershell
npx -y gitnexus@latest analyze --skip-git --index-only --name minecraft-workspace-template
npx -y gitnexus@latest list
npx -y gitnexus@latest status
```

## Codex MCP

Workspace config:

```text
.codex/config.toml
```

Check in Codex CLI:

```powershell
codex mcp list
```

Inside Codex TUI:

```text
/mcp
```

## GitHub Copilot MCP

Workspace config:

```text
.vscode/mcp.json
```

In VS Code:

1. Open Command Palette.
2. Run `MCP: List Servers`.
3. Start `gitnexus`.
4. Open Copilot Chat.
5. Select Agent Mode.
6. Enable GitNexus tools if needed.

## Agent rule

Before reading many files manually, agents should use GitNexus to narrow the relevant files, classes, symbols and dependency impact. If the index is missing, run the index-only command above instead of generating new skills by default.
