#!/usr/bin/env python3
"""Debug individual test"""
import subprocess
import sys
import os

def debug_test(test_name):
    """Run a single test with detailed output"""
    os.chdir("testing")
    try:
        result = subprocess.run(
            ["python", "tester.py", "--show=3", test_name],
            capture_output=True,
            text=True,
            timeout=30
        )
        print(result.stdout)
        if result.stderr:
            print("STDERR:", result.stderr)
    finally:
        os.chdir("..")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        debug_test(sys.argv[1])
    else:
        print("Usage: python debug_test.py <test_name>")
        print("Example: python debug_test.py student_tests/test-global-log.in")


