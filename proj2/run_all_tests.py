#!/usr/bin/env python3
"""Run all student tests using the official tester"""
import subprocess
import os
import sys
import glob

def run_test(test_file):
    """Run a single test file"""
    print(f"\n{'='*60}")
    print(f"Running: {test_file}")
    print('='*60)
    
    # Change to testing directory
    os.chdir("testing")
    
    try:
        # Run the test
        result = subprocess.run(
            ["python", "tester.py", "--show=1", test_file],
            capture_output=True,
            text=True,
            timeout=30
        )
        
        output = result.stdout + result.stderr
        
        # Check if test passed
        if "ERROR" in output or "FAILED" in output:
            print(f"FAILED: {test_file}")
            print("\nDetails:")
            print(output[-500:])  # Print last 500 chars
            return False
        elif "passed" in output.lower() or result.returncode == 0:
            print(f"PASSED: {test_file}")
            return True
        else:
            print(f"UNKNOWN: {test_file}")
            print(output[-200:])
            return False
            
    except subprocess.TimeoutExpired:
        print(f"TIMEOUT: {test_file}")
        return False
    except Exception as e:
        print(f"ERROR running {test_file}: {e}")
        return False
    finally:
        os.chdir("..")

def main():
    """Run all student tests"""
    test_files = glob.glob("testing/student_tests/test-*.in")
    test_files.sort()
    
    if not test_files:
        print("No test files found!")
        return
    
    print(f"Found {len(test_files)} test files")
    
    results = []
    for test_file in test_files:
        # Get relative path from testing directory
        rel_path = os.path.relpath(test_file, "testing")
        success = run_test(rel_path)
        results.append((rel_path, success))
    
    # Summary
    print("\n" + "="*60)
    print("SUMMARY")
    print("="*60)
    
    passed = [name for name, success in results if success]
    failed = [name for name, success in results if not success]
    
    print(f"\nPassed: {len(passed)}/{len(results)}")
    for name in passed:
        print(f"  [PASS] {name}")
    
    if failed:
        print(f"\nFailed: {len(failed)}/{len(results)}")
        for name in failed:
            print(f"  [FAIL] {name}")
    
    return 0 if len(failed) == 0 else 1

if __name__ == "__main__":
    sys.exit(main())

