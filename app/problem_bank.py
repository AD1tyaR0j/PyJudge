"""Extra practice exercises, added to the seed after the original ten.

Each spec has:
  solve(inp) -> expected output  (reference implementation used to build the tests)
  solution                        (an independent stdin/stdout program; tests/test_problem_bank.py
                                   runs it through the real judge to prove statement, tests,
                                   and reference agree)
"""
import random
from collections import Counter, deque
from math import gcd

_rng = random.Random(4242)


def _arr(nums):
    return f"{len(nums)}\n{' '.join(map(str, nums))}"


def _lines(inp):
    return inp.strip("\n").split("\n")


# ---------------------------------------------------------------- reference solvers

def _even_odd(inp):
    return "Even" if int(inp) % 2 == 0 else "Odd"


def _largest_three(inp):
    return str(max(map(int, inp.split())))


def _digit_sum(inp):
    return str(sum(int(c) for c in inp.strip()))


def _factorial(inp):
    r = 1
    for i in range(2, int(inp) + 1):
        r *= i
    return str(r)


def _c_to_f(inp):
    return f"{int(inp) * 9 / 5 + 32:.1f}"


def _count_words(inp):
    return str(len(inp.split()))


def _leap(inp):
    y = int(inp)
    return "Leap" if (y % 4 == 0 and y % 100 != 0) or y % 400 == 0 else "Not leap"


def _table(inp):
    n = int(inp)
    return "\n".join(f"{n} x {i} = {n * i}" for i in range(1, 11))


def _second_largest(inp):
    nums = sorted(set(map(int, _lines(inp)[1].split())), reverse=True)
    return str(nums[1]) if len(nums) > 1 else "-1"


def _char_freq(inp):
    c = Counter(inp.strip())
    return "\n".join(f"{ch} {c[ch]}" for ch in sorted(c))


def _anagram(inp):
    a, b = _lines(inp)[:2]
    norm = lambda s: sorted(s.replace(" ", "").lower())
    return "YES" if norm(a) == norm(b) else "NO"


def _to_binary(inp):
    return bin(int(inp))[2:]


def _gcd_lcm(inp):
    a, b = map(int, inp.split())
    g = gcd(a, b)
    return f"{g} {a // g * b}"


def _dedupe(inp):
    seen, out = set(), []
    for x in _lines(inp)[1].split():
        if x not in seen:
            seen.add(x)
            out.append(x)
    return " ".join(out)


def _transpose(inp):
    lines = _lines(inp)
    r, c = map(int, lines[0].split())
    m = [lines[1 + i].split() for i in range(r)]
    return "\n".join(" ".join(m[i][j] for i in range(r)) for j in range(c))


def _rotate(inp):
    lines = _lines(inp)
    n, k = map(int, lines[0].split())
    a = lines[1].split()
    k %= n
    return " ".join(a[n - k:] + a[:n - k])


def _rle(inp):
    s = inp.strip()
    out, i = [], 0
    while i < len(s):
        j = i
        while j < len(s) and s[j] == s[i]:
            j += 1
        out.append(f"{s[i]}{j - i}")
        i = j
    return "".join(out)


def _merge_sorted(inp):
    lines = _lines(inp)
    a = list(map(int, lines[1].split())) if int(lines[0]) else []
    b = list(map(int, lines[3].split())) if int(lines[2]) else []
    return " ".join(map(str, sorted(a + b)))


def _binary_search(inp):
    lines = _lines(inp)
    pos = {int(x): i for i, x in enumerate(lines[1].split())}
    return "\n".join(str(pos.get(int(x), -1)) for x in lines[3].split())


def _top_words(inp):
    lines = _lines(inp)
    words = lines[0].lower().split()
    k = int(lines[1])
    ranked = sorted(Counter(words).items(), key=lambda kv: (-kv[1], kv[0]))[:k]
    return "\n".join(f"{w} {c}" for w, c in ranked)


