param(
    [ValidateSet('Install', 'Start', 'Run', 'Stop', 'Status', 'Uninstall')]
    [string]$Action = 'Install'
)

$ErrorActionPreference = 'Stop'
$taskName = 'Wayki SOS Backend'
$projectRoot = Split-Path -Parent $PSScriptRoot
$serverRoot = Join-Path $projectRoot 'sos-server'
$distributionRoot = Join-Path $serverRoot 'build\install\sos-server'
$stateRoot = Join-Path $env:LOCALAPPDATA 'Waykiu\SosBackend'
$configPath = Join-Path $stateRoot 'credentials.json'
$pidPath = Join-Path $stateRoot 'server.pid'
$stdoutPath = Join-Path $stateRoot 'server.out.log'
$stderrPath = Join-Path $stateRoot 'server.err.log'
$taskPrincipal = "$env:USERDOMAIN\$env:USERNAME"

function Get-SecureValue([string]$EncryptedValue) {
    $secureValue = ConvertTo-SecureString $EncryptedValue
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Get-BackendProcess {
    if (-not (Test-Path $pidPath)) {
        return $null
    }
    $savedPid = 0
    if (-not [int]::TryParse((Get-Content $pidPath -Raw), [ref]$savedPid)) {
        Remove-Item $pidPath -Force
        return $null
    }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $savedPid" -ErrorAction SilentlyContinue
    if ($process -and $process.CommandLine -like "*$distributionRoot*") {
        return $process
    }
    Remove-Item $pidPath -Force
    return $null
}

function Start-Backend([switch]$WaitForExit) {
    $existing = Get-BackendProcess
    if ($existing) {
        Write-Output 'El backend SOS ya está ejecutándose.'
        return
    }

    $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($listener) {
        $owner = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)" -ErrorAction SilentlyContinue
        if ($owner -and $owner.CommandLine -like "*$distributionRoot*") {
            Set-Content -Path $pidPath -Value $owner.ProcessId
            Write-Output 'El backend SOS ya está escuchando en el puerto 8080.'
            return
        }
        throw "El puerto 8080 ya está ocupado por otro proceso (PID $($listener.OwningProcess)). No se detuvo ese proceso."
    }

    if (-not (Test-Path $distributionRoot)) {
        throw 'Falta la distribución del backend. Ejecuta la acción Install para compilarla.'
    }
    if (-not (Test-Path $configPath)) {
        throw 'Falta la configuración local protegida. Ejecuta la acción Install para configurarla.'
    }

    $config = Get-Content $configPath -Raw | ConvertFrom-Json
    $env:SOS_DEVICE_TOKENS = "$($config.deviceId)=$(Get-SecureValue $config.encryptedDeviceToken)"
    $env:SOS_MOBILE_LOCATION_TOKEN = Get-SecureValue $config.encryptedMobileToken
    $env:SOS_SERVER_PORT = '8080'
    $env:SOS_SERVER_BIND = '0.0.0.0'

    $java = (Get-Command java.exe -ErrorAction Stop).Source
    $arguments = "-cp `"$distributionRoot\lib\*`" com.gowayki.nesh.sos.server.MainKt"
    $process = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $serverRoot `
        -WindowStyle Hidden -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath -PassThru
    Set-Content -Path $pidPath -Value $process.Id

    $deadline = (Get-Date).AddSeconds(20)
    do {
        Start-Sleep -Milliseconds 500
        $process.Refresh()
        if ($process.HasExited) {
            Remove-Item $pidPath -Force -ErrorAction SilentlyContinue
            throw "El backend terminó durante el arranque. Revisa los logs locales: $stderrPath"
        }
        $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
            Select-Object -First 1
    } while (-not $listener -and (Get-Date) -lt $deadline)

    if (-not $listener) {
        throw "El backend no abrió el puerto 8080. Revisa los logs locales: $stderrPath"
    }
    Write-Output 'Backend SOS iniciado en el puerto 8080.'
    if ($WaitForExit) {
        $process.WaitForExit()
        Remove-Item $pidPath -Force -ErrorAction SilentlyContinue
        if ($process.ExitCode -ne 0) {
            throw "El backend terminó con código $($process.ExitCode). Revisa los logs locales: $stderrPath"
        }
    }
}

