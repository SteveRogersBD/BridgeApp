import subprocess
import datetime
import sys

def run_git_command(command):
    try:
        result = subprocess.run(
            command,
            check=True,
            text=True,
            capture_output=True,
            shell=True
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error running command {' '.join(command)}:")
        print(e.stderr)
        return None

def main():
    # 1. Check for changes
    print("Checking for changes...")
    status_output = run_git_command(['git', 'status', '--porcelain'])
    
    # If None, error occurred
    if status_output is None:
        return

    # If empty string, no changes
    if not status_output:
        print("No changes to commit. Checking if push is needed...")
        # Even if no changes to commit, we might have unpushed commits
        # Check if we are ahead of remote
        # This is a simple check, usually 'git push' handles "everything up-to-date" gracefully
    else:
        print("Changes found:")
        print(status_output)

        # 2. Add all changes
        print("\nAdding changes...")
        if run_git_command(['git', 'add', '.']) is None:
            print("Failed to add changes.")
            return

        # 3. Commit
        timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        commit_message = f"Auto-commit: {timestamp}"
        
        # Allow optional custom message
        if len(sys.argv) > 1:
            commit_message = " ".join(sys.argv[1:])

        print(f"Committing with message: '{commit_message}'...")
        commit_output = run_git_command(['git', 'commit', '-m', commit_message])
        
        if commit_output:
            print("Commit successful!")
            print(commit_output)
        else:
            print("Commit failed.")
            return

    # 4. Push
    print("\nPushing to remote...")
    push_output = run_git_command(['git', 'push'])
    
    if push_output is not None:
        print("Push successful!")
        if push_output:
            print(push_output)
        else:
            print("Everything up-to-date.")
    else:
        print("Push failed.")

if __name__ == "__main__":
    main()
