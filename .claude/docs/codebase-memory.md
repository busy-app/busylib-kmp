# Codebase Navigation

To navigate the codebase and project files you must use `codebase-memory-mcp`.

## Keep the index up-to-date

Before navigating the codebase you must update the project index so the knowledge graph reflects the current state of the code:

1. Check the project with `index_status` (run `list_projects` if you are not sure the project is registered).
2. If the project is not indexed yet, or the working tree has changed since the last indexing (new branch, fresh pull, local edits), run `index_repository` first.
3. Use `detect_changes` to verify whether re-indexing is needed instead of guessing.

Never explore stale graph data — re-index first, then navigate.

## Navigation tools

Always prefer `codebase-memory-mcp` tools over plain `ls`/`grep`/`find` for code exploration:

- `search_graph` — find functions, classes, and routes by name pattern, label, or qualified-name pattern.
- `trace_path` — trace call chains and data flow (`mode=calls|data_flow|cross_service`).
- `get_code_snippet` — read the exact source of a symbol by its qualified name.
- `query_graph` — complex Cypher queries over the code graph.
- `get_architecture` — project structure overview.
- `search_code` — graph-augmented text search across the codebase.

`Grep`/`Glob`/`Read` are acceptable only for non-code files (configs, docs, resources) and for reading a file before editing it.
