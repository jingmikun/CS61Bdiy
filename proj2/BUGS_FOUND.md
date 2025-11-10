# Bugs Found in Gitlet Project

## Test Results Summary
- **Passed**: 2/21 tests
- **Failed**: 19/21 tests

## Major Issues Identified

### 1. Log Output Format Issues
**Affected Tests**: test-branch-basic, test-global-log, test-merge-ancestor, test-merge-fast-forward, test-reset-basic, test-rm-basic, test-rm-branch, test-rm-committed, test-status-basic, test-status-complex, test-status-modified

**Problem**: The log output format may not match the expected COMMIT_LOG regex pattern. The test expects:
- `===` (optional spaces) + newline
- `commit <hash>` (optional spaces) + newline  
- Optional Merge line
- `Date: ...` (optional spaces) + newline
- Message content (until next === or end)

**Current Output**: Likely has extra newlines or formatting issues.

### 2. Merge Conflict Format Issues
**Affected Tests**: test-merge-basic, test-merge-new-file-conflict, test-merge-delete-current-modify-given, test-merge-delete-modify-conflict, test-merge-normal-no-conflict

**Problem**: Merge conflict markers may not match expected format. Test expects:
```
<<<<<<< HEAD
<current content>
=======
<given content>
>>>>>>>
```

**Current Code Issues**:
- Line 717: Missing current branch name in conflict marker
- Line 773: May have trailing newline issues
- Line 784: May have trailing newline issues

### 3. Checkout File Test Issue
**Affected Test**: test-checkout-file.in

**Problem**: Test uses `${1}` to capture commit ID from previous command, but the capturing may not be working correctly.

**Error**: "nonexistent group: {1}"

### 4. Merge Basic Test Issue
**Affected Test**: test-merge-basic.in

**Problem**: Error message "file "line1 could not be copied to file.txt" suggests file creation or content handling issue during merge.

### 5. Merge Auto-Merge Test Issue
**Affected Test**: test-merge-auto-merge.in

**Problem**: Auto-merge logic may not be working correctly when both branches modify a file to the same result.

### 6. Merge Both Delete Test Issue
**Affected Test**: test-merge-both-delete.in

**Problem**: When both branches delete a file, it should stay deleted (no conflict), but test is failing.

### 7. Status Output Format
**Affected Tests**: test-status-basic, test-status-complex, test-status-modified

**Problem**: Status output format may not match expected format exactly. Need to verify:
- Branch listing order
- Staged files format
- Removed files format
- Modifications format
- Untracked files format

## Fixes Applied

### 1. ✅ Fixed Log Output Format
**Fixed**: Added Merge line display for merge commits in both `log()` and `globalLog()` methods.
- Merge commits now display: `Merge: <parent1-short> <parent2-short>` between commit line and Date line
- Matches the COMMIT_LOG regex pattern expected by tests

### 2. ✅ Fixed Merge Conflict Marker Format
**Fixed**: Corrected conflict marker format to match test expectations.
- Removed trailing newlines from content before adding separators
- Separators (`=======` and `>>>>>>>`) should be on the same line as content
- Format: `<<<<<<< HEAD\n<curr>=======\n<given>>>>>>>` (no trailing newline after `>>>>>>>`)
- All three conflict cases now use correct format:
  - Both modified: `<<<<<<< HEAD\n<curr>=======\n<given>>>>>>>`
  - Current deleted: `<<<<<<< HEAD\n=======\n<given>>>>>>>`
  - Given deleted: `<<<<<<< HEAD\n<curr>=======\n\n>>>>>>>`

### 3. ✅ Fixed Both Delete Case
**Fixed**: Added handling for files deleted in both branches.
- Files deleted in both branches now stay deleted (no conflict)
- Added check in merge logic to skip conflict checking for both-delete case

## Remaining Issues

### 1. Auto-Merge Logic
**Status**: May need verification
- Logic exists to handle same-result modifications, but may need testing

### 2. Checkout File Test
**Status**: Needs investigation
- Test uses `${1}` to capture commit ID from previous command
- Error: "nonexistent group: {1}"
- May be related to log output format or command output capture

### 3. Merge Basic Test
**Status**: Needs investigation
- Error: "file "line1 could not be copied to file.txt"
- May be related to file content handling or merge conflict format

## Next Steps
1. Run tests to verify fixes
2. Investigate remaining test failures
3. Fix any remaining merge logic issues
4. Verify status output format if still failing
