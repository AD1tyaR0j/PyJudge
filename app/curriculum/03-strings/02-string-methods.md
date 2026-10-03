---
title: String Methods
minutes: 9
practice: strings
---
A **method** is a function that belongs to a value, called with a dot: `text.upper()`. Because strings are immutable, string methods **return a new string**. They never change the original.

## Changing case and trimming

```pyrun
s = "  Hello, World!  "
print(s.upper())
print(s.lower())
print(s.strip())          # remove spaces at both ends
print(s.strip().title())
print(repr(s))            # the original is unchanged
```

## Splitting and joining

`split()` breaks a string into a **list** of words; `join()` glues a list back together:

```pyrun
sentence = "the quick brown fox"
words = sentence.split()           # split on any whitespace
print(words)
print(len(words), "words")

csv = "red,green,blue"
print(csv.split(","))              # split on a specific separator

print("-".join(words))             # the-quick-brown-fox
print("".join(reversed("abc")))    # cba
```

`split()` with no argument also ignores extra spaces, which is perfect for the *Count Words* exercise.

## Searching

```pyrun
text = "banana"
print(text.count("a"))        # 3
print(text.find("na"))        # 2: index of the first match
print(text.find("xyz"))       # -1: not found
print(text.startswith("ban"), text.endswith("na"))
print(text.replace("a", "o")) # bonono
```

## Checking content

```pyrun
print("123".isdigit())    # True
print("abc".isalpha())    # True
print("abc1".isalnum())   # True
print("   ".isspace())    # True
print("Hello".isupper(), "hello".islower())
```

## Putting it together: a palindrome check

```pyrun
phrase = "A man, a plan, a canal: Panama"
cleaned = "".join(ch.lower() for ch in phrase if ch.isalnum())
print(cleaned)
print(cleaned == cleaned[::-1])
```

This "clean then compare" approach is exactly what the *Palindrome Check* exercise needs.

> **Common mistake:** writing `s.upper()` on its own line and expecting `s` to change. Strings are immutable, so you must keep the result: `s = s.upper()`.

<!-- quiz -->

Q: After running these lines, what is `name`?
```python
name = "ada"
name.upper()
```
- [ ] `"ADA"`
- [x] `"ada"`
- [ ] `None`
- [ ] An error
> `upper()` returns a new string, but the result was thrown away. Write `name = name.upper()` to keep it.

Q: What does `" a  b ".split()` return?
- [ ] `[" a", " b "]`
- [ ] `["", "a", "", "b", ""]`
- [x] `["a", "b"]`
- [ ] `"ab"`
> With no argument, `split()` splits on runs of whitespace and ignores leading/trailing spaces.

Q: How do you turn `["x", "y", "z"]` into `"x, y, z"`?
- [ ] `["x", "y", "z"].join(", ")`
- [x] `", ".join(["x", "y", "z"])`
- [ ] `str(["x", "y", "z"])`
- [ ] `"x" + "y" + "z"`
> `join` is a method of the **separator** string and takes the list as its argument.
