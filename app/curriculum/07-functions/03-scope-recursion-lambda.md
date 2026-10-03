---
title: Scope, Recursion, and Lambdas
minutes: 10
practice: recursion, backtracking, dynamic-programming
---
## Scope: where names live

Variables created inside a function are **local**. They exist only during that call:

```pyrun
def make_message():
    message = "local!"        # local to this function
    return message

print(make_message())
# print(message)  -> NameError: message only exists inside the function

counter = 0                   # global
def peek():
    return counter            # reading a global is fine
print(peek())
```

To *change* a global inside a function you'd need the `global` keyword, but it's usually better to pass values in and return results out.

## Recursion: a function that calls itself

A recursive function solves a problem by solving a **smaller version** of the same problem. Every recursion needs a **base case** that stops it:

```pyrun
def factorial(n):
    if n <= 1:                  # base case
        return 1
    return n * factorial(n - 1) # recursive case: a smaller problem

print(factorial(5))             # 5 * 4 * 3 * 2 * 1 = 120

def countdown(n):
    if n == 0:
        print("Go!")
        return
    print(n)
    countdown(n - 1)

countdown(3)
```

## When recursion gets slow, remember answers

Naive Fibonacci recomputes the same values exponentially many times. **Memoization** caches results:

```pyrun
from functools import lru_cache

@lru_cache(maxsize=None)
def fib(n):
    if n < 2:
        return n
    return fib(n - 1) + fib(n - 2)

print(fib(80))
```

Without `@lru_cache`, `fib(80)` would take longer than the age of the universe. This idea, storing answers to sub-problems, is the heart of **dynamic programming** (*Coin Change*, *Edit Distance*).

## The recursion limit

Python stops recursion at about 1000 levels deep with a `RecursionError`. For huge inputs, like flood-filling a 500×500 grid, use a loop with your own stack or queue instead. The *Number of Islands* exercise warns about exactly this.

## Lambdas: tiny anonymous functions

`lambda args: expression` creates a small function inline. It's perfect for `key=`:

```pyrun
points = [(3, 1), (1, 2), (2, 0)]
print(sorted(points, key=lambda p: p[1]))       # sort by y
print(max(points, key=lambda p: p[0] + p[1]))   # largest x + y
square = lambda x: x * x
print(list(map(square, [1, 2, 3])))
```

If a lambda grows beyond one simple expression, write a normal `def` instead.

<!-- quiz -->

Q: What's missing from this function?
```python
def f(n):
    return n + f(n - 1)
```
- [ ] A loop
- [x] A base case, so the recursion never stops
- [ ] A `print` statement
- [ ] A docstring
> Without a base case it recurses until Python raises `RecursionError`.

Q: What does `sorted(["bob", "Al", "cy"], key=lambda s: len(s))` return?
- [x] `["Al", "cy", "bob"]`
- [ ] `["Al", "bob", "cy"]`
- [ ] `["bob", "Al", "cy"]`
- [ ] `["cy", "Al", "bob"]`
> Sorting by length puts the two 2-letter names first, in their original order (sorting is stable), then `"bob"`.

Q: What does `@lru_cache` do for a recursive function like `fib`?
- [ ] Makes Python allow deeper recursion
- [x] Remembers results for arguments already computed, so each value is calculated once
- [ ] Runs the function in parallel
- [ ] Converts it into a loop automatically
> Caching (memoization) avoids recomputing overlapping sub-problems, turning exponential time into linear.
