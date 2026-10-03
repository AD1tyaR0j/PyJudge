---
title: Indexing and Slicing
minutes: 8
practice: strings
---
A string is a **sequence** of characters, and each character has a position called an **index**. Indexes start at **0**.

```
 P   y   t   h   o   n
 0   1   2   3   4   5     ← positive index
-6  -5  -4  -3  -2  -1     ← negative index
```

```pyrun
word = "Python"
print(word[0])      # P
print(word[5])      # n
print(word[-1])     # n, the last character
print(len(word))    # 6
```

Asking for `word[6]` raises an `IndexError`, because the last valid index is `len(word) - 1`.

## Slicing: `s[start:stop:step]`

A slice takes a piece of the string. `start` is **included**, `stop` is **excluded**:

```pyrun
word = "Python"
print(word[0:2])    # Py
print(word[2:])     # thon: omit stop to go to the end
print(word[:4])     # Pyth: omit start to begin at 0
print(word[-3:])    # hon: the last three characters
print(word[::2])    # Pto: every second character
print(word[::-1])   # nohtyP: step -1 reverses!
```

`s[::-1]` is the idiomatic way to reverse a string. It makes *Reverse a String* a one-liner, and palindrome checks become `s == s[::-1]`.

Slices never raise an `IndexError`; out-of-range bounds are simply clipped: `"abc"[1:100]` is `"bc"`.

## Strings are immutable

You can't change a character in place:

```python
word = "cat"
word[0] = "b"      # TypeError: 'str' object does not support item assignment
```

Instead, build a new string:

```pyrun
word = "cat"
word = "b" + word[1:]
print(word)         # bat
```

## Looping over characters

```pyrun
for ch in "hey":
    print(ch, ord(ch))     # ord() gives the character's code number

print("y" in "Python")     # membership test: True
print("Py" in "Python")    # works for substrings too
```

<!-- quiz -->

Q: What is `"programming"[3:7]`?
- [ ] `"rogr"`
- [x] `"gram"`
- [ ] `"ogra"`
- [ ] `"gramm"`
> Counting from 0: `p`=0, `r`=1, `o`=2, `g`=3. The slice starts at index 3 and stops **before** index 7, giving `g r a m`.

Q: Which expression gives the **last** character of a non-empty string `s`?
- [ ] `s[len(s)]`
- [x] `s[-1]`
- [ ] `s[last]`
- [ ] `s[0:-1]`
> Negative indexes count from the end; `-1` is the last character. `s[len(s)]` is one past the end and raises `IndexError`.

Q: What does `"stressed"[::-1]` give?
- [ ] `"stressed"`
- [x] `"desserts"`
- [ ] `"s"`
- [ ] An error
> A step of `-1` walks the string backwards, reversing it.
