---
title: Arithmetic Operators
minutes: 7
practice: math, basics
---
Python's arithmetic operators:

| Operator | Meaning | Example | Result |
|---|---|---|---|
| `+` | add | `7 + 2` | `9` |
| `-` | subtract | `7 - 2` | `5` |
| `*` | multiply | `7 * 2` | `14` |
| `/` | divide (always a float) | `7 / 2` | `3.5` |
| `//` | floor division | `7 // 2` | `3` |
| `%` | remainder (modulo) | `7 % 2` | `1` |
| `**` | power | `7 ** 2` | `49` |

```pyrun
print(7 / 2, 7 // 2, 7 % 2)
print(8 / 2)          # 4.0, because / always gives a float
print(2 ** 10)        # 1024
print(-7 // 2)        # -4, floor division rounds DOWN, not toward zero
```

## Why `//` and `%` are so useful

`%` gives the remainder, which answers "is it divisible?" and "what's the last digit?":

```pyrun
n = 1234
print(n % 2 == 0)     # is it even?
print(n % 10)         # last digit: 4
print(n // 10)        # drop the last digit: 123

minutes = 135
print(minutes // 60, "h", minutes % 60, "min")   # 2 h 15 min
```

Pulling digits off with `% 10` and `// 10` in a loop is a classic pattern. You'll use it in the *Sum of Digits* exercise.

## Order of operations

Python follows the usual math rules: `**` first, then `* / // %`, then `+ -`. Use parentheses to make your intent obvious:

```pyrun
print(2 + 3 * 4)      # 14
print((2 + 3) * 4)    # 20
print(-2 ** 2)        # -4: ** binds tighter than the minus sign
```

## Shortcut assignment

```pyrun
score = 10
score += 5     # same as score = score + 5
score *= 2
score -= 1
print(score)   # 29
```

> **Watch out:** `^` is **not** power in Python, it's bitwise XOR. `2 ^ 3` is `1`, not `8`. Use `**`.

<!-- quiz -->

Q: What is `17 % 5`?
- [ ] `3.4`
- [ ] `3`
- [x] `2`
- [ ] `12`
> `17 = 3 × 5 + 2`, so the remainder is `2`. (`17 // 5` is `3`.)

Q: What type is the result of `10 / 5`?
- [ ] `int`, value `2`
- [x] `float`, value `2.0`
- [ ] `str`
- [ ] It depends on the numbers
> The `/` operator always produces a float in Python 3. Use `//` for whole-number division.

Q: How do you compute 2 to the power of 8?
- [ ] `2 ^ 8`
- [x] `2 ** 8`
- [ ] `pow 2 8`
- [ ] `2 ^^ 8`
> `**` is exponentiation. `^` is bitwise XOR, which is a common trap.
