# AI 固定评测集

`cases.zh-CN.jsonl` 覆盖普通对话、RAG 有依据回答/无依据拒答、提示注入、Analytics 和 Agent 工具选择、补参及高风险操作确认。

离线校验不会调用模型，也不会伪造准确率、耗时或 Token：

```bash
python evals/run_evals.py
```

对已有预测文件评分：

```bash
python evals/run_evals.py --predictions path/to/predictions.jsonl
```

真实集成评测必须由操作者显式授权并主动执行；它会向已运行的 SmartMall 服务发送请求，可能产生模型费用：

```bash
export SMARTMALL_EVAL_ALLOW_REAL_MODEL=YES
export SMARTMALL_EVAL_BASE_URL=https://mall.example.com
export SMARTMALL_EVAL_TOKEN=your-short-lived-test-token
python evals/run_real_model.py
```

Token 只从环境变量读取，脚本不会把它写入预测或报告。请使用专用测试账号和短时令牌。
