import subprocess, os, shutil, sys, re

root = os.path.abspath(os.path.dirname(__file__))
os.chdir(root)

repo = os.path.join(root, 'tmp_find')
if os.path.exists(repo):
    shutil.rmtree(repo)
os.makedirs(repo)
shutil.copy('testing/src/wug.txt', os.path.join(repo, 'wug.txt'))
shutil.copy('testing/src/notwug.txt', os.path.join(repo, 'notwug.txt'))

cmd = ['java', '-ea', '-cp', root, 'gitlet.Main']

def run(*args):
    result = subprocess.run(cmd + list(args), cwd=repo,
                            capture_output=True, text=True)
    if result.returncode != 0:
        print(f'$ gitlet {" ".join(args)} (exit {result.returncode})')
        sys.stdout.write(result.stdout)
        sys.stderr.write(result.stderr)
        sys.exit(result.returncode)
    return result.stdout

try:
    run('init')
    shutil.copy(os.path.join(repo, 'notwug.txt'), os.path.join(repo, 'g.txt'))
    shutil.copy(os.path.join(repo, 'wug.txt'), os.path.join(repo, 'f.txt'))
    run('add', 'g.txt')
    run('add', 'f.txt')
    run('commit', 'Two files')
    run('rm', 'f.txt')
    run('commit', 'Remove one file')
    log_out = run('log')
    print('LOG OUTPUT:\n' + log_out)
    ids = re.findall(r'commit ([0-9a-f]{40})', log_out)
    print('IDS:', ids)
    second = ids[1]
    third = ids[0]
    run('reset', second)
    find_out = run('find', 'Remove one file')
    print('FIND OUTPUT:\n' + find_out)
finally:
    pass
