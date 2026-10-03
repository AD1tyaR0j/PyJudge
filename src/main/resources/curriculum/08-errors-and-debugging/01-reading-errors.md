---
title: Reading Error Messages
minutes: 8
practice: basics
---
When Python hits a problem it can't handle, it stops and prints a **traceback**. Run this on purpose:

```pyrun-error
def average(nums):
    return sum(nums) / len(nums)

print(average([4, 6]))
print(average([]))
```

Read a traceback **from the bottom up**:

1. **Last line:** the error type and message, `ZeroDivisionError: division by zero`. This tells you *what* went wrong.
2. **Lines above:** the chain of calls that led there, with file names and line numbers. This tells you *where*.

## The errors you'll meet most

| Error | Typical cause | Example |
|---|---|---|
| `SyntaxError` | Code isn't valid Python | missing `:` or `)` |
| `IndentationError` | Inconsistent indentation | mixing tabs and spaces |
| `NameError` | Using a name that doesn't exist | typo: `pritn`, or using a variable before assigning it |
| `TypeError` | Wrong type for an operation | `"age: " + 5` |
| `ValueError` | Right type, bad value | `int("abc")` |
| `IndexError` | List/string index out of range | `[1, 2][5]` |
| `KeyError` | Missing dictionary key | `{"a": 1}["b"]` |
| `ZeroDivisionError` | Dividing by zero | `x / 0` |
| `AttributeError` | Value doesn't have that method | `[1, 2].upper()` |
| `RecursionError` | Recursion too deep | no base case |

## On PyJudge

- A `SyntaxError` gives the verdict **Syntax Error**, and your code never ran.
- Any other uncaught exception gives **Runtime Error**, with the traceback in the result panel.
- `EOFError` usually means you called `input()` more times than there are input lines.

## Debugging strategies

**1. Print what you think is true.** Most bugs are a wrong assumption:

```pyrun
nums = [3, 8, 1]
total = 0
for i in range(1, len(nums)):      # bug: skips nums[0]
    print(f"{i=} {nums[i]=} {total=}")
    total += nums[i]
print(total)                        # expected 12
```

The debug output shows the loop never sees index 0. The fix is `range(len(nums))` (or just `for x in nums`).

**2. Shrink the input.** If it fails on 1,000 numbers, try 3. Find the smallest case that breaks.

**3. Test the edges.** Empty input, one item, all equal, negative numbers, the biggest allowed value. Hidden tests love edge cases.

**4. Explain it out loud.** "Rubber duck debugging": describing each line to someone (or something) often reveals the bug.

> Remember to **remove debug prints** before submitting. Extra output means *Wrong Answer*.

<!-- quiz -->

Q: Where should you look first in a traceback?
- [ ] The first line
- [x] The last line, which names the error type and message
- [ ] The middle
- [ ] Only the line numbers
> The bottom line says *what* happened. The lines above show *where* it happened.

Q: What error does `"Total: " + 10` raise?
- [ ] `ValueError`
- [x] `TypeError`
- [ ] `SyntaxError`
- [ ] `NameError`
> You can't add a `str` and an `int`. Use `"Total: " + str(10)` or an f-string.

Q: Your submission gets *Runtime Error* with `EOFError`. What's the likely cause?
- [ ] The judge is broken
- [ ] You printed too much
- [x] Your program called `input()` more times than there are input lines
- [ ] You used a `for` loop
> `EOFError` means `input()` hit the end of the input. Re-read the input format.
