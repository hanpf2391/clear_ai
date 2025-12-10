# ClearAI Admin Scanner
# This script will request admin privileges automatically

Write-Host "ClearAI Admin Mode Scanner" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan
Write-Host ""

# Check if running as administrator
if (-NOT ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "Requesting administrator privileges..." -ForegroundColor Yellow
    Write-Host ""

    # Relaunch as administrator
    Start-Process PowerShell -Verb RunAs "-File `"$PSCommandPath`""
    exit
}

Write-Host "Running with administrator privileges!" -ForegroundColor Green
Write-Host ""

Set-Location $PSScriptRoot
Write-Host "Current directory: $(Get-Location)" -ForegroundColor Gray
Write-Host ""

Write-Host "Starting ClearAI with full system access..." -ForegroundColor Yellow
try {
    java -cp "classes" com.hanpf.clearai.clustering.SimpleClusteringDemo
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")