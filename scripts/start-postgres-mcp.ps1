$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = Split-Path -Parent $PSScriptRoot
$localConfig = Join-Path $projectRoot "backend\smartmall-application\src\main\resources\application-local.yml"
$uv = Join-Path $env:USERPROFILE ".local\bin\uv.exe"
$python = Join-Path $env:APPDATA "uv\python\cpython-3.12.14-windows-x86_64-none\python.exe"

if (-not (Test-Path -LiteralPath $localConfig)) {
    throw "Missing local database configuration: $localConfig"
}
if (-not (Test-Path -LiteralPath $uv)) {
    throw "uv is not installed at $uv"
}
if (-not (Test-Path -LiteralPath $python)) {
    throw "Managed Python 3.12 is not installed at $python"
}

$yaml = Get-Content -LiteralPath $localConfig -Raw

function Get-YamlScalar([string]$name) {
    $pattern = '(?m)^\s*{0}:\s*"?([^"\r\n]+)"?\s*$' -f [regex]::Escape($name)
    $match = [regex]::Match($yaml, $pattern)
    if (-not $match.Success) {
        throw "Missing '$name' in application-local.yml"
    }
    return $match.Groups[1].Value.Trim()
}

$jdbcUrl = Get-YamlScalar "url"
$username = Get-YamlScalar "username"
$password = Get-YamlScalar "password"

if (-not $jdbcUrl.StartsWith("jdbc:postgresql://")) {
    throw "Only PostgreSQL JDBC URLs are supported"
}

$databaseAddress = [Uri]($jdbcUrl.Substring(5))
$encodedUsername = [Uri]::EscapeDataString($username)
$encodedPassword = [Uri]::EscapeDataString($password)
$authority = if ($databaseAddress.IsDefaultPort) {
    $databaseAddress.Host
} else {
    "$($databaseAddress.Host):$($databaseAddress.Port)"
}

# 连接串只存在于当前进程及其子进程的环境变量中，不写入 Codex 全局配置或控制台。
[Environment]::SetEnvironmentVariable("DATABASE_URI", ("postgresql://" + $encodedUsername + ":" + $encodedPassword + "@" + $authority + $databaseAddress.PathAndQuery), [EnvironmentVariableTarget]::Process)
if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable("DATABASE_URI", [EnvironmentVariableTarget]::Process))) {
    throw "DATABASE_URI was not set in the PowerShell process"
}
& $uv run --python $python --with postgres-mcp --with "mcp<2" `
    python -c "from postgres_mcp import main; main()" --access-mode=restricted
exit $LASTEXITCODE
