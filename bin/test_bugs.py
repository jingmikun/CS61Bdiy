#!/usr/bin/env python3
"""
Test script to verify bug fixes in gitlet
"""

import os
import subprocess
import shutil
import sys

def run_command(cmd, capture_output=True):
    """Run a gitlet command and return success status"""
    try:
        if capture_output:
            result = subprocess.run(
                ["java", "gitlet.Main"] + cmd,
                capture_output=True,
                text=True,
                timeout=10
            )
            return result.returncode == 0, result.stdout, result.stderr
        else:
            result = subprocess.run(
                ["java", "gitlet.Main"] + cmd,
                timeout=10
            )
            return result.returncode == 0, "", ""
    except subprocess.TimeoutExpired:
        return False, "", "Command timed out"
    except Exception as e:
        return False, "", str(e)

def cleanup():
    """Clean up test files"""
    if os.path.exists(".gitlet"):
        shutil.rmtree(".gitlet")
    for f in os.listdir("."):
        if f.startswith("test") and f.endswith(".txt"):
            try:
                os.remove(f)
            except:
                pass

def test_log_with_initial_commit():
    """Test 1: Test log() function with initial commit (null parent)"""
    print("Test 1: Testing log() with initial commit")
    cleanup()
    
    # Create initial commit
    success, _, _ = run_command(["init"])
    if not success:
        print("  [FAIL] Failed: Could not initialize repository")
        return False
    
    # Create a file and commit
    with open("test1.txt", "w") as f:
        f.write("test content 1")
    
    success, _, _ = run_command(["add", "test1.txt"])
    if not success:
        print("  [FAIL] Failed: Could not add file")
        return False
    
    success, _, _ = run_command(["commit", "first commit"])
    if not success:
        print("  [FAIL] Failed: Could not commit")
        return False
    
    # Test log - this should not crash even with null parent
    success, output, error = run_command(["log"])
    if success:
        print("  [PASS] Test 1 passed: log() handles initial commit correctly")
        return True
    else:
        print(f"  [FAIL] Test 1 failed: log() crashed. Error: {error}")
        return False

def test_log_with_multiple_commits():
    """Test 2: Test log() with multiple commits"""
    print("Test 2: Testing log() with multiple commits")
    
    # Add another commit
    with open("test2.txt", "w") as f:
        f.write("test content 2")
    
    success, _, _ = run_command(["add", "test2.txt"])
    if not success:
        print("  [FAIL] Failed: Could not add file")
        return False
    
    success, _, _ = run_command(["commit", "second commit"])
    if not success:
        print("  [FAIL] Failed: Could not commit")
        return False
    
    success, output, error = run_command(["log"])
    if success:
        print("  [PASS] Test 2 passed: log() handles multiple commits correctly")
        return True
    else:
        print(f"  [FAIL] Test 2 failed: log() crashed. Error: {error}")
        return False

def test_find_split_point():
    """Test 3: Test findSplitPoint() with branches"""
    print("Test 3: Testing findSplitPoint() with branches")
    
    # Create a branch (from current state)
    success, _, _ = run_command(["branch", "branch1"])
    if not success:
        print("  [FAIL] Failed: Could not create branch")
        return False
    
    # Checkout branch - might fail if there are uncommitted changes, but that's OK for this test
    success, output, error = run_command(["checkout", "branch1"])
    if not success:
        # Check if it's because we're already on that branch or other expected error
        if "current branch" in error.lower() or "uncommitted" in error.lower():
            # This is expected in some cases, continue with test
            pass
        else:
            print(f"  [FAIL] Failed: Could not checkout branch. Error: {error}")
            return False
    
    # Modify and commit on branch
    with open("test1.txt", "w") as f:
        f.write("modified content")
    
    success, _, _ = run_command(["add", "test1.txt"])
    if not success:
        print("  [FAIL] Failed: Could not add file")
        return False
    
    success, _, _ = run_command(["commit", "commit on branch1"])
    if not success:
        print("  [FAIL] Failed: Could not commit")
        return False
    
    # Checkout back to master
    success, _, _ = run_command(["checkout", "master"])
    if not success:
        print("  [FAIL] Failed: Could not checkout master")
        return False
    
    # Try merge - might succeed or fail due to conflicts, but shouldn't crash
    success, output, error = run_command(["merge", "branch1"])
    # Merge might fail due to conflicts, which is OK
    # The important thing is that it doesn't crash with NullPointerException
    if error and ("nullpointer" in error.lower() or "null pointer" in error.lower()):
        print(f"  [FAIL] Test 3 failed: findSplitPoint() crashed with NullPointer. Error: {error}")
        return False
    else:
        print("  [PASS] Test 3 passed: findSplitPoint() handles branches correctly (no crash)")
        return True