def _spiral(inp):
    n = int(inp)
    m = [[0] * n for _ in range(n)]
    top, left, bottom, right, v = 0, 0, n - 1, n - 1, 1
    while top <= bottom and left <= right:
        for j in range(left, right + 1):
            m[top][j] = v; v += 1
        top += 1
        for i in range(top, bottom + 1):
            m[i][right] = v; v += 1
        right -= 1
        if top <= bottom:
            for j in range(right, left - 1, -1):
                m[bottom][j] = v; v += 1
            bottom -= 1
        if left <= right:
            for i in range(bottom, top - 1, -1):
                m[i][left] = v; v += 1
            left += 1
    return "\n".join(" ".join(map(str, row)) for row in m)


def _coin_change(inp):
    lines = _lines(inp)
    coins = list(map(int, lines[1].split()))
    amount = int(lines[2])
    INF = float("inf")
    dp = [0] + [INF] * amount
    for a in range(1, amount + 1):
        for c in coins:
            if c <= a and dp[a - c] + 1 < dp[a]:
                dp[a] = dp[a - c] + 1
    return str(dp[amount] if dp[amount] != INF else -1)


def _longest_unique(inp):
    s = inp.rstrip("\n")
    last, start, best = {}, 0, 0
    for i, ch in enumerate(s):
        if last.get(ch, -1) >= start:
            start = last[ch] + 1
        last[ch] = i
        best = max(best, i - start + 1)
    return str(best)


def _islands(inp):
    lines = _lines(inp)
    r, c = map(int, lines[0].split())
    g = [list(lines[1 + i]) for i in range(r)]
    count = 0
    for i in range(r):
        for j in range(c):
            if g[i][j] == "1":
                count += 1
                g[i][j] = "0"
                q = deque([(i, j)])
                while q:
                    x, y = q.popleft()
                    for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                        if 0 <= nx < r and 0 <= ny < c and g[nx][ny] == "1":
                            g[nx][ny] = "0"
                            q.append((nx, ny))
    return str(count)


def _edit_distance(inp):
    parts = inp.split("\n")  # not _lines(): a blank first line is a real (empty) string
    a, b = parts[0], (parts[1] if len(parts) > 1 else "")
    prev = list(range(len(b) + 1))
    for i in range(1, len(a) + 1):
        cur = [i] + [0] * len(b)
        for j in range(1, len(b) + 1):
            cur[j] = prev[j - 1] if a[i - 1] == b[j - 1] else 1 + min(prev[j - 1], prev[j], cur[j - 1])
        prev = cur
    return str(prev[len(b)])


def _grid_path(inp):
    lines = _lines(inp)
    r, c = map(int, lines[0].split())
    g = lines[1:1 + r]
    start = end = None
    for i in range(r):
        for j in range(c):
            if g[i][j] == "S":
                start = (i, j)
            elif g[i][j] == "E":
                end = (i, j)
    dist = {start: 0}
    q = deque([start])
    while q:
        cell = q.popleft()
        if cell == end:
            return str(dist[cell])
        x, y = cell
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < r and 0 <= ny < c and g[nx][ny] != "#" and (nx, ny) not in dist:
                dist[(nx, ny)] = dist[cell] + 1
                q.append((nx, ny))
    return "-1"


def _n_queens(inp):
    n = int(inp)
    cols, d1, d2 = set(), set(), set()

    def place(row):
        if row == n:
            return 1
        total = 0
        for col in range(n):
            if col in cols or row - col in d1 or row + col in d2:
                continue
            cols.add(col); d1.add(row - col); d2.add(row + col)
            total += place(row + 1)
            cols.remove(col); d1.remove(row - col); d2.remove(row + col)
        return total

    return str(place(0))


# ---------------------------------------------------------------- large / generated inputs

def _grid(r, c, p, rng):
    return "\n".join("".join("1" if rng.random() < p else "0" for _ in range(c)) for _ in range(r))


def _maze(r, c, rng, wall_p=0.28):
    rows = [["#" if rng.random() < wall_p else "." for _ in range(c)] for _ in range(r)]
    rows[0][0], rows[r - 1][c - 1] = "S", "E"
    return f"{r} {c}\n" + "\n".join("".join(row) for row in rows)


