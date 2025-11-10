#!/usr/bin/env python3
"""Test merge actual output"""
import subprocess
import os
import tempfile
import shutil

# Create a temp directory
tmpdir = tempfile.mkdtemp()
os.chdir(tmpdir)

try:
    proj_path = "C:/Users/19578/Desktop/CSAIdiy/CS61B/skeleton-sp21/proj2"
    
    # Initialize
    subprocess.run(["java", "-cp", proj_path, "gitlet.Main", "init"], check=True, capture_output=True)
    
    # Create file and commit
    with open("file.txt", "w") as f:
        f.write("original\n")
    subprocess.run(["java", "-cp", proj_path, "gitlet.Main", "add", "file.txt"], check=True, capture_output=True)
    subprocess.run(["java", "-cp", proj_path, "gitlet.Main", "commit", "initial commit"], check=True, capture_output=True)
    
    # Create branch and checkout
    subprocess.run(["java", "-cp", proj_path, "gitlet.Main", "branch", "other"], check=True, capture_output=True)
    subprocess.run(["java", "-cp", proj_path, "gitlet.Main", "checkout", "other"], check=True, capture_output=True)
    
    # Modify file and commit
    with open("file.txt", "w") as f:
        f.write("modified in other\n")
    subprocess.run(["java", "-cp", proj_path, "gitlet.Main", "add", "file.txt"], check=True, capture_output=True)
    subprocess.run(["java", "-cp", proj_path, "gitlet.Main", "commit", "modified in other"], check=True, capture_output=True)
    
    # Checkout master
    subprocess.run(["java", "-cp", proj_path, "gitlet.Main", "checkout", "master"], check=True, capture_output=True)
    
    # Merge
    result = subprocess.run(
        ["java", "-cp", proj_path, "gitlet.Main", "merge", "other"],
        capture_output=True,
        text=True
    )
    
    print("=== MERGE STDOUT ===")
    print(repr(result.stdout))
    print("\n=== MERGE STDERR ===")
    print(repr(result.stderr))
    print("\n=== RETURN CODE ===")
    print(result.returncode)
    
    print("\n=== FILE CONTENT ===")
    if os.path.exists("file.txt"):
        with open("file.txt", "r") as f:
            content = f.read()
        print(repr(content))
    else:
        print("File does not exist")
        
finally:
    os.chdir("..")
    shutil.rmtree(tmpdir)