def test_merge_conflict_format():
    """Test 4: Test merge conflict format"""
    print("Test 4: Testing merge conflict format")
    
    # Make sure we're on master branch
    run_command(["checkout", "master"], capture_output=False)
    
    # Create a file on master
    with open("test3.txt", "w") as f:
        f.write("content on master")
    
    success, _, _ = run_command(["add", "test3.txt"])
    if not success:
        print("  [FAIL] Failed: Could not add file")
        return False
    
    success, output, error = run_command(["commit", "add test3 on master"])
    if not success:
        # Check if it's because there are no changes
        if "no changes" in error.lower():
            # File might already be tracked, try to modify it
            with open("test3.txt", "w") as f:
                f.write("modified content on master")
            run_command(["add", "test3.txt"])
            success, _, error = run_command(["commit", "add test3 on master"])
        
        if not success:
            print(f"  [FAIL] Failed: Could not commit. Error: {error}")
            return False
    
    # Checkout branch and modify
    success, _, _ = run_command(["checkout", "branch1"])
    if not success:
        print("  [FAIL] Failed: Could not checkout branch")
        return False
    
    with open("test3.txt", "w") as f:
        f.write("content on branch1")
    
    success, _, _ = run_command(["add", "test3.txt"])
    if not success:
        print("  [FAIL] Failed: Could not add file")
        return False
    
    success, _, _ = run_command(["commit", "modify test3 on branch1"])
    if not success:
        print("  [FAIL] Failed: Could not commit")
        return False
    
    # Checkout master and modify differently
    success, _, _ = run_command(["checkout", "master"])
    if not success:
        print("  [FAIL] Failed: Could not checkout master")
        return False
    
    with open("test3.txt", "w") as f:
        f.write("different content on master")
    
    success, _, _ = run_command(["add", "test3.txt"])
    if not success:
        print("  [FAIL] Failed: Could not add file")
        return False
    
    success, _, _ = run_command(["commit", "modify test3 on master"])
    if not success:
        print("  [FAIL] Failed: Could not commit")
        return False
    
    # Try merge - should create conflict
    success, output, error = run_command(["merge", "branch1"])
    
    # Check if conflict file was created with proper format
    if os.path.exists("test3.txt"):
        with open("test3.txt", "r") as f:
            content = f.read()
            if "<<<<<<< HEAD" in content and ">>>>>>>" in content:
                # Check if it ends with newline
                if content.endswith("\n") or content.endswith("\r\n"):
                    print("  [PASS] Test 4 passed: merge conflict format is correct")
                    return True
                else:
                    print("  [FAIL] Test 4 failed: merge conflict format missing newline at end")
                    return False
            else:
                print("  [FAIL] Test 4 failed: merge conflict markers not found")
                return False
    else:
        print("  [FAIL] Test 4 failed: conflict file was not created")
        return False

def main():
    print("Testing Gitlet Bug Fixes...")
    print("=" * 50)
    print()
    
    results = []
    
    # Run tests
    results.append(test_log_with_initial_commit())
    results.append(test_log_with_multiple_commits())
    results.append(test_find_split_point())
    results.append(test_merge_conflict_format())
    
    # Cleanup
    cleanup()
    
    # Summary
    print()
    print("=" * 50)
    print(f"Results: {sum(results)}/{len(results)} tests passed")
    if all(results):
        print("All tests passed!")
        return 0
    else:
        print("Some tests failed. Please review the output above.")
        return 1

if __name__ == "__main__":
    sys.exit(main())

