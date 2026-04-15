param(
	[string] $JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot",
	[int] $StartupTimeoutSeconds = 120
)

$ErrorActionPreference = "Stop"

$versions = @("1.21.8", "1.21.9", "1.21.10", "1.21.11")
$runDir = Join-Path (Split-Path -Parent $PSScriptRoot) "run\shared-client"
$logPath = Join-Path $runDir "logs\latest.log"

function Get-AttentionLaunchProcesses {
	Get-CimInstance Win32_Process | Where-Object {
		$_.CommandLine -and $_.Name -match 'java|javaw|cmd|powershell' -and (
			$_.CommandLine -match 'attention-gradle-root' -or
			$_.CommandLine -match 'run-client-1\.21' -or
			$_.CommandLine -match 'fabric-loader' -or
			$_.CommandLine -match 'run/shared-client' -or
			$_.CommandLine -match 'attention!'
		)
	}
}

function Stop-ProcessesById {
	param([int[]] $ProcessIds)

	foreach ($processId in ($ProcessIds | Sort-Object -Descending -Unique)) {
		$process = Get-Process -Id $processId -ErrorAction SilentlyContinue
		if ($process) {
			Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
		}
	}
}

function Wait-ForClientLaunch {
	param(
		[string] $Version,
		[string] $OutPath,
		[string] $ErrPath,
		[int] $TimeoutSeconds
	)

	$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

	while ((Get-Date) -lt $deadline) {
		$outText = if (Test-Path $OutPath) { (Get-Content $OutPath -Raw -ErrorAction SilentlyContinue) } else { "" }
		$errText = if (Test-Path $ErrPath) { (Get-Content $ErrPath -Raw -ErrorAction SilentlyContinue) } else { "" }
		$logText = if (Test-Path $logPath) { (Get-Content $logPath -Raw -ErrorAction SilentlyContinue) } else { "" }
		$combinedText = "$outText`n$logText"

		if ($outText -match 'BUILD FAILED' -or $errText -match 'BUILD FAILED') {
			return [pscustomobject]@{
				Version = $Version
				Success = $false
				Reason = "Gradle launch failed"
			}
		}

		if ($combinedText -match 'Sound engine started' -or
				$combinedText -match 'Reloading ResourceManager:' -or
				$combinedText -match 'Loaded \d+ recipes' -or
				$combinedText -match 'joined the game') {
			return [pscustomobject]@{
				Version = $Version
				Success = $true
				Reason = "startup reached client rendering/resource initialization"
			}
		}

		if ($combinedText -match 'Crash report saved|This crash report has been saved|Encountered an unexpected exception') {
			return [pscustomobject]@{
				Version = $Version
				Success = $false
				Reason = "startup log contains a crash signature"
			}
		}

		Start-Sleep -Seconds 2
	}

	return [pscustomobject]@{
		Version = $Version
		Success = $false
		Reason = "timed out waiting for client window"
	}
}

$results = foreach ($version in $versions) {
	if (Test-Path $logPath) {
		Clear-Content -Path $logPath -ErrorAction SilentlyContinue
	}

	$outPath = Join-Path $env:TEMP "attention-launch-$($version)-out.txt"
	$errPath = Join-Path $env:TEMP "attention-launch-$($version)-err.txt"
	if (Test-Path $outPath) { Remove-Item $outPath -Force }
	if (Test-Path $errPath) { Remove-Item $errPath -Force }

	$scriptPath = Join-Path $PSScriptRoot "run-client-$version.ps1"
	$baselineIds = @(Get-AttentionLaunchProcesses | ForEach-Object { [int] $_.ProcessId })
	$process = Start-Process -FilePath "powershell.exe" `
		-ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "`"$scriptPath`"", "-JavaHome", "`"$JavaHome`"") `
		-RedirectStandardOutput $outPath `
		-RedirectStandardError $errPath `
		-PassThru

	try {
		Wait-ForClientLaunch -Version $version -OutPath $outPath -ErrPath $errPath -TimeoutSeconds $StartupTimeoutSeconds
	} finally {
		$newIds = @(Get-AttentionLaunchProcesses |
				Where-Object { $baselineIds -notcontains [int] $_.ProcessId } |
				ForEach-Object { [int] $_.ProcessId })
		if ($process) {
			$newIds += $process.Id
		}
		Stop-ProcessesById -ProcessIds $newIds
		Start-Sleep -Seconds 2
	}
}

$results | Format-Table -AutoSize

if ($results.Success -contains $false) {
	throw "One or more client launch verifications failed."
}
