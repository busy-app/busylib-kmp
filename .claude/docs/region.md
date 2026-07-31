## Code region rules

Do not add region markers or artificial section delimiters.

Forbidden examples:

```ts
// region
// endregion
// #region
// #endregion
// --------
// ========
// === Helpers ===
```

Do not add decorative comments, separator comments, or “section header” comments unless they already exist in the file and are part of the established style.

Prefer simple, readable code structure instead:

* use small functions
* use meaningful names
* group related logic naturally
* rely on file/module structure instead of comment regions

Comments should explain non-obvious behavior, not organize the file visually.

Before editing a file, follow the style already used in that file. If the file does not use region markers, do not introduce them.
