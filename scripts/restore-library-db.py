import argparse
import gzip
import json
import re
import subprocess
import tarfile


parser = argparse.ArgumentParser()
parser.add_argument("archive")
parser.add_argument("--database")
parser.add_argument("--user", default="root")
args = parser.parse_args()

if not args.database or not re.fullmatch(r"[A-Za-z0-9_]+", args.database):
    parser.error("请使用 --database 指定合法的目标数据库名")

statements = [f"CREATE DATABASE IF NOT EXISTS `{args.database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;",
              f"USE `{args.database}`;", "SET FOREIGN_KEY_CHECKS=0;"]

with tarfile.open(args.archive) as archive:
    for member in archive.getmembers():
        if not member.name.endswith(".meta.json.gz"):
            continue
        metadata = json.loads(gzip.decompress(archive.extractfile(member).read()))
        statements.append(metadata["DDL"] + ";")
        for data in metadata.get("Data", []):
            rows = gzip.decompress(archive.extractfile(data["Filename"]).read())
            rows = rows.decode("utf-8", "replace").strip("\x1e\r\n")
            if rows:
                statements.append(f"INSERT INTO `{metadata['Name']}` VALUES\n{rows.replace(chr(30), ',\n')};")

statements.append("SET FOREIGN_KEY_CHECKS=1;")
subprocess.run(["mysql", "--user", args.user], input="\n".join(statements),
               text=True, encoding="utf-8", check=True)
