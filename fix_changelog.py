content = open('app/src/main/java/com/acuranza/app/ui/about/LocalChangelog.kt', 'r').read()
# Fix escaped quotes in bill countdown lines
content = content.replace(
    'ChangeItem(\\"Bill countdown badges on home: urgent/overdue bills show a coloured \\\"Due today\\\" / \\\"Overdue\\\" pill\\\", ChangeCategory.ADDED),',
    'ChangeItem("Bill countdown badges on home: urgent/overdue bills show a coloured Due today / Overdue pill", ChangeCategory.ADDED),'
)
content = content.replace(
    'ChangeItem(\\"Bill countdown badges on the home screen: urgent and overdue bills show a coloured \\\"Due today\\\" / \\\"Overdue\\\" pill next to the card name\\\", ChangeCategory.ADDED),',
    'ChangeItem("Bill countdown badges on the home screen: urgent and overdue bills show a coloured Due today / Overdue pill next to the card name", ChangeCategory.ADDED),'
)
open('app/src/main/java/com/acuranza/app/ui/about/LocalChangelog.kt', 'w').write(content)
print('Done')
lines = content.split('\n')
for i in [55, 71]:
    if i < len(lines):
        print(f'Line {i+1}: {lines[i][:100]}')
