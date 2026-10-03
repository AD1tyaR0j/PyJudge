---
title: Formatting with f-strings
minutes: 7
practice: strings, basics
---
An **f-string** is a string with an `f` before the quote. Anything inside `{ }` is evaluated as Python and inserted into the text:

```pyrun
name = "Grace"
age = 85
print(f"{name} is {age} years old")
print(f"In 5 years: {age + 5}")
print(f"Shouting: {name.upper()}!")
```

That's much cleaner than gluing pieces with `+` and `str()`:

```python
print(name + " is " + str(age) + " years old")   # the old, clunky way
```

## Format specifiers

After a colon inside the braces you can control *how* a value is shown:

```pyrun
pi = 3.14159265
print(f"{pi:.2f}")        # 3.14: two decimal places
print(f"{pi:.0f}")        # 3
print(f"{1234567:,}")     # 1,234,567: thousands separator
print(f"{0.4567:.1%}")    # 45.7%: as a percentage
print(f"{42:05d}")        # 00042: pad with zeros to width 5
print(f"{255:b} {255:x}") # 11111111 ff: binary and hex
```

## Alignment: building tables

`<` left-aligns, `>` right-aligns, and `^` centers, inside a given width:

```pyrun
items = [("apple", 1.5), ("banana", 0.25), ("cherry", 12.0)]
for name, price in items:
    print(f"{name:<10}|{price:>8.2f}")
print(f"{'menu':^20}")
print(f"{'':-^20}")
```

## Debugging shortcut

Add `=` after an expression to print both the expression and its value:

```pyrun
x = 7
y = 3
print(f"{x * y = }")
print(f"{x // y = }, {x % y = }")
```

> **When exercises say "print with one decimal place"**, use `f"{value:.1f}"`. It rounds correctly *and* always shows the decimal, so `5` prints as `5.0`.

<!-- quiz -->

Q: What does `f"{7 / 2:.0f}"` produce?
- [ ] `"3.5"`
- [ ] `"3"`
- [x] `"4"`
- [ ] `"3.50"`
> `.0f` rounds to zero decimal places. Exact halves round to the nearest **even** number, and `3.5` → `4` (4 is even).

Q: Which prints `Total: 007`?
- [ ] `print(f"Total: {7:3}")`
- [x] `print(f"Total: {7:03d}")`
- [ ] `print(f"Total: {7:.3f}")`
- [ ] `print("Total: " + 007)`
> `03d` pads the integer with zeros to a width of 3. Plain `3` pads with spaces.

Q: What does `f"{'hi':>5}"` give?
- [ ] `"hi   "`
- [x] `"   hi"`
- [ ] `" hi  "`
- [ ] `">>>hi"`
> `>` right-aligns the value in a field of width 5, filling with spaces on the left.
