---
title: Defining Functions
minutes: 8
practice: basics, math
---
Define a function with `def`, give it **parameters**, and send back a result with `return`:

```pyrun
def area(width, height):
    return width * height

print(area(3, 4))
print(area(10, 2) + area(1, 1))
```

The code inside a function doesn't run until you **call** it. Defining it just teaches Python the recipe.

## `return` vs `print`

`print` shows a value on screen; `return` hands it back to whoever called the function so they can use it:

```pyrun
def double_print(x):
    print(x * 2)

def double_return(x):
    return x * 2

a = double_print(5)     # shows 10, but a is None
b = double_return(5)    # shows nothing, b is 10
print(a, b, b + 1)
```

A function without a `return` statement returns `None`. Returning lets functions be **combined**: `area(double_return(2), 3)`.

## `return` ends the function

```pyrun
def is_prime(n):
    if n < 2:
        return False
    i = 2
    while i * i <= n:          # only test divisors up to √n
        if n % i == 0:
            return False       # found a divisor, so stop right here
        i += 1
    return True

print([n for n in range(30) if is_prime(n)])
```

## Docstrings

A string right after the `def` line documents what the function does:

```pyrun
def celsius_to_fahrenheit(c):
    """Convert a temperature from Celsius to Fahrenheit."""
    return c * 9 / 5 + 32

print(celsius_to_fahrenheit(100))
print(celsius_to_fahrenheit.__doc__)
```

## Structuring exercise solutions

A clean habit: put the logic in a function, and keep input/output at the bottom.

```pyrun
def solve(nums):
    best = cur = nums[0]
    for x in nums[1:]:
        cur = max(x, cur + x)
        best = max(best, cur)
    return best

n = int(input())
nums = list(map(int, input().split()))
print(solve(nums))
### stdin
9
-2 1 -3 4 -1 2 1 -5 4
```

That's Kadane's algorithm for *Maximum Subarray Sum*. Notice how `solve` can be tested without any input at all.

<!-- quiz -->

Q: What does this print?
```python
def f(x):
    x * 3
print(f(2))
```
- [ ] `6`
- [x] `None`
- [ ] `2`
- [ ] An error
> The function computes `x * 3` but never returns it, so it returns `None`.

Q: When does the code inside a `def` block run?
- [ ] As soon as Python reads the `def` line
- [x] Each time the function is called
- [ ] Only once, the first time it's called
- [ ] When the program ends
> `def` only creates the function. The body runs on every call.

Q: Why prefer `return` over `print` inside a helper function?
- [ ] `return` is faster
- [x] The caller can use the returned value in further calculations
- [ ] `print` doesn't work inside functions
- [ ] `return` shows the value in color
> Returning a value makes the function composable: its result can be stored, compared, or passed on.
