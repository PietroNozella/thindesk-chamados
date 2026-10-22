param()

$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
Push-Location $raizProjeto
try {
    & docker compose -f docker-compose.test.yml up --build --abort-on-container-exit --exit-code-from testes
    $codigoTestes = $LASTEXITCODE
}
finally {
    & docker compose -f docker-compose.test.yml down
    $codigoLimpeza = $LASTEXITCODE
    Pop-Location
}
if ($codigoTestes -ne 0) {
    throw "Os testes integrados falharam (código $codigoTestes)."
}
if ($codigoLimpeza -ne 0) {
    throw "Os testes passaram, mas a limpeza dos contêineres falhou."
}
Write-Output 'Testes integrados aprovados. Relatórios disponíveis em target/surefire-reports.'
