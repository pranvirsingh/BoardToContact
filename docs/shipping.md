# Shipping

Owner says **"Ship `vX.Y.Z`"** — that phrase alone authorizes the whole
pipeline, zero per-step confirmation:

1. Work happens on `feat/bt-<line>` (never directly on `main`).
2. Stage specific files only — never `git add -A` blindly.
3. Commit as `Imp : <one line>` (new things) or `Fix : <one line>` (repairs).
4. Push branch → open PR (short, plain-language title + a few bullets).
5. CI must go green (`gh pr checks --watch`) before merging.
6. Merge with `gh pr merge --merge` — true merge commit, never squash/rebase.
7. Tag on `main`, push tag, publish the GitHub release with the APK.

Outside a "Ship" moment, git state is read-only — no speculative commits.
