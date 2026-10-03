---
title: Working with Files
minutes: 8
practice: strings
---
Programs often read data from files and save results to them. The safe way to open a file is with a `with` block, which closes the file automatically, even if an error happens.

> The playgrounds on this page use `io.StringIO`, an in-memory "file", so they can run here. With real files you'd use `open("data.txt")` exactly the same way.

## Writing a file

```python
with open("notes.txt", "w") as f:        # "w" = write (creates or overwrites)
    f.write("first line\n")
    f.write("second line\n")

with open("notes.txt", "a") as f:        # "a" = append to the end
    print("third line", file=f)          # print() can write to files too
```

## Reading a file

```python
with open("notes.txt") as f:             # default mode "r" = read
    text = f.read()                      # the whole file as one string

with open("notes.txt") as f:
    for line in f:                       # line by line: memory-friendly
        print(line.strip())
```

Try the same reading patterns on an in-memory file:

```pyrun
import io

f = io.StringIO("alice,90\nbob,72\ncarol,85\n")
scores = {}
for line in f:
    name, score = line.strip().split(",")
    scores[name] = int(score)
print(scores)
print("best:", max(scores, key=scores.get))
```

## CSV files

Real CSV data can contain quoted commas, so use the `csv` module instead of a plain `split(",")`:

```pyrun
import csv, io

data = io.StringIO('name,city\n"Smith, Jo",Paris\nLee,Seoul\n')
for row in csv.DictReader(data):
    print(row["name"], "->", row["city"])

out = io.StringIO()
writer = csv.writer(out)
writer.writerow(["product", "price"])
writer.writerow(["tea, green", 2.5])
print(out.getvalue())
```

## JSON: saving structured data

```pyrun
import json

profile = {"name": "Ada", "skills": ["python", "math"], "level": 3}
text = json.dumps(profile, indent=2)     # to a string
print(text)
back = json.loads(text)                  # and back again
print(back["skills"][0])
```

With files, use `json.dump(obj, f)` and `json.load(f)`.

## File modes at a glance

| Mode | Meaning |
|---|---|
| `"r"` | read (default); error if the file doesn't exist |
| `"w"` | write; **erases** existing content |
| `"a"` | append to the end |
| `"x"` | create; error if the file already exists |
| `"rb"`, `"wb"` | binary read/write (images, etc.) |

Opening a missing file for reading raises `FileNotFoundError`. Catch it if the file is optional.

<!-- quiz -->

Q: Why use `with open(...) as f:`?
- [ ] It's the only way to open files
- [x] It closes the file automatically, even if an error occurs
- [ ] It makes reading faster
- [ ] It locks the file from other programs
> The `with` block guarantees cleanup when the block ends.

Q: What happens if you open an existing file with mode `"w"`?
- [ ] New text is added to the end
- [x] The existing content is erased
- [ ] Python raises an error
- [ ] It opens read-only
> `"w"` truncates the file. Use `"a"` to append instead.

Q: Why use the `csv` module instead of `line.split(",")`?
- [ ] `split` doesn't work on files
- [x] CSV fields can contain quoted commas, which `split` would break apart
- [ ] The `csv` module is required by Python
- [ ] `split` is deprecated
> `"Smith, Jo"` is one field, but `split(",")` would cut it in two.
