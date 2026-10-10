#!/usr/bin/env python3
"""Validate the fixed dataset and optionally score externally produced JSONL predictions."""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path
from statistics import mean
from typing import Any


ROOT = Path(__file__).resolve().parent
DEFAULT_CASES = ROOT / "cases.zh-CN.jsonl"
DEFAULT_JSON_REPORT = ROOT / "reports" / "latest.json"
DEFAULT_MD_REPORT = ROOT / "reports" / "latest.md"
REQUIRED_CASE_FIELDS = {"id", "category", "input", "knowledgeEnabled", "agentEnabled", "expected"}


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    with path.open("r", encoding="utf-8") as source:
        for line_number, line in enumerate(source, 1):
            if not line.strip():
                continue
            try:
                row = json.loads(line)
            except json.JSONDecodeError as error:
                raise ValueError(f"{path}:{line_number}: JSON 无效：{error.msg}") from error
            if not isinstance(row, dict):
                raise ValueError(f"{path}:{line_number}: 每行必须是 JSON 对象")
            rows.append(row)
    return rows


def validate_cases(cases: list[dict[str, Any]]) -> None:
    if not cases:
        raise ValueError("评测集不能为空")
    seen: set[str] = set()
    for case in cases:
        missing = REQUIRED_CASE_FIELDS - case.keys()
        if missing:
            raise ValueError(f"案例缺少字段 {sorted(missing)}：{case.get('id', '<unknown>')}")
        case_id = case["id"]
        if not isinstance(case_id, str) or not case_id or case_id in seen:
            raise ValueError(f"案例 id 无效或重复：{case_id!r}")
        seen.add(case_id)
        if not isinstance(case["expected"], dict):
            raise ValueError(f"案例 expected 必须是对象：{case_id}")
        if not isinstance(case["input"], str) or not case["input"].strip():
            raise ValueError(f"案例 input 不能为空：{case_id}")


def evaluate(case: dict[str, Any], prediction: dict[str, Any]) -> tuple[bool, list[str]]:
    expected = case["expected"]
    failures: list[str] = []
    content = prediction.get("content") if isinstance(prediction.get("content"), str) else ""
    if expected.get("responseType") and prediction.get("responseType") != expected["responseType"]:
        failures.append("responseType")
    if expected.get("tool") and expected["tool"] not in prediction.get("tools", []):
        failures.append("tool")
    if expected.get("sourcesRequired") and not prediction.get("sourceCount", 0):
        failures.append("sourcesRequired")
    if expected.get("pendingActionRequired") and not prediction.get("pendingAction", False):
        failures.append("pendingActionRequired")
    required_any = expected.get("mustContainAny", [])
    if required_any and not any(value in content for value in required_any):
        failures.append("mustContainAny")
    forbidden = expected.get("mustNotContain", [])
    if any(value in content for value in forbidden):
        failures.append("mustNotContain")
    return not failures, failures


def build_report(cases: list[dict[str, Any]], predictions: list[dict[str, Any]], real: bool) -> dict[str, Any]:
    predictions_by_id = {row.get("id"): row for row in predictions if isinstance(row.get("id"), str)}
    details: list[dict[str, Any]] = []
    latencies: list[float] = []
    input_tokens: list[int] = []
    output_tokens: list[int] = []
    for case in cases:
        prediction = predictions_by_id.get(case["id"])
        if prediction is None:
            continue
        passed, failures = evaluate(case, prediction)
        details.append({"id": case["id"], "category": case["category"], "passed": passed, "failures": failures})
        if isinstance(prediction.get("latencyMs"), (int, float)):
            latencies.append(float(prediction["latencyMs"]))
        if isinstance(prediction.get("inputTokens"), int):
            input_tokens.append(prediction["inputTokens"])
        if isinstance(prediction.get("outputTokens"), int):
            output_tokens.append(prediction["outputTokens"])
    passed_count = sum(item["passed"] for item in details)
    return {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "executionMode": "REAL_MODEL_INTEGRATION" if real else (
            "OFFLINE_PREDICTION_SCORING" if predictions else "OFFLINE_DATASET_VALIDATION"),
        "realModelExecuted": real,
        "dataset": {"locale": "zh-CN", "caseCount": len(cases), "categories": sorted({c["category"] for c in cases})},
        "results": {
            "predictionsEvaluated": len(details),
            "passed": passed_count if details else None,
            "accuracy": round(passed_count / len(details), 4) if details else None,
            "latencyMsAverage": round(mean(latencies), 2) if latencies else None,
            "inputTokensTotal": sum(input_tokens) if input_tokens else None,
            "outputTokensTotal": sum(output_tokens) if output_tokens else None,
        },
        "details": details,
        "notice": "真实模型未执行；本报告只验证数据集结构。" if not predictions else (
            "真实模型调用已由显式集成评测命令执行。" if real else "仅对外部预测文件进行离线评分；未由本脚本调用真实模型。"),
    }


def write_reports(report: dict[str, Any], json_path: Path, md_path: Path) -> None:
    json_path.parent.mkdir(parents=True, exist_ok=True)
    md_path.parent.mkdir(parents=True, exist_ok=True)
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    results = report["results"]
    accuracy = "未执行" if results["accuracy"] is None else f"{results['accuracy']:.2%}"
    latency = "未执行" if results["latencyMsAverage"] is None else f"{results['latencyMsAverage']} ms"
    lines = [
        "# SmartMall TLAI AI 评测报告", "", f"- 执行模式：`{report['executionMode']}`",
        f"- 是否执行真实模型：`{str(report['realModelExecuted']).lower()}`",
        f"- 固定数据集：{report['dataset']['caseCount']} 条中文案例",
        f"- 已评分预测：{results['predictionsEvaluated']} 条", f"- 准确率：{accuracy}",
        f"- 平均耗时：{latency}", f"- 输入 Token：{results['inputTokensTotal'] or '未执行'}",
        f"- 输出 Token：{results['outputTokensTotal'] or '未执行'}", "", f"> {report['notice']}", "",
    ]
    if report["details"]:
        lines += ["## 分项结果", "", "| ID | 类别 | 结果 | 未满足规则 |", "|---|---|---|---|"]
        for item in report["details"]:
            lines.append(f"| {item['id']} | {item['category']} | {'通过' if item['passed'] else '失败'} | {', '.join(item['failures']) or '-'} |")
    md_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cases", type=Path, default=DEFAULT_CASES)
    parser.add_argument("--predictions", type=Path)
    parser.add_argument("--real-model-executed", action="store_true")
    parser.add_argument("--json-report", type=Path, default=DEFAULT_JSON_REPORT)
    parser.add_argument("--markdown-report", type=Path, default=DEFAULT_MD_REPORT)
    args = parser.parse_args()
    cases = read_jsonl(args.cases)
    validate_cases(cases)
    predictions = read_jsonl(args.predictions) if args.predictions else []
    if args.real_model_executed and not args.predictions:
        parser.error("--real-model-executed 必须与 --predictions 一起使用")
    report = build_report(cases, predictions, args.real_model_executed)
    write_reports(report, args.json_report, args.markdown_report)
    print(f"validated={len(cases)} predictions={len(report['details'])} mode={report['executionMode']}")


if __name__ == "__main__":
    main()
