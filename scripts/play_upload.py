"""Upload the release bundle to a Google Play track.

Usage:
    python scripts/play_upload.py [--track internal] [--notes "What's new"] [--aab path]

Needs a Google Cloud service account that has been invited in Play Console
(Users and permissions) with release rights for this app. The path to its JSON
key comes from the PLAY_SERVICE_ACCOUNT_JSON environment variable or from
`playServiceAccountJson=` in the gitignored keystore.properties. Keep the key
outside the repo.

Requires: pip install google-api-python-client google-auth
"""
import argparse
import os
import sys
from pathlib import Path

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

PACKAGE = "ro.daydreamstalgia.duelmastersinventory"
ROOT = Path(__file__).resolve().parent.parent
DEFAULT_AAB = ROOT / "app/build/outputs/bundle/release/app-release.aab"


def service_account_path() -> str:
    path = os.environ.get("PLAY_SERVICE_ACCOUNT_JSON")
    props = ROOT / "keystore.properties"
    if not path and props.exists():
        for line in props.read_text(encoding="utf-8").splitlines():
            key, _, value = line.partition("=")
            if key.strip() == "playServiceAccountJson":
                path = value.strip()
    if not path or not Path(path).exists():
        sys.exit("No service account key: set PLAY_SERVICE_ACCOUNT_JSON or playServiceAccountJson in keystore.properties")
    return path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--track", default="internal")
    parser.add_argument("--notes", default=None, help="en-US release notes")
    parser.add_argument("--aab", default=str(DEFAULT_AAB))
    parser.add_argument("--draft", action="store_true", help="create the release as a draft instead of rolling it out")
    args = parser.parse_args()

    credentials = service_account.Credentials.from_service_account_file(
        service_account_path(), scopes=["https://www.googleapis.com/auth/androidpublisher"]
    )
    edits = build("androidpublisher", "v3", credentials=credentials, cache_discovery=False).edits()

    edit_id = edits.insert(packageName=PACKAGE, body={}).execute()["id"]
    print(f"Uploading {args.aab} ...")
    bundle = edits.bundles().upload(
        packageName=PACKAGE,
        editId=edit_id,
        media_body=MediaFileUpload(args.aab, mimetype="application/octet-stream", resumable=True, chunksize=8 * 1024 * 1024),
    ).execute()
    version_code = bundle["versionCode"]
    print(f"Uploaded version code {version_code}")

    release = {"versionCodes": [str(version_code)], "status": "draft" if args.draft else "completed"}
    if args.notes:
        release["releaseNotes"] = [{"language": "en-US", "text": args.notes}]
    edits.tracks().update(
        packageName=PACKAGE, editId=edit_id, track=args.track, body={"track": args.track, "releases": [release]}
    ).execute()
    edits.commit(packageName=PACKAGE, editId=edit_id).execute()
    print(f"Released version code {version_code} to the {args.track} track ({release['status']})")


if __name__ == "__main__":
    main()
