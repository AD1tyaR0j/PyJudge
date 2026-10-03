---
title: Hello, Python
minutes: 5
practice: basics
---
Python is a programming language designed to be easy to read. A Python program is just a text file of instructions that run **top to bottom**, one line at a time.

## Your first program

The `print()` function shows text on the screen. Press **Run** below (or `Ctrl+Enter` inside the editor), then change the message and run it again.

```pyrun
print("Hello, Python!")
print("I am learning to code.")
```

Text inside quotes is called a **string**. You can use single `'...'` or double `"..."` quotes. Just be consistent.

## Printing several things

`print()` accepts several values separated by commas and puts a space between them:

```pyrun
print("The answer is", 42)
print(1, 2, 3)
print()            # an empty print() prints a blank line
print("Done!")
```

## Comments

Anything after a `#` is a **comment**. Python ignores it, so comments are notes for humans:

```python
# This whole line is a comment
print("Comments don't run")  # neither does this part
```

Good comments explain *why* the code does something, not *what* it obviously does.

## Python is picky about spelling

Python is **case-sensitive**: `print` works, but `Print` or `PRINT` does not. Forgetting a closing quote or bracket is the most common beginner error:

```python
print("Hello)      # SyntaxError: the string is never closed
Print("Hello")     # NameError: name 'Print' is not defined
```

When you see an error, read the **last line** of the message first. It names the problem.

> **Key idea:** a program is a list of instructions that run in order. `print()` is how your program shows its results, and that's exactly how every exercise on PyJudge is checked.

<!-- quiz -->

Q: What does `print("a", "b", 3)` display?
- [ ] `ab3`
- [x] `a b 3`
- [ ] `"a" "b" 3`
- [ ] `a, b, 3`
> `print()` separates its arguments with a single space and shows strings without their quotes.

Q: Which line is a comment?
- [ ] `print("# not a comment")`
- [x] `# print("hello")`
- [ ] `// print("hello")`
- [ ] `/* print("hello") */`
> In Python, `#` starts a comment. A `#` inside a string is just a character. `//` and `/* */` are comments in other languages.

Q: Why does `Print("hi")` fail?
- [ ] Strings must use single quotes
- [ ] `print` needs a semicolon after it
- [x] Python is case-sensitive, so `Print` is not the same as `print`
- [ ] You can only print numbers
> Names in Python are case-sensitive. The built-in function is spelled `print` in lowercase.
