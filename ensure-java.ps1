param(
    [switch] $ForceLocalDownload
)

$ErrorActionPreference = 'Stop'

$minimumJavaMajor = 21
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtimeRoot = Join-Path $repoRoot '.runtime'
$javaRoot = Join-Path $runtimeRoot 'java'
$downloadsRoot = Join-Path $runtimeRoot 'downloads'
$localJdk = Join-Path $javaRoot 'temurin-21'
$extractRoot = Join-Path $javaRoot '.extract-temurin-21'
$downloadFile = Join-Path $downloadsRoot 'temurin-21-jdk-windows-x64.zip'
$adoptiumUrl = 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse'

function Write-Status([string] $message) {
    [Console]::Error.WriteLine($message)
}

function Get-JavaMajorVersion([string] $javaExecutable) {
    $previousErrorAction = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $versionText = (& $javaExecutable -version 2>&1 | Select-Object -First 1) -join ' '
    $ErrorActionPreference = $previousErrorAction

    if ($versionText -notmatch 'version "([^"]+)"') {
        return 0
    }

    $parts = $Matches[1].Split('.')
    if ($parts[0] -eq '1' -and $parts.Length -gt 1) {
        return [int] $parts[1]
    }

    return [int] $parts[0]
}

function Test-JdkHome([string] $jdkHome) {
    if (-not $jdkHome) {
        return $false
    }

    $javaExecutable = Join-Path $jdkHome 'bin\java.exe'
    $javacExecutable = Join-Path $jdkHome 'bin\javac.exe'
    if (-not (Test-Path -LiteralPath $javaExecutable) -or -not (Test-Path -LiteralPath $javacExecutable)) {
        return $false
    }

    return (Get-JavaMajorVersion $javaExecutable) -ge $minimumJavaMajor
}

function Resolve-SystemJdkHome {
    $candidates = @()

    if ($env:JAVA_HOME) {
        $candidates += $env:JAVA_HOME
    }

    $javacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($javacCommand) {
        $candidates += (Split-Path -Parent (Split-Path -Parent $javacCommand.Source))
    }

    foreach ($jdkParent in @(
        (Join-Path $env:ProgramFiles 'Java'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
        (Join-Path $env:ProgramFiles 'Microsoft')
    )) {
        if (Test-Path -LiteralPath $jdkParent) {
            $candidates += Get-ChildItem -LiteralPath $jdkParent -Directory -ErrorAction SilentlyContinue |
                Select-Object -ExpandProperty FullName
        }
    }

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if (Test-JdkHome $candidate) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    return $null
}

function Assert-WorkspacePath([string] $path) {
    $repoPath = [System.IO.Path]::GetFullPath($repoRoot)
    $fullPath = [System.IO.Path]::GetFullPath($path)
    if (-not $fullPath.StartsWith($repoPath, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify path outside workspace: $fullPath"
    }
}

if (-not $ForceLocalDownload) {
    $systemJdk = Resolve-SystemJdkHome
    if ($systemJdk) {
        Write-Output $systemJdk
        exit 0
    }
}

if (Test-JdkHome $localJdk) {
    Write-Output (Resolve-Path -LiteralPath $localJdk).Path
    exit 0
}

Write-Status 'JDK 21 or newer was not found. Downloading local Eclipse Temurin JDK 21...'
New-Item -ItemType Directory -Force -Path $downloadsRoot, $javaRoot | Out-Null

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Invoke-WebRequest -Uri $adoptiumUrl -OutFile $downloadFile

foreach ($path in @($extractRoot, $localJdk)) {
    Assert-WorkspacePath $path
    if (Test-Path -LiteralPath $path) {
        Remove-Item -LiteralPath $path -Recurse -Force
    }
}

Expand-Archive -LiteralPath $downloadFile -DestinationPath $extractRoot -Force
$extractedJdk = Get-ChildItem -LiteralPath $extractRoot -Directory |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } |
    Select-Object -First 1

if (-not $extractedJdk) {
    throw 'The downloaded Java archive did not contain a JDK.'
}

Move-Item -LiteralPath $extractedJdk.FullName -Destination $localJdk
Remove-Item -LiteralPath $extractRoot -Recurse -Force

if (-not (Test-JdkHome $localJdk)) {
    throw 'The local JDK download is not usable.'
}

Write-Status "Local JDK is ready at $localJdk."
Write-Output (Resolve-Path -LiteralPath $localJdk).Path
