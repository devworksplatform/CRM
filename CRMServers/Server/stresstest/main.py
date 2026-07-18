import time
import subprocess

def get_cpu_usage_linux():
    with open("/proc/stat", "r") as f:
        line = f.readline()
    fields = [float(column) for column in line.strip().split()[1:]]
    idle1 = fields[3]
    total1 = sum(fields)

    time.sleep(1)

    with open("/proc/stat", "r") as f:
        line = f.readline()
    fields = [float(column) for column in line.strip().split()[1:]]
    idle2 = fields[3]
    total2 = sum(fields)

    idle_delta = idle2 - idle1
    total_delta = total2 - total1
    usage = 100.0 * (1.0 - idle_delta / total_delta)
    return round(usage, 2)

def get_ram_info_linux():
    meminfo = {}
    with open("/proc/meminfo", "r") as f:
        for line in f:
            parts = line.split(":")
            key = parts[0]
            value = int(parts[1].strip().split()[0])  # in KB
            meminfo[key] = value

    total_kb = meminfo["MemTotal"]
    available_kb = meminfo.get("MemAvailable", meminfo["MemFree"])  # fallback to MemFree
    used_kb = total_kb - available_kb
    percent = (used_kb / total_kb) * 100

    return {
        "total_mb": round(total_kb / 1024, 2),
        "used_mb": round(used_kb / 1024, 2),
        "free_mb": round(available_kb / 1024, 2),
        "percent": round(percent, 2)
    }

# Measure execution time for RAM info
start_time = time.time()
ram = get_ram_info_linux()
end_time = time.time()

elapsed_ms = (end_time - start_time) * 1000  # milliseconds

print(f"RAM Total: {ram['total_mb']} MB")
print(f"RAM Used: {ram['used_mb']} MB")
print(f"RAM Free: {ram['free_mb']} MB")
print(f"RAM Usage: {ram['percent']}%")
print(f"Time taken: {elapsed_ms:.2f} ms")
