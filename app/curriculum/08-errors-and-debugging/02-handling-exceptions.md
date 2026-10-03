---
title: Handling Exceptions
minutes: 8
practice: basics
---
Some errors are *expected*: a user types letters instead of a number, a file is missing, a key isn't in a dictionary. `try`/`except` lets your program recover instead of crashing.

```pyrun
for text in ["42", "abc", "7"]:
    try:
        number = int(text)
        print("Got", number)
    except ValueError:
        print(f"{text!r} is not a number")
```

Python runs the `try` block. If an exception of the named type happens, it jumps to the matching `except` block and carries on afterwards.

## Catch specific exceptions

```pyrun
def safe_divide(a, b):
    try:
        return a / b
    except ZeroDivisionError:
        return None

print(safe_divide(10, 4), safe_divide(1, 0))

data = {"x": 1}
try:
    print(data["y"])
except KeyError as err:            # `as` gives you the exception object
    print("missing key:", err)
```

Avoid a bare `except:` (or `except Exception:`) wrapped around everything. It hides real bugs, like typos, that you *want* to see.

## Multiple handlers, `else`, and `finally`

```pyrun
def parse(text):
    try:
        value = 100 / int(text)
    except ValueError:
        print("not an integer")
    except ZeroDivisionError:
        print("can't divide by zero")
    else:
        print("result:", value)    # runs only if no exception happened
    finally:
        print("checked", repr(text))   # ALWAYS runs: good for cleanup

parse("4")
parse("0")
parse("x")
```

## "Ask forgiveness" vs "look before you leap"

Python code often just *tries* the operation and handles the failure:

```pyrun
counts = {}
for word in "to be or not to be".split():
    try:
        counts[word] += 1
    except KeyError:
        counts[word] = 1
print(counts)
```

(`counts.get(word, 0) + 1` is equally good here. Pick whichever reads better.)

## Reading input safely

```pyrun
while True:
    try:
        age = int(input())
        break
    except ValueError:
        print("Please type a whole number")
    except EOFError:
        age = None
        break
print("age =", age)
### stdin
twelve
12
```

<!-- quiz -->

Q: What prints?
```python
try:
    x = int("5")
except ValueError:
    print("bad")
else:
    print("good")
```
- [ ] `bad`
- [x] `good`
- [ ] `bad` then `good`
- [ ] nothing
> `int("5")` succeeds, so the `except` block is skipped and the `else` block runs.

Q: When does a `finally` block run?
- [ ] Only when an exception happened
- [ ] Only when no exception happened
- [x] Always, whether or not an exception happened
- [ ] Never inside functions
> `finally` is for cleanup that must happen no matter what.

Q: Why is `except:` with no exception type usually a bad idea?
- [ ] It's a syntax error
- [x] It also catches bugs you didn't expect, hiding them
- [ ] It's slower
- [ ] It only catches `KeyError`
> Catch the specific exceptions you expect, so genuine mistakes still surface.
