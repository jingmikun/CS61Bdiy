#!/usr/bin/env python3
"""Quick test to verify gitlet functionality"""
import subprocess
import os
import shutil

def run_gitlet(cmd, cwd="."):
    """Run gitlet command"""
    try:
        # Get the absolute path to the project root
        project_root = os.path.abspath(os.path.dirname(__file__))
        
        # Use project root as classpath (package structure)
        result = subprocess.run(
            ["java", "-ea", "-cp", project_root, "gitlet.Main"] + cmd,
            cwd=cwd,
            capture_output=True,
            text=True,
            timeout=5
        )
        return result.returncode == 0, result.stdout, result.stderr
    except Exception as e:
        return False, "", str(e)

# Test 1: Status basic
print("=" * 60)
print("Test 1: Status Basic")
print("=" * 60)

test_dir = "quick_test_dir"
if os.path.exists(test_dir):
    shutil.rmtree(test_dir)
os.makedirs(test_dir)

# Copy test file
shutil.copy("testing/src/wug.txt", os.path.join(test_dir, "wug.txt"))

os.chdir(test_dir)

try:
    # init
    print("\n1. init")
    success, out, err = run_gitlet(["init"])
    if not success:
        print(f"   ERROR: {err}")
    else:
        print("   OK")
    
    # add
    print("\n2. add wug.txt")
    success, out, err = run_gitlet(["add", "wug.txt"])
    if not success:
        print(f"   ERROR: {err}")
    else:
        print("   OK")
    
    # status
    print("\n3. status")
    success, out, err = run_gitlet(["status"])
    if not success:
        print(f"   ERROR: {err}")
    else:
        print("   Output:")
        for line in out.split("\n"):
            print(f"   {line}")
        
        # Check key sections
        checks = [
            ("Branches", "=== Branches ===" in out),
            ("Staged Files", "=== Staged Files ===" in out),
            ("wug.txt in staged", "wug.txt" in out.split("=== Staged Files ===")[1].split("===")[0] if "=== Staged Files ===" in out else False),
        ]
        
        print("\n   Checks:")
        all_ok = True
        for name, check in checks:
            status = "[PASS]" if check else "[FAIL]"
            print(f"   {status} {name}")
            if not check:
                all_ok = False
        
        if all_ok:
            print("\n   Test 1 PASSED")
        else:
            print("\n   Test 1 FAILED")
    
    # commit
    print("\n4. commit")
    success, out, err = run_gitlet(["commit", "added wug"])
    if not success:
        print(f"   ERROR: {err}")
    else:
        print("   OK")
    
    # status after commit
    print("\n5. status (after commit)")
    success, out, err = run_gitlet(["status"])
    if success:
        print("   Output:")
        for line in out.split("\n"):
            print(f"   {line}")
        
        # Check staging area is empty
        staged_section = out.split("=== Staged Files ===")[1].split("===")[0] if "=== Staged Files ===" in out else ""
        if "wug.txt" not in staged_section:
            print("\n   [PASS] Staging area is empty (correct)")
        else:
            print("\n   [FAIL] Staging area should be empty")
    
finally:
    os.chdir("..")
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)

print("\n" + "=" * 60)
print("Test completed")
print("=" * 60)

