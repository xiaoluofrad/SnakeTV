"""
setup_and_build.py
一键解压JDK + Gradle，下载Android SDK cmdline-tools，构建APK。
运行：python setup_and_build.py
"""
import os, sys, subprocess, shutil, zipfile, urllib.request, glob
from pathlib import Path

# ─── 路径 ────────────────────────────────────────────────
WORKSPACE   = Path("C:/03AI工作空间")
PROJECT     = Path("C:/03AI工作空间/SnakeTV")
JDK_ZIP     = WORKSPACE / "jdk17.zip"
GRADLE_ZIP  = WORKSPACE / "gradle-8.1.1.zip"
JDK_HOME    = WORKSPACE / "tools" / "jdk17"
GRADLE_HOME = WORKSPACE / "tools" / "gradle-8.1.1"
SDK_ROOT    = WORKSPACE / "tools" / "android-sdk"
CMDLINE_ZIP = WORKSPACE / "cmdline-tools.zip"
CMDLINE_DIR = SDK_ROOT / "cmdline-tools" / "latest"

CMDLINE_URL     = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
PLATFORM_VER    = "34"
BUILD_TOOLS_VER = "34.0.0"

def log(msg): print(f"[setup] {msg}", flush=True)

# ─── 解压工具 ────────────────────────────────────────────
def unzip(src: Path, dst: Path, strip_top=False):
    """解压zip，strip_top=True则去掉最外层目录名"""
    dst.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(src, "r") as z:
        members = z.infolist()
        prefix = ""
        if strip_top and members:
            first = members[0].filename
            prefix = first.split("/")[0] + "/"
        for m in members:
            name = m.filename
            if strip_top and name.startswith(prefix):
                name = name[len(prefix):]
            if not name:
                continue
            target = dst / name
            if m.is_dir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                with z.open(m) as sf, open(target, "wb") as tf:
                    shutil.copyfileobj(sf, tf)

def download(url, dest: Path):
    if dest.exists():
        log(f"  Already exists: {dest.name}")
        return
    log(f"  Downloading {dest.name} ...")
    def hook(b, bs, total):
        pct = min(100, int(b * bs * 100 / total)) if total > 0 else 0
        print(f"\r  {pct}%", end="", flush=True)
    urllib.request.urlretrieve(url, dest, reporthook=hook)
    print()

# ─── 步骤1：解压JDK ──────────────────────────────────────
def step_jdk():
    if (JDK_HOME / "bin" / "java.exe").exists():
        log("JDK already extracted.")
        return
    if not JDK_ZIP.exists():
        log(f"ERROR: {JDK_ZIP} not found. Re-run download.")
        sys.exit(1)
    log("Extracting JDK ...")
    unzip(JDK_ZIP, JDK_HOME, strip_top=True)
    log(f"JDK ready: {JDK_HOME}")

# ─── 步骤2：解压Gradle ───────────────────────────────────
def step_gradle():
    gradle_exe = GRADLE_HOME / "bin" / "gradle.bat"
    if gradle_exe.exists():
        log("Gradle already extracted.")
        return
    if not GRADLE_ZIP.exists():
        log(f"ERROR: {GRADLE_ZIP} not found.")
        sys.exit(1)
    log("Extracting Gradle ...")
    unzip(GRADLE_ZIP, WORKSPACE / "tools", strip_top=False)
    log(f"Gradle ready: {GRADLE_HOME}")

# ─── 步骤3：下载并安装 Android SDK cmdline-tools ─────────
def step_sdk():
    env = build_env()
    if CMDLINE_DIR.exists():
        log("Android cmdline-tools already installed.")
    else:
        download(CMDLINE_URL, CMDLINE_ZIP)
        log("Extracting cmdline-tools ...")
        tmp = SDK_ROOT / "_tmp"
        unzip(CMDLINE_ZIP, tmp)
        inner = tmp / "cmdline-tools"
        if inner.exists():
            CMDLINE_DIR.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(inner), str(CMDLINE_DIR))
        shutil.rmtree(tmp, ignore_errors=True)

    sdkmanager = CMDLINE_DIR / "bin" / "sdkmanager.bat"
    if not sdkmanager.exists():
        log(f"ERROR: sdkmanager not found at {sdkmanager}")
        sys.exit(1)

    log("Accepting licenses ...")
    subprocess.run(
        [str(sdkmanager), "--licenses"],
        input="y\n" * 20,
        text=True,
        env=env,
    )
    packages = [
        "platform-tools",
        f"platforms;android-{PLATFORM_VER}",
        f"build-tools;{BUILD_TOOLS_VER}",
    ]
    log(f"Installing SDK packages: {packages}")
    subprocess.run([str(sdkmanager)] + packages, env=env, check=True)

# ─── 步骤4：生成 gradle-wrapper.jar（从本地Gradle提取）──
def step_wrapper_jar():
    jar_dest = PROJECT / "gradle" / "wrapper" / "gradle-wrapper.jar"
    if jar_dest.exists() and jar_dest.stat().st_size > 1000:
        log("gradle-wrapper.jar already present.")
        return
    # 在已下载的gradle包中找jar
    candidates = list((GRADLE_HOME / "lib").glob("gradle-wrapper-*.jar"))
    if not candidates:
        candidates = list((GRADLE_HOME).glob("**/gradle-wrapper*.jar"))
    if candidates:
        shutil.copy2(candidates[0], jar_dest)
        log(f"Copied gradle-wrapper.jar from {candidates[0]}")
    else:
        log("Warning: gradle-wrapper.jar not found in local gradle; Gradle may download it automatically.")

# ─── 步骤5：构建APK ──────────────────────────────────────
def step_build():
    env = build_env()
    gradlew = PROJECT / "gradlew.bat"
    log("Building APK (assembleRelease) ...")
    result = subprocess.run(
        ["cmd", "/c", str(gradlew), "assembleRelease"],
        cwd=str(PROJECT), env=env,
    )
    if result.returncode != 0:
        log("Release build failed, trying debug ...")
        result = subprocess.run(
            ["cmd", "/c", str(gradlew), "assembleDebug"],
            cwd=str(PROJECT), env=env,
        )
    if result.returncode == 0:
        apks = list((PROJECT / "app" / "build").glob("**/*.apk"))
        for apk in apks:
            log(f"  APK: {apk}")
        # 复制到工作空间根目录方便取用
        if apks:
            dest = WORKSPACE / "贪吃小队吃豆豆_v1.0.apk"
            shutil.copy2(apks[0], dest)
            log(f"\n✅ APK已复制到：{dest}")
        return True
    else:
        log("❌ 构建失败，请查看上方日志。")
        return False

def build_env():
    env = os.environ.copy()
    env["JAVA_HOME"]         = str(JDK_HOME)
    env["ANDROID_HOME"]      = str(SDK_ROOT)
    env["ANDROID_SDK_ROOT"]  = str(SDK_ROOT)
    env["GRADLE_HOME"]       = str(GRADLE_HOME)
    env["PATH"] = (
        str(JDK_HOME / "bin") + ";" +
        str(GRADLE_HOME / "bin") + ";" +
        str(SDK_ROOT / "platform-tools") + ";" +
        env.get("PATH", "")
    )
    return env

# ─── 主流程 ──────────────────────────────────────────────
if __name__ == "__main__":
    log("=== 贪吃小队吃豆豆 v1.0 自动构建 ===")
    step_jdk()
    step_gradle()
    step_sdk()
    step_wrapper_jar()
    step_build()
