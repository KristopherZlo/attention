param(
	[ValidateSet("1.21.8", "1.21.9", "1.21.10", "1.21.11")]
	[string] $McVersion = "1.21.11",
	[string] $JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
)

$ErrorActionPreference = "Stop"

$fabricApiVersions = @{
	"1.21.8" = "0.136.1+1.21.8"
	"1.21.9" = "0.134.1+1.21.9"
	"1.21.10" = "0.138.4+1.21.10"
	"1.21.11" = "0.141.3+1.21.11"
}

$yarnMappings = @{
	"1.21.8" = "1.21.8+build.1"
	"1.21.9" = "1.21.9+build.1"
	"1.21.10" = "1.21.10+build.3"
	"1.21.11" = "1.21.11+build.4"
}

if (!(Test-Path $JavaHome)) {
	throw "JDK not found at '$JavaHome'. Pass -JavaHome with a Java 21+ JDK path."
}

$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$env:PATH"

$root = Split-Path -Parent $PSScriptRoot
$safeRoot = Join-Path $env:TEMP "attention-gradle-root"
$gradleArgs = @(
	"runClient",
	"-Pminecraft_version=$McVersion",
	"-Pyarn_mappings=$($yarnMappings[$McVersion])",
	"-Pfabric_api_version=$($fabricApiVersions[$McVersion])",
	"--no-daemon"
)

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

	$safeRunRoot = Join-Path $safeRoot "run"
	$safeSharedRunDir = Join-Path $safeRunRoot "shared-client"
	$realSharedRunDir = Join-Path $root "run\shared-client"
	New-Item -ItemType Directory -Force -Path $safeRunRoot | Out-Null
	New-Item -ItemType Directory -Force -Path $realSharedRunDir | Out-Null
	if (Test-Path $safeSharedRunDir) {
		$item = Get-Item $safeSharedRunDir -Force
		if ($item.LinkType) {
			[System.IO.Directory]::Delete($safeSharedRunDir)
		} else {
			Remove-Item $safeSharedRunDir -Recurse -Force
		}
	}
	New-Item -ItemType Junction -Path $safeSharedRunDir -Target $realSharedRunDir | Out-Null
} else {
	$safeRoot = $root
}

$gradle = Join-Path $safeRoot "gradlew.bat"

Push-Location $safeRoot
try {
	& $gradle @gradleArgs
} finally {
	Pop-Location
}
