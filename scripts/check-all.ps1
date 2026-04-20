param(
	[string] $JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot"
)

$ErrorActionPreference = "Stop"

if (!(Test-Path $JavaHome)) {
	throw "JDK not found at '$JavaHome'. Pass -JavaHome with a Java 21+ JDK path."
}

$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$env:PATH"

$root = Split-Path -Parent $PSScriptRoot
$safeRoot = Join-Path $env:TEMP "attention-gradle-root"

if ($root.Contains("!")) {
	if (Test-Path $safeRoot) {
		$item = Get-Item $safeRoot -Force
		if ($item.LinkType) {
			[System.IO.Directory]::Delete($safeRoot)
		}
	}

	New-Item -ItemType Directory -Force -Path $safeRoot | Out-Null
	& robocopy $root $safeRoot /MIR /XD .git .gradle build run /NFL /NDL /NJH /NJS /NP | Out-Null
	if ($LASTEXITCODE -gt 7) {
		throw "Failed to prepare safe Gradle workspace. Robocopy exit code: $LASTEXITCODE"
	}
} else {
	$safeRoot = $root
}

$gradle = Join-Path $safeRoot "gradlew.bat"
Push-Location $safeRoot
try {
	& $gradle clean test build --no-daemon
} finally {
	Pop-Location
}

if ($safeRoot -ne $root) {
	$sourceLibs = Join-Path $safeRoot "build\libs"
	$targetLibs = Join-Path $root "build\libs"

	if (Test-Path $sourceLibs) {
		New-Item -ItemType Directory -Force -Path $targetLibs | Out-Null
		Copy-Item -Path (Join-Path $sourceLibs "*") -Destination $targetLibs -Force
	}
}

& (Join-Path $root "scripts\verify-launches.ps1") -JavaHome $JavaHome
