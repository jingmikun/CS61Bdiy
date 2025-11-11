#!/usr/bin/env python3
"""Test global-log actual output"""
import subprocess
import os
import tempfile
import shutil

# Create a temp directory
tmpdir = tempfile.mkdtemp()
os.chdir(tmpdir)

try:
    # Initialize
    subprocess.run(["java", "-cp", "C:/Users/19578/Desktop/CSAIdiy/CS61B/skeleton-sp21/proj2", "gitlet.Main", "init"], check=True)
    
    # Create file and commit
    with open("wug.txt", "w") as f:
        f.write("This is a wug.")
    subprocess.run(["java", "-cp", "C:/Users/19578/Desktop/CSAIdiy/CS61B/skeleton-sp21/proj2", "gitlet.Main", "add", "wug.txt"], check=True)
    subprocess.run(["java", "-cp", "C:/Users/19578/Desktop/CSAIdiy/CS61B/skeleton-sp21/proj2", "gitlet.Main", "commit", "first commit"], check=True)
    
    # Run global-log
    result = subprocess.run(
        ["java", "-cp", "C:/Users/19578/Desktop/CSAIdiy/CS61B/skeleton-sp21/proj2", "gitlet.Main", "global-log"],
        capture_output=True,
        text=True
    )
    
    print("=== ACTUAL OUTPUT ===")
    print(repr(result.stdout))
    print("\n=== FORMATTED OUTPUT ===")
    print(result.stdout)
    
finally:
    os.chdir("..")
    shutil.rmtree(tmpdir)


