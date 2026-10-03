---
title: Comprehensions
minutes: 8
practice: arrays, hash-map, matrix
---
A **comprehension** builds a list, set, or dictionary in a single readable expression.

## List comprehensions

The pattern is `[expression for item in iterable]`:

```pyrun
squares = [x * x for x in range(1, 6)]
print(squares)

# the same thing with a loop:
squares = []
for x in range(1, 6):
    squares.append(x * x)
print(squares)
```

Add a filter with `if` at the end:

```pyrun
nums = [4, -2, 7, 0, -9, 3]
positives = [n for n in nums if n > 0]
print(positives)
words = ["Python", "is", "a", "joy"]
print([w.upper() for w in words if len(w) > 2])
```

Use `if/else` **before** the `for` to transform every item:

```pyrun
nums = [1, 2, 3, 4, 5]
print(["even" if n % 2 == 0 else "odd" for n in nums])
```

## Dict and set comprehensions

```pyrun
words = ["apple", "kiwi", "banana"]
lengths = {w: len(w) for w in words}
print(lengths)

first_letters = {w[0] for w in words}
print(first_letters)

prices = {"tea": 2.5, "cake": 4.0, "water": 1.0}
cheap = {item: p for item, p in prices.items() if p < 3}
print(cheap)
```

## Grids the right way

```pyrun
rows, cols = 3, 4
grid = [[0] * cols for _ in range(rows)]   # independent rows
grid[0][0] = 9
print(grid)

table = [[r * c for c in range(1, 4)] for r in range(1, 4)]
for row in table:
    print(*row)
```

## Generator expressions

Swap the brackets for parentheses (or pass directly to a function) and Python produces items **one at a time** without building a list, which saves memory:

```pyrun
print(sum(x * x for x in range(1_000_000)))
print(any(n < 0 for n in [3, 5, -1]))
print(all(len(w) > 1 for w in ["hi", "yo"]))
```

> **Readability rule:** if a comprehension needs more than one `for` and an `if`, a normal loop is usually clearer.

<!-- quiz -->

Q: What does `[n * 2 for n in range(4) if n % 2 == 1]` produce?
- [ ] `[0, 2, 4, 6]`
- [x] `[2, 6]`
- [ ] `[1, 3]`
- [ ] `[2, 4, 6]`
> `range(4)` is 0–3; only the odd values 1 and 3 pass the filter, and doubled they give 2 and 6.

Q: What is `{c: c * 2 for c in "ab"}`?
- [ ] `["aa", "bb"]`
- [x] `{"a": "aa", "b": "bb"}`
- [ ] `{"aa", "bb"}`
- [ ] `{"a": 2, "b": 2}`
> A dict comprehension produces `key: value` pairs; `"a" * 2` is `"aa"`.

Q: Why use `[[0] * 3 for _ in range(3)]` instead of `[[0] * 3] * 3`?
- [ ] It's shorter
- [x] The second version repeats the same row object, so changing one row changes all of them
- [ ] The second version is a syntax error
- [ ] The first version is faster to type
> Multiplying a list of lists copies references. The comprehension creates a fresh row each time.
