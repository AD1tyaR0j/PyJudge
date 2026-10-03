---
title: Working with Numbers
minutes: 7
practice: math
---
Python comes with handy built-in functions for numbers, and a `math` module for more.

## Built-in helpers

```pyrun
print(abs(-7))            # 7
print(min(4, 9, 2))       # 2
print(max(4, 9, 2))       # 9
print(round(3.14159, 2))  # 3.14
print(round(2.5), round(3.5))   # 2 4, see "banker's rounding" below
print(divmod(17, 5))      # (3, 2): quotient and remainder together
print(pow(2, 10))         # 1024
```

## The `math` module

```pyrun
import math

print(math.sqrt(16))      # 4.0
print(math.factorial(5))  # 120
print(math.gcd(12, 18))   # 6
print(math.floor(2.7), math.ceil(2.1))   # 2 3
print(math.pi)
```

`import math` makes the module available. You'll learn more about modules later.

## Floats are approximate

Computers store floats in binary, so some decimals can't be represented exactly:

```pyrun
print(0.1 + 0.2)              # 0.30000000000000004
print(0.1 + 0.2 == 0.3)       # False!
print(abs((0.1 + 0.2) - 0.3) < 1e-9)   # compare with a tolerance instead
print(round(0.1 + 0.2, 2))    # 0.3
```

This isn't a Python bug. It happens in almost every language. Two practical rules:

1. **Don't compare floats with `==`.** Check that they're close, or use `math.isclose()`.
2. **Use integers when you can.** Count money in cents, not dollars.

## Formatting numbers for output

When an exercise asks for "one decimal place", format the number when you print it:

```pyrun
c = 36.6
f = c * 9 / 5 + 32
print(f)              # 97.88000000000001
print(f"{f:.1f}")     # 97.9
print(f"{1234567:,}") # 1,234,567
```

You'll learn f-strings properly in the *Strings* topic.

> **Banker's rounding:** `round()` rounds exact halves to the nearest *even* number, so `round(2.5)` is `2`. It reduces bias when adding many rounded values.

<!-- quiz -->

Q: What does `0.1 + 0.2 == 0.3` evaluate to?
- [ ] `True`
- [x] `False`
- [ ] An error
- [ ] `0.3`
> Floats are stored in binary and `0.1 + 0.2` is actually `0.30000000000000004`. Compare floats with a tolerance or `math.isclose()`.

Q: Which gives `3`?
- [ ] `math.floor(3.9) + 1`
- [x] `math.ceil(2.1)`
- [ ] `round(2.1)`
- [ ] `int(2.9)`
> `ceil` rounds **up** to the next whole number: `ceil(2.1)` is `3`. The others give `4`, `2`, and `2`.

Q: How should you print `x = 2.34567` with exactly two decimals?
- [ ] `print(x, 2)`
- [x] `print(f"{x:.2f}")`
- [ ] `print(int(x * 100))`
- [ ] `print(str(x)[:4])`
> The `.2f` format spec rounds to two decimal places and always shows both digits.
