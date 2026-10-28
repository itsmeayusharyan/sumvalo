content = open('/data/workspace/sumvalo-work/app/src/main/java/com/acuranza/app/ui/about/LocalChangelog.kt', 'r').read()

# Replace escaped quotes in bill countdown lines
# The file has \"Due today\" which in Kotlin means "Due today" inside the string
# We want to replace \"Due today\" with Due today (no quotes)

old1 = 'coloured \"Due today\" / \"Overdue\" pill'
new1 = 'coloured Due today / Overdue pill'
content = content.replace(old1, new1)

old2 = 'coloured \"Due today\" / \"Overdue\" pill next to the card name'
new2 = 'coloured Due today / Overdue pill next to the card name'
content = content.replace(old2, new2)

open('/data/workspace/sumvalo-work/app/src/main/java/com/acuranza/app/ui/about/LocalChangelog.kt', 'w').write(content)

# Verify
lines = content.split('\n')
for i in [55, 71]:
    if i < len(lines):
        print(f'Line {i+1}: {lines[i][:100]}')
