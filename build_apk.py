"""
自动化构建脚本：下载 Android SDK cmdline-tools，配置环境，构建APK
运行方式：python build_apk.py
"""
import os
import sys
import subprocess
import zipfile
import shutil
import urllib.request
import urllib.error
from pathlib import Path

# ── 路径配置 ──────────────────────────────────────────────
HOME        = Path(os.environ.get("USERPROFILE", Path.home()))
SDK_ROOT    = HOME / ".workbuddy" / "android-sdk"
CMDLINE_DIR = SDK_ROOT / "cmdline-tools" / "latest"
BUILD_TOOLS_VER = "34.0.0"
PLATFORM_VER    = "34"
PROJECT_DIR = Path(__file__).parent.resolve()
APK_OUT     = PROJECT_DIR / "app" / "build" / "outputs" / "apk" / "release"

# Android cmdline-tools 下载 URL（官方最新稳定版）
CMDLINE_URL = (
    "https://dl.google.com/android/repository/"
    "commandlinetools-win-11076708_latest.zip"
)

JDK_HOME_CANDIDATES = [
    HOME / ".workbuddy" / "binaries" / "jdk",
    Path("C:/Program Files/Microsoft") ,
    Path("C:/Program Files/Java"),
    Path("C:/Program Files/Eclipse Adoptium"),
]

def log(msg):
    print(f"[build] {msg}", flush=True)

def find_jdk():
    """在常见位置寻找 java.exe"""
    for base in JDK_HOME_CANDIDATES:
        if not base.exists():
            continue
        for child in base.iterdir():
            java = child / "bin" / "java.exe"
            if java.exists():
                return child
    return None

def download_file(url, dest: Path):
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists():
        log(f"Already downloaded: {dest.name}")
        return
    log(f"Downloading {dest.name} ...")
    try:
        urllib.request.urlretrieve(url, dest, reporthook=lambda b, bs, t: print(
            f"\r  {min(100, int(b*bs*100/t if t>0 else 0))}%", end="", flush=True))
        print()
    except Exception as e:
        log(f"ERROR downloading {url}: {e}")
        sys.exit(1)

def extract_zip(src: Path, dst: Path, strip_prefix=None):
    log(f"Extracting {src.name} → {dst}")
    with zipfile.ZipFile(src, "r") as z:
        for member in z.infolist():
            name = member.filename
            if strip_prefix and name.startswith(strip_prefix):
                name = name[len(strip_prefix):]
            if not name:
                continue
            target = dst / name
            if member.is_dir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                with z.open(member) as src_f, open(target, "wb") as dst_f:
                    shutil.copyfileobj(src_f, dst_f)

def setup_sdk(jdk_home: Path):
    """下载 cmdline-tools 并接受 licenses"""
    zip_path = SDK_ROOT / "cmdline-tools.zip"
    download_file(CMDLINE_URL, zip_path)

    if not CMDLINE_DIR.exists():
        tmp_extract = SDK_ROOT / "_tmp_cmdline"
        extract_zip(zip_path, tmp_extract)
        # 官方zip内有 cmdline-tools/ 目录，移动到 latest/
        inner = tmp_extract / "cmdline-tools"
        if inner.exists():
            CMDLINE_DIR.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(inner), str(CMDLINE_DIR))
        shutil.rmtree(tmp_extract, ignore_errors=True)

    sdkmanager = CMDLINE_DIR / "bin" / "sdkmanager.bat"
    if not sdkmanager.exists():
        log(f"ERROR: sdkmanager not found at {sdkmanager}")
        sys.exit(1)

    env = os.environ.copy()
    env["JAVA_HOME"] = str(jdk_home)
    env["ANDROID_HOME"] = str(SDK_ROOT)
    env["ANDROID_SDK_ROOT"] = str(SDK_ROOT)

    log("Accepting Android SDK licenses ...")
    proc = subprocess.run(
        [str(sdkmanager), "--licenses"],
        input="y\ny\ny\ny\ny\ny\ny\n",
        capture_output=False,
        text=True,
        env=env,
    )

    log(f"Installing platform-tools, platforms;android-{PLATFORM_VER}, build-tools;{BUILD_TOOLS_VER} ...")
    subprocess.run(
        [str(sdkmanager),
         "platform-tools",
         f"platforms;android-{PLATFORM_VER}",
         f"build-tools;{BUILD_TOOLS_VER}"],
        env=env,
        check=True,
    )
    return env

def build_apk(env: Path):
    log("Running Gradle assembleRelease ...")
    gradlew = PROJECT_DIR / "gradlew.bat"
    if not gradlew.exists():
        # 生成 gradlew wrapper
        log("gradlew.bat not found, will use gradle wrapper from wrapper properties")

    result = subprocess.run(
        ["cmd", "/c", str(gradlew), "assembleRelease", "--stacktrace"],
        cwd=str(PROJECT_DIR),
        env=env,
    )
    if result.returncode != 0:
        log("Release build failed, trying debug ...")
        result = subprocess.run(
            ["cmd", "/c", str(gradlew), "assembleDebug", "--stacktrace"],
            cwd=str(PROJECT_DIR),
            env=env,
        )
    return result.returncode == 0

def main():
    log("=== 贪吃小队吃豆豆 APK 自动构建脚本 ===")

    # 1. 找JDK
    jdk_home = find_jdk()
    if not jdk_home:
        log("ERROR: JDK 未找到，请确保 JDK 已解压到 ~/.workbuddy/binaries/jdk/")
        sys.exit(1)
    log(f"JDK found: {jdk_home}")

    # 2. 配置SDK
    env = setup_sdk(jdk_home)

    # 3. 构建
    ok = build_apk(env)
    if ok:
        # 找APK
        for pattern in ["**/*.apk"]:
            for f in (PROJECT_DIR / "app" / "build").glob(pattern):
                log(f"APK ready: {f}")
    else:
        log("构建失败，请查看上方错误信息")
        sys.exit(1)

if __name__ == "__main__":
    main()
