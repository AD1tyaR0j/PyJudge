---
title: Comparisons and Boolean Logic
minutes: 7
practice: conditionals, basics
---
Comparisons produce a **Boolean**: `True` or `False`.

| Operator | Meaning |
|---|---|
| `==` | equal to |
| `!=` | not equal to |
| `<`, `>` | less than, greater than |
| `<=`, `>=` | less/greater than or equal |

```pyrun
age = 17
print(age >= 18)      # False
print(age == 17)      # True
print("apple" < "banana")   # strings compare alphabetically
print(1 < age < 20)   # chained comparison: 1 < age and age < 20
```

> `=` **assigns**, `==` **compares**. Mixing them up is one of the most common bugs.

## Combining conditions: `and`, `or`, `not`

- `a and b`: True only if **both** are true
- `a or b`: True if **at least one** is true
- `not a`: flips True and False

```pyrun
year = 2024
is_leap = (year % 4 == 0 and year % 100 != 0) or year % 400 == 0
print(is_leap)

has_ticket = True
is_vip = False
print(has_ticket or is_vip)
print(not is_vip)
```

That leap-year rule is exactly what the *Leap Year* exercise asks for.

## Truthiness

In a condition, Python treats "empty" or "zero" values as false and everything else as true:

| Falsy | Truthy |
|---|---|
| `0`, `0.0` | any other number |
| `""` (empty string) | `"hi"`, `" "` |
| `[]`, `{}`, `set()` | non-empty collections |
| `None`, `False` | `True` |

```pyrun
name = ""
print(bool(name))       # False
items = [3, 1]
if items:
    print("the list is not empty")
```

## Short-circuiting

`and` stops at the first false value; `or` stops at the first true value. That lets you guard risky checks:

```pyrun
nums = []
# len(nums) > 0 is False, so nums[0] is never evaluated (no IndexError)
print(len(nums) > 0 and nums[0] > 10)
```

<!-- quiz -->

Q: What does `5 != 5 or 3 > 2` evaluate to?
- [x] `True`
- [ ] `False`
- [ ] `None`
- [ ] An error
> `5 != 5` is False, but `3 > 2` is True, and `or` needs just one true side.

Q: Which value is **truthy**?
- [ ] `0`
- [ ] `""`
- [ ] `[]`
- [x] `" "` (a string containing one space)
> Only empty strings are falsy. A string with a space has length 1, so it's truthy.

Q: What does `1 < 5 < 3` evaluate to?
- [ ] `True`, because `1 < 5`
- [x] `False`, because it means `1 < 5 and 5 < 3`
- [ ] An error
- [ ] `3`
> Chained comparisons check every adjacent pair. `5 < 3` is False, so the whole thing is False.
