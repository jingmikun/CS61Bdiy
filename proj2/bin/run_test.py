#!/usr/bin/env python3
"""
Simple test runner to test gitlet functionality
"""
import os
import subprocess
import shutil
import sys

def run_cmd(cmd, cwd=None):
    """Run gitlet command"""
    try:
        result = subprocess.run(
            ["java", "-ea", "-cp", ".", "gitlet.Main"] + cmd,
            cwd=cwd,
            capture_output=True,
            text=True,
            timeout=5
        )
        return result.returncode == 0, result.stdout, result.stderr
    except Exception as e:
        return False, "", str(e)

def cleanup_test_dir(test_dir):
    """Clean up test directory"""
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)

def test_status_basic():
    """Test status basic functionality"""
    print("=" * 60)
    print("Test: test-status-basic")
    print("=" * 60)
    
    test_dir = "test_status_basic.dir"
    cleanup_test_dir(test_dir)
    os.makedirs(test_dir)
    
    # Copy wug.txt
    shutil.copy("testing/src/wug.txt", os.path.join(test_dir, "wug.txt"))
    
    os.chdir(test_dir)
    
    try:
        # init
        print("\n1. Running: init")
        success, out, err = run_cmd(["init"])
        if not success:
            print(f"   FAIL: {err}")
            return False
        print("   PASS")
        
        # add
        print("\n2. Running: add wug.txt")
        success, out, err = run_cmd(["add", "wug.txt"])
        if not success:
            print(f"   FAIL: {err}")
            return False
        print("   PASS")
        
        # status
        print("\n3. Running: status")
        success, out, err = run_cmd(["status"])
        if not success:
            print(f"   FAIL: {err}")
            return False
        
        print("   Output:")
        print("   " + "\n   ".join(out.split("\n")))
        
        # Check expected output
        expected_sections = [
            "=== Branches ===",
            "*master",
            "=== Staged Files ===",
            "wug.txt",
            "=== Removed Files ===",
            "=== Modifications Not Staged For Commit ===",
            "=== Untracked Files ==="
        ]
        
        all_found = True
        for section in expected_sections:
            if section not in out:
                print(f"   FAIL: Missing section '{section}'")
                all_found = False
        
        if all_found:
            print("   PASS: All expected sections found")
        else:
            print("   FAIL: Missing some sections")
            return False
        
        # commit
        print("\n4. Running: commit 'added wug'")
        success, out, err = run_cmd(["commit", "added wug"])
        if not success:
            print(f"   FAIL: {err}")
            return False
        print("   PASS")
        
        # status after commit
        print("\n5. Running: status (after commit)")
        success, out, err = run_cmd(["status"])
        if not success:
            print(f"   FAIL: {err}")
            return False
        
        print("   Output:")
        print("   " + "\n   ".join(out.split("\n")))
        
        # Check that staging area is empty
        if "wug.txt" in out.split("=== Staged Files ===")[1].split("===")[0]:
            print("   FAIL: Staging area should be empty")
            return False
        
        print("   PASS: Staging area is empty")
        return True
        
    finally:
        os.chdir("..")
        if not "--keep" in sys.argv:
            cleanup_test_dir(test_dir)

def test_rm_basic():
    """Test rm basic functionality"""
    print("\n" + "=" * 60)
    print("Test: test-rm-basic")
    print("=" * 60)
    
    test_dir = "test_rm_basic.dir"
    cleanup_test_dir(test_dir)
    os.makedirs(test_dir)
    
    shutil.copy("testing/src/wug.txt", os.path.join(test_dir, "wug.txt"))
    os.chdir(test_dir)
    
    try:
        # init
        run_cmd(["init"])
        
        # add
        run_cmd(["add", "wug.txt"])
        
        # status - should show wug.txt in staged
        success, out, err = run_cmd(["status"])
        if "wug.txt" not in out.split("=== Staged Files ===")[1].split("===")[0]:
            print("FAIL: wug.txt should be in staged files")
            return False
        
        # rm
        print("\nRunning: rm wug.txt")
        success, out, err = run_cmd(["rm", "wug.txt"])
        if not success:
            print(f"FAIL: {err}")
            return False
        
        # status - should not show wug.txt in staged
        success, out, err = run_cmd(["status"])
        if "wug.txt" in out.split("=== Staged Files ===")[1].split("===")[0]:
            print("FAIL: wug.txt should not be in staged files after rm")
            return False
        
        print("PASS: rm correctly removes staged file")
        return True
        
    finally:
        os.chdir("..")
        if not "--keep" in sys.argv:
            cleanup_test_dir(test_dir)

if __name__ == "__main__":
    results = []
    results.append(test_status_basic())
    results.append(test_rm_basic())
    
    print("\n" + "=" * 60)
    print(f"Results: {sum(results)}/{len(results)} tests passed")
    print("=" * 60)
    
    sys.exit(0 if all(results) else 1)

