## Code ordering rules

When writing or modifying code, order declarations by dependency, not by call-site convenience.

Required order:

1. `val` / `var` declarations, constants, configuration, and shared state must be placed at the top of the file or scope, before functions.
2. Private helper functions must be placed above the functions that use them.
3. If function `A` calls function `B`, then `B` must appear before `A`.
4. Lower-level utilities should appear before higher-level orchestration functions.
5. Do not place dependent helper functions at the bottom of the file.
6. Do not append private functions after public functions when those public functions depend on them.
7. Preserve this ordering when refactoring existing code.
8. `companion object` should be at bottom
