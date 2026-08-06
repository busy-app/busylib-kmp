# Source Code Navigation

### Kotlin project source files → `kotlin-lsp` via the LSP tool
Use the built-in **LSP** tool (backed by `kotlin-lsp`) to navigate and inspect Kotlin source files that
are part of this project. The server supports `--stdio` and `--socket` modes — the LSP tool handles
the connection automatically, so never hardcode a socket address:
- Go-to-definition, find references, hover/type info, symbol search.
- Do **not** use `grep`/`find`/`cat` as a substitute when the LSP can answer the question directly.

### Dependency (`.jar`) sources → `ksrc`
Use the **`ksrc`** CLI to read and search source code that lives inside Gradle dependency `.jar` files:

```
# Search for a symbol/snippet across dependency sources
ksrc search <pattern> [--module <:subproject>] [--artifact <group:artifact>]

# Read a specific file by its file-id (returned by `ksrc search`)
ksrc cat <group:artifact:version!/path/inside/jar.kt>

# List resolved dependencies and check source availability
ksrc deps [--module <:subproject>]

# Ensure sources are downloaded for a coordinate
ksrc fetch <group:artifact:version>
```

**Never** use `jar tf`, `jar xf`, `unzip`, or similar shell commands to inspect `.jar` contents —
`ksrc` handles caching, source attachment, and provides a clean API for this.

### Searching for dependency API shapes
When you need to understand an external library's API:
1. Try `ksrc search` first for a symbol or snippet.
2. Use `ksrc cat` with the returned file-id to read the full source.
3. Fall back to `ksrc deps` if you're not sure which artifact contains the symbol.
