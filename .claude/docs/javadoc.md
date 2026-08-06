# CLAUDE.md

## Code documentation (KDoc)

This is a Kotlin project. Documentation is written in KDoc format. The core principle: **a comment exists only if it adds information not already present in the signature**. If the signature says everything on its own — no comment at all.

### Mandatory self-check before writing

Before writing each KDoc element, ask yourself the question and write the element only if the answer is "yes":

| Element | Question to ask yourself |
|---|---|
| Summary | "Will the reader learn anything from the first sentence that isn't already in the function name and types?" |
| `@param` | "Could a reader fail to understand why this parameter exists, what its constraints are, or its non-obvious semantics?" |
| `@return` | "Is it non-obvious from the name and type what exactly is returned (especially in edge cases: null, empty collection, -1)?" |
| `@throws` | "Does the function throw an exception the caller is required to handle or account for?" |
| `@sample` / example | "Is the usage so non-obvious that omitting an example would lead to mistakes?" |

If the answer is "no" — the element is **not written**. A partially documented function (only `@throws`, no summary and no `@param`) is normal and correct.

### What to write

- The first sentence — the essence in one line. Beyond that — only the non-obvious: contract, side effects, invariants, thread safety, units of measurement, behavior on null/empty values.
- **Why**, not **what**. The code shows "what"; the comment explains "why" and "under which conditions".
- Units and ranges always explicit: `timeoutMs`, "in the range 0..1", "UTC".
- `@param` describes semantics and constraints, never a restatement of the name.

### What is forbidden

- Filler and signature restatement: "This method is responsible for...", "This function allows you to...", "Returns the result of the operation".
- `@param userId the identifier of the user` (restates the name — delete it).
- `@return the returned value`, `@return the result`.
- Comments on trivial getters, data classes with self-explanatory fields, or overrides that don't change the contract.
- Summaries longer than 3–4 sentences. If it doesn't fit — that's a signal the function does too much, not that a longer comment is needed.
- Javadoc-style HTML tags (`<p>`, `<code>`). In KDoc use Markdown and `[ClassName]`-style links.

### Examples

**Bad** (verbose, zero information):

```kotlin
/**
 * This method is intended for retrieving a user by their identifier.
 * The method takes a user identifier and returns the user object.
 *
 * @param userId the identifier of the user
 * @return the user object
 */
fun getUser(userId: Long): User
```

**Good** (only what the signature doesn't say):

```kotlin
/**
 * @throws UserNotFoundException if the user is deleted or does not exist
 */
fun getUser(userId: Long): User
```

**Bad**:

```kotlin
/**
 * Function for calculating a discount. Takes a price and a rate and returns the discount.
 */
fun calculateDiscount(price: BigDecimal, rate: Double): BigDecimal
```

**Good** (semantics and constraints not expressed by the types):

```kotlin
/**
 * Calculates the discount amount, excluding promotional items.
 *
 * @param rate discount as a fraction in the range 0.0..1.0, not a percentage
 * @return amount rounded to cents using HALF_UP
 */
fun calculateDiscount(price: BigDecimal, rate: Double): BigDecimal
```

**Good** (no comment, because none is needed):

```kotlin
fun isEmpty(): Boolean = items.isEmpty()
```

### Final review

After writing a KDoc, reread it and delete every line for which you cannot answer "yes" to: "Will this line save the reader a trip into the function body or a debugging session?"