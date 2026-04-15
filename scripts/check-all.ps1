param(
	[string] $JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
)

$ErrorActionPreference = "Stop"

if (!(Test-Path $JavaHome)) {
	throw "JDK not found at '$JavaHome'. Pass -JavaHome with a Java 21+ JDK path."
}

$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$env:PATH"

$root = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $root "gradlew.bat"
$versions = @("1.21.8", "1.21.9", "1.21.10", "1.21.11")
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

& $gradle clean test build --no-daemon

foreach ($version in $versions) {
	& $gradle build `
		"-Pminecraft_version=$version" `
		"-Pyarn_mappings=$($yarnMappings[$version])" `
		"-Pfabric_api_version=$($fabricApiVersions[$version])" `
		"--no-daemon"
}

