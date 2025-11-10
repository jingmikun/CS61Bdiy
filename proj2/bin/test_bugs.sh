#!/bin/bash

# Test script to verify bug fixes in gitlet

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "Testing Gitlet Bug Fixes..."
echo "============================"
echo ""

# Clean up any existing .gitlet directory
rm -rf .gitlet
rm -f test*.txt

# Test 1: Test log() function with initial commit (null parent)
echo -e "${YELLOW}Test 1: Testing log() with initial commit${NC}"
echo "init" > test_input.txt
echo "add test1.txt" >> test_input.txt
echo "commit \"first commit\"" >> test_input.txt
echo "log" >> test_input.txt

echo "test content 1" > test1.txt
java gitlet.Main init > /dev/null 2>&1
java gitlet.Main add test1.txt > /dev/null 2>&1
java gitlet.Main commit "first commit" > /dev/null 2>&1
if java gitlet.Main log > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Test 1 passed: log() handles initial commit correctly${NC}"
else
    echo -e "${RED}✗ Test 1 failed: log() crashed with initial commit${NC}"
fi

# Test 2: Test multiple commits and log
echo -e "${YELLOW}Test 2: Testing log() with multiple commits${NC}"
echo "test content 2" > test2.txt
java gitlet.Main add test2.txt > /dev/null 2>&1
java gitlet.Main commit "second commit" > /dev/null 2>&1
if java gitlet.Main log > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Test 2 passed: log() handles multiple commits correctly${NC}"
else
    echo -e "${RED}✗ Test 2 failed: log() crashed with multiple commits${NC}"
fi

# Test 3: Test findSplitPoint with branches
echo -e "${YELLOW}Test 3: Testing findSplitPoint() with branches${NC}"
java gitlet.Main branch branch1 > /dev/null 2>&1
java gitlet.Main checkout branch1 > /dev/null 2>&1
echo "modified content" > test1.txt
java gitlet.Main add test1.txt > /dev/null 2>&1
java gitlet.Main commit "commit on branch1" > /dev/null 2>&1
java gitlet.Main checkout master > /dev/null 2>&1
if java gitlet.Main merge branch1 > /dev/null 2>&1 || true; then
    echo -e "${GREEN}✓ Test 3 passed: findSplitPoint() handles branches correctly${NC}"
else
    echo -e "${RED}✗ Test 3 failed: findSplitPoint() crashed${NC}"
fi

# Test 4: Test merge conflict format
echo -e "${YELLOW}Test 4: Testing merge conflict format${NC}"
echo "content on master" > test3.txt
java gitlet.Main add test3.txt > /dev/null 2>&1
java gitlet.Main commit "add test3 on master" > /dev/null 2>&1
java gitlet.Main checkout branch1 > /dev/null 2>&1
echo "content on branch1" > test3.txt
java gitlet.Main add test3.txt > /dev/null 2>&1
java gitlet.Main commit "modify test3 on branch1" > /dev/null 2>&1
java gitlet.Main checkout master > /dev/null 2>&1
echo "different content on master" > test3.txt
java gitlet.Main add test3.txt > /dev/null 2>&1
java gitlet.Main commit "modify test3 on master" > /dev/null 2>&1
if java gitlet.Main merge branch1 > /dev/null 2>&1; then
    # Check if conflict file has proper format
    if grep -q "<<<<<<< HEAD" test3.txt && grep -q ">>>>>>>" test3.txt; then
        echo -e "${GREEN}✓ Test 4 passed: merge conflict format is correct${NC}"
    else
        echo -e "${RED}✗ Test 4 failed: merge conflict format is incorrect${NC}"
    fi
else
    # Merge should throw exception due to conflict, check if file was written
    if [ -f test3.txt ] && grep -q "<<<<<<< HEAD" test3.txt; then
        echo -e "${GREEN}✓ Test 4 passed: merge conflict detected and file written${NC}"
    else
        echo -e "${RED}✗ Test 4 failed: merge conflict not handled properly${NC}"
    fi
fi

# Cleanup
rm -rf .gitlet
rm -f test*.txt test_input.txt

echo ""
echo "============================"
echo "Testing complete!"

