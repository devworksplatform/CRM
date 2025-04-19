import subprocess
import sys
import re

key_path = "C:\\Users\\Jay\\Downloads\\AllowAll.pem"
remote_host = "ec2-13-235-78-112.ap-south-1.compute.amazonaws.com"
remote_user = "ubuntu"

def send_file():
    # """Sends the main.py file to the remote server using scp.
    # Sample Command : scp -i "C:\Users\Jay\Downloads\AllowAll.pem" "main.py" ubuntu@ec2-13-235-78-112.ap-south-1.compute.amazonaws.com:~/CRM
    # """
    source_file = "*"
    remote_path = "~/CRM"

    scp_command = [
        "scp",
        "-i",
        key_path,
        source_file,
        f"{remote_user}@{remote_host}:{remote_path}"
    ]

    try:
        print("Sending file...")
        result = subprocess.run(scp_command, check=True)
        print("File sent successfully.")
    except subprocess.CalledProcessError as e:
        print(f"Error sending file: {e}")
    except FileNotFoundError:
        print(f"Error: Key file not found at '{key_path}' or source file not found at '{source_file}'.")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")


def execute_remote_command():
    """Executes commands on the remote server via SSH with working directory tracking."""
    # Initialize the working directory to home
    current_dir = "~/CRM"

    # temps
    server_cmd_prefix = "source venv/bin/activate && python3 serv.py"
    
    print(f"SSH session started. Current directory: {current_dir}")
    print("Type 'exit' to quit the session")
    
    while True:
        command = input(f"[{current_dir}]> ")
        if command.lower() == "exit":
            break

        if command.lower().startswith("server "):
            cmds = command.split(" ")
            if(len(cmds) > 1):
                if(cmds[1] == "send"):
                    send_file()
                    continue                    
                else:
                    command = server_cmd_prefix + " " + (" ".join(cmds[1:]))

        # Check if the command is a cd command
        cd_match = re.match(r"^\s*cd\s+(.+)$", command)
        if cd_match:
            path = cd_match.group(1).strip()
            
            # Handle relative paths
            if not path.startswith('/') and not path.startswith('~'):
                if current_dir == '~':
                    new_path = f"~/{path}"
                else:
                    new_path = f"{current_dir}/{path}"
            else:
                new_path = path
                
            # Handle parent directory navigation
            if '..' in path:
                # We'll let the server handle this and just check the result
                check_command = [
                    "ssh",
                    "-i",
                    key_path,
                    f"{remote_user}@{remote_host}",
                    f"cd {new_path} && pwd"
                ]
                try:
                    result = subprocess.run(check_command, capture_output=True, text=True, check=True)
                    current_dir = result.stdout.strip()
                    # print(f"Changed directory to: {current_dir}")
                    continue
                except subprocess.CalledProcessError as e:
                    print(f"Error: Could not change to directory '{new_path}'")
                    print(e.stderr)
                    continue
            else:
                # Update the current directory
                current_dir = new_path
                # print(f"Changed directory to: {current_dir}")
                continue
        
        # Prepare the command with the current directory prefix
        full_command = f"cd {current_dir} && {command}"

        if command == "":
            continue
        
        ssh_command = [
            "ssh",
            "-i",
            key_path,
            f"{remote_user}@{remote_host}",
            full_command
        ]

        try:
            # print(f"Executing: {command}")
            result = subprocess.run(ssh_command, capture_output=True, text=True, check=True)
            if result.stdout:
                print(result.stdout.rstrip())
        except subprocess.CalledProcessError as e:
            print(f"Error executing command: {e}")
            if e.stderr:
                print(e.stderr.rstrip())
        except FileNotFoundError:
            print(f"Error: Key file not found at '{key_path}'.")
        except Exception as e:
            print(f"An unexpected error occurred: {e}")
        # finally:
        #     print("-----------")


if __name__ == "__main__":
    if len(sys.argv) == 2:
        if sys.argv[1] == "send":
            send_file()
        elif sys.argv[1] == "ssh":
            execute_remote_command()
        else:
            print("Usage: python config.py send | ssh")
    else:
        print("Usage: python config.py send | ssh")