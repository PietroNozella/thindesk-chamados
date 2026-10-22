param()

$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
$arquivoEnv = Join-Path $raizProjeto '.env'
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Nova-SenhaLocal {
    $bytes = New-Object byte[] 24
    $gerador = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $gerador.GetBytes($bytes)
        return [Convert]::ToBase64String($bytes)
    }
    finally {
        $gerador.Dispose()
    }
}

if (-not (Test-Path -LiteralPath $arquivoEnv)) {
    $modelo = [System.IO.File]::ReadAllText((Join-Path $raizProjeto '.env.example'))
    $modelo = $modelo.Replace('THINDESK_PASSWORD=', ('THINDESK_PASSWORD=' + (Nova-SenhaLocal)))
    $modelo = $modelo.Replace('THINDESK_TECNICO_PASSWORD=', ('THINDESK_TECNICO_PASSWORD=' + (Nova-SenhaLocal)))
    [System.IO.File]::WriteAllText($arquivoEnv, $modelo, $utf8)
}

# Use a mesma interpretação de aspas e variáveis que será aplicada ao contêiner.
$jsonCompose = & docker compose --project-directory $raizProjeto --file (Join-Path $raizProjeto 'docker-compose.yml') config --format json
if ($LASTEXITCODE -ne 0) {
    throw 'Nao foi possivel ler a configuracao do Docker Compose. Confira o .env.'
}
$aplicacao = (($jsonCompose -join [Environment]::NewLine) | ConvertFrom-Json).services.app.environment
$configuracao = @{}
foreach ($propriedade in $aplicacao.PSObject.Properties) {
    $configuracao[$propriedade.Name] = $propriedade.Value
}
foreach ($chave in @('THINDESK_USERNAME', 'THINDESK_PASSWORD', 'THINDESK_TECNICO_USERNAME', 'THINDESK_TECNICO_PASSWORD')) {
    if ([string]::IsNullOrWhiteSpace($configuracao[$chave])) {
        throw "Preencha $chave no arquivo .env existente. Nenhuma senha foi sobrescrita."
    }
}

$ambiente = Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $raizProjeto 'postman/ThinDesk.postman_environment.json') | ConvertFrom-Json
$ambiente.name = 'ThinDesk - Local'
$valores = @{
    usuario = $configuracao['THINDESK_USERNAME']
    usuarioId = $configuracao['THINDESK_USERNAME']
    outroUsuarioId = ($configuracao['THINDESK_USERNAME'] + '-outro')
    senha = $configuracao['THINDESK_PASSWORD']
    tecnico = $configuracao['THINDESK_TECNICO_USERNAME']
    senhaTecnico = $configuracao['THINDESK_TECNICO_PASSWORD']
}
foreach ($variavel in $ambiente.values) {
    if ($valores.ContainsKey($variavel.key)) {
        $variavel.value = $valores[$variavel.key]
    }
}
$destino = Join-Path $raizProjeto 'postman/ThinDesk.local.postman_environment.json'
[System.IO.File]::WriteAllText($destino, ($ambiente | ConvertTo-Json -Depth 10), $utf8)
Write-Output 'Ambiente preparado. Importe a colecao e postman/ThinDesk.local.postman_environment.json no Postman.'
Write-Output 'As senhas permanecem nos arquivos locais ignorados pelo Git.'
