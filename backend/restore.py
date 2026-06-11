import json
import os
import re

logs = [
    r"C:\Users\ganuk\.gemini\antigravity\brain\3901e5fd-6899-481f-bd88-511dfab49733\.system_generated\logs\transcript.jsonl",
    r"C:\Users\ganuk\.gemini\antigravity\brain\2eb4607e-a1c9-478f-8b87-bc88785a159f\.system_generated\logs\transcript.jsonl",
    r"C:\Users\ganuk\.gemini\antigravity\brain\ad3b4f5f-522b-43fe-88bf-4a89c01f715b\.system_generated\logs\transcript.jsonl",
    r"C:\Users\ganuk\.gemini\antigravity\brain\990bc63d-dd0a-4388-8da3-f98711575185\.system_generated\logs\transcript.jsonl",
    r"C:\Users\ganuk\.gemini\antigravity\brain\11839ec0-05f7-4efb-a248-9c4e1814b32d\.system_generated\logs\transcript.jsonl"
]

def restore_view_file(output):
    # output contains lines like "File Path: `file:///c:/...`"
    path_match = re.search(r"File Path: `file:///(.*?)`", output, re.IGNORECASE)
    if not path_match:
        return
    path = path_match.group(1).replace('/', os.sep)
    
    if not path.endswith('.java'): return
    
    lines = output.split('\n')
    start_idx = -1
    for i, line in enumerate(lines):
        if line.startswith("The following code has been modified"):
            start_idx = i + 1
            break
    if start_idx == -1: return
    
    content = []
    for line in lines[start_idx:]:
        if line.startswith("The above content shows"):
            break
        # Match "1: package com..."
        match = re.match(r"^\d+: (.*)$", line)
        if match:
            content.append(match.group(1))
        elif line.strip() == "":
            pass # ignore empty line at end
            
    if content:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(content))
        print(f"Restored: {path}")

for log_path in logs:
    if not os.path.exists(log_path): continue
    with open(log_path, 'r', encoding='utf-8') as f:
        for line in f:
            try:
                data = json.loads(line)
                
                # Check tool calls for write_to_file
                if 'tool_calls' in data:
                    for call in data['tool_calls']:
                        # The tool call format might be 'default_api:write_to_file' or just 'write_to_file'
                        if call.get('name', '').endswith('write_to_file') and 'args' in call:
                            args = call['args']
                            if 'TargetFile' in args and 'CodeContent' in args:
                                target = args['TargetFile']
                                if target.endswith('.java'):
                                    os.makedirs(os.path.dirname(target), exist_ok=True)
                                    with open(target, 'w', encoding='utf-8') as out:
                                        out.write(args['CodeContent'])
                                    print(f"Restored from write_to_file: {target}")
                
                # Check VIEW_FILE responses
                if data.get('type') == 'VIEW_FILE' and data.get('content'):
                    restore_view_file(data['content'])
            except Exception as e:
                pass
