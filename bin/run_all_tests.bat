@echo off
REM Run all student tests
cd testing
python tester.py --show=all student_tests\test-*.in
cd ..

