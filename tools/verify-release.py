#!/usr/bin/env python3
"""Verify a signed Duo APK before it is archived or attached to a release."""
import argparse
import hashlib
import os
from pathlib import Path
import re
import subprocess
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--version", required=True)
    parser.add_argument("--code", required=True, type=int)
    parser.add_argument("--certificate", required=True, help="Approved signer SHA-256 fingerprint")
    args = parser.parse_args()
    sdk = Path(os.environ["ANDROID_HOME"]) / "build-tools" / "37.0.0"
    apk = args.apk.resolve(strict=True)
    with zipfile.ZipFile(apk) as archive:
        if archive.testzip() is not None:
            raise SystemExit("APK ZIP integrity failed")
    subprocess.run([str(sdk / "zipalign"), "-c", "-P", "16", "4", str(apk)], check=True,
                   stdout=subprocess.DEVNULL)
    verify = subprocess.check_output([str(sdk / "apksigner"), "verify", "--verbose",
                                      "--print-certs", str(apk)], text=True)
    if "CN=Android Debug" in verify:
        raise SystemExit("Development certificates cannot identify an official release")
    fingerprints = re.findall(r"(?:Signer #\d+|V[234](?:\.\d+)? Signer(?: #\d+)?):? certificate SHA-256 digest: ([0-9a-fA-F]+)", verify)
    expected = args.certificate.replace(":", "").lower()
    if not re.fullmatch(r"[0-9a-f]{64}", expected) or {value.lower() for value in fingerprints} != {expected}:
        raise SystemExit("APK does not have the approved single signing certificate")
    badging = subprocess.check_output([str(sdk / "aapt2"), "dump", "badging", str(apk)], text=True)
    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    if package is None or package.groups() != ("com.zhuhuibin.hypermusiccover.duo", str(args.code), args.version):
        raise SystemExit("APK identity/version differs from the requested release")
    if "application-debuggable" in badging:
        raise SystemExit("An official APK must not be debuggable")
    with apk.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    print(f"Verified {apk.name}: {args.version} ({args.code}), {apk.stat().st_size} bytes")
    print(f"SHA256 {digest}")
    print(f"Signer SHA256 {expected}")


if __name__ == "__main__":
    main()