_big_sorted = sorted(_rng.sample(range(-10**9, 10**9), 100_000))
_big_queries = [_rng.choice(_big_sorted) if _rng.random() < 0.6 else _rng.randint(-10**9, 10**9) for _ in range(100_000)]
_merge_a = sorted(_rng.randint(-10**6, 10**6) for _ in range(100_000))
_merge_b = sorted(_rng.randint(-10**6, 10**6) for _ in range(100_000))
_dupes = [_rng.randint(1, 5000) for _ in range(100_000)]
_long_text = "".join(_rng.choice("abcdefghijklmnopqrstuvwxyz") for _ in range(100_000))
_edit_a = "".join(_rng.choice("acgt") for _ in range(1000))
_edit_b = "".join(_rng.choice("acgt") for _ in range(1000))
_word_pool = "the quick brown fox jumps over lazy dog python code test loop list dict set".split()
_words_line = " ".join(_rng.choice(_word_pool) for _ in range(20_000))

_IO = "### Input\n{}\n\n### Output\n{}"

EXTRA_PROBLEMS = [
    # ------------------------------------------------------------ Easy
    {
        "title": "Even or Odd", "difficulty": "Easy", "tags": "basics, math",
        "description": "Decide whether an integer is even or odd.\n\n" + _IO.format(
            "A single integer `n` (−10¹⁸ ≤ n ≤ 10¹⁸).", "`Even` or `Odd`."),
        "starter": "n = int(input())\n",
        "solve": _even_odd, "samples": ["4", "7"], "hidden": ["0", "-3", "-10", "999999999999999999"],
        "solution": "n = int(input())\nprint('Even' if n % 2 == 0 else 'Odd')",
    },
    {
        "title": "Largest of Three", "difficulty": "Easy", "tags": "basics, conditionals",
        "description": "Print the largest of three integers.\n\n" + _IO.format(
            "One line with three integers `a b c`.", "The largest value."),
        "solve": _largest_three, "samples": ["3 9 4", "-1 -5 -2"], "hidden": ["7 7 7", "0 -1 1", "1000000 999999 -1000000"],
        "solution": "a, b, c = map(int, input().split())\nbig = a\nif b > big: big = b\nif c > big: big = c\nprint(big)",
    },
    {
        "title": "Sum of Digits", "difficulty": "Easy", "tags": "math, loops",
        "description": "Add up the digits of a non-negative integer.\n\n" + _IO.format(
            "A single integer `n` (0 ≤ n ≤ 10¹⁸).", "The sum of its digits."),
        "solve": _digit_sum, "samples": ["1234", "0"], "hidden": ["9", "1000000000000000000", "987654321987654321"],
        "solution": "n = int(input())\ns = 0\nwhile n:\n    s += n % 10\n    n //= 10\nprint(s)",
    },
    {
        "title": "Factorial", "difficulty": "Easy", "tags": "math, loops",
        "description": "Compute `n!` (the product 1 × 2 × … × n). By definition `0! = 1`.\n\n" + _IO.format(
            "A single integer `n` (0 ≤ n ≤ 50).", "`n!`"),
        "solve": _factorial, "samples": ["5", "0"], "hidden": ["1", "10", "20", "50"],
        "solution": "import math\nprint(math.factorial(int(input())))",
    },
    {
        "title": "Celsius to Fahrenheit", "difficulty": "Easy", "tags": "basics, math",
        "description": "Convert a temperature from Celsius to Fahrenheit using `F = C × 9/5 + 32`.\n\n" + _IO.format(
            "A single integer `C` (−273 ≤ C ≤ 1000).", "`F` rounded to exactly one decimal place, e.g. `98.6`."),
        "solve": _c_to_f, "samples": ["100", "37"], "hidden": ["0", "-40", "-273", "1000", "21"],
        "solution": "c = int(input())\nprint(f'{c * 9 / 5 + 32:.1f}')",
    },
    {
        "title": "Count Words", "difficulty": "Easy", "tags": "strings",
        "description": "Count the words in a line. Words are separated by one or more spaces.\n\n" + _IO.format(
            "One line of text (it may have extra spaces).", "The number of words."),
        "solve": _count_words, "samples": ["Python is fun", "   lots   of   space   "],
        "hidden": ["single", "a b c d e f g", " ".join(["word"] * 5000)],
        "solution": "print(len(input().split()))",
    },
    {
        "title": "Leap Year", "difficulty": "Easy", "tags": "conditionals, basics",
        "description": ("A year is a leap year if it is divisible by 4, **except** years divisible by 100, "
                        "**unless** they are also divisible by 400.\n\n") + _IO.format(
            "A year `y` (1 ≤ y ≤ 10⁹).", "`Leap` or `Not leap`."),
        "solve": _leap, "samples": ["2024", "1900"], "hidden": ["2000", "2023", "4", "100", "400"],
        "solution": "y = int(input())\nimport calendar\nprint('Leap' if calendar.isleap(y) else 'Not leap')",
    },
    {
        "title": "Multiplication Table", "difficulty": "Easy", "tags": "loops, basics",
        "description": "Print the multiplication table of `n` from 1 to 10.\n\n" + _IO.format(
            "A single integer `n` (−1000 ≤ n ≤ 1000).",
            "Ten lines in the form `n x i = product`, for i = 1…10. Example line: `3 x 4 = 12`."),
        "solve": _table, "samples": ["3"], "hidden": ["1", "0", "-7", "1000"],
        "solution": "n = int(input())\nfor i in range(1, 11):\n    print(n, 'x', i, '=', n * i)",
    },
    {
        "title": "Second Largest", "difficulty": "Easy", "tags": "arrays, sorting",
        "description": "Find the second largest **distinct** value in a list. If there isn't one, print `-1`.\n\n" + _IO.format(
            "- Line 1: `n` (1 ≤ n ≤ 10⁵)\n- Line 2: `n` integers (0 ≤ aᵢ ≤ 10⁹)", "The second largest distinct value, or `-1`."),
        "solve": _second_largest, "samples": [_arr([4, 9, 2, 9, 7]), _arr([5, 5, 5])],
        "hidden": [_arr([1]), _arr([0, 1]), _arr([_rng.randint(0, 10**9) for _ in range(100_000)])],
        "solution": "input()\nnums = set(map(int, input().split()))\nif len(nums) < 2:\n    print(-1)\nelse:\n    nums.remove(max(nums))\n    print(max(nums))",
    },
    {
        "title": "Character Frequency", "difficulty": "Easy", "tags": "strings, hash-map",
        "description": "Count how often each letter appears in a word.\n\n" + _IO.format(
            "One word of lowercase letters (1 ≤ length ≤ 10⁵).",
            "One line per distinct letter, in alphabetical order: the letter, a space, and its count."),
        "solve": _char_freq, "samples": ["banana", "python"], "hidden": ["a", "mississippi", _long_text],
        "solution": "s = input().strip()\ncounts = {}\nfor ch in s:\n    counts[ch] = counts.get(ch, 0) + 1\nfor ch in sorted(counts):\n    print(ch, counts[ch])",
    },
    # ------------------------------------------------------------ Medium
    {
        "title": "Anagram Check", "difficulty": "Medium", "tags": "strings, sorting",
        "description": ("Two phrases are anagrams if they use exactly the same letters, ignoring spaces and "
                        "upper/lower case.\n\n") + _IO.format("Two lines, one phrase each.", "`YES` if they are anagrams, otherwise `NO`."),
        "solve": _anagram, "samples": ["listen\nsilent", "hello\nworld"],
        "hidden": ["Dormitory\nDirty room", "abc\nabcc", "a\nA", "The eyes\nThey see"],
        "solution": "from collections import Counter\nnorm = lambda s: Counter(s.replace(' ', '').lower())\nprint('YES' if norm(input()) == norm(input()) else 'NO')",
    },
    {
        "title": "Decimal to Binary", "difficulty": "Medium", "tags": "math, bits",
        "description": "Convert a non-negative integer to binary **without** a leading `0b`.\n\n" + _IO.format(
            "A single integer `n` (0 ≤ n ≤ 10¹⁸).", "`n` written in base 2."),
        "solve": _to_binary, "samples": ["10", "0"], "hidden": ["1", "255", "1024", "1000000000000000000"],
        "solution": "n = int(input())\nif n == 0:\n    print(0)\nelse:\n    bits = ''\n    while n:\n        bits = str(n % 2) + bits\n        n //= 2\n    print(bits)",
    },
    {
        "title": "GCD and LCM", "difficulty": "Medium", "tags": "math",
        "description": "Find the greatest common divisor and least common multiple of two positive integers.\n\n" + _IO.format(
            "One line with `a b` (1 ≤ a, b ≤ 10⁹).", "`gcd lcm` separated by a space."),
        "solve": _gcd_lcm, "samples": ["12 18", "7 5"], "hidden": ["1 1", "1000000000 999999999", "48 180", "17 289"],
        "solution": "a, b = map(int, input().split())\nx, y = a, b\nwhile y:\n    x, y = y, x % y\nprint(x, a * b // x)",
    },
    {
        "title": "Remove Duplicates", "difficulty": "Medium", "tags": "arrays, hash-map",
        "description": "Remove repeated values from a list, keeping the **first** occurrence of each in its original order.\n\n" + _IO.format(
            "- Line 1: `n` (1 ≤ n ≤ 10⁵)\n- Line 2: `n` integers",
            "The remaining values, separated by spaces.\n\n> Tip: checking `x in result_list` each time is O(n²) and will be too slow."),
        "solve": _dedupe, "samples": [_arr([3, 1, 3, 2, 1]), _arr([7])], "hidden": [_arr([5, 5, 5, 5]), _arr(_dupes)],
        "solution": "input()\nseen = set()\nout = []\nfor x in input().split():\n    if x not in seen:\n        seen.add(x)\n        out.append(x)\nprint(*out)",
    },
    {
        "title": "Matrix Transpose", "difficulty": "Medium", "tags": "arrays, matrix",
        "description": "Flip a matrix over its diagonal: row `i` becomes column `i`.\n\n" + _IO.format(
            "- Line 1: `r c` (1 ≤ r, c ≤ 300)\n- Next `r` lines: `c` integers each", "`c` lines with `r` integers each."),
        "solve": _transpose, "samples": ["2 3\n1 2 3\n4 5 6", "1 1\n9"],
        "hidden": ["3 1\n1\n2\n3", "300 300\n" + "\n".join(" ".join(str(_rng.randint(-99, 99)) for _ in range(300)) for _ in range(300))],
        "solution": "r, c = map(int, input().split())\nm = [input().split() for _ in range(r)]\nfor col in zip(*m):\n    print(*col)",
    },
    {
        "title": "Rotate Array", "difficulty": "Medium", "tags": "arrays",
        "description": "Rotate a list to the right by `k` steps. `k` may be larger than the list length.\n\n" + _IO.format(
            "- Line 1: `n k` (1 ≤ n ≤ 10⁵, 0 ≤ k ≤ 10⁹)\n- Line 2: `n` integers", "The rotated list, separated by spaces."),
        "solve": _rotate, "samples": ["5 2\n1 2 3 4 5", "3 4\n7 8 9"],
        "hidden": ["1 1000000000\n42", "4 0\n1 2 3 4", "100000 999999937\n" + " ".join(map(str, range(100_000)))],
        "solution": "n, k = map(int, input().split())\na = input().split()\nk %= n\nprint(*(a[-k:] + a[:-k]) if k else a)",
    },
    {
        "title": "Run-Length Encoding", "difficulty": "Medium", "tags": "strings, two-pointers",
        "description": ("Compress a string by replacing each run of the same character with the character followed by "
                        "the run length. `aaabbc` becomes `a3b2c1`.\n\n") + _IO.format(
            "One string of letters (1 ≤ length ≤ 10⁵).", "The encoded string."),
        "solve": _rle, "samples": ["aaabbc", "abc"], "hidden": ["z", "zzzzzzzzzz", "aabbaa", "ab" * 20000 + "c" * 30000],
        "solution": "from itertools import groupby\ns = input().strip()\nprint(''.join(f'{ch}{len(list(g))}' for ch, g in groupby(s)))",
    },
    {
        "title": "Merge Sorted Lists", "difficulty": "Medium", "tags": "two-pointers, sorting",
        "description": "Merge two lists that are already sorted in ascending order into one sorted list.\n\n" + _IO.format(
            "- Line 1: `n` (0 ≤ n ≤ 10⁵)\n- Line 2: `n` sorted integers (empty if n = 0)\n- Line 3: `m` (0 ≤ m ≤ 10⁵)\n- Line 4: `m` sorted integers (empty if m = 0)",
            "All `n + m` values in ascending order, separated by spaces."),
        "solve": _merge_sorted, "samples": ["3\n1 4 9\n4\n2 3 9 10", "1\n5\n0\n\n"],
        "hidden": ["2\n-5 -1\n2\n-3 0", _arr(_merge_a) + "\n" + _arr(_merge_b)],
        "solution": ("n = int(input()); a = list(map(int, input().split()))\nm = int(input()); b = list(map(int, input().split()))\n"
                     "i = j = 0; out = []\nwhile i < len(a) and j < len(b):\n    if a[i] <= b[j]: out.append(a[i]); i += 1\n"
                     "    else: out.append(b[j]); j += 1\nout += a[i:] + b[j:]\nprint(*out)"),
    },
    {
        "title": "Binary Search Queries", "difficulty": "Medium", "tags": "binary-search, arrays",
        "description": ("Given a sorted list of **distinct** integers, answer many lookups: for each query, print the 0-based "
                        "index of the value, or `-1` if it is not in the list.\n\n") + _IO.format(
            "- Line 1: `n` (1 ≤ n ≤ 10⁵)\n- Line 2: `n` sorted distinct integers\n- Line 3: `q` (1 ≤ q ≤ 10⁵)\n- Line 4: `q` query values",
            "`q` lines, one answer per query.\n\n> Tip: `list.index()` for every query is O(n·q), far too slow for the big test."),
        "solve": _binary_search, "samples": ["5\n1 3 5 7 9\n3\n7 2 1", "1\n10\n2\n10 11"],
        "hidden": ["3\n-5 0 5\n4\n-5 5 0 6", _arr(_big_sorted) + "\n" + _arr(_big_queries)],
        "solution": ("from bisect import bisect_left\ninput(); a = list(map(int, input().split()))\ninput()\nout = []\n"
                     "for x in map(int, input().split()):\n    i = bisect_left(a, x)\n    out.append(i if i < len(a) and a[i] == x else -1)\n"
                     "print('\\n'.join(map(str, out)))"),
    },
    {
        "title": "Top K Frequent Words", "difficulty": "Medium", "tags": "hash-map, sorting, strings",
        "description": ("Find the `k` most frequent words in a line of text (case-insensitive). Break ties alphabetically. "
                        "If there are fewer than `k` distinct words, print them all.\n\n") + _IO.format(
            "- Line 1: the text (words separated by spaces)\n- Line 2: `k` (1 ≤ k ≤ 100)",
            "Up to `k` lines: the word in lowercase, a space, and its count, most frequent first."),
        "solve": _top_words, "samples": ["the cat and the hat and the bat\n2", "One two three\n5"],
        "hidden": ["b a B A c\n2", _words_line + "\n5"],
        "solution": ("from collections import Counter\nwords = input().lower().split()\nk = int(input())\n"
                     "for w, c in sorted(Counter(words).items(), key=lambda t: (-t[1], t[0]))[:k]:\n    print(w, c)"),
    },
    {
        "title": "Spiral Matrix", "difficulty": "Medium", "tags": "matrix, simulation",
        "description": ("Fill an `n × n` grid with the numbers `1 … n²` in a clockwise spiral, starting at the top-left "
                        "corner and moving right.\n\n") + _IO.format(
            "A single integer `n` (1 ≤ n ≤ 100).", "`n` lines of `n` space-separated numbers."),
        "solve": _spiral, "samples": ["3", "1"], "hidden": ["2", "4", "5", "100"],
        "solution": ("n = int(input())\ng = [[0] * n for _ in range(n)]\ndr, dc = [0, 1, 0, -1], [1, 0, -1, 0]\nr = c = d = 0\n"
                     "for v in range(1, n * n + 1):\n    g[r][c] = v\n    nr, nc = r + dr[d], c + dc[d]\n"
                     "    if not (0 <= nr < n and 0 <= nc < n and g[nr][nc] == 0):\n        d = (d + 1) % 4\n        nr, nc = r + dr[d], c + dc[d]\n"
                     "    r, c = nr, nc\nfor row in g:\n    print(*row)"),
    },
    # ------------------------------------------------------------ Hard
    {
        "title": "Coin Change", "difficulty": "Hard", "tags": "dynamic-programming",
        "description": ("Find the minimum number of coins that add up to `amount`. You have unlimited coins of each "
                        "denomination. Print `-1` if the amount can't be made.\n\n") + _IO.format(
            "- Line 1: `n` (1 ≤ n ≤ 50), the number of denominations\n- Line 2: `n` positive integers\n- Line 3: `amount` (0 ≤ amount ≤ 10⁴)",
            "The minimum number of coins, or `-1`.\n\n> Tip: always taking the largest coin (greedy) gives wrong answers for some coin sets."),
        "solve": _coin_change, "samples": ["3\n1 2 5\n11", "1\n2\n3"],
        "hidden": ["3\n1 3 4\n6", "2\n5 10\n0", "4\n7 13 29 31\n10000", "5\n2 5 10 20 50\n9999"],
        "solution": ("input(); coins = list(map(int, input().split())); amt = int(input())\nINF = amt + 1\n"
                     "dp = [0] + [INF] * amt\nfor c in coins:\n    for a in range(c, amt + 1):\n        if dp[a - c] + 1 < dp[a]:\n            dp[a] = dp[a - c] + 1\n"
                     "print(dp[amt] if dp[amt] <= amt else -1)"),
    },
    {
        "title": "Longest Substring Without Repeats", "difficulty": "Hard", "tags": "strings, sliding-window, hash-map",
        "description": "Find the length of the longest substring that has no repeated characters.\n\n" + _IO.format(
            "One string of lowercase letters (0 ≤ length ≤ 10⁵).",
            "The length of the longest substring with all-distinct characters.\n\n> Tip: checking every substring is O(n²) or worse. Try a sliding window."),
        "solve": _longest_unique, "samples": ["abcabcbb", "bbbbb"], "hidden": ["pwwkew", "\n", "abcdefghijklmnopqrstuvwxyz", _long_text],
        "solution": ("s = input()\nseen = set(); left = best = 0\nfor right, ch in enumerate(s):\n"
                     "    while ch in seen:\n        seen.remove(s[left]); left += 1\n    seen.add(ch)\n    best = max(best, right - left + 1)\nprint(best)"),
    },
    {
        "title": "Number of Islands", "difficulty": "Hard", "tags": "graphs, bfs, matrix",
        "description": ("A grid contains land (`1`) and water (`0`). An island is a group of land cells connected "
                        "up, down, left, or right. Count the islands.\n\n") + _IO.format(
            "- Line 1: `r c` (1 ≤ r, c ≤ 500)\n- Next `r` lines: a string of `c` characters, each `0` or `1`",
            "The number of islands.\n\n> Tip: a recursive flood fill can hit Python's recursion limit on the big grid. Use a queue or stack."),
        "solve": _islands, "samples": ["4 5\n11000\n11000\n00100\n00011", "1 1\n0"],
        "hidden": ["3 3\n111\n101\n111", "2 4\n1010\n0101", "500 500\n" + _grid(500, 500, 0.55, _rng), "500 500\n" + "1" * 500 + ("\n" + "1" * 500) * 499],
        "solution": ("import sys\nr, c = map(int, input().split())\ng = [list(input()) for _ in range(r)]\ncount = 0\n"
                     "for i in range(r):\n    for j in range(c):\n        if g[i][j] == '1':\n            count += 1\n            stack = [(i, j)]; g[i][j] = '0'\n"
                     "            while stack:\n                x, y = stack.pop()\n                for nx, ny in ((x+1,y),(x-1,y),(x,y+1),(x,y-1)):\n"
                     "                    if 0 <= nx < r and 0 <= ny < c and g[nx][ny] == '1':\n                        g[nx][ny] = '0'; stack.append((nx, ny))\nprint(count)"),
    },
    {
        "title": "Edit Distance", "difficulty": "Hard", "tags": "dynamic-programming, strings", "time_limit": 3,
        "description": ("Find the minimum number of single-character insertions, deletions, or replacements needed to "
                        "turn the first string into the second.\n\n") + _IO.format(
            "Two lines, each a string of lowercase letters (0 ≤ length ≤ 1000).", "The edit distance."),
        "solve": _edit_distance, "samples": ["horse\nros", "intention\nexecution"],
        "hidden": ["abc\nabc", "\nabc", "kitten\nsitting", f"{_edit_a}\n{_edit_b}"],
        "solution": ("a = input(); b = input()\nn, m = len(a), len(b)\ndp = [[0] * (m + 1) for _ in range(n + 1)]\n"
                     "for i in range(n + 1): dp[i][0] = i\nfor j in range(m + 1): dp[0][j] = j\nfor i in range(1, n + 1):\n"
                     "    ai = a[i - 1]; row = dp[i]; up = dp[i - 1]\n    for j in range(1, m + 1):\n"
                     "        row[j] = up[j - 1] if ai == b[j - 1] else 1 + min(up[j - 1], up[j], row[j - 1])\nprint(dp[n][m])"),
    },
    {
        "title": "Shortest Path in a Maze", "difficulty": "Hard", "tags": "graphs, bfs",
        "description": ("Find the fewest steps from `S` to `E` in a maze. Each step moves up, down, left, or right "
                        "onto an open cell (`.`, `S`, or `E`). Walls are `#`. Print `-1` if `E` can't be reached.\n\n") + _IO.format(
            "- Line 1: `r c` (1 ≤ r, c ≤ 500)\n- Next `r` lines: `c` characters each. There is exactly one `S` and one `E`.",
            "The minimum number of steps, or `-1`."),
        "solve": _grid_path, "samples": ["3 4\nS..#\n.#..\n...E", "2 2\nS#\n#E"],
        "hidden": ["1 2\nSE", "3 3\nS#.\n.#.\n..E", _maze(300, 300, _rng), _maze(500, 500, _rng, 0.2)],
        "solution": ("from collections import deque\nr, c = map(int, input().split())\ng = [input() for _ in range(r)]\n"
                     "for i, row in enumerate(g):\n    if 'S' in row: s = (i, row.index('S'))\n    if 'E' in row: e = (i, row.index('E'))\n"
                     "dist = [[-1] * c for _ in range(r)]\ndist[s[0]][s[1]] = 0\nq = deque([s])\nwhile q:\n    x, y = q.popleft()\n"
                     "    for nx, ny in ((x+1,y),(x-1,y),(x,y+1),(x,y-1)):\n        if 0 <= nx < r and 0 <= ny < c and g[nx][ny] != '#' and dist[nx][ny] < 0:\n"
                     "            dist[nx][ny] = dist[x][y] + 1; q.append((nx, ny))\nprint(dist[e[0]][e[1]])"),
    },
    {
        "title": "N-Queens Count", "difficulty": "Hard", "tags": "backtracking, recursion",
        "description": ("Count the ways to place `n` queens on an `n × n` chessboard so that no two queens attack each "
                        "other (same row, column, or diagonal).\n\n") + _IO.format(
            "A single integer `n` (1 ≤ n ≤ 10).", "The number of valid placements."),
        "solve": _n_queens, "samples": ["4", "1"], "hidden": ["2", "3", "6", "8", "10"],
        "solution": ("n = int(input())\ndef solve(row, cols, d1, d2):\n    if row == n: return 1\n    total = 0\n"
                     "    for c in range(n):\n        if not (cols >> c & 1 or d1 >> (row + c) & 1 or d2 >> (row - c + n) & 1):\n"
                     "            total += solve(row + 1, cols | 1 << c, d1 | 1 << (row + c), d2 | 1 << (row - c + n))\n    return total\nprint(solve(0, 0, 0, 0))"),
    },
]
