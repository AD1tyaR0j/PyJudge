---
title: Raising Errors and Testing Your Code
minutes: 8
practice: basics, math
---
## Raising your own exceptions

When a function receives input it can't work with, **raise** an exception with a clear message:

```pyrun
def withdraw(balance, amount):
    if amount <= 0:
        raise ValueError("amount must be positive")
    if amount > balance:
        raise ValueError(f"insufficient funds: balance is {balance}")
    return balance - amount

print(withdraw(100, 30))
try:
    withdraw(100, 500)
except ValueError as err:
    print("Error:", err)
```

Failing loudly and early beats returning a wrong answer that breaks something later.

## Custom exception classes

```pyrun
class OutOfStock(Exception):
    pass

def buy(stock, item):
    if stock.get(item, 0) == 0:
        raise OutOfStock(item)
    stock[item] -= 1

stock = {"pen": 1}
buy(stock, "pen")
try:
    buy(stock, "pen")
except OutOfStock as e:
    print("Sorry, out of", e)
```

(Classes are explained in *Object-Oriented Python*.)

## `assert`: quick sanity checks

`assert condition, message` raises an `AssertionError` if the condition is false. It's great for testing your own functions:

```pyrun
def reverse_words(sentence):
    return " ".join(sentence.split()[::-1])

assert reverse_words("hello world") == "world hello"
assert reverse_words("one") == "one"
assert reverse_words("") == ""
print("all tests passed ✔")
```

## Test before you submit

Before pressing **Submit**, test your solution against:

1. **The samples** from the problem statement
2. **Edge cases:** smallest input, empty strings, a single element, duplicates, negative numbers
3. **A big input** to check speed, e.g. `" ".join(["9"] * 100000)` as the number list

A tiny test harness makes this easy:

```pyrun
def second_largest(nums):
    distinct = sorted(set(nums), reverse=True)
    return distinct[1] if len(distinct) > 1 else -1

tests = [
    ([4, 9, 2, 9, 7], 7),
    ([5, 5, 5], -1),
    ([1], -1),
    ([0, 1], 0),
]
for nums, expected in tests:
    got = second_largest(nums)
    status = "✔" if got == expected else "✘"
    print(status, nums, "->", got, "(expected", expected, ")")
```

This is how PyJudge itself works: it runs your code against hidden tests and compares output.

<!-- quiz -->

Q: Which statement raises an error on purpose?
- [ ] `throw ValueError("bad")`
- [x] `raise ValueError("bad")`
- [ ] `error ValueError("bad")`
- [ ] `except ValueError("bad")`
> Python uses `raise`. (`throw` is used in Java and JavaScript.)

Q: What happens when `assert 2 + 2 == 5, "math is broken"` runs?
- [ ] Nothing
- [ ] It prints `math is broken`
- [x] It raises `AssertionError: math is broken`
- [ ] It returns `False`
> A failed assert raises `AssertionError` with your message.

Q: Which input is the most valuable **edge case** for a "second largest value" function?
- [ ] `[1, 2, 3, 4, 5]`
- [x] `[7, 7, 7]`, where every value is the same
- [ ] `[10, 20, 30]`
- [ ] `[5, 3, 9, 1]`
> With all values equal there is no second distinct value, a case many first attempts get wrong.