function Stop-Backend {
    $process = Get-BackendProcess
    if (-not $process) {
        Write-Output 'El backend SOS no está ejecutándose.'
        return
    }
    Stop-Process -Id $process.ProcessId
    Remove-Item $pidPath -Force -ErrorAction SilentlyContinue
    Write-Output 'Backend SOS detenido.'
}

switch ($Action) {
    'Install' {
        $secretsPath = Join-Path $serverRoot '..\hardware\esp32_sos_button\secrets.h'
        if (-not (Test-Path $secretsPath)) {
            throw 'Crea hardware\esp32_sos_button\secrets.h con el ID y token del dispositivo antes de instalar el backend.'
        }
        $secrets = Get-Content $secretsPath -Raw
        $deviceIdMatch = [regex]::Match($secrets, '#define\s+SOS_DEVICE_ID\s+"([^"]+)"')
        $deviceTokenMatch = [regex]::Match($secrets, '#define\s+SOS_DEVICE_TOKEN\s+"([^"]+)"')
        if (-not $deviceIdMatch.Success -or -not $deviceTokenMatch.Success -or
            $deviceTokenMatch.Groups[1].Value.Length -lt 32 -or
            $deviceTokenMatch.Groups[1].Value -like 'REEMPLAZAR*') {
            throw 'Revisa SOS_DEVICE_ID y SOS_DEVICE_TOKEN en hardware\esp32_sos_button\secrets.h.'
        }

        $mobileToken = Read-Host 'Pega el mismo token móvil que usarás en la pantalla SOS' -AsSecureString
        $mobilePointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($mobileToken)
        try {
            $mobilePlain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($mobilePointer)
            if ($mobilePlain.Length -lt 32) {
                throw 'El token móvil debe tener al menos 32 caracteres.'
            }
            $encryptedMobile = ConvertFrom-SecureString (ConvertTo-SecureString $mobilePlain -AsPlainText -Force)
        }
        finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($mobilePointer)
        }
        $encryptedDevice = ConvertFrom-SecureString (ConvertTo-SecureString $deviceTokenMatch.Groups[1].Value -AsPlainText -Force)

        New-Item -ItemType Directory -Path $stateRoot -Force | Out-Null
        [pscustomobject]@{
            deviceId = $deviceIdMatch.Groups[1].Value
            encryptedDeviceToken = $encryptedDevice
            encryptedMobileToken = $encryptedMobile
        } | ConvertTo-Json | Set-Content -Path $configPath

        Push-Location $projectRoot
        try {
            & (Join-Path $projectRoot 'gradlew.bat') ':sos-server:installDist' '--no-daemon'
            if ($LASTEXITCODE -ne 0) {
                throw 'No se pudo compilar la distribución del backend SOS.'
            }
        }
        finally {
            Pop-Location
        }

        $scriptPath = $MyInvocation.MyCommand.Path
        $powershellPath = Join-Path $PSHOME 'powershell.exe'
        $taskAction = New-ScheduledTaskAction -Execute $powershellPath `
            -Argument "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$scriptPath`" -Action Run"
        $trigger = New-ScheduledTaskTrigger -AtLogOn -User $taskPrincipal
        $principal = New-ScheduledTaskPrincipal -UserId $taskPrincipal -LogonType Interactive -RunLevel Limited
        $settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -ExecutionTimeLimit ([TimeSpan]::Zero)
        Register-ScheduledTask -TaskName $taskName -Action $taskAction -Trigger $trigger `
            -Principal $principal -Settings $settings -Description 'Inicia el backend SOS local de Waykiu al iniciar sesión.' `
            -Force | Out-Null
        Start-Backend
        Write-Output "Inicio automático configurado para el usuario actual. Token protegido localmente en $stateRoot."
    }
    'Start' {
        Start-Backend
    }
    'Run' {
        Start-Backend -WaitForExit
    }
    'Stop' {
        Stop-Backend
    }
    'Status' {
        $scheduledTask = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
        $process = Get-BackendProcess
        if ($process) {
            Write-Output 'Backend SOS: ejecutándose.'
        }
        else {
            Write-Output 'Backend SOS: detenido.'
        }
        if ($scheduledTask) {
            Write-Output 'Inicio automático: configurado.'
        }
        else {
            Write-Output 'Inicio automático: no configurado.'
        }
    }
    'Uninstall' {
        Stop-Backend
        Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue
        Remove-Item $configPath -Force -ErrorAction SilentlyContinue
        Write-Output 'Inicio automático eliminado y credenciales locales protegidas borradas.'
    }
}
