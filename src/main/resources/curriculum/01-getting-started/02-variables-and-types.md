---
title: Variables and Data Types
minutes: 8
practice: basics, math
---
A **variable** is a name that refers to a value. You create one with `=`, which means *"store the value on the right under the name on the left"*.

```pyrun
name = "Ada"
age = 36
height = 1.65
likes_python = True

print(name, "is", age, "years old")
age = age + 1          # reassign: the name now points at a new value
print("Next year:", age)
```

## The four basic types

| Type | Example | Used for |
|---|---|---|
| `int` | `42`, `-7`, `0` | whole numbers (any size!) |
| `float` | `3.14`, `-0.5`, `2.0` | numbers with a decimal point |
| `str` | `"hello"`, `'A'` | text |
| `bool` | `True`, `False` | yes/no values |

Use `type()` to ask Python what type a value has:

```pyrun
print(type(42))
print(type(3.14))
print(type("42"))
print(type(True))
print(2 ** 100)   # Python ints never overflow
```

Notice that `"42"` (with quotes) is a **string**, not a number.

## Converting between types

You often need to convert, especially when reading input, which always arrives as text:

```pyrun
text = "25"
number = int(text)      # str -> int
print(number + 5)       # 30
print(text + "5")       # "255": joining two strings
print(float("2.5") * 2) # 5.0
print(str(99) + " bottles")
print(int(7.9))         # 7: int() cuts off the decimals, it doesn't round
```

Converting something that isn't a number fails loudly: `int("hello")` raises a `ValueError`.

## Naming rules

- Use letters, digits, and underscores, and don't start with a digit: `score2` ✔, `2score` ✘
- Use `snake_case` for readability: `total_price`, not `TotalPrice` or `tp`
- Don't reuse built-in names like `list`, `str`, `input`, or `sum`. You'd hide the real function!

Python is **dynamically typed**: the same name can point to an `int` now and a `str` later. That's flexible, but give names a clear meaning so readers aren't surprised.

<!-- quiz -->

Q: What is the type of `"3.5"`?
- [ ] `float`
- [x] `str`
- [ ] `int`
- [ ] `bool`
> The quotes make it a string. `float("3.5")` would convert it to a number.

Q: What does this print?
```python
x = "4"
print(x * 2)
```
- [ ] `8`
- [x] `44`
- [ ] `x2`
- [ ] An error
> Multiplying a string by an integer repeats it: `"4" * 2` is `"44"`.

Q: What does `int(9.99)` give?
- [ ] `10`
- [x] `9`
- [ ] `9.99`
- [ ] An error
> `int()` truncates toward zero. It drops the fractional part. Use `round()` to round instead.
