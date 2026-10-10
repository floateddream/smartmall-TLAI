#!/usr/bin/env python3
"""Explicit opt-in integration evaluation against a running SmartMall instance."""

from __future__ import annotations

import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent


def required(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise SystemExit(f"缺少必需环境变量：{name}")
    return value


def main() -> None:
    if os.environ.get("SMARTMALL_EVAL_ALLOW_REAL_MODEL") != "YES":
        raise SystemExit("真实模型评测未获显式授权；请设置 SMARTMALL_EVAL_ALLOW_REAL_MODEL=YES 后主动运行本脚本。")
    base_url = required("SMARTMALL_EVAL_BASE_URL").rstrip("/")
    token = required("SMARTMALL_EVAL_TOKEN")
    predictions_path = ROOT / "reports" / "real-model-predictions.jsonl"
    predictions_path.parent.mkdir(parents=True, exist_ok=True)
    cases = [json.loads(line) for line in (ROOT / "cases.zh-CN.jsonl").read_text(encoding="utf-8").splitlines() if line.strip()]
    with predictions_path.open("w", encoding="utf-8") as output:
        for case in cases:
            payload = json.dumps({
                "messages": [{"role": "USER", "content": case["input"]}],
                "knowledgeEnabled": case["knowledgeEnabled"], "agentEnabled": case["agentEnabled"],
            }, ensure_ascii=False).encode("utf-8")
            request = urllib.request.Request(
                f"{base_url}/api/ai/chat", data=payload, method="POST",
                headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"})
            started = time.perf_counter()
            try:
                with urllib.request.urlopen(request, timeout=180) as response:
                    body = json.load(response)
            except urllib.error.HTTPError as error:
                raise SystemExit(f"案例 {case['id']} 请求失败：HTTP {error.code}") from error
            data = body.get("data", {})
            prediction = {
                "id": case["id"], "content": data.get("content", ""),
                "responseType": data.get("responseType"),
                "tools": [tool.get("toolName") for tool in data.get("tools", []) if isinstance(tool, dict)],
                "sourceCount": len(data.get("sources", [])), "pendingAction": bool(data.get("pendingAction")),
                "latencyMs": round((time.perf_counter() - started) * 1000),
                "inputTokens": None, "outputTokens": None,
            }
            output.write(json.dumps(prediction, ensure_ascii=False) + "\n")
    subprocess.run([
        sys.executable, str(ROOT / "run_evals.py"), "--predictions", str(predictions_path),
        "--real-model-executed"], check=True)


if __name__ == "__main__":
    main()
